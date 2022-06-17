package net.trexis.experts.identity.authenticator;

import freemarker.core.ParseException;
import freemarker.template.*;
import org.jboss.logging.Logger;
import org.keycloak.email.EmailException;
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

    public UpdatePasswordAdminEventListenerProvider(KeycloakSession keycloakSession) {
        this.keycloakSession = keycloakSession;
    }

    public void onEvent(Event event) {
        // Nothing to do
    }

    public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
        Map<String,String> systemEnv = System.getenv();
        if(systemEnv.containsKey(UPDATE_PASSWORD_EMAIL)?systemEnv.get(UPDATE_PASSWORD_EMAIL).equalsIgnoreCase(TRUE):true
                && adminEvent.getOperationType().equals(OperationType.ACTION) && adminEvent.getResourceType().equals(ResourceType.USER)) {

            String[] resourcePathArray = adminEvent.getResourcePath().split("/");
            if(resourcePathArray.length==3 && resourcePathArray[2].equalsIgnoreCase("reset-password")) {

                RealmModel realm = keycloakSession.realms().getRealm(adminEvent.getRealmId());
                var user = keycloakSession.users().getUserById(resourcePathArray[1], realm);
                if(user!=null & user.getEmail()!=null) {

                    org.keycloak.email.DefaultEmailSenderProvider senderProvider = new org.keycloak.email.DefaultEmailSenderProvider(keycloakSession);
                    try {
                        senderProvider.send(keycloakSession.getContext().getRealm().getSmtpConfig(), user, systemEnv.containsKey(EMAIL_SUBJECT)?systemEnv.get(EMAIL_SUBJECT):DEFAULT_EMAIL_SUBJECT, null,
                                getHtmlBody());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    } catch (MalformedTemplateNameException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (TemplateException e) {
                        e.printStackTrace();
                    } catch (EmailException e) {
                        e.printStackTrace();
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
        templateInput.put("emailSubject", systemEnv.containsKey(EMAIL_SUBJECT)?systemEnv.get(EMAIL_SUBJECT):DEFAULT_EMAIL_SUBJECT);
        templateInput.put("emailFooter", systemEnv.containsKey(EMAIL_FOOTER)?systemEnv.get(EMAIL_FOOTER):DEFAULT_EMAIL_FOOTER);
        templateInput.put("emailMessage", systemEnv.containsKey(MESSAGE)?systemEnv.get(MESSAGE):DEFAULT_MESSAGE);
        Configuration configuration =  new Configuration(new Version("2.3.23"));
        configuration.setClassForTemplateLoading(UpdatePasswordAdminEventListenerProvider.class, "/emails");
        configuration.setDefaultEncoding("UTF-8");
        Writer out = new StringWriter();
        Template template = configuration.getTemplate(systemEnv.containsKey(TEMPLATE)?systemEnv.get(TEMPLATE):DEFAULT_TEMPLATE);
        template.process(templateInput, out);
        return out.toString();
    }

    public void close() {
        // Nothing to do
    }
}
