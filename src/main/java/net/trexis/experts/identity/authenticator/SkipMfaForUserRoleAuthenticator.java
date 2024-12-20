package net.trexis.experts.identity.authenticator;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.jboss.logging.Logger;

public class SkipMfaForUserRoleAuthenticator implements Authenticator {

    private static final Logger log = Logger.getLogger(SkipMfaForUserRoleAuthenticator.class);

    // Define the user role of the admin user for whom MFA should be skipped
    private static final String SKIP_MFA_FOR_USER_ROLE = "skip-mfa";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        UserModel user = context.getUser();

        if (user == null) {
            log.error("User is null, cannot proceed with authentication.");
            context.attempted();  // Proceed with MFA since user is invalid
            return;
        }

        // Retrieve the role from the realm
        var skipMfaRole = context.getRealm().getRole(SKIP_MFA_FOR_USER_ROLE);

        // Check if the role exists
        if (skipMfaRole == null) {
            log.warn("Role '" + SKIP_MFA_FOR_USER_ROLE + "' not found in realm.");
            context.attempted();  // Proceed with MFA as the role doesn't exist
            return;
        }

        // Check if the user has the "skip-mfa" role
        if (user.hasRole(skipMfaRole)) {
            log.info("Skipping MFA for user with role: " + user.getUsername());
            context.success();  // Skip MFA
            return;
        } else {
            log.info("Proceeding with MFA for user: " + user.getUsername());
            context.attempted();  // Move to the next authenticator for MFA
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
