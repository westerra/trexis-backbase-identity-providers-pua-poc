package net.trexis.experts.identity.authenticator;

import com.backbase.identity.authenticators.otp.CommunicationService;
import com.backbase.identity.authenticators.otp.SecretProvider;
import com.backbase.identity.authenticators.otp.exception.OtpDeliveryException;
import com.backbase.identity.authenticators.otp.model.IdentityTotpConfig;
import com.backbase.identity.authenticators.otp.model.OtpChoice;
import com.backbase.identity.authenticators.otp.model.PostBatchesRequestBody;
import com.backbase.identity.m10y.models.Tenant;
import com.backbase.identity.m10y.providers.TenantResolverProvider;
import com.backbase.identity.spi.store.OtpStoreProvider;
import com.backbase.identity.util.IdentityTotpUtil;
import com.backbase.identity.util.LimitedActionMap;

import java.util.*;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import javax.ws.rs.core.Response;

import freemarker.template.TemplateException;
import freemarker.template.Template;
import freemarker.template.Version;
import freemarker.template.Configuration;
import net.trexis.experts.identity.configuration.Constants;
import net.trexis.experts.identity.model.MfaAttributeEnum;
import net.trexis.experts.identity.service.OtpChannelService;
import net.trexis.experts.identity.model.MfaEmailConfiguration;

import net.trexis.experts.identity.util.ChannelSelectorUtil;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.provider.ProviderConfigProperty;

import static java.lang.Integer.parseInt;
import static java.time.ZonedDateTime.now;
import static java.util.Map.Entry.comparingByKey;
import static net.trexis.experts.identity.configuration.Constants.*;
import static org.keycloak.authentication.AuthenticationFlowError.INVALID_CREDENTIALS;

public class OtpAuthenticator implements Authenticator {

    private static final String CHANNEL_TYPE = "channelType";
    private static final String CHANNEL_NUMBER = "channelNumber";
    private static final String OPT_OUT_ENABLED = "OPT_OUT_ENABLED";
    private static final Logger log = Logger.getLogger(OtpAuthenticator.class);
    private static final String MFA_OTP_TEMPLATE = "mfa-otp.ftl";

    private final KeycloakSession session;
    private final OtpChannelService otpChannelService;
    private final SecretProvider secretProvider;
    private final CommunicationService communicationService;
    private final OtpTemplateProviderImplFactory otpTemplateProviderImplFactory;
    private final OtpStoreProvider otpStoreProvider;
    private final MfaEmailConfiguration mfaEmailConfiguration;
    private TimeBasedOTP timeBasedOtp;
    private org.infinispan.Cache<String, LimitedActionMap> infinispanCache;
    private IdentityTotpConfig totpConfig;

    OtpAuthenticator(KeycloakSession session, List<ProviderConfigProperty> configProperties,
            OtpChannelService otpChannelService, SecretProvider secretProvider,
            CommunicationService communicationService, OtpTemplateProviderImplFactory otpTemplateProviderImplFactory,
            OtpStoreProvider otpStoreProvider,MfaEmailConfiguration mfaEmailConfiguration) {
        this.session = session;
        this.otpChannelService = otpChannelService;
        this.secretProvider = secretProvider;
        this.communicationService = communicationService;
        this.otpTemplateProviderImplFactory = otpTemplateProviderImplFactory;
        this.otpStoreProvider = otpStoreProvider;
        this.mfaEmailConfiguration = mfaEmailConfiguration;

        //TODO: Resent limit testing
        var algorithmDefault = configProperties.stream().filter(p -> p.getName() == "trexis.otp.algorithm").findFirst().orElseThrow().getDefaultValue().toString();
        int digitsDefault = Integer.valueOf(configProperties.stream().filter(p -> p.getName() == "trexis.otp.digits").findFirst().orElseThrow().getDefaultValue().toString());
        int lookAheadWindowDefault = Integer.valueOf(configProperties.stream().filter(p -> p.getName() == "trexis.otp.lookAheadWindow").findFirst().orElseThrow().getDefaultValue().toString());
        int otpPeriodDefault = Integer.valueOf(configProperties.stream().filter(p -> p.getName() == "trexis.otp.otpPeriod").findFirst().orElseThrow().getDefaultValue().toString());
        int otpResendPeriodDefault = Integer.valueOf(configProperties.stream().filter(p -> p.getName() == "trexis.otp.otpResendPeriod").findFirst().orElseThrow().getDefaultValue().toString());
        int otpResendLimitDefault = Integer.valueOf(configProperties.stream().filter(p -> p.getName() == "trexis.otp.otpResendLimit").findFirst().orElseThrow().getDefaultValue().toString());

        this.totpConfig = new IdentityTotpConfig(algorithmDefault, digitsDefault, lookAheadWindowDefault, otpPeriodDefault, otpResendPeriodDefault, otpResendLimitDefault);
        this.timeBasedOtp = new IdentityTotpUtil().getTimeBasedOtp(totpConfig);
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        log.warn("context config: " + context.getAuthenticatorConfig());

        if (mfaIsRequired(context.getUser())) {
            configureTotp(context);
            log.debugv("User {0} is required to do MFA", context.getUser().getUsername());
            String otpChoiceAddressId = context.getAuthenticationSession().getAuthNote(OTP_CHOICE_ADDRESS_ID);
            log.warn("otpChoiceAddressId: " + otpChoiceAddressId);
            verifyOtpMethodAndSendOtp(context, otpChoiceAddressId);
            Optional<OtpChoice> selectedOtpChoiceOptional = findMatchingOtpChoice(context, otpChoiceAddressId);
            String channelNumber = selectedOtpChoiceOptional.get().getAddress();
            var input = context.form()
                .setAttribute(CHANNEL_NUMBER, maskChannelNumber(channelNumber)) //channel number
                .setAttribute(CHANNEL_TYPE, selectedOtpChoiceOptional.get().getChannel()) //channel type
                .createForm(MFA_OTP_TEMPLATE);
            context.challenge(input);
        } else {
            log.debugv("User {0} is NOT required to do MFA", context.getUser().getUsername());
            context.success();
        }
    }

