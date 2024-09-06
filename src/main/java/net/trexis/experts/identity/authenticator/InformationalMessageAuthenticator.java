package net.trexis.experts.identity.authenticator;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.jboss.logging.Logger;

import javax.ws.rs.core.Response;

public class InformationalMessageAuthenticator implements Authenticator {

    private static final String INFORMATION_MESSAGE_SEEN = "information_message_seen";
    private static final Logger log = Logger.getLogger(InformationalMessageAuthenticator.class);

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        UserModel user = context.getUser();

        if (user == null) {
            log.error("User is null, cannot proceed with authentication.");
            context.failure(AuthenticationFlowError.INVALID_USER, Response.status(400).entity("User not found").build());
            return;
        }

        // Check if the user has already seen the information message
        String messageSeen = user.getFirstAttribute(INFORMATION_MESSAGE_SEEN);

        if (messageSeen == null || !messageSeen.equals("true")) {
            log.info("Displaying informational message to user: " + user.getUsername());
            // Display the informational message without hardcoding the message in the backend

            Response challenge = context.form()
                    .createForm("information-message.ftl");
            context.challenge(challenge);

            user.setSingleAttribute(INFORMATION_MESSAGE_SEEN, "true");
            context.challenge(challenge);  // Stop further execution until user input
        } else {
            log.info("User has already seen the informational message, proceeding with login.");
            context.success();
        }
    }


    @Override
    public void action(AuthenticationFlowContext context) {
        log.info("User interaction with informational message completed, proceeding with login.");
        context.success();  // Proceed with login after user interacts with the message
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
        return true;  // Assuming this authenticator is always configured for the user
    }

    @Override
    public void setRequiredActions(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
        // No required actions needed
    }

    @Override
    public void close() {
        // No resources to close
    }
}
