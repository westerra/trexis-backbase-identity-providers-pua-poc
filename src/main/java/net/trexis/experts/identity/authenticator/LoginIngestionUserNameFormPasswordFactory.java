package net.trexis.experts.identity.authenticator;

import java.util.ArrayList;
import java.util.List;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public class LoginIngestionUserNameFormPasswordFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "ing-auth-username-pass-form";
    public static final LoginIngestionUsernamePasswordForm SINGLETON = new LoginIngestionUsernamePasswordForm();
    public static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES;

    static {
        REQUIREMENT_CHOICES = new AuthenticationExecutionModel.Requirement[]{AuthenticationExecutionModel.Requirement.REQUIRED};
    }

    public LoginIngestionUserNameFormPasswordFactory() {
    }

    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }

    public Authenticator createDisplay(KeycloakSession session, String displayType) {
        return displayType == null || "console".equalsIgnoreCase(displayType) ? SINGLETON : null;
    }

    public void init(Config.Scope config) {
    }

    public void postInit(KeycloakSessionFactory factory) {
    }

    public void close() {
    }

    public String getId() {
        return PROVIDER_ID;
    }

    public String getReferenceCategory() {
        return "password";
    }

    public boolean isConfigurable() {
        return false;
    }

    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    public String getDisplayType() {
        return "Ingestion Authentication Username Password Form";
    }

    public String getHelpText() {
        return "Extends Username Password Form.";
    }

    public List<ProviderConfigProperty> getConfigProperties() {
        return new ArrayList<>();
    }

    public boolean isUserSetupAllowed() {
        return false;
    }
}