    private String maskChannelNumber(String channelNumber) {
        var length = channelNumber.length();
        return channelNumber.substring(0, length - 4).replaceAll("[0-9]", "*")+channelNumber.substring(length - 4,length);
    }

    private String generateOtp(AuthenticationFlowContext authenticationFlowContext) {
        String communicationServiceSecret = secretProvider.getCommunicationServiceSecret(authenticationFlowContext);
        return timeBasedOtp.generateTOTP(communicationServiceSecret);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        var formData = context.getHttpRequest().getDecodedFormParameters();
        String choiceAddressId = context.getAuthenticationSession()
                .getAuthNote(OTP_CHOICE_ADDRESS_ID);
        Optional<OtpChoice> selectedOtpChoiceOptional = findMatchingOtpChoice(context, choiceAddressId);

        if (formData.containsKey("cancel")) {
            log.info("form data included cancel, resetFlow called.");
            context.resetFlow();
            return;
        }

        OtpChoice otpChoice = selectedOtpChoiceOptional.get();
        if (formData.containsKey("resend")) {
            verifyOtpMethodAndSendOtp(context, choiceAddressId);
            Response challenge = challengeWithInfo(context, "OTP resent.",maskChannelNumber(otpChoice.getAddress()),otpChoice.getChannel());
            context.challenge(challenge);
            return;
        }

        if (!formData.containsKey("totp") || formData.getFirst("totp") == null) {
            issueFailureChallenge(context, "OTP value is missing.", maskChannelNumber(otpChoice.getAddress()),otpChoice.getChannel());
        }

        String userInput = formData.getFirst("totp");
        verifyOtp(context, choiceAddressId, userInput);
    }

    private void issueFailureChallenge(AuthenticationFlowContext context, String message, String channelNumber, String channelType) {
        Response challenge = context.form()
                .setAttribute(CHANNEL_NUMBER, maskChannelNumber(channelNumber)) //channel number
                .setAttribute(CHANNEL_TYPE, channelType) //channel type
                .setError(message)
                .createForm(MFA_OTP_TEMPLATE);
        context.failureChallenge(INVALID_CREDENTIALS, challenge);
    }

    private void issueFailureChallengeWithErrorMessage(AuthenticationFlowContext context, String message) {
        Response challenge = context.form()
                .setError(message)
                .createForm(MFA_OTP_TEMPLATE);
        context.failureChallenge(INVALID_CREDENTIALS, challenge);
    }

    protected Response challengeWithInfo(AuthenticationFlowContext context, String infoMessage, String channelNumber, String channelType) {
        return context.form()
            .setAttribute(CHANNEL_NUMBER,channelNumber)
            .setAttribute(CHANNEL_TYPE, channelType)
            .setInfo(infoMessage)
        .createForm(MFA_OTP_TEMPLATE);
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
        return true;
    }

