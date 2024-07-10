package net.trexis.experts.identity.util;

import lombok.experimental.UtilityClass;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.ClientModel;

import java.util.Arrays;
import java.util.List;

@UtilityClass
public class ChannelSelectorUtil {

    private static final boolean USE_ATTRIBUTE_WHITELIST = true; // Set this based on your configuration needs

    // Hardcoded whitelist IPs for fallback or default use
    private static final List<String> HARDCODED_WHITELISTED_IPS = Arrays.asList(
            "34.215.116.35", "34.215.234.87", "35.165.2.59",
            "34.214.37.223", "34.210.53.158", "52.89.52.36",
            "52.35.98.213", "52.36.72.121", "68.142.133.184",
            "209.236.107.68", "64.226.133.180"
    );

    public static String maskPhoneNumber(String tel) {
        return tel.length() < 4 ? tel :
                tel.substring(0, tel.length() - 4).replaceAll("\\d", "*") + tel.substring(tel.length() - 4);
    }


    public static boolean byPassMFAIfIpWhiteListed(AuthenticationFlowContext context) {
        // Obtain the client model and client's IP address
        ClientModel client = context.getAuthenticationSession().getClient();
        String clientIP = context.getConnection().getRemoteAddr();

        List<String> whitelistedIPs = getWhitelistedIPs(client);

        // Check if the client IP is in the whitelisted IPs
        return whitelistedIPs.contains(clientIP);
    }

    private static List<String> getWhitelistedIPs(ClientModel client) {
        if (USE_ATTRIBUTE_WHITELIST) {
            // Fetch the whitelist from the client's attributes
            String whitelist = client.getAttribute("whitelist_ips");
            if (whitelist != null && !whitelist.isEmpty()) {
                return Arrays.stream(whitelist.split(","))
                        .map(String::trim)
                        .toList();
            }
        }
        return HARDCODED_WHITELISTED_IPS; // Use hardcoded IPs if attribute is not set or empty
    }
}
