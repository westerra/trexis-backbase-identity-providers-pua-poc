package net.trexis.experts.identity.spi.impl;

import com.google.common.base.Strings;
import net.trexis.experts.identity.spi.EnrollmentServiceProvider;

import net.trexis.experts.identity.spi.EnrollmentServiceProviderFactory;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class EnrollmentServiceProviderFactoryImpl implements EnrollmentServiceProviderFactory {

    private static final Logger log = Logger.getLogger(EnrollmentServiceProviderFactoryImpl.class);
    private static final String ID = "enrollment-service-provider";
    private static final String IDENTITY_ENROLLMENT_SERVICE_HOST = "ENROLLMENT_SERVICE_HOST";
    private static final String IDENTITY_ENROLLMENT_SERVICE_PORT = "ENROLLMENT_SERVICE_PORT";
    private static final String IDENTITY_ENROLLMENT_SERVICE_SCHEME = "ENROLLMENT_SERVICE_SCHEME";
    private static final String IDENTITY_ENROLLMENT_SERVICE_BASEPATH = "ENROLLMENT_SERVICE_BASEPATH";
    private static final String ENROLLMENT_SERVICE_REBASE_PATH = "ENROLLMENT_SERVICE_REBASE_PATH";
    private static final String ENROLLMENT_SERVICE_EVALUATE_LIMITED_PATH = "ENROLLMENT_SERVICE_EVALUATE_LIMITED_PATH";

    private String enrollmentServiceHost;
    private String enrollmentServiceScheme;
    private int enrollmentServicePortValue;
    private String enrollmentServiceBasePath;
    private String enrollmentServiceRebasePath;
    private String enrollmentServiceEvaluateLimitedPath;
    private static EnrollmentServiceProperties enrollmentServiceProperties;

    @Override
    public EnrollmentServiceProvider create(KeycloakSession keycloakSession) {
        return new EnrollmentServiceProviderImpl();
    }

    @Override
    public void init(Config.Scope scope) {
        String enrollmentServicePort = System.getenv(IDENTITY_ENROLLMENT_SERVICE_PORT);
        enrollmentServiceHost = System.getenv(IDENTITY_ENROLLMENT_SERVICE_HOST);
        enrollmentServiceScheme = System.getenv(IDENTITY_ENROLLMENT_SERVICE_SCHEME);
        enrollmentServiceBasePath = System.getenv(IDENTITY_ENROLLMENT_SERVICE_BASEPATH);
        enrollmentServiceRebasePath = System.getenv(ENROLLMENT_SERVICE_REBASE_PATH);
        enrollmentServiceEvaluateLimitedPath = System.getenv(ENROLLMENT_SERVICE_EVALUATE_LIMITED_PATH);

        log.info("Settings for enrollment Service: " +
                " host " + enrollmentServiceHost +
                " port " + enrollmentServicePort +
                " scheme " + enrollmentServiceScheme +
                " base path " + enrollmentServiceBasePath +
                " user enrollment rebase path " + enrollmentServiceRebasePath +
                " user enrollment evaluate limited path " + enrollmentServiceEvaluateLimitedPath);

        if (Strings.isNullOrEmpty(enrollmentServicePort)
                || Strings.isNullOrEmpty(enrollmentServiceHost)
                || Strings.isNullOrEmpty(enrollmentServiceScheme)) {
            throw new IllegalStateException("Environment variables for enrollment service required");
        }
        enrollmentServicePortValue = Integer.valueOf(enrollmentServicePort);

        enrollmentServiceProperties =
                new EnrollmentServiceProperties(
                        enrollmentServiceHost,
                        enrollmentServicePortValue,
                        enrollmentServiceScheme,
                        enrollmentServiceBasePath,
                        enrollmentServiceRebasePath,
                        enrollmentServiceEvaluateLimitedPath);
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

    public static EnrollmentServiceProperties getEnrollmentServiceProperties() {
        return enrollmentServiceProperties;
    }
}
