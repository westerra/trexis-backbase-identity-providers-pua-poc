package net.trexis.experts.identity.authenticator;

import com.backbase.identity.authenticators.otp.CommunicationService;
import com.backbase.identity.authenticators.otp.OtpChannelService;
import com.backbase.identity.authenticators.otp.SecretProvider;
import com.backbase.identity.authenticators.otp.exception.OtpDeliveryException;
import com.backbase.identity.authenticators.otp.model.IdentityTotpConfig;
import com.backbase.identity.authenticators.otp.model.OtpChoice;
import com.backbase.identity.authenticators.otp.model.PostBatchesRequestBody;
import com.backbase.identity.m10y.models.Tenant;
import com.backbase.identity.m10y.providers.TenantResolverProvider;
import com.backbase.identity.util.CacheSupplier;
import com.backbase.identity.util.IdentityTotpUtil;
import com.backbase.identity.util.LimitedActionMap;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.provider.ProviderConfigProperty;
import net.trexis.experts.identity.configuration.Constants;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static net.trexis.experts.identity.configuration.Constants.FALSE;
import static java.lang.Integer.parseInt;
import static java.time.Duration.between;
import static java.time.ZonedDateTime.now;
import static java.util.Map.Entry.comparingByKey;
import static net.trexis.experts.identity.configuration.Constants.OTP_CHOICE_ADDRESS_ID;
import static net.trexis.experts.identity.configuration.Constants.TRUE;
import static net.trexis.experts.identity.configuration.Constants.USER_ATTRIBUTE_MFA_REQUIRED;
import static org.keycloak.authentication.AuthenticationFlowError.INVALID_CREDENTIALS;

public class OtpAuthenticator implements Authenticator {

    private static final Logger log = Logger.getLogger(OtpAuthenticator.class);
    private static final String MFA_OTP_TEMPLATE = "mfa-otp.ftl";
    private static final String MFA_REQUIRED = "mfa_required";

    private final KeycloakSession session;
    private final OtpChannelService otpChannelService;
    private final SecretProvider secretProvider;
    private final CommunicationService communicationService;
    private final OtpTemplateProviderImplFactory otpTemplateProviderImplFactory;
    private final CacheSupplier cacheSupplier;

    private TimeBasedOTP timeBasedOtp;
    private org.infinispan.Cache<String, LimitedActionMap> infinispanCache;
    private IdentityTotpConfig totpConfig;

    OtpAuthenticator(KeycloakSession session, List<ProviderConfigProperty> configProperties,
            OtpChannelService otpChannelService, SecretProvider secretProvider,
            CommunicationService communicationService, OtpTemplateProviderImplFactory otpTemplateProviderImplFactory,
            CacheSupplier cacheSupplier) {
        this.session = session;
        this.otpChannelService = otpChannelService;
        this.secretProvider = secretProvider;
        this.communicationService = communicationService;
        this.otpTemplateProviderImplFactory = otpTemplateProviderImplFactory;
        this.cacheSupplier = cacheSupplier;

        //TODO: Resent limit testing
        var algorithmDefault = configProperties.stream()
                .map(ProviderConfigProperty::getName)
                .filter("trexis.otp.algorithm"::equals)
                .findFirst().orElseThrow();
        int digitsDefault = parseInt(configProperties.stream()
                .map(ProviderConfigProperty::getName)
                .filter("trexis.otp.digits"::equals)
                .findFirst().orElseThrow());
        int lookAheadWindowDefault = parseInt(configProperties.stream()
                .map(ProviderConfigProperty::getName)
                .filter("trexis.otp.lookAheadWindow"::equals)
                .findFirst().orElseThrow());
        int otpPeriodDefault = parseInt(configProperties.stream()
                .map(ProviderConfigProperty::getName)
                .filter("trexis.otp.otpPeriod"::equals)
                .findFirst()
                .orElseThrow());
        int otpResendPeriodDefault = parseInt(configProperties.stream()
                .map(ProviderConfigProperty::getName)
                .filter("trexis.otp.otpResendPeriod"::equals)
                .findFirst()
                .orElseThrow());
        int otpResendLimitDefault = parseInt(configProperties.stream()
                .map(ProviderConfigProperty::getName)
                .filter("trexis.otp.otpResendLimit"::equals)
                .findFirst()
                .orElseThrow());

        this.totpConfig = new IdentityTotpConfig(algorithmDefault, digitsDefault, lookAheadWindowDefault, otpPeriodDefault, otpResendPeriodDefault, otpResendLimitDefault);
        this.timeBasedOtp = new IdentityTotpUtil().getTimeBasedOtp(totpConfig);
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        if (mfaIsRequired(context.getUser())) {
            configureTotp(context);
            log.debugv("User {0} is required to do MFA", context.getUser().getUsername());
            String otpChoiceAddressId = context.getAuthenticationSession().getAuthNote(OTP_CHOICE_ADDRESS_ID);
            verifyOtpMethodAndSendOtp(context, otpChoiceAddressId);
            var input = context.form().createForm(MFA_OTP_TEMPLATE);
            context.challenge(input);
        } else {
            log.debugv("User {0} is NOT required to do MFA", context.getUser().getUsername());
            context.success();
        }
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

        if (formData.containsKey("cancel")) {
            log.info("form data included cancel, resetFlow called.");
            context.resetFlow();
            return;
        }

        if (formData.containsKey("resend")) {
            verifyOtpMethodAndSendOtp(context, choiceAddressId);
            Response challenge = challengeWithInfo(context, "OTP resent.");
            context.challenge(challenge);
            return;
        }

        if (!formData.containsKey("totp") || formData.getFirst("totp") == null) {
            issueFailureChallenge(context, "OTP value is missing.");
        }

        String userInput = formData.getFirst("totp");
        verifyOtp(context, choiceAddressId, userInput);
    }

