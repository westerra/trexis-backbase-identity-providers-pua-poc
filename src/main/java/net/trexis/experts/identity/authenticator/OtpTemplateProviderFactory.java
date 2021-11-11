package net.trexis.experts.identity.authenticator;

import org.keycloak.models.KeycloakSession;
import org.keycloak.theme.FreeMarkerUtil;

public class OtpTemplateProviderFactory {
    public OtpTemplateProviderFactory() {}
    public OtpTemplateProvider create(KeycloakSession keycloakSession) {
        return new OtpTemplateProvider(keycloakSession, new FreeMarkerUtil());
    }
}