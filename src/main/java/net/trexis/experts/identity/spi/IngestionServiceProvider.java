package net.trexis.experts.identity.spi;

import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;

public interface IngestionServiceProvider extends Provider {

    void callIngestionService(UserModel userModel);
}
