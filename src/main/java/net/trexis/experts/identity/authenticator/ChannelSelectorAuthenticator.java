package net.trexis.experts.identity.authenticator;

import com.backbase.identity.authenticators.otp.OtpChannelService;
import com.backbase.identity.authenticators.otp.model.OtpChoice;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import net.trexis.experts.identity.configuration.Constants;
import net.trexis.experts.identity.model.AccessTokenModel;
import net.trexis.experts.identity.model.OtpChoiceRepresentation;
import net.trexis.experts.identity.model.UserLoginDetails;
import net.trexis.experts.identity.util.ChannelSelectorUtil;
import okhttp3.*;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static net.trexis.experts.identity.configuration.Constants.FALSE;
import static net.trexis.experts.identity.configuration.Constants.TRUE;

public class ChannelSelectorAuthenticator implements Authenticator {

    protected static final String MFA_CHOICE_TEMPLATE = "mfa-devices.ftl";
    private static final String MFA_REQUIRED = "mfa_required";
    private static final String GET_ACCESS_TOKEN_BASE_URL = "GET_ACCESS_TOKEN_BASE_URL";
    private static final String GET_USER_EVENTS_BASE_URL = "GET_USER_EVENTS_BASE_URL";
    private static final String CLIENT_ID = "CLIENT_ID";
    private static final String USERNAME = "USERNAME";
    private static final String PASSWORD = "PASSWORD";
    private static final String GRANT_TYPE = "GRANT_TYPE";
    private static final String LAST_LOGIN_DAYS = "LAST_LOGIN_DAYS";
    private static final Logger log = Logger.getLogger(ChannelSelectorAuthenticator.class);
    private final OtpChannelService otpChannelService;

    public ChannelSelectorAuthenticator(OtpChannelService otpChannelService) {
        this.otpChannelService = otpChannelService;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        if(!mfaIsRequired(context.getUser()) &&
                !(FALSE.equalsIgnoreCase(context.getUser().getFirstAttribute(Constants.USER_ATTRIBUTE_MFA_REQUIRED))) ){
            AccessTokenModel accessTokenModel = getAccessToken(context);
            if(!mfaIsRequired(context.getUser()) && accessTokenModel!=null) {
                checkLastValidLogin(context,accessTokenModel);
            } else {
                context.getUser().addRequiredAction(MFA_REQUIRED);
            }
        }

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

    private void checkLastValidLogin(AuthenticationFlowContext context, AccessTokenModel accessTokenModel) {
        String lastLoginDaysToCheck = System.getenv(LAST_LOGIN_DAYS);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate currentLocalDate = LocalDate.now().minusDays(Long.parseLong(lastLoginDaysToCheck));
        String dateFrom = currentLocalDate.format(formatter);
        String currentLoginIpAddress = context.getHttpRequest().getRemoteAddress();
        String userId = context.getUser().getId();
        String eventType = "LOGIN";
        String getUserEventsBaseUrl = System.getenv(GET_USER_EVENTS_BASE_URL)+"?type="+eventType+"&user="+userId+"&dateFrom="+dateFrom;

        OkHttpClient client = new OkHttpClient().newBuilder().build();
        Request getUserEventsRequestRequest = new Request.Builder()
                .url(getUserEventsBaseUrl)
                .method("GET", null)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + accessTokenModel.getAccess_token())
                .build();
        try {
            okhttp3.Response getUserEventsRequestResponse = client.newCall(getUserEventsRequestRequest).execute();
            if (getUserEventsRequestResponse.isSuccessful() && getUserEventsRequestResponse.body()!=null) {
                String convertedObjectForUserLoginDetails = getUserEventsRequestResponse.body().string();
                log.debug("convertedObjectForUserLoginDetails :"+convertedObjectForUserLoginDetails);
                UserLoginDetails userLoginDetails[] = new Gson().fromJson(convertedObjectForUserLoginDetails, UserLoginDetails[].class);
                if(userLoginDetails!=null && userLoginDetails.length>0){
                    String lastLoginIpAddress = userLoginDetails[0].getIpAddress();
                    if(lastLoginIpAddress.equals(currentLoginIpAddress)){
                        log.info("Same IpAddress Found,DO NOT Setting MFA for User");
                    } else {
                        log.info("New IpAddress Found, Setting MFA for User");
                        context.getUser().addRequiredAction(MFA_REQUIRED);
                    }
                } else {
                    log.info("User Login Details Not Found, Setting MFA for User");
                    context.getUser().addRequiredAction(MFA_REQUIRED);
                }
            } else {
                log.info("Setting MFA for User,Due to unexpected getUserEventsRequestResponse : " + getUserEventsRequestResponse);
                context.getUser().addRequiredAction(MFA_REQUIRED);
            }
        } catch (IOException e) {
            e.printStackTrace();
            context.getUser().addRequiredAction(MFA_REQUIRED);
        }
    }

    private AccessTokenModel getAccessToken(AuthenticationFlowContext context) {
        String getAccessTokenBaseUrl = System.getenv(GET_ACCESS_TOKEN_BASE_URL);
        String clientId = System.getenv(CLIENT_ID);
        String username = System.getenv(USERNAME);
        String password = System.getenv(PASSWORD);
        String grantType = System.getenv(GRANT_TYPE);
        AccessTokenModel accessTokenModel = null;

        OkHttpClient client = new OkHttpClient().newBuilder().build();
        String getAccessTokenBodyContent = "client_id=" + clientId + "&username=" + username + "&password=" + password + "&grant_type=" + grantType;
        RequestBody getAccessTokenBody = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), getAccessTokenBodyContent);
        Request getAccessTokenRequest = new Request.Builder()
                .url(getAccessTokenBaseUrl)
                .method("POST", getAccessTokenBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();

        try {
            okhttp3.Response getAccessTokenResponse = client.newCall(getAccessTokenRequest).execute();
            if (getAccessTokenResponse.isSuccessful()  && getAccessTokenResponse.body()!=null) {
                String convertedObjectForAccessToken = getAccessTokenResponse.body().string();
                if(convertedObjectForAccessToken!=null && !convertedObjectForAccessToken.isEmpty()){
                    accessTokenModel = new Gson().fromJson(convertedObjectForAccessToken, AccessTokenModel.class);
                    log.debug("accessTokenModel" + accessTokenModel);
                } else {
                    log.info("Access Token Not Found, Setting MFA for User");
                    context.getUser().addRequiredAction(MFA_REQUIRED);
                }
            } else {
                log.info("Setting MFA for User,Due to unexpected getAccessTokenResponse : " + getAccessTokenResponse);
                context.getUser().addRequiredAction(MFA_REQUIRED);
            }
        } catch (IOException e) {
            e.printStackTrace();
            context.getUser().addRequiredAction(MFA_REQUIRED);
        }
        return accessTokenModel;
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
        if(FALSE.equalsIgnoreCase(userModel.getFirstAttribute(Constants.USER_ATTRIBUTE_MFA_REQUIRED)) &&
                !(userModel.getRequiredActions().contains(MFA_REQUIRED))) {
            return false;
        } else {
            return TRUE.equalsIgnoreCase(userModel.getFirstAttribute(Constants.USER_ATTRIBUTE_MFA_REQUIRED)) ||
                    userModel.getRequiredActions().contains(MFA_REQUIRED);
        }
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