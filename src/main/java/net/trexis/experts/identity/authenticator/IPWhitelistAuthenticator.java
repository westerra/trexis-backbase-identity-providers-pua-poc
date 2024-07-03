package net.trexis.experts.identity.authenticator;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.*;

public class IPWhitelistAuthenticator implements Authenticator {

    private static final String IP_WHITELISTED_NOTE = "ip-whitelisted";

    private static final Logger log = Logger.getLogger(IPWhitelistAuthenticator.class);

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        String clientIP = context.getConnection().getRemoteAddr();
        log.debug("Client IP: " + clientIP);

        if (isIPWhitelisted(clientIP)) {
            context.getAuthenticationSession().setAuthNote(IP_WHITELISTED_NOTE, "true");
            log.debug(" Authentication successful ::: " + clientIP);
            context.success();
        } else {
            context.getAuthenticationSession().removeAuthNote(IP_WHITELISTED_NOTE);
            context.failure(AuthenticationFlowError.INVALID_CLIENT_SESSION);
        }
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // No action needed
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
        // No required actions
    }

    @Override
    public void close() {
        // Cleanup if needed
    }

    private boolean isIPWhitelisted(String clientIP) {
        // Example: Hardcoded whitelist (replace with your dynamic whitelist logic)
        String[] whitelist = {"34.215.116.35", "34.215.234.87","35.165.2.59","34.214.37.223","34.210.53.158","52.89.52.36","52.35.98.213","52.36.72.121","64.226.133.180"};
        for (String allowedIP : whitelist) {
            if (allowedIP.equals(clientIP)) {
                return true;
            }
        }
        return false;
    }
}
