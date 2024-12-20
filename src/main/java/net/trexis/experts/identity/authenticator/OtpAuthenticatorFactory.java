package net.trexis.experts.identity.authenticator;

import com.backbase.identity.authenticators.otp.CommunicationService;
import com.backbase.identity.authenticators.otp.OtpAuthenticatorConfiguration;
import com.backbase.identity.authenticators.otp.SecretProvider;
import com.backbase.identity.spi.store.OtpStoreProvider;
import com.backbase.identity.util.OtpChannelPropertiesConverter;

import net.trexis.experts.identity.model.MfaEmailConfiguration;
import net.trexis.experts.identity.service.OtpChannelService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.config.ConfigProvider;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.ConfigurableAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import static com.backbase.identity.authenticators.otp.OtpAuthenticatorConfiguration.PROPERTY_NAME_PREFIX;
import static org.keycloak.models.AuthenticationExecutionModel.Requirement.ALTERNATIVE;
import static org.keycloak.models.AuthenticationExecutionModel.Requirement.DISABLED;
import static org.keycloak.models.AuthenticationExecutionModel.Requirement.REQUIRED;
import static org.keycloak.provider.ProviderConfigProperty.STRING_TYPE;

public class OtpAuthenticatorFactory implements AuthenticatorFactory, ConfigurableAuthenticatorFactory {

    private static final String PROVIDER_ID = "otp-authenticator";
    private static final String OTP_DIGIT = "OTP_DIGIT";
    private static final String OTP_PERIOD = "OTP_PERIOD";
    private static final String LOOKAHEAD_WINDOW = "LOOKAHEAD_WINDOW";

    private static final Requirement[] requirements;
    private static final OtpAuthenticatorConfiguration otpAuthenticatorConfiguration;
    private static final SecretProvider secretProvider;
    private static final OtpTemplateProviderImplFactory otpTemplateProviderImplFactory;
    private static final OtpChannelService otpChannelService;
    private static final CommunicationService communicationService;
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        requirements = new Requirement[]{REQUIRED, DISABLED, ALTERNATIVE};
        otpAuthenticatorConfiguration = new OtpAuthenticatorConfiguration(new OtpChannelPropertiesConverter(), ConfigProvider.getConfig());
        secretProvider = new SecretProvider();
        otpTemplateProviderImplFactory = new OtpTemplateProviderImplFactory();
        otpChannelService = new OtpChannelService(otpAuthenticatorConfiguration);
        communicationService = new CommunicationService(otpAuthenticatorConfiguration.getCommunicationsServiceEndpoint());
    }

    static {
        ProviderConfigProperty algorithm, digits, lookAheadWindow, otpPeriod, otpResendPeriod, otpResendLimit;
        Map<String,String> systemEnv = System.getenv();

        algorithm = new ProviderConfigProperty();
        algorithm.setName("trexis.otp.algorithm");
        algorithm.setLabel("Algorithm");
        algorithm.setType(STRING_TYPE);
        algorithm.setHelpText("Algorithm");
        algorithm.setDefaultValue("HmacSHA512"); // Default shows up in the console, but WILL NOT be passed forward in context if a config is not created in keycloak console
        configProperties.add(algorithm);

        digits = new ProviderConfigProperty();
        digits.setName("trexis.otp.digits");
        digits.setLabel("Digits");
        digits.setType(STRING_TYPE);
        digits.setHelpText("Digits");
        digits.setDefaultValue(systemEnv.containsKey(OTP_DIGIT)?systemEnv.get(OTP_DIGIT):8);
        configProperties.add(digits);

        lookAheadWindow = new ProviderConfigProperty();
        lookAheadWindow.setName("trexis.otp.lookAheadWindow");
        lookAheadWindow.setLabel("Lookahead Window");
        lookAheadWindow.setType(STRING_TYPE);
        lookAheadWindow.setHelpText("Lookahead Window");
        lookAheadWindow.setDefaultValue(systemEnv.containsKey(LOOKAHEAD_WINDOW)?systemEnv.get(LOOKAHEAD_WINDOW):1);
        configProperties.add(lookAheadWindow);

        otpPeriod = new ProviderConfigProperty();
        otpPeriod.setName("trexis.otp.otpPeriod");
        otpPeriod.setLabel("OTP Period");
        otpPeriod.setType(STRING_TYPE);
        otpPeriod.setHelpText("OTP Period");
        otpPeriod.setDefaultValue(systemEnv.containsKey(OTP_PERIOD)?systemEnv.get(OTP_PERIOD):60);
        configProperties.add(otpPeriod);

        otpResendPeriod = new ProviderConfigProperty();
        otpResendPeriod.setName("trexis.otp.otpResendPeriod");
        otpResendPeriod.setLabel("OTP Resend Period");
        otpResendPeriod.setType(STRING_TYPE);
        otpResendPeriod.setHelpText("OTP Resend Period");
        otpResendPeriod.setDefaultValue(0);
        configProperties.add(otpResendPeriod);

        otpResendLimit = new ProviderConfigProperty();
        otpResendLimit.setName("trexis.otp.otpResendLimit");
        otpResendLimit.setLabel("OTP Resend Limit");
        otpResendLimit.setType(STRING_TYPE);
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
                otpTemplateProviderImplFactory,
                keycloakSession.getProvider(OtpStoreProvider.class),
                new MfaEmailConfiguration());
    }

    /**
     * Friendly name for the authenticator.
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
     * Allowed requirement switches. While there are four different requirement types: ALTERNATIVE, REQUIRED, OPTIONAL, DISABLED, AuthenticatorFactory implementations can limit
     * which requirement options are shown in the admin console when defining a flow
     */
    @Override
    public Requirement[] getRequirementChoices() {
        return requirements;
    }

    /**
     * Flag that tells the flow manager whether or not Authenticator.setRequiredActions() method will be called. If an Authenticator is not configured for a user, the flow manager
     * checks isUserSetupAllowed(). If it is false, then the flow aborts with an error. If it returns true, then the flow manager will invoke Authenticator.setRequiredActions().
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
     * Only called once when the factory is first created. This config is pulled from a number of places including but not limited to the application.yml and environment vars. The
     * param will be scoped to keycloak.<spi-name>.<getId()>
     */
    @Override
    public void init(Config.Scope config) {
        // not used
    }

    /**
     * Called after all provider factories have been initialized.
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
     */
    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
