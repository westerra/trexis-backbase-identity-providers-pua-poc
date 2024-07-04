package net.trexis.experts.identity.util;

import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.List;

@UtilityClass
public class ChannelSelectorUtil {

    private static final List<String> WHITELISTED_IPS = Arrays.asList(
            "34.215.116.35", "34.215.234.87", "35.165.2.59",
            "34.214.37.223", "34.210.53.158", "52.89.52.36",
            "52.35.98.213", "52.36.72.121", "64.226.133.180"
    );

    public static String maskPhoneNumber(String tel) {
        return tel.length() < 4 ? tel :
                tel.substring(0, tel.length() - 4).replaceAll("\\d", "*") + tel.substring(tel.length() - 4);
    }

    public static boolean isIpWhitelisted(String ip) {
        return WHITELISTED_IPS.contains(ip);
    }

}
