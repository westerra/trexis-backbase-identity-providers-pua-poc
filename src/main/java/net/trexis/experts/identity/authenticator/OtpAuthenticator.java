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
import net.trexis.experts.identity.configuration.Constants;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.TimeBasedOTP;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

import static net.trexis.experts.identity.configuration.Constants.TRUE;

public class OtpAuthenticator implements Authenticator {

    private static final Logger log = Logger.getLogger(OtpAuthenticator.class);

    private static final String TEMPLATE = "mfa-otp.ftl";
    private final KeycloakSession session;
    private final OtpChannelService otpChannelService;
    private final SecretProvider secretProvider;
    private final CommunicationService communicationService;
    private final OtpTemplateProviderFactory otpTemplateProviderFactory;
    private final CacheSupplier cacheSupplier;
    private TimeBasedOTP timeBasedOtp;
    private org.infinispan.Cache<String, LimitedActionMap> infinispanCache;
    private IdentityTotpConfig totpConfig;

    OtpAuthenticator(
            KeycloakSession session,
            OtpChannelService otpChannelService,
            SecretProvider secretProvider,
            CommunicationService communicationService,
            OtpTemplateProviderFactory otpTemplateProviderFactory,
            CacheSupplier cacheSupplier) {
        this.session = session;
        this.otpChannelService = otpChannelService;
        this.secretProvider = secretProvider;
        this.communicationService = communicationService;
        this.otpTemplateProviderFactory = otpTemplateProviderFactory;
        this.cacheSupplier = cacheSupplier;
        //TODO: Resent limit testing
        this.totpConfig = new IdentityTotpConfig("HmacSHA512", 5, 1, 30, 0, 0);
        this.timeBasedOtp = new IdentityTotpUtil().getTimeBasedOtp(totpConfig);
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        if (mfaIsRequired(context.getUser())) {
            log.debugv("User {0} is required to do MFA", context.getUser().getUsername());

            String otpChoiceAddressId = context.getAuthenticationSession().getAuthNote(Constants.OTP_CHOICE_ADDRESS_ID);
            verifyOtpMethodAndSendOtp(context, otpChoiceAddressId);
            Response input = context.form().createForm(TEMPLATE);
            context.challenge(input);
        } else {
            log.debugv("User {0} is NOT required to do MFA", context.getUser().getUsername());
            context.success();
        }

    }

