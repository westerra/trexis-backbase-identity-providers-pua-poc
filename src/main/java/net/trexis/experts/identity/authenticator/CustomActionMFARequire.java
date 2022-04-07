package net.trexis.experts.identity.authenticator;

import org.jboss.logging.Logger;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;

public class CustomActionMFARequire implements RequiredActionProvider {

    public static final String MFA_REQUIRED_ID = "mfa_required";
    private static final Logger log = Logger.getLogger(CustomActionMFARequire.class);

    @Override
    public void evaluateTriggers(RequiredActionContext requiredActionContext) {
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
    }

    @Override
    public void processAction(RequiredActionContext context) {
    }

    @Override
    public void close() {
    }
}
