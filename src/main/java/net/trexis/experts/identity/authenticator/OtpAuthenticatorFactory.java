package net.trexis.experts.identity.authenticator;

import com.backbase.identity.authenticators.otp.CommunicationService;
import com.backbase.identity.authenticators.otp.OtpAuthenticatorConfiguration;
import com.backbase.identity.authenticators.otp.OtpChannelService;
import com.backbase.identity.authenticators.otp.SecretProvider;
import com.backbase.identity.util.DefaultCacheSupplier;
import com.backbase.identity.util.OtpChannelPropertiesConverter;
import org.eclipse.microprofile.config.ConfigProvider;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.ConfigurableAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;

public class OtpAuthenticatorFactory implements AuthenticatorFactory, ConfigurableAuthenticatorFactory {

    private static final String PROVIDER_ID = "otp-authenticator";

    private static final Requirement[] requirements;
    private static final OtpAuthenticatorConfiguration otpAuthenticatorConfiguration;
    private static final SecretProvider secretProvider;
    private static final OtpTemplateProviderFactory otpTemplateProviderFactory;
    private static final OtpChannelService otpChannelService;
    private static final CommunicationService communicationService;

    static {
        requirements = new Requirement[]{Requirement.REQUIRED, Requirement.DISABLED, Requirement.ALTERNATIVE};
        otpAuthenticatorConfiguration = new OtpAuthenticatorConfiguration(new OtpChannelPropertiesConverter(), ConfigProvider.getConfig());
        secretProvider = new SecretProvider();
        otpTemplateProviderFactory = new OtpTemplateProviderFactory();
        otpChannelService = new OtpChannelService(otpAuthenticatorConfiguration);
        communicationService = new CommunicationService(otpAuthenticatorConfiguration.getCommunicationsServiceEndpoint());
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        ProviderConfigProperty algorithm, digits, lookAheadWindow, otpPeriod, otpResendPeriod, otpResendLimit;

        algorithm = new ProviderConfigProperty();
        algorithm.setName("trexis.otp.algorithm");
        algorithm.setLabel("Algorithm");
        algorithm.setType(ProviderConfigProperty.STRING_TYPE);
        algorithm.setHelpText("Algorithm");
        algorithm.setDefaultValue("HmacSHA512"); // Default shows up in the console, but WILL NOT be passed forward in context if a config is not created in keycloak console
        configProperties.add(algorithm);

        digits = new ProviderConfigProperty();
        digits.setName("trexis.otp.digits");
        digits.setLabel("Digits");
        digits.setType(ProviderConfigProperty.STRING_TYPE);
        digits.setHelpText("Digits");
        digits.setDefaultValue(5);
        configProperties.add(digits);

        lookAheadWindow = new ProviderConfigProperty();
        lookAheadWindow.setName("trexis.otp.lookAheadWindow");
        lookAheadWindow.setLabel("Lookahead Window");
        lookAheadWindow.setType(ProviderConfigProperty.STRING_TYPE);
        lookAheadWindow.setHelpText("Lookahead Window");
        lookAheadWindow.setDefaultValue(1);
        configProperties.add(lookAheadWindow);

        otpPeriod = new ProviderConfigProperty();
        otpPeriod.setName("trexis.otp.otpPeriod");
        otpPeriod.setLabel("OTP Period");
        otpPeriod.setType(ProviderConfigProperty.STRING_TYPE);
        otpPeriod.setHelpText("OTP Period");
        otpPeriod.setDefaultValue(30);
        configProperties.add(otpPeriod);

        otpResendPeriod = new ProviderConfigProperty();
        otpResendPeriod.setName("trexis.otp.otpResendPeriod");
        otpResendPeriod.setLabel("OTP Resend Period");
        otpResendPeriod.setType(ProviderConfigProperty.STRING_TYPE);
        otpResendPeriod.setHelpText("OTP Resend Period");
        otpResendPeriod.setDefaultValue(0);
        configProperties.add(otpResendPeriod);

        otpResendLimit = new ProviderConfigProperty();
        otpResendLimit.setName("trexis.otp.otpResendLimit");
        otpResendLimit.setLabel("OTP Resend Limit");
        otpResendLimit.setType(ProviderConfigProperty.STRING_TYPE);
        otpResendLimit.setHelpText("OTP Resend Limit");
        otpResendLimit.setDefaultValue(0);
        configProperties.add(otpResendLimit);

    }

    @Override
    public Authenticator create(KeycloakSession keycloakSession) {
        return new OtpAuthenticator(
                keycloakSession,
                configProperties,
                otpChannelService,
                secretProvider,
                communicationService,
                otpTemplateProviderFactory,
                new DefaultCacheSupplier());
    }

    /**
     * Friendly name for the authenticator.
     *
     * @return
     */
    @Override
    public String getDisplayType() {
        return "OTP Authenticator";
    }

    /**
     * General authenticator type, i.e. totp, password, cert.
     *
     * @return null if not a referencable category.
     */
    @Override
    public String getReferenceCategory() {
        return "OTP Authenticators";
    }

    /**
     * Flag which specifies to the admin console on whether the Authenticator can be configured within a flow.
     */
    @Override
    public boolean isConfigurable() {
        return true;
    }

    /**
     * Allowed requirement switches. While there are four different requirement types: ALTERNATIVE, REQUIRED, OPTIONAL,
     * DISABLED, AuthenticatorFactory implementations can limit which requirement options are shown in the admin console
     * when defining a flow
     */
    @Override
    public Requirement[] getRequirementChoices() {
        return requirements;
    }

    /**
     * Flag that tells the flow manager whether or not Authenticator.setRequiredActions() method will be called. If an
     * Authenticator is not configured for a user, the flow manager checks isUserSetupAllowed(). If it is false, then
     * the flow aborts with an error. If it returns true, then the flow manager will invoke
     * Authenticator.setRequiredActions().
     */
    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "OTP Auth";
    }

    /**
     * Returns a list of ProviderConfigProperty objects. These objects define a specific configuration attribute.
     */
    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    /**
     * Only called once when the factory is first created.
     * This config is pulled from a number of places including but not limited to
     * the application.yml and environment vars.
     * The param will be scoped to keycloak.<spi-name>.<getId()>
     *
     * @param config
     */
    @Override
    public void init(Config.Scope config) {
        // not used
    }

    /**
     * Called after all provider factories have been initialized.
     *
     * @param keycloakSessionFactory
     */
    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
        // not used
    }

    /**
     * This is called when the server shuts down.
     */
    @Override
    public void close() {
        // not used
    }

    /**
     * This is the name of the provider and will be shown in the admin console as an option.
     * @return
     */
    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
