package net.trexis.experts.identity.authenticator;

import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;

public class CustomActionMFARequire implements RequiredActionProvider {

    public static final String MFA_REQUIRED_ID = "mfa_required";

    @Override
    public void evaluateTriggers(RequiredActionContext requiredActionContext) {
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        context.success();
    }

    @Override
    public void processAction(RequiredActionContext context) {
    }

    @Override
    public void close() {
    }
}
