package net.trexis.experts.identity.util;

import lombok.experimental.UtilityClass;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.ClientModel;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@UtilityClass
public class ChannelSelectorUtil {

    private static final Logger log = Logger.getLogger(ChannelSelectorUtil.class);

    public static String maskPhoneNumber(String tel) {
        return tel.length() < 4 ? tel :
                tel.substring(0, tel.length() - 4).replaceAll("\\d", "*") + tel.substring(tel.length() - 4);
    }


    /**
     * Determines if MFA should be bypassed based on the client's IP address being in a whitelist and a specific flag being enabled.
     * @param context the context of the authentication flow
     * @return true if MFA should be bypassed, false otherwise
     */
    public static boolean byPassMFAIfIpWhiteListed(AuthenticationFlowContext context) {
        ClientModel client = context.getAuthenticationSession().getClient();
        String clientIP = context.getConnection().getRemoteAddr();

        if (isIpBasedBypassMfaEnabled(client)) {
            List<String> whitelistedIPs = getWhitelistedIPs(client);
            return whitelistedIPs.contains(clientIP);
        }
        return false;
    }

    /**
     * Checks if the IP-based MFA bypass flag is enabled for the client.
     * @param client the client model
     * @return true if the flag is enabled, false otherwise
     */
    private static boolean isIpBasedBypassMfaEnabled(ClientModel client) {
        return "true".equalsIgnoreCase(client.getAttribute("ip-based-bypass-mfa-flag-enabled"));
    }

    /**
     * Fetches the list of whitelisted IP addresses from the client's attributes.
     * @param client the client model
     * @return a list of whitelisted IP addresses, possibly empty
     */
    public static List<String> getWhitelistedIPs(ClientModel client) {
        String whitelist = client.getAttribute("whitelist_ips");
        if (whitelist != null && !whitelist.isEmpty()) {
            return Arrays.stream(whitelist.split(","))
                    .map(String::trim)
                    .toList();
        }
        return Collections.emptyList();
    }
}
