package net.trexis.experts.identity.spi.impl;

import com.google.common.base.Strings;
import net.trexis.experts.identity.spi.IngestionServiceProvider;
import net.trexis.experts.identity.spi.IngestionServiceProviderFactory;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class IngestionServiceProviderFactoryImpl implements IngestionServiceProviderFactory {

    private static final Logger log = Logger.getLogger(IngestionServiceProviderFactoryImpl.class);
    private static final String ID = "ingestion-service-provider";
    private static final String IDENTITY_INGESTION_SERVICE_HOST = "INGESTION_SERVICE_HOST";
    private static final String IDENTITY_INGESTION_SERVICE_PORT = "INGESTION_SERVICE_PORT";
    private static final String IDENTITY_INGESTION_SERVICE_SCHEME = "INGESTION_SERVICE_SCHEME";
    private static final String IDENTITY_INGESTION_SERVICE_BASEPATH = "INGESTION_SERVICE_BASEPATH";
    private static final String INGESTION_SERVICE_INGESTION_PATH = "INGESTION_SERVICE_INGESTION_PATH";
    private static IngestionServiceProperties ingestionServiceProperties;
    private String ingestionServiceHost;
    private String ingestionServiceScheme;
    private int ingestionServicePortValue;
    private String ingestionServiceBasePath;
    private String ingestionServiceUserIngestionPath;

    public static IngestionServiceProperties getIngestionServiceProperties() {
        return ingestionServiceProperties;
    }

    @Override
    public IngestionServiceProvider create(KeycloakSession keycloakSession) {
        return new IngestionServiceProviderImpl();
    }

    @Override
    public void init(Config.Scope scope) {
        String ingestionServicePort = System.getenv(IDENTITY_INGESTION_SERVICE_PORT);
        ingestionServiceHost = System.getenv(IDENTITY_INGESTION_SERVICE_HOST);
        ingestionServiceScheme = System.getenv(IDENTITY_INGESTION_SERVICE_SCHEME);
        ingestionServiceBasePath = System.getenv(IDENTITY_INGESTION_SERVICE_BASEPATH);
        ingestionServiceUserIngestionPath = System.getenv(INGESTION_SERVICE_INGESTION_PATH);

        log.info("Settings for Ingestion Service: " +
                " host " + ingestionServiceHost +
                " port " + ingestionServicePort +
                " scheme " + ingestionServiceScheme +
                " base path " + ingestionServiceBasePath +
                " user ingestion path " + ingestionServiceUserIngestionPath);

        if (Strings.isNullOrEmpty(ingestionServicePort)
                || Strings.isNullOrEmpty(ingestionServiceHost)
                || Strings.isNullOrEmpty(ingestionServiceScheme)) {
            throw new IllegalStateException("Environment variables for ingestion service required");
        }
        ingestionServicePortValue = Integer.valueOf(ingestionServicePort);

        ingestionServiceProperties =
                new IngestionServiceProperties(
                        ingestionServiceHost,
                        ingestionServicePortValue,
                        ingestionServiceScheme,
                        ingestionServiceBasePath,
                        ingestionServiceUserIngestionPath);
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
        // do nothing
    }

    @Override
    public void close() {
        // do nothing
    }

    @Override
    public String getId() {
        return ID;
    }
}
