package net.trexis.experts.identity.authenticator;

import com.finite.ApiClient;
import com.finite.api.EntityApi;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class UpdateProfileListenerProviderFactory implements EventListenerProviderFactory {

    private static final EntityApi ENTITY_API;
    private static final String PRIMARY_EMAIL_CONTACT_POINT_NAME;
    private static final String IDENTITY_FINITE_ENTITY_IDENTIFIER_CLAIM;

    static {
        String finiteHost = System.getenv("FINITE_HOSTURL");
        String finiteApiKey = System.getenv("FINITE_API_CORE_APIKEY");

        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(finiteHost);
        apiClient.setApiKey(finiteApiKey);
        ENTITY_API = new EntityApi(apiClient);

        PRIMARY_EMAIL_CONTACT_POINT_NAME = System.getenv("MAPPINGS_PRIMARYEMAIL");
        IDENTITY_FINITE_ENTITY_IDENTIFIER_CLAIM = System.getenv("IDENTITY_FINITE_ENTITY_IDENTIFIER_CLAIM");
    }

    @Override
    public EventListenerProvider create(KeycloakSession keycloakSession) {
        return new UpdateProfileListenerProvider(keycloakSession, ENTITY_API, IDENTITY_FINITE_ENTITY_IDENTIFIER_CLAIM, PRIMARY_EMAIL_CONTACT_POINT_NAME);
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
        return "update-profile-event-listener";
    }
}