    private void issueFailureChallenge(AuthenticationFlowContext context, String message) {
        Response challenge = context.form()
                .setError(message)
                .createForm(MFA_OTP_TEMPLATE);
        context.failureChallenge(INVALID_CREDENTIALS, challenge);
    }

    protected Response challengeWithInfo(AuthenticationFlowContext context, String infoMessage) {
        return context.form().setInfo(infoMessage).createForm(MFA_OTP_TEMPLATE);
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
        if(FALSE.equalsIgnoreCase(userModel.getFirstAttribute(Constants.USER_ATTRIBUTE_MFA_REQUIRED)) &&
                !(userModel.getRequiredActions().contains(MFA_REQUIRED))) {
            return false;
        } else {
            return TRUE.equalsIgnoreCase(userModel.getFirstAttribute(Constants.USER_ATTRIBUTE_MFA_REQUIRED)) ||
                    userModel.getRequiredActions().contains(MFA_REQUIRED);
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
                    long timeUntilResendAllowed = getTimeNextResendAllowed(context);

                    if (timeUntilResendAllowed == 0L) {
                        String otp = generateOtp(context);
                        cacheOtpSendingRequest(context, otp);
                        boolean otpIsSent = sendOtp(otp, present, context);
                        if (!otpIsSent) {
                            throw new OtpDeliveryException("Error occurred while sending OTP via communication service");
                        }
                    } else {
                        log.infov("OTP sending not allowed, {0} seconds remaining from {1} second configured period",
                                timeUntilResendAllowed, totpConfig.getOtpResendPeriod());
                        issueFailureChallenge(context, "OTP sending not allowed, resume after " + timeUntilResendAllowed);
                    }
                }, () -> issueFailureChallenge(context, "Error sending OTP."));
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
                    String secret = secretProvider.getCommunicationServiceSecret(context);
                    if (timeBasedOtp.validateTOTP(otp, secret.getBytes())) {
                        context.success();
                    } else {
                        issueFailureChallenge(context, "Invalid OTP.");
                    }
                }, () -> issueFailureChallenge(context, "Unknown device."));
    }

    private Optional<OtpChoice> findMatchingOtpChoice(AuthenticationFlowContext authenticationFlowContext, String otpChoiceAddressId) {
        return otpChannelService.getAvailableOtpChoices(authenticationFlowContext).stream()
                .filter(otpChoice -> otpChoiceAddressId.equals(otpChoice.getAddressId()))
                .findFirst();
    }

    private long getTimeNextResendAllowed(AuthenticationFlowContext context) {
        if (unlimitedResendEnabled()) {
            log.debug("Unlimited otp sending enabled");
            return 0L;
        }

        this.infinispanCache = this.cacheSupplier.getOtpAuthenticatorCache(this.session);
        var userAttemptCache = this.infinispanCache.get(context.getUser().getId());

        if (userAttemptCache == null || userAttemptCache.getCacheMap() == null ||
                userAttemptCache.getCacheMap().entrySet().size() < totpConfig.getOtpResendLimit()) {
            return 0L;
        }
        ZonedDateTime resendTime = userAttemptCache.getCacheMap().entrySet().stream()
                .sorted(comparingByKey())
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new OtpDeliveryException("Unable to retrieve existing otp attempt from cache"))
                .plusSeconds(totpConfig.getOtpResendPeriod());
        return between(now(), resendTime).getSeconds();
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
