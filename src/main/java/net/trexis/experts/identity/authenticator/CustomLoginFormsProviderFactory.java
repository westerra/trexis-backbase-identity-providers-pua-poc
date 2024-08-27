package net.trexis.experts.identity.authenticator;

import org.keycloak.forms.login.freemarker.FreeMarkerLoginFormsProvider;
import org.keycloak.forms.login.freemarker.FreeMarkerLoginFormsProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.theme.FreeMarkerUtil;

public class CustomLoginFormsProviderFactory extends FreeMarkerLoginFormsProviderFactory {

    @Override
    public FreeMarkerLoginFormsProvider create(KeycloakSession session) {
        FreeMarkerUtil freeMarkerUtil = new FreeMarkerUtil(); // Or retrieve it from the session context
        return new CustomLoginFormsProvider(session, freeMarkerUtil);
    }

    @Override
    public String getId() {
        return "custom-login-forms-provider";
    }

    @Override
    public void init(org.keycloak.Config.Scope config) {
        // Initialization code
    }

    @Override
    public void postInit(org.keycloak.models.KeycloakSessionFactory factory) {
        // Post-initialization code
    }

    @Override
    public void close() {
        // Cleanup code if necessary
    }
}
