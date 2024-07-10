package net.trexis.experts.identity.util;

import lombok.experimental.UtilityClass;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.ClientModel;

import java.util.Arrays;
import java.util.List;

@UtilityClass
public class ChannelSelectorUtil {

    public static String maskPhoneNumber(String tel) {
        return tel.length() < 4 ? tel :
                tel.substring(0, tel.length() - 4).replaceAll("\\d", "*") + tel.substring(tel.length() - 4);
    }

    public static boolean byPassMFAIfIpWhiteListed(AuthenticationFlowContext context) {
        // Obtain the client model and client's IP address
        ClientModel client = context.getAuthenticationSession().getClient();
        String clientIP = context.getConnection().getRemoteAddr();

        // Fetch the whitelist from the client's attributes
        String whitelist = client.getAttribute("whitelist_ips");

        if (whitelist != null && !whitelist.isEmpty()) {
            // Split and trim the whitelist to avoid issues with spaces
            List<String> whitelistedIPs = Arrays.stream(whitelist.split(","))
                    .map(String::trim)
                    .toList();
            // Check if the client IP is in the whitelisted IPs
            return whitelistedIPs.contains(clientIP);
        }
        return false;
    }
}
