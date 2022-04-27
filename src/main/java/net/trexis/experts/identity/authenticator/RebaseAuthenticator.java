package net.trexis.experts.identity.authenticator;

import net.trexis.experts.identity.spi.EnrollmentServiceProvider;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.ServicesLogger;

public class RebaseAuthenticator implements Authenticator {

    protected static ServicesLogger log;

    static {
        log = ServicesLogger.LOGGER;
    }

    /**
     * Initial call for the authenticator. This method should check the current HTTP request to determine if the request satisfies the Authenticator's requirements. If it doesn't,
     * it should send back a challenge response by calling the AuthenticationFlowContext.challenge(Response).
     */
    @Override
    public void authenticate(AuthenticationFlowContext context) {
        log.info("Starting enrollment rebase process");
        EnrollmentServiceProvider enrollmentServiceProvider = context.getSession().getProvider(EnrollmentServiceProvider.class);
        enrollmentServiceProvider.callRebaseService(context.getUser());
        context.success();
        log.info("Finished call to enrollment service");
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
