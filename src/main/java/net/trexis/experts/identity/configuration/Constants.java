package net.trexis.experts.identity.configuration;

public class Constants {

    private Constants() {
        // to hide public constructor
    }

    public static final String OTP_CHOICE_ADDRESS_ID = "mfa.selector.choice.addressId";
    public static final String TRUE = "true";
    public static final String USER_ATTRIBUTE_MFA_REQUIRED = "mfaRequired";
    public static final String FALSE = "false";
    public static final String MESSAGE = "MESSAGE";
    public static final String EMAIL_SUBJECT = "EMAIL_SUBJECT";
    public static final String EMAIL_FOOTER = "EMAIL_FOOTER";
    public static final String TEMPLATE = "TEMPLATE";
    public static final String UPDATE_PASSWORD_EMAIL = "UPDATE_PASSWORD_EMAIL";
    public static final String DEFAULT_MESSAGE = "If you did not make this change or if you have any questions, please contact us immediately at our contact number (123-456-7890).<br><br>Please do not reply directly to this email as we will not receive your message.";
    public static final String DEFAULT_EMAIL_SUBJECT = "Alert: Digital Banking Password Changed";
    public static final String DEFAULT_EMAIL_FOOTER = "&copy;2022 By Your Bank.<br>All rights reserved.";
    public static final String DEFAULT_TEMPLATE = "sendContactUpdateEmail.ftl";

}
