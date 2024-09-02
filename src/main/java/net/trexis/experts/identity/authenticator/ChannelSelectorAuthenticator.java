package net.trexis.experts.identity.authenticator;

import com.backbase.identity.authenticators.otp.model.OtpChoice;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import net.trexis.experts.identity.configuration.Constants;
import net.trexis.experts.identity.model.AccessTokenModel;
import net.trexis.experts.identity.model.MfaAttributeEnum;
import net.trexis.experts.identity.model.OtpChoiceRepresentation;
import net.trexis.experts.identity.model.UserLoginDetails;
import net.trexis.experts.identity.service.OtpChannelService;
import net.trexis.experts.identity.util.ChannelSelectorUtil;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import static net.trexis.experts.identity.configuration.Constants.*;

public class ChannelSelectorAuthenticator implements Authenticator {

    protected static final String MFA_CHOICE_TEMPLATE = "mfa-devices.ftl";
    private static final String GET_ACCESS_TOKEN_BASE_URL = "GET_ACCESS_TOKEN_BASE_URL";
    private static final String GET_USER_EVENTS_BASE_URL = "GET_USER_EVENTS_BASE_URL";
    private static final String CLIENT_ID = "CLIENT_ID";
    private static final String USERNAME = "USERNAME";
    private static final String PASSWORD = "PASSWORD";
    private static final String GRANT_TYPE = "GRANT_TYPE";
    private static final String LAST_LOGIN_DAYS = "LAST_LOGIN_DAYS";
    private static final String LAST_IP_CHECK = "LAST_IP_CHECK";
    private static final Integer LAST_IP_CHECK_DEFAULT = 4;
    private static final String INFORMATION_MESSAGE_SEEN = "information_message_seen";


    private static final Logger log = Logger.getLogger(ChannelSelectorAuthenticator.class);

    private final OtpChannelService otpChannelService;
    private final OkHttpClient client;

    public ChannelSelectorAuthenticator(OtpChannelService otpChannelService) {
        this(otpChannelService, new OkHttpClient().newBuilder().build());
    }

