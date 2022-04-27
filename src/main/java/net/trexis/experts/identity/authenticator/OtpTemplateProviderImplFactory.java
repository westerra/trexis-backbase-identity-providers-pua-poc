package net.trexis.experts.identity.authenticator;

import org.keycloak.models.KeycloakSession;
import org.keycloak.theme.FreeMarkerUtil;

public class OtpTemplateProviderImplFactory {

    public OtpTemplateProviderImplFactory() {
    }

    public OtpTemplateProviderImpl create(KeycloakSession keycloakSession) {
        return new OtpTemplateProviderImpl(keycloakSession, new FreeMarkerUtil());
    }
}