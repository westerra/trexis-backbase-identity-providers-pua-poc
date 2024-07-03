package net.trexis.experts.identity.spi;

import net.trexis.experts.identity.authenticator.IPWhitelistAuthenticator;
import net.trexis.experts.identity.authenticator.IPWhitelistAuthenticatorFactory;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class IPWhitelistSpi implements Spi {

    private static final String ID = "ip-whitelist-spi";

    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getName() {
        return ID;
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return IPWhitelistAuthenticator.class; // Implement this class
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return IPWhitelistAuthenticatorFactory.class; // Implement this class
    }
}