    private boolean mfaIsRequired(UserModel userModel) {
        if(MfaAttributeEnum.TRUE.getValue().equalsIgnoreCase(userModel.getFirstAttribute(Constants.USER_ATTRIBUTE_MFA_REQUIRED)) ||
                MfaAttributeEnum.ALWAYS_TRUE.getValue().equalsIgnoreCase(userModel.getFirstAttribute(Constants.USER_ATTRIBUTE_MFA_REQUIRED))) {
            return true;
        } else if (MfaAttributeEnum.FALSE.getValue().equalsIgnoreCase(userModel.getFirstAttribute(Constants.USER_ATTRIBUTE_MFA_REQUIRED)) ||
                        MfaAttributeEnum.ALWAYS_FALSE.getValue().equalsIgnoreCase(userModel.getFirstAttribute(Constants.USER_ATTRIBUTE_MFA_REQUIRED))) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void setRequiredActions(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
        // not required
    }

    @Override
    public void close() {
        // not required
    }

    // Configure TOTP with settings from Keycloak
    private void configureTotp(AuthenticationFlowContext context) {
        var config = context.getAuthenticatorConfig();

        if (null == config || null == config.getConfig()) {
            log.warn("TOTP configuration does not exist.");
            return;
        }

        var algorithm = config.getConfig().get("trexis.otp.algorithm");
        int digits = parseInt(config.getConfig().get("trexis.otp.digits"));
        int lookAheadWindow = parseInt(config.getConfig().get("trexis.otp.lookAheadWindow"));
        int otpPeriod = parseInt(config.getConfig().get("trexis.otp.otpPeriod"));
        int otpResendPeriod = parseInt(config.getConfig().get("trexis.otp.otpResendPeriod"));
        int otpResendLimit = parseInt(config.getConfig().get("trexis.otp.otpResendLimit"));

        this.totpConfig = new IdentityTotpConfig(algorithm, digits, lookAheadWindow, otpPeriod, otpResendPeriod, otpResendLimit);
        this.timeBasedOtp = new IdentityTotpUtil().getTimeBasedOtp(totpConfig);
    }

    private void verifyOtpMethodAndSendOtp(AuthenticationFlowContext context, String otpChoiceAddressId) {
        findMatchingOtpChoice(context, otpChoiceAddressId)
                .ifPresentOrElse(present -> {
                    long timeUntilResendAllowed = otpStoreProvider.getTimeUntilResendAllowed(context.getUser(), context.getExecution(), totpConfig.getOtpResendPeriod());
                    Optional<OtpChoice> selectedOtpChoiceOptional = findMatchingOtpChoice(context, otpChoiceAddressId);
                    if (timeUntilResendAllowed == 0L) {
                        String otp = generateOtp(context);
                        cacheOtpSendingRequest(context, otp);
                        boolean otpIsSent = sendOtp(otp, present, context);
                        if (!otpIsSent) {
                            if(!(System.getenv().containsKey(OPT_OUT_ENABLED) && Boolean.parseBoolean(System.getenv(OPT_OUT_ENABLED)))) {
                                throw new OtpDeliveryException("Error occurred while sending OTP via communication service");
                            }
                            // If we allow opt out (text message) in this case we don't throw an error,But we set an error message on OTP page. On .ftl page we can check message.summary == 'Error sending OTP.'
                            issueFailureChallengeWithErrorMessage(context, "Error sending OTP.");
                        }
                    } else {
                        log.infov("OTP sending not allowed, {0} seconds remaining from {1} second configured period",
                                timeUntilResendAllowed, totpConfig.getOtpResendPeriod());
                        issueFailureChallenge(context, "OTP sending not allowed, resume after " + timeUntilResendAllowed, maskChannelNumber(selectedOtpChoiceOptional.get().getAddress()),selectedOtpChoiceOptional.get().getChannel());
                    }
                }, () -> issueFailureChallenge(context, "Error sending OTP.","",""));
    }

    private boolean sendOtp(String otp, OtpChoice otpChoice, AuthenticationFlowContext authenticationFlowContext) {
        var templateProvider = otpTemplateProviderImplFactory.create(authenticationFlowContext.getSession());
        templateProvider.setRealm(authenticationFlowContext.getRealm());

        var user = authenticationFlowContext.getUser();
        templateProvider.setUser(user);

        var postBatchesRequestBody = new PostBatchesRequestBody();
        postBatchesRequestBody.setRecipients(otpChannelService.getRecipients(otpChoice, otp, user));
        postBatchesRequestBody.setContent(templateProvider.getContent(otpChoice, otp, user));

        String realmId = authenticationFlowContext.getRealm().getId();
        String tenantId = null;
        if (session.getProvider(TenantResolverProvider.class) != null) {
            tenantId = session.getProvider(TenantResolverProvider.class)
                    .resolveTenant(realmId)
                    .map(Tenant::getId)
                    .orElse(null);
        }
        return communicationService.send(session, postBatchesRequestBody, tenantId);
    }

    private void verifyOtp(AuthenticationFlowContext context, String otpMethod, String otp) {
        findMatchingOtpChoice(context, otpMethod)
                .ifPresentOrElse(present -> {
                    Optional<OtpChoice> selectedOtpChoiceOptional = findMatchingOtpChoice(context, otpMethod);
                    String secret = secretProvider.getCommunicationServiceSecret(context);
                    if (timeBasedOtp.validateTOTP(otp, secret.getBytes())) {
                        var user = context.getUser();
                        // Sending MFA email
                        if( TRUE.equalsIgnoreCase(mfaEmailConfiguration.getEnabled()) &&
                                (MfaAttributeEnum.TRUE.getValue().equalsIgnoreCase(user.getFirstAttribute(Constants.USER_ATTRIBUTE_MFA_REQUIRED)) ||
                                        MfaAttributeEnum.ALWAYS_TRUE.getValue().equalsIgnoreCase(user.getFirstAttribute(Constants.USER_ATTRIBUTE_MFA_REQUIRED))) ) {
                            if(user.getEmail()!=null) {
                                org.keycloak.email.DefaultEmailSenderProvider senderProvider = new org.keycloak.email.DefaultEmailSenderProvider(session);
                                try {
                                    senderProvider.send(session.getContext().getRealm().getSmtpConfig(), user, mfaEmailConfiguration.getSubject(), null, getHtmlBody());
                                } catch (Exception e) {
                                    log.error("Error sending email to {}", user.getEmail(), e);
                                }
                            } else {
                                log.info("User or User email not found while sending MFA email for userId :" +user.getId());
                            }
                        }
                        if(! ( MfaAttributeEnum.ALWAYS_FALSE.getValue().equalsIgnoreCase(user.getFirstAttribute(Constants.USER_ATTRIBUTE_MFA_REQUIRED)) ||
                                MfaAttributeEnum.ALWAYS_TRUE.getValue().equalsIgnoreCase(user.getFirstAttribute(Constants.USER_ATTRIBUTE_MFA_REQUIRED)) ||
                                MfaAttributeEnum.FALSE.getValue().equalsIgnoreCase(user.getFirstAttribute(Constants.USER_ATTRIBUTE_MFA_REQUIRED)) )) {
                            context.getUser().setSingleAttribute(USER_ATTRIBUTE_MFA_REQUIRED,MfaAttributeEnum.FALSE.getValue());
                        }
                        context.success();
                    } else {
                        issueFailureChallenge(context, "Invalid OTP.", maskChannelNumber(selectedOtpChoiceOptional.get().getAddress()),selectedOtpChoiceOptional.get().getChannel());
                    }
                }, () -> issueFailureChallenge(context, "Unknown device.","",""));
    }

    private String getHtmlBody() throws IOException, TemplateException {
        Map<String,String> systemEnv = System.getenv();
        Map<String, String> templateInput = new HashMap<>();
        templateInput.put("emailSubject", mfaEmailConfiguration.getSubject());
        templateInput.put("emailFooter", mfaEmailConfiguration.getFooter());
        templateInput.put("emailMessage", mfaEmailConfiguration.getMessage());
        Configuration configuration =  new Configuration(new Version("2.3.23"));
        configuration.setClassForTemplateLoading(OtpAuthenticator.class, "/emails");
        configuration.setDefaultEncoding("UTF-8");
        Writer out = new StringWriter();
        Template template = configuration.getTemplate(mfaEmailConfiguration.getTemplate());
        template.process(templateInput, out);
        return out.toString();
    }

    private Optional<OtpChoice> findMatchingOtpChoice(AuthenticationFlowContext authenticationFlowContext, String otpChoiceAddressId) {
        return otpChannelService.getAvailableOtpChoices(authenticationFlowContext).stream()
                .filter(otpChoice -> otpChoiceAddressId.equals(otpChoice.getAddressId()))
                .findFirst();
    }

    private void cacheOtpSendingRequest(AuthenticationFlowContext context, String otp) {
        if (!unlimitedResendEnabled()) {
            String id = context.getUser().getId();
            var userAttemptCache = infinispanCache.get(id);
            userAttemptCache.getCacheMap().put(now(), otp);
            infinispanCache.put(id, userAttemptCache);
        }
    }

    private boolean unlimitedResendEnabled() {
        return 0 == totpConfig.getOtpResendLimit() ||
                0 == totpConfig.getOtpResendPeriod();
    }
}
