package net.trexis.experts.identity.spi.impl;

import net.trexis.experts.identity.spi.EnrollmentServiceProvider;
import org.jboss.logging.Logger;
import org.keycloak.models.UserModel;

public class EnrollmentServiceProviderImpl implements EnrollmentServiceProvider {

    private static final Logger log = Logger.getLogger(EnrollmentServiceProviderFactoryImpl.class);
    private final EnrollmentServiceClient client;

    public EnrollmentServiceProviderImpl() {
        log.info("Creating Enrollment Service Provider");
        EnrollmentServiceProperties properties = EnrollmentServiceProviderFactoryImpl.getEnrollmentServiceProperties();
        client = new EnrollmentServiceClient(properties);
    }

    @Override
    public void callRebaseService(UserModel userModel) {
        log.info("Calling enrollment service, user rebase: " + client.toString());
        client.startRebase(userModel.getUsername());
    }

    @Override
    public boolean callEvaluateLimitedService(UserModel userModel) {
        log.info("Calling enrollment service, user validate limited: " + client.toString());
        return client.evaluateLimited(userModel.getUsername());
    }

    @Override
    public void close() {
        // do nothing
    }
}
