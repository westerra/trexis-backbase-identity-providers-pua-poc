package net.trexis.experts.identity.authenticator;

import org.jboss.logging.Logger;
import org.keycloak.forms.login.freemarker.FreeMarkerLoginFormsProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.theme.FreeMarkerUtil;

import javax.ws.rs.core.Response;

public class CustomLoginFormsProvider extends FreeMarkerLoginFormsProvider {

    private static final Logger logger = Logger.getLogger(CustomLoginFormsProvider.class);

    public CustomLoginFormsProvider(KeycloakSession session, FreeMarkerUtil freeMarkerUtil) {
        super(session, freeMarkerUtil);
    }

    // Method for handling form rendering
    @Override
    public Response createForm(String form) {
        // Access the client attributes
        addCustomMessagesToAttributes();

        // Log the form being created
        logger.debugf("Creating form: %s", form);

        // Proceed with the original behavior
        return super.createForm(form);
    }

    // Helper method to add custom messages to the Freemarker context with logging
    private void addCustomMessagesToAttributes() {
        ClientModel client = session.getContext().getClient();

        // Log the client ID
        logger.debugf("Processing client ID: %s", client.getClientId());

        // Retrieve and log the 'information_message' attribute
        String informationMessage = client.getAttribute("information_message");
        if (informationMessage != null) {
            attributes.put("informationMessage", informationMessage);
            logger.debugf("Set 'informationMessage': %s", informationMessage);
        } else {
            logger.debug("No 'information_message' attribute found.");
        }

        // Retrieve and log the 'maintenance_alert_message' attribute
        String maintenanceAlertMessage = client.getAttribute("maintenance_alert_message");
        if (maintenanceAlertMessage != null) {
            attributes.put("maintenanceAlertMessage", maintenanceAlertMessage);
            logger.debugf("Set 'maintenanceAlertMessage': %s", maintenanceAlertMessage);
        } else {
            logger.debug("No 'maintenance_alert_message' attribute found.");
            attributes.put("maintenanceAlertMessage", ""); // Optional: Set a default value to avoid nulls
        }
    }
}
