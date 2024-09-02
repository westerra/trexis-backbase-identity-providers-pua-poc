package net.trexis.experts.identity.authenticator;

import net.trexis.experts.identity.spi.IngestionServiceProvider;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.ServicesLogger;

import javax.ws.rs.core.Response;

public class LoginIngestionAuthenticator implements Authenticator {

    protected static ServicesLogger log;
    private static final String INFORMATION_MESSAGE_SEEN = "information_message_seen";


    static {
        log = ServicesLogger.LOGGER;
    }

    /**
     * Initial call for the authenticator. This method should check the current HTTP request to determine if the request satisfies the Authenticator's requirements. If it doesn't,
     * it should send back a challenge response by calling the AuthenticationFlowContext.challenge(Response).
     */
    @Override
    public void authenticate(AuthenticationFlowContext context) {
        log.info("Starting ingestion process");
        IngestionServiceProvider ingestionService = context.getSession().getProvider(IngestionServiceProvider.class);
        ingestionService.callIngestionService(context.getUser());
        context.success();
        checkAndDisplayInformationMessage(context);
        log.info("Finished call to ingestion service");
    }


    private void checkAndDisplayInformationMessage(AuthenticationFlowContext context) {
        UserModel user = context.getUser();

        // Check if the user has already seen the information message
        String messageSeen = user.getFirstAttribute(INFORMATION_MESSAGE_SEEN);

        if (messageSeen == null || !messageSeen.equals("true")) {
            log.info("Displaying informational message to user: " + user.getUsername());
            Response challenge = context.form()
                    .setAttribute("informationMessage", "Your Credit Card has been activated successfully. Please check your account for details.")
                    .createForm("information-message.ftl");

            // Save the attribute that the message was seen
            user.setSingleAttribute(INFORMATION_MESSAGE_SEEN, "true");

            // Challenge the user with the informational message
            context.challenge(challenge);
        } else {
            log.info("User has already seen the informational message, proceeding with login.");
            context.success(); // Log in the user directly
        }
    }
    /**
     * Called from a form action invocation.
     */
    @Override
    public void action(AuthenticationFlowContext context) {
    }

    /**
     * Does this authenticator require that the user has already been identified?
     */
    @Override
    public boolean requiresUser() {
        return false;
    }

    /**
     * Is the user configured for this authenticator.
     */
    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return false;
    }

    /**
     * The method is responsible for registering any required actions that must be performed by the user. If configuredFor() returns false this method will be called, but only if
     * the associated AuthenticatorFactory’s isUserSetupAllowed method returns true.
     */
    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel setRequiredActions) {
    }

    /**
     * This is called when the server shuts down, if any resource is 'held' it should be released here.
     */
    @Override
    public void close() {
    }
}
