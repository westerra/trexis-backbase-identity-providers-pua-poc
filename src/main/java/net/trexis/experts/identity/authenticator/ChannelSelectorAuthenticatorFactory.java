package net.trexis.experts.identity.authenticator;

import com.backbase.identity.authenticators.otp.OtpAuthenticatorConfiguration;
import com.backbase.identity.util.OtpChannelPropertiesConverter;

import net.trexis.experts.identity.service.OtpChannelService;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.microprofile.config.ConfigProvider;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.ConfigurableAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import static org.keycloak.models.AuthenticationExecutionModel.Requirement.DISABLED;
import static org.keycloak.models.AuthenticationExecutionModel.Requirement.REQUIRED;

public class ChannelSelectorAuthenticatorFactory implements AuthenticatorFactory, ConfigurableAuthenticatorFactory {

    public static final String PROVIDER_ID = "channel-selector";
    private static final OtpChannelService otpChannelService;
    private static final OtpAuthenticatorConfiguration otpAuthenticatorConfiguration;
    private static final AuthenticationExecutionModel.Requirement[] requirementChoices = {
            REQUIRED,
            DISABLED
    };

    static {
        otpAuthenticatorConfiguration = new OtpAuthenticatorConfiguration(new OtpChannelPropertiesConverter(), ConfigProvider.getConfig());
        otpChannelService = new OtpChannelService(otpAuthenticatorConfiguration);
    }

    @Override
    public String getDisplayType() {
        return "Channel Selector";
    }

    @Override
    public String getReferenceCategory() {
        return "Channel Selector";
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return requirementChoices;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return true;
    }

    @Override
    public String getHelpText() {
        return "OTP channel selector.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return new ArrayList<>();
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new ChannelSelectorAuthenticator(otpChannelService);
    }

    @Override
    public void init(Config.Scope config) {
        // nothing to do
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // nothing to do
    }

    @Override
    public void close() {
        // nothing to do
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