    private String generateOtp(AuthenticationFlowContext authenticationFlowContext) {
        String communicationServiceSecret = secretProvider.getCommunicationServiceSecret(authenticationFlowContext);
        return this.timeBasedOtp.generateTOTP(communicationServiceSecret);
    }


    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData =
                context.getHttpRequest().getDecodedFormParameters();
        String choiceAddressId = context.getAuthenticationSession().getAuthNote(Constants.OTP_CHOICE_ADDRESS_ID);

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
                .createForm(TEMPLATE);
        context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challenge);
    }

    protected Response challengeWithInfo(AuthenticationFlowContext context, String infoMessage) {
        return context.form().setInfo(infoMessage).createForm(TEMPLATE);
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
        String mfaRequired = userModel.getFirstAttribute(Constants.USER_ATTRIBUTE_MFA_REQUIRED);
        return TRUE.equalsIgnoreCase(mfaRequired);
    }

    @Override
    public void setRequiredActions(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
        // not required
    }

    @Override
    public void close() {
        // not required
    }

    private void verifyOtpMethodAndSendOtp(AuthenticationFlowContext context, String otpChoiceAddressId) {
        Optional<OtpChoice> selectedOtpChoiceOptional = findMatchingOtpChoice(context, otpChoiceAddressId);
        if (selectedOtpChoiceOptional.isPresent()) {
            long timeUntilResendAllowed = getTimeNextResendAllowed(context);
            if (timeUntilResendAllowed == 0L) {
                String otp = generateOtp(context);
                cacheOtpSendingRequest(context, otp);
                boolean otpIsSent = sendOtp(otp, selectedOtpChoiceOptional.get(), context);
                if (!otpIsSent) {
                    throw new OtpDeliveryException("Error occurred while sending OTP via communication service");
                }
            } else {
                log.infov(
                        "OTP sending not allowed, {0} seconds remaining from {1} second configured period",
                        timeUntilResendAllowed,
                        totpConfig.getOtpResendPeriod());
                issueFailureChallenge(context, "OTP sending not allowed, resume after " + timeUntilResendAllowed);
            }
        } else {
            issueFailureChallenge(context, "Error sending OTP.");
        }
    }

    private boolean sendOtp(String otp, OtpChoice otpChoice, AuthenticationFlowContext authenticationFlowContext) {
        OtpTemplateProvider templateProvider = otpTemplateProviderFactory.create(authenticationFlowContext.getSession());
        UserModel user = authenticationFlowContext.getUser();
        templateProvider.setUser(user);
        PostBatchesRequestBody postBatchesRequestBody = new PostBatchesRequestBody();
        postBatchesRequestBody.setRecipients(otpChannelService.getRecipients(otpChoice, otp, user));
        postBatchesRequestBody.setContent(templateProvider.getContent(otpChoice, otp, user));
        String realmId = authenticationFlowContext.getRealm().getId();
        String tenantId = null;
        if (session.getProvider(TenantResolverProvider.class) != null) {
            tenantId = session.getProvider(TenantResolverProvider.class).resolveTenant(realmId).map(Tenant::getId).orElse(null);
        }
        return this.communicationService.send(this.session, postBatchesRequestBody, tenantId);
    }

    private void verifyOtp(AuthenticationFlowContext context, String otpMethod, String otp) {
        if (this.findMatchingOtpChoice(context, otpMethod).isPresent()) {
            String secret = secretProvider.getCommunicationServiceSecret(context);
            boolean otpCorrect = this.timeBasedOtp.validateTOTP(otp, secret.getBytes());
            if (otpCorrect) {
                context.success();
            } else {
                issueFailureChallenge(context, "Invalid OTP.");
            }
        } else {
            issueFailureChallenge(context, "Unknown device.");
        }
    }

    private Optional<OtpChoice> findMatchingOtpChoice(AuthenticationFlowContext authenticationFlowContext, String otpChoiceAddressId) {
        return otpChannelService.getAvailableOtpChoices(authenticationFlowContext).stream()
                .filter((otpChoice) -> otpChoiceAddressId.equals(otpChoice.getAddressId())).findFirst();
    }

    private long getTimeNextResendAllowed(AuthenticationFlowContext context) {
        if (unlimitedResendEnabled()) {
            log.debug("Unlimited otp sending enabled");
            return 0L;
        } else {
            this.infinispanCache = this.cacheSupplier.getOtpAuthenticatorCache(this.session);
            var userAttemptCache = this.infinispanCache.get(context.getUser().getId());
            if (userAttemptCache != null && userAttemptCache.getCacheMap().entrySet().stream().count() >= (long) this.totpConfig.getOtpResendLimit()) {
                ZonedDateTime resendTime = userAttemptCache.getCacheMap().entrySet().stream()
                        .sorted(Map.Entry.comparingByKey()).map(Map.Entry::getKey)
                        .findFirst().orElseThrow(() -> new OtpDeliveryException("Unable to retrieve existing otp attempt from cache"))
                        .plusSeconds(this.totpConfig.getOtpResendPeriod());
                return Duration.between(ZonedDateTime.now(), resendTime).getSeconds();
            } else {
                return 0L;
            }
        }
    }

    private void cacheOtpSendingRequest(AuthenticationFlowContext context, String otp) {
        if (!unlimitedResendEnabled()) {
            String id = context.getUser().getId();
            var userAttemptCache = this.infinispanCache.get(id);
            userAttemptCache.getCacheMap().put(ZonedDateTime.now(), otp);
            infinispanCache.put(id, userAttemptCache);
        }
    }

    private boolean unlimitedResendEnabled() {
        return this.totpConfig.getOtpResendLimit() == 0 || this.totpConfig.getOtpResendPeriod() == 0;
    }
}
