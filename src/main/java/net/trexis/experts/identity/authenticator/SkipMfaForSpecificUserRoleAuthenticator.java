package net.trexis.experts.identity.authenticator;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.jboss.logging.Logger;

public class SkipMfaForSpecificUserRoleAuthenticator implements Authenticator {

    private static final Logger log = Logger.getLogger(SkipMfaForSpecificUserRoleAuthenticator.class);

    // Define the user role of the admin user for whom MFA should be skipped
    private static final String SKIP_MFA_FOR_USER_ROLE = "skip-mfa";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        UserModel user = context.getUser();

        // Check if the user has the "skip-mfa" role
        if (user.hasRole(context.getRealm().getRole(SKIP_MFA_FOR_USER_ROLE))) {
            log.info("Skipping MFA for user with role: " + user.getUsername());
            context.success();
        } else {
            log.info("Proceeding with MFA for user: " + user.getUsername());
            context.attempted();
        }
    }


    @Override
    public void action(AuthenticationFlowContext context) {
        context.success();  // Proceed with the flow after action (if needed)
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;  // This authenticator is always configured for the user
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // No required actions
    }

    @Override
    public void close() {
        // No resources to close
    }
}
