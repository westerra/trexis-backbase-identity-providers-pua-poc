package net.trexis.experts.identity.authenticator;

import freemarker.template.*;
import net.trexis.experts.identity.model.EmailConfiguration;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import static net.trexis.experts.identity.configuration.Constants.*;

public class UpdatePasswordAdminEventListenerProvider implements EventListenerProvider {

    private static final Logger log = Logger.getLogger(UpdatePasswordAdminEventListenerProvider.class);
    private final KeycloakSession keycloakSession;
    private final EmailConfiguration emailConfiguration;

    public UpdatePasswordAdminEventListenerProvider(KeycloakSession keycloakSession, EmailConfiguration emailConfiguration) {
        this.keycloakSession = keycloakSession;
        this.emailConfiguration = emailConfiguration;
    }

    public void onEvent(Event event) {
        // Nothing to do
    }

    public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
        Map<String,String> systemEnv = System.getenv();
        if(emailConfiguration.getEnabled().equalsIgnoreCase(TRUE) && OperationType.ACTION.equals(adminEvent.getOperationType())
                && ResourceType.USER.equals(adminEvent.getResourceType()) && adminEvent.getResourcePath()!=null) {

            String[] resourcePathArray = adminEvent.getResourcePath().split("/");
            if(resourcePathArray.length==3 && resourcePathArray[2].equalsIgnoreCase("reset-password")) {

                RealmModel realm = keycloakSession.realms().getRealm(adminEvent.getRealmId());
                var user = keycloakSession.users().getUserById(resourcePathArray[1], realm);
                if(user!=null & user.getEmail()!=null) {

                    org.keycloak.email.DefaultEmailSenderProvider senderProvider = new org.keycloak.email.DefaultEmailSenderProvider(keycloakSession);
                    try {
                        senderProvider.send(keycloakSession.getContext().getRealm().getSmtpConfig(), user, emailConfiguration.getSubject(), null,
                                getHtmlBody());
                    } catch (Exception e) {
                        log.error("Error sending email to {}",
                                user.getEmail(), e);
                    }
                } else {
                    log.info("User or User email not found while sending password update email for userId :" +resourcePathArray[1]);
                }
            }
        }
    }

    private String getHtmlBody() throws IOException, TemplateException {
        Map<String,String> systemEnv = System.getenv();
        Map<String, String> templateInput = new HashMap<>();
        templateInput.put("emailSubject", emailConfiguration.getSubject());
        templateInput.put("emailFooter", emailConfiguration.getFooter());
        templateInput.put("emailMessage", emailConfiguration.getMessage());
        Configuration configuration =  new Configuration(new Version("2.3.23"));
        configuration.setClassForTemplateLoading(UpdatePasswordAdminEventListenerProvider.class, "/emails");
        configuration.setDefaultEncoding("UTF-8");
        Writer out = new StringWriter();
        Template template = configuration.getTemplate(emailConfiguration.getTemplate());
        template.process(templateInput, out);
        return out.toString();
    }

    public void close() {
        // Nothing to do
    }
}
