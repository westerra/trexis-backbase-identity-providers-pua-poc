package net.trexis.experts.identity.spi;

import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;

public interface EnrollmentServiceProvider extends Provider {

    void callRebaseService(UserModel userModel);
    boolean callEvaluateLimitedService(UserModel userModel);
}
