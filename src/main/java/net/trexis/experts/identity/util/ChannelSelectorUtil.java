package net.trexis.experts.identity.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ChannelSelectorUtil {

    public static String maskPhoneNumber(String tel) {
        return tel.length() < 4 ? tel :
                tel.substring(0, tel.length() - 4).replaceAll("\\d", "*") + tel.substring(tel.length() - 4);
    }
}
