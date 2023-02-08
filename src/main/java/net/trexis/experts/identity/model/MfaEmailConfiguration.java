package net.trexis.experts.identity.model;

import lombok.Data;

import java.util.Map;

import static net.trexis.experts.identity.configuration.Constants.*;

@Data
public class MfaEmailConfiguration {

    public MfaEmailConfiguration() {
        Map<String,String> systemEnv = System.getenv();
        this.template = systemEnv.containsKey(MFA_EMAIL_TEMPLATE)?systemEnv.get(MFA_EMAIL_TEMPLATE):DEFAULT_MFA_EMAIL_TEMPLATE;
        this.footer = systemEnv.containsKey(MFA_EMAIL_FOOTER)?systemEnv.get(MFA_EMAIL_FOOTER):DEFAULT_MFA_EMAIL_FOOTER;
        this.message = systemEnv.containsKey(MFA_EMAIL_MESSAGE)?systemEnv.get(MFA_EMAIL_MESSAGE):DEFAULT_MFA_EMAIL_MESSAGE;
        this.subject = systemEnv.containsKey(MFA_EMAIL_SUBJECT)?systemEnv.get(MFA_EMAIL_SUBJECT):DEFAULT_MFA_EMAIL_SUBJECT;
        this.enabled = systemEnv.containsKey(MFA_EMAIL_ENABLED)?systemEnv.get(MFA_EMAIL_ENABLED):FALSE;
    }

    private String template;
    private String footer;
    private String message;
    private String subject;
    private String enabled;
    
}