    ChannelSelectorAuthenticator(OtpChannelService otpChannelService, OkHttpClient client) {
        this.otpChannelService = otpChannelService;
        this.client = client;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {

        if(MfaAttributeEnum.ALWAYS_FALSE.getValue().equalsIgnoreCase(context.getUser().getFirstAttribute(Constants.USER_ATTRIBUTE_MFA_REQUIRED))) {
            log.info("MFA required attribute is alwaysFalse, NOT required to do MFA");
            context.success();
            checkAndDisplayInformationMessage(context);
            return;
        }

        // by pass the MFA if ip white listed and bypass-mfa-flag-enabled
        if (ChannelSelectorUtil.byPassMFAIfIpWhiteListed(context)) {
            log.debugv("IP {} is whitelisted; skipping MFA for user {}", context.getConnection().getRemoteAddr(), context.getUser().getUsername());
            context.success();
            checkAndDisplayInformationMessage(context);
            return;
        }

        //If MFA required attribute is false, We need to compare with last login IP addresses
        if(!mfaIsRequired(context.getUser()) && checkLastValidLogin(context)) {
            context.success();
            checkAndDisplayInformationMessage(context);
            return;
        } else if(MfaAttributeEnum.FALSE.getValue().equalsIgnoreCase(context.getUser().getFirstAttribute(Constants.USER_ATTRIBUTE_MFA_REQUIRED))){
            log.warn("Setting MFA required attribute to true");
            context.getUser().setSingleAttribute(Constants.USER_ATTRIBUTE_MFA_REQUIRED,TRUE);
        }

        resetUsersChoices(context);
        var otpChoiceList = getChoiceRepresentationList(context);
        //group optChoiceList by channel
        log.debug("Choices count: " + otpChoiceList.size());
        if (otpChoiceList.isEmpty()) {
            log.warn("No choices found for user: " + context.getUser().getUsername());
            context.clearUser();
            context.challenge(context.form()
                    .setInfo("We are sorry we could not find any MFA choices associated to your account.")
                    .createLoginUsernamePassword());
            return;
        }

        // TODO: This behavior could be configurable. Leaving commented out in case it is desireable in the future.
        // if (otpChoiceList.size() == 1) {
        //     log.debug("Only one mfa choice found, skipping selection");
        //     OtpChoiceRepresentation otpChoice = otpChoiceList.stream()
        //     .findFirst()
        //     .orElseThrow();
        //     context.getAuthenticationSession().setAuthNote(OTP_CHOICE_ADDRESS_ID, otpChoice.getAddressId());
        //     context.success();
        //     return;
        // }

        var otpChoiceListByChannel = otpChoiceList.stream().collect(Collectors.groupingBy(OtpChoiceRepresentation::getChannel));
        Response challenge = context.form()
                .setAttribute("otpChoiceList", otpChoiceList)//TODO: should we leave this to support the old style?
                .setAttribute("otpChoiceByChannel", otpChoiceListByChannel)
                .createForm(MFA_CHOICE_TEMPLATE);
        context.challenge(challenge);
    }

    private void checkAndDisplayInformationMessage(AuthenticationFlowContext context) {
        UserModel user = context.getUser();

        // Check if the user has already seen the information message
        String messageSeen = user.getFirstAttribute(INFORMATION_MESSAGE_SEEN);
        if (messageSeen == null || !messageSeen.equals("true")) {
            log.info("Displaying informational message to user: " + user.getUsername());
            context.challenge(context.form()
                    .setAttribute("informationMessage", "Your Credit Card has been activated successfully. Please check your account for details.")
                    .createForm("information-message.ftl"));
            user.setSingleAttribute(INFORMATION_MESSAGE_SEEN, "true");
        } else {
            log.info("User has already seen the informational message, proceeding with login.");
            context.success();
        }
    }

    private boolean checkLastValidLogin(AuthenticationFlowContext context) {
        log.warn("checking last valid login!!");
        //fetch token
        AccessTokenModel accessTokenModel = getAccessToken(context);
        if(accessTokenModel == null) {
            log.warn("access token is null");
            return false;
        }
        String lastLoginDaysToCheck = System.getenv(LAST_LOGIN_DAYS);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate currentLocalDate = LocalDate.now().minusDays(Long.parseLong(lastLoginDaysToCheck));
        String dateFrom = currentLocalDate.format(formatter);
        String currentLoginIpAddress = context.getHttpRequest().getRemoteAddress();
        String userId = context.getUser().getId();
        String eventType = "LOGIN";
        String getUserEventsBaseUrl = System.getenv(GET_USER_EVENTS_BASE_URL) + "?type=" + eventType + "&user=" + userId + "&dateFrom=" + dateFrom;
        Integer lastIpAddressCheck = System.getenv().containsKey(LAST_IP_CHECK)?Integer.parseInt(System.getenv(LAST_IP_CHECK)):LAST_IP_CHECK_DEFAULT;

        Request getUserEventsRequestRequest = new Request.Builder()
                .url(getUserEventsBaseUrl)
                .method("GET", null)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + accessTokenModel.getAccessToken())
                .build();
        try {
            okhttp3.Response getUserEventsRequestResponse = client.newCall(getUserEventsRequestRequest).execute();
            if (getUserEventsRequestResponse == null || getUserEventsRequestResponse.body() == null || !getUserEventsRequestResponse.isSuccessful()) {
                log.warn("Setting MFA for User,Due to unexpected getUserEventsRequestResponse : " + getUserEventsRequestResponse);
                return false;
            }

            String convertedObjectForUserLoginDetails = getUserEventsRequestResponse.body().string();
            log.warn("convertedObjectForUserLoginDetails :" + convertedObjectForUserLoginDetails);
            UserLoginDetails[] userLoginDetails = new Gson().fromJson(convertedObjectForUserLoginDetails, UserLoginDetails[].class);
            if (userLoginDetails == null || userLoginDetails.length == 0) {
                log.warn("User Login Details Not Found, Setting MFA for User");
                return false;
            }
            boolean isLoginValid = false;
            //checking for last 4 ip address
            var lastLoginCheckMaxIndex = userLoginDetails.length >= lastIpAddressCheck ? lastIpAddressCheck-1 : userLoginDetails.length - 1;
            for (int i = 0; i <= lastLoginCheckMaxIndex; i++) {
                if (userLoginDetails[i].getIpAddress().equals(currentLoginIpAddress)) {
                    isLoginValid = true;
                    break;
                }
            }
            if (!isLoginValid) {
                log.warn("New IpAddress Found, Setting MFA for User");
                return false;
            }
        } catch (IOException e) {
            log.error("Error while getting user events MFA required!\n", e);
            return false;
        }
        log.warn("Same IpAddress Found From Last 4 Logins , Hence NOT required to do MFA");
        return true;

    }

    private AccessTokenModel getAccessToken(AuthenticationFlowContext context) {
        String getAccessTokenBaseUrl = System.getenv(GET_ACCESS_TOKEN_BASE_URL);
        String clientId = System.getenv(CLIENT_ID);
        String username = System.getenv(USERNAME);
        String password = System.getenv(PASSWORD);
        String grantType = System.getenv(GRANT_TYPE);
        AccessTokenModel accessTokenModel = null;

        try {
            RequestBody getAccessTokenBodyContent = new FormBody.Builder()
                    .add("client_id", clientId)
                    .add("username", username)
                    .add("password", password)
                    .add("grant_type", grantType)
                    .build();
            Request getAccessTokenRequest = new Request.Builder()
                    .url(getAccessTokenBaseUrl)
                    .post(getAccessTokenBodyContent)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build();
            okhttp3.Response getAccessTokenResponse = client.newCall(getAccessTokenRequest).execute();
            if (getAccessTokenResponse.isSuccessful() && getAccessTokenResponse.body() != null) {
                String convertedObjectForAccessToken = getAccessTokenResponse.body().string();
                if (convertedObjectForAccessToken != null && !convertedObjectForAccessToken.isEmpty()) {
                    accessTokenModel = new Gson().fromJson(convertedObjectForAccessToken, AccessTokenModel.class);
                    log.debug("accessTokenModel" + accessTokenModel);
                } else {
                    log.warn("Access Token Not Found, Setting MFA for User");
                    context.getUser().setSingleAttribute(Constants.USER_ATTRIBUTE_MFA_REQUIRED,MfaAttributeEnum.TRUE.getValue());
                }
            } else {
                log.warn("Setting MFA for User,Due to unexpected getAccessTokenResponse : " + getAccessTokenResponse);
                context.getUser().setSingleAttribute(Constants.USER_ATTRIBUTE_MFA_REQUIRED,MfaAttributeEnum.TRUE.getValue());
            }
        } catch (IOException e) {
            log.error("Setting MFA for User,Due to IOException while getting access token :\n",e);
            context.getUser().setSingleAttribute(Constants.USER_ATTRIBUTE_MFA_REQUIRED,MfaAttributeEnum.TRUE.getValue());
        }
        return accessTokenModel;
    }

