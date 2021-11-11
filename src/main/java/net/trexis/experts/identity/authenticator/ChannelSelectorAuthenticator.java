package net.trexis.experts.identity.authenticator;

import com.backbase.identity.authenticators.otp.OtpChannelService;
import com.backbase.identity.authenticators.otp.model.OtpChoice;
import com.google.common.base.Strings;
import net.trexis.experts.identity.configuration.Constants;
import net.trexis.experts.identity.model.OtpChoiceRepresentation;
import net.trexis.experts.identity.util.ChannelSelectorUtil;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static net.trexis.experts.identity.configuration.Constants.TRUE;

public class ChannelSelectorAuthenticator implements Authenticator {

    protected static final String MFA_CHOICE_TEMPLATE = "mfa-devices.ftl";
    private static final Logger log = Logger.getLogger(ChannelSelectorAuthenticator.class);
    private final OtpChannelService otpChannelService;

    public ChannelSelectorAuthenticator(OtpChannelService otpChannelService) {
        this.otpChannelService = otpChannelService;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        if (mfaIsRequired(context.getUser())) {
            resetUsersChoices(context);
            Response challenge;
            List<OtpChoiceRepresentation> otpChoiceList = getChoiceRepresentationList(context);
            log.debug("Choices count: " + otpChoiceList.size());
            log.debug(otpChoiceList);
            if (otpChoiceList.isEmpty()) {
                log.warn("No choices found for user: " + context.getUser().getUsername());
                context.clearUser();
                context.challenge(context.form()
                        .setInfo("Could not find any MFA choices associated to your account.")
                        .createLoginUsernamePassword());
                return;
            }
            if (otpChoiceList.size() == 1) {
                log.debug("Only one mfa choice found, skipping selection");
                OtpChoiceRepresentation otpChoice = otpChoiceList.stream().findFirst().get();
                context.getAuthenticationSession().setAuthNote(Constants.OTP_CHOICE_ADDRESS_ID, otpChoice.getAddressId());
                context.success();
                return;
            }
            challenge = context.form()
                    .setAttribute("otpChoiceList", otpChoiceList)
                    .createForm(MFA_CHOICE_TEMPLATE);
            context.challenge(challenge);
        } else {
            log.debugv("User {0} is NOT required to do MFA", context.getUser().getUsername());
            context.success();
        }
    }

    protected List<OtpChoiceRepresentation> getChoiceRepresentationList(AuthenticationFlowContext context) {
        List<OtpChoice> availableOtpChoices = otpChannelService.getAvailableOtpChoices(context);
        List<OtpChoiceRepresentation> otpChoiceRepresentations = new ArrayList<>();
        for (OtpChoice choice : availableOtpChoices) {
            log.debug("OTP chose address: " + choice.getAddress() + " addressId: " + choice.getAddressId() + " channel: " + choice.getChannel());
            otpChoiceRepresentations.add(OtpChoiceRepresentation.builder()
                    .address(ChannelSelectorUtil.maskPhoneNumber(choice.getAddress()))
                    .addressId(choice.getAddressId())
                    .channel(choice.getChannel())
                    .selected(false)
                    .build());
        }
        return otpChoiceRepresentations;
    }

    protected void resetUsersChoices(AuthenticationFlowContext context) {
        log.debug("Resetting users's choices in MFA authenticator selector.");
        context.getAuthenticationSession().setAuthNote(Constants.OTP_CHOICE_ADDRESS_ID, null);
    }

    @Override
    public void action(AuthenticationFlowContext context) {

        MultivaluedMap<String, String> inputData = context.getHttpRequest().getDecodedFormParameters();

        if (inputData.containsKey("cancel")) {
            log.debug("form data included cancel, resetFlow called.");
            context.resetFlow();
            return;
        }

        String otpChoiceAddressId = inputData.getFirst(Constants.OTP_CHOICE_ADDRESS_ID);
        if (Strings.isNullOrEmpty(otpChoiceAddressId)) {
            Response challenge = context.form()
                    .setError("Selection is required.")
                    .createForm(MFA_CHOICE_TEMPLATE);
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challenge);
            return;
        }

        Optional<OtpChoice> otpChoice = findMatchingOtpChoice(context, otpChoiceAddressId);
        if (!otpChoice.isPresent()) {
            log.error("Could not find MFA device with id: " + otpChoiceAddressId);
            Response challenge = context.form()
                    .setError("Something went wrong when trying to send your code.")
                    .createForm(MFA_CHOICE_TEMPLATE);
            context.challenge(challenge);
            return;
        }
        context.getAuthenticationSession().setAuthNote(Constants.OTP_CHOICE_ADDRESS_ID, otpChoiceAddressId);
        context.success();
    }

    private Optional<OtpChoice> findMatchingOtpChoice(AuthenticationFlowContext authenticationFlowContext, String otpChoiceAddressId) {
        return otpChannelService.getAvailableOtpChoices(authenticationFlowContext).stream()
                .filter((otpChoice) -> otpChoiceAddressId.equals(otpChoice.getAddressId())).findFirst();
    }

    private boolean mfaIsRequired(UserModel userModel) {
        String mfaRequired = userModel.getFirstAttribute(Constants.USER_ATTRIBUTE_MFA_REQUIRED);
        return TRUE.equalsIgnoreCase(mfaRequired);
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // no required actions to set up this step, it's only to choose a method of MFA
    }

    @Override
    public void close() {
        // nothing to do
    }
}