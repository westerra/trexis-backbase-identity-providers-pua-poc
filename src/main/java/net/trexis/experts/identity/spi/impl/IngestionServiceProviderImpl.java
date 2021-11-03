package net.trexis.experts.identity.spi.impl;

import org.jboss.logging.Logger;
import org.keycloak.models.UserModel;
import net.trexis.experts.identity.spi.IngestionServiceProvider;

public class IngestionServiceProviderImpl implements IngestionServiceProvider {
    private static final Logger log = Logger.getLogger(IngestionServiceProviderFactoryImpl.class);
    private IngestionServiceClient client;

    public IngestionServiceProviderImpl() {
        log.info("Creating Ingestion Service Provider");
        IngestionServiceProperties properties = IngestionServiceProviderFactoryImpl.getIngestionServiceProperties();
        client = new IngestionServiceClient(properties);
    }

    @Override
    public void callIngestionService(UserModel userModel) {
        log.info("Calling ingestion service, user ingestion: " + client.toString());
        client.startIngestion(userModel.getUsername());
    }

    @Override
    public void close() {
        // do nothing
    }
}
