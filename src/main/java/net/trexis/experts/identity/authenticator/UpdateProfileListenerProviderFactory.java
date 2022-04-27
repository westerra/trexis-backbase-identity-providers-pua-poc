package net.trexis.experts.identity.authenticator;

import com.finite.ApiClient;
import com.finite.api.EntityApi;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class UpdateProfileListenerProviderFactory implements EventListenerProviderFactory {

    private static final EntityApi entityApi;

    static {
        String finiteHost = System.getenv("FINITE_HOSTURL");
        String finiteApiKey = System.getenv("FINITE_API_CORE_APIKEY");

        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(finiteHost);
        apiClient.setApiKey(finiteApiKey);
        entityApi = new EntityApi(apiClient);
    }

    @Override
    public EventListenerProvider create(KeycloakSession keycloakSession) {
        return new UpdateProfileListenerProvider(keycloakSession, entityApi);
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
