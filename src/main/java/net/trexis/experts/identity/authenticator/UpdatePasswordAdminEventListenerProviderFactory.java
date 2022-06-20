package net.trexis.experts.identity.authenticator;

import net.trexis.experts.identity.model.EmailConfiguration;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class UpdatePasswordAdminEventListenerProviderFactory implements EventListenerProviderFactory {

    @Override
    public EventListenerProvider create(KeycloakSession keycloakSession) {
        //In no-arg constructor(EmailConfiguration) we are setting values from System variables
        return new UpdatePasswordAdminEventListenerProvider(keycloakSession,new EmailConfiguration());
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
