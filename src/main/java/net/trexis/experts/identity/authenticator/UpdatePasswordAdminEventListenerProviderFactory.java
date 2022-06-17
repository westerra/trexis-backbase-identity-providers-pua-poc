package net.trexis.experts.identity.authenticator;

import com.finite.ApiClient;
import com.finite.api.EntityApi;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class UpdatePasswordAdminEventListenerProviderFactory implements EventListenerProviderFactory {

    @Override
    public EventListenerProvider create(KeycloakSession keycloakSession) {
        return new UpdatePasswordAdminEventListenerProvider(keycloakSession);
    }

    @Override
    public void init(Config.Scope scope) {
        // Do nothing
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
        // Do nothing
    }

    @Override
    public void close() {
        // Do nothing
    }

    @Override
    public String getId() {
        return "update-password-email";
    }
}
