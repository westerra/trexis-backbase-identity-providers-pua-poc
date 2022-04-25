package net.trexis.experts.identity.authenticator;

import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class CustomActionMFARequireFactory implements RequiredActionFactory {

    private static final CustomActionMFARequire SINGLETON = new CustomActionMFARequire();

    @Override
    public String getDisplayText() {
        return "MFA Required";
    }

    @Override
    public RequiredActionProvider create(KeycloakSession keycloakSession) {
        return SINGLETON;
    }

    @Override
    public void init(Config.Scope scope) {
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return CustomActionMFARequire.MFA_REQUIRED_ID;
    }
}
