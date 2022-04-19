package net.trexis.experts.identity.authenticator;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;

public class EvaluateRestrictedAuthenticatorFactory implements AuthenticatorFactory {

    @Override
    public Authenticator create(KeycloakSession session) {
        return new EvaluateRestrictedAuthenticator();
    }

    /**
     * What requirement settings are allowed.
     *
     * @return
     */
    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[]{AuthenticationExecutionModel.Requirement.REQUIRED, AuthenticationExecutionModel.Requirement.DISABLED};
    }

    /**
     * Determines if the setRequiredActions() will be called. If a user is not configured for an Authenticator, the flow manager checks isUserSetupAllowed(). If it is false, then
     * the flow aborts with an error. However if it returns true, then the flow manager will invoke setRequiredActions().
     *
     * @return
     */
    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    /**
     * Flag which specifies to the admin console on whether the Authenticator can be configured within a flow.
     *
     * @return
     */
    @Override
    public boolean isConfigurable() {
        return false;
    }

    /**
     * The tooltip text that will be shown when you are picking the Authenticator.
     *
     * @return
     */
    @Override
    public String getHelpText() {
        return "Validate if a user is restricted on the core, and update enrollment in Backbase based on evaluation";
    }

    /**
     * Configuration property metadata.
     * <p>
     * Returns a list of ProviderConfigProperty objects. These objects detail a specific configuration attribute. Each ProviderConfigProperty defines the name of the config
     * property.
     *
     * @return
     */
    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return new ArrayList<>();
    }

    /**
     * Friendly name for the authenticator.
     *
     * @return
     */
    @Override
    public String getDisplayType() {
        return "Enrollment: Evaluate Restricted Authenticator";
    }

    /**
     * General authenticator type, i.e. totp, password, cert.
     *
     * @return null if not a referencable category.
     */
    @Override
    public String getReferenceCategory() {
        return "enrollment";
    }

    /**
     * Only called once when the factory is first created. This config is pulled from a number of places including but not limited to the application.yml and environment vars. The
     * param will be scoped to keycloak.<spi-name>.<getId()>
     *
     * @param config
     */
    @Override
    public void init(Config.Scope config) {
    }

    /**
     * Called after all provider factories have been initialized.
     *
     * @param factory
     */
    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    /**
     * This is the name of the provider and will be shown in the admin console as an option.
     *
     * @return
     */
    @Override
    public String getId() {
        return "evaluate-restricted-authenticator";
    }

    /**
     * This is called when the server shuts down.
     */
    @Override
    public void close() {
    }

}
