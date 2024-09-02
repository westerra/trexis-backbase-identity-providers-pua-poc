package net.trexis.experts.identity.authenticator;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import javax.ws.rs.core.Response;

public class InformationalMessageAuthenticator implements Authenticator {

    private static final String INFORMATION_MESSAGE_SEEN = "information_message_seen";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        UserModel user = context.getUser();

        // Check if the user has already seen the information message
        String messageSeen = user.getFirstAttribute(INFORMATION_MESSAGE_SEEN);

        if (messageSeen == null || !messageSeen.equals("true")) {
            // Display the informational message
            Response challenge = context.form()
                .setAttribute("informationMessage", "Your Credit Card has been activated successfully. Please check your account for details.")
                .createForm("information-message.ftl");
            user.setSingleAttribute(INFORMATION_MESSAGE_SEEN, "true");
            context.challenge(challenge);  // Stop further execution until user input
        } else {
            // User has already seen the message, proceed with login
            context.success();
        }
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        context.success();  // Proceed with login after user interacts with the message
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {

    }

    @Override
    public void close() {
        // No resources to close
    }
}
