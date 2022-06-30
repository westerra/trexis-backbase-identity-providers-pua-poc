package net.trexis.experts.identity.model;

import lombok.Data;

import java.util.Map;

import static net.trexis.experts.identity.configuration.Constants.*;

@Data
public class EmailConfiguration {

    public EmailConfiguration() {
        Map<String,String> systemEnv = System.getenv();
        this.template = systemEnv.containsKey(TEMPLATE)?systemEnv.get(TEMPLATE):DEFAULT_TEMPLATE;
        this.footer = systemEnv.containsKey(EMAIL_FOOTER)?systemEnv.get(EMAIL_FOOTER):DEFAULT_EMAIL_FOOTER;
        this.message = systemEnv.containsKey(MESSAGE)?systemEnv.get(MESSAGE):DEFAULT_MESSAGE;
        this.subject = systemEnv.containsKey(EMAIL_SUBJECT)?systemEnv.get(EMAIL_SUBJECT):DEFAULT_EMAIL_SUBJECT;
        this.enabled = systemEnv.containsKey(UPDATE_PASSWORD_EMAIL)?systemEnv.get(UPDATE_PASSWORD_EMAIL):TRUE;
    }

    private String template;
    private String footer;
    private String message;
    private String subject;
    private String enabled;
    
}
