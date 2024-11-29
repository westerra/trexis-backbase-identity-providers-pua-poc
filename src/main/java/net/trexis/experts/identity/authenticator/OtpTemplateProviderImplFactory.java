package net.trexis.experts.identity.authenticator;

import org.keycloak.models.KeycloakSession;

public class OtpTemplateProviderImplFactory {

    public OtpTemplateProviderImplFactory() {
    }

    public OtpTemplateProviderImpl create(KeycloakSession keycloakSession) {
        return new OtpTemplateProviderImpl(keycloakSession);
    }
}