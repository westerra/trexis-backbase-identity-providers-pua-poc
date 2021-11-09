package net.trexis.experts.identity.util;

public class ChannelSelectorUtil
{
    public static String maskPhoneNumber(String tel) {
        if (tel.length() < 4) {
            return tel;
        }
        return tel.substring(0, tel.length() - 4).replaceAll("\\d", "*")
                + tel.substring(tel.length() - 4);
    }
}
