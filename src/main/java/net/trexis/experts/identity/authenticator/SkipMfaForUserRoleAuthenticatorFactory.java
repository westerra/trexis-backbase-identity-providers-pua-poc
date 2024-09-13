package net.trexis.experts.identity.authenticator;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

public class SkipMfaForUserRoleAuthenticatorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "skip-mfa-for-admin-user";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new SkipMfaForUserRoleAuthenticator(); // Replace with your custom authenticator logic
    }

    @Override
    public void init(Config.Scope config) {
        // No special initialization required
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // No special post-initialization required
    }

    @Override
    public void close() {
        // No resources to close
    }

    @Override
    public String getDisplayType() {
        return "Skip MFA for Specific Admin User";
    }

    @Override
    public String getReferenceCategory() {
        return "Skip MFA";
    }

    @Override
    public boolean isConfigurable() {
        return false; // Set to true if you want to allow configurations via Keycloak UI
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[]{
                AuthenticationExecutionModel.Requirement.REQUIRED,
                AuthenticationExecutionModel.Requirement.ALTERNATIVE,
                AuthenticationExecutionModel.Requirement.DISABLED,
                AuthenticationExecutionModel.Requirement.CONDITIONAL
        };
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Skips MFA for a specific admin user";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of(); // No special properties required
    }
}