    protected List<OtpChoiceRepresentation> getChoiceRepresentationList(AuthenticationFlowContext context) {
        log.debug("Fetching MFA options for user: " + context.getUser().getUsername());
        return otpChannelService.getAvailableOtpChoices(context).stream()
                .map(choice -> {
                    log.debug("OTP chose address: " + choice.getAddress() +
                            " addressId: " + choice.getAddressId() +
                            " channel: " + choice.getChannel());
                    return OtpChoiceRepresentation.builder()
                            .address(ChannelSelectorUtil.maskPhoneNumber(choice.getAddress()))
                            .addressId(choice.getAddressId())
                            .channel(choice.getChannel())
                            .selected(false)
                            .build();
                }).collect(Collectors.toList());
    }

    protected void resetUsersChoices(AuthenticationFlowContext context) {
        log.debug("Resetting users's choices in MFA authenticator selector.");
        context.getAuthenticationSession().setAuthNote(OTP_CHOICE_ADDRESS_ID, null);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        var inputData = context.getHttpRequest().getDecodedFormParameters();

        if (inputData.containsKey("cancel")) {
            log.debug("form data included cancel, resetFlow called.");
            context.resetFlow();
            return;
        }

        String otpChoiceAddressId = inputData.getFirst(OTP_CHOICE_ADDRESS_ID);
        if (Strings.isNullOrEmpty(otpChoiceAddressId)) {
            var otpChoiceList = getChoiceRepresentationList(context);
            var otpChoiceListByChannel = otpChoiceList.stream().collect(Collectors.groupingBy(OtpChoiceRepresentation::getChannel));
            Response challenge = context.form()
                    .setError("otpSelectionRequired")
                    .setAttribute("otpChoiceList", otpChoiceList)
                    .setAttribute("otpChoiceByChannel", otpChoiceListByChannel)
                    .createForm(MFA_CHOICE_TEMPLATE);
            context.challenge(challenge);
            return;
        }

        findMatchingOtpChoice(context, otpChoiceAddressId)
                .ifPresentOrElse(present -> {
                    context.getAuthenticationSession().setAuthNote(OTP_CHOICE_ADDRESS_ID, otpChoiceAddressId);
                    context.success();
                }, () -> {
                    log.error("Could not find MFA device with id: " + otpChoiceAddressId);
                    Response challenge = context.form()
                            .setError("Something went wrong when trying to send your code.")
                            .createForm(MFA_CHOICE_TEMPLATE);
                    context.challenge(challenge);
                });
    }

    private Optional<OtpChoice> findMatchingOtpChoice(AuthenticationFlowContext authenticationFlowContext, String otpChoiceAddressId) {
        return otpChannelService.getAvailableOtpChoices(authenticationFlowContext).stream()
                .filter(otpChoice -> otpChoiceAddressId.equals(otpChoice.getAddressId()))
                .findFirst();
    }

    private boolean mfaIsRequired(UserModel userModel) {
        if(MfaAttributeEnum.TRUE.getValue().equalsIgnoreCase(userModel.getFirstAttribute(Constants.USER_ATTRIBUTE_MFA_REQUIRED)) ||
                MfaAttributeEnum.ALWAYS_TRUE.getValue().equalsIgnoreCase(userModel.getFirstAttribute(Constants.USER_ATTRIBUTE_MFA_REQUIRED))) {
            log.warn("MFA required attribute is true or alwaysTrue, Required to do MFA");
            return true;
        } else if (MfaAttributeEnum.FALSE.getValue().equalsIgnoreCase(userModel.getFirstAttribute(Constants.USER_ATTRIBUTE_MFA_REQUIRED))) {
            log.warn("MFA required attribute is false, Checking MFA conditions");
            return false;
        } else {
            // For any other value (Except : true,false,alwaysTrue,alwaysFalse) we set it to default value : true
            userModel.setSingleAttribute(Constants.USER_ATTRIBUTE_MFA_REQUIRED, MfaAttributeEnum.TRUE.getValue());
            log.warn("Required to do MFA cause MFA required attribute is null, empty or any garbage value, Setting it to True");
            return true;
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