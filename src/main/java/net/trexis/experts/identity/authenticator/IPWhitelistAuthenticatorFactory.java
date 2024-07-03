package net.trexis.experts.identity.authenticator;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;

import java.util.ArrayList;
import java.util.List;

public class IPWhitelistAuthenticatorFactory implements AuthenticatorFactory, ProviderFactory<Authenticator> {

    public static final String PROVIDER_ID = "ip-whitelist-authenticator";
    private static final IPWhitelistAuthenticator SINGLETON = new IPWhitelistAuthenticator();

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }

    @Override
    public void init(Config.Scope config) {
        // Initialization code if needed
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // Post initialization code if needed
    }

    @Override
    public void close() {
        // Cleanup code if needed
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "IP Whitelist Authenticator";
    }

    @Override
    public String getReferenceCategory() {
        return "IP Whitelist";
    }

    @Override
    public boolean isConfigurable() {
        return false; // Set to true if you have configuration options
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[0];
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return new ArrayList<>(); // Add configuration properties if needed
    }
}
