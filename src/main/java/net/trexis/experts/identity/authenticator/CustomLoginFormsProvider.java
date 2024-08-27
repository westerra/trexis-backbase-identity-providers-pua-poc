package net.trexis.experts.identity.authenticator;

import org.keycloak.forms.login.freemarker.FreeMarkerLoginFormsProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.theme.FreeMarkerUtil;

import javax.ws.rs.core.Response;

public class CustomLoginFormsProvider extends FreeMarkerLoginFormsProvider {

    public CustomLoginFormsProvider(KeycloakSession session, FreeMarkerUtil freeMarkerUtil) {
        // Pass both KeycloakSession and FreeMarkerUtil to the superclass constructor
        super(session, freeMarkerUtil);
    }

    // Method for handling form rendering
    @Override
    public Response createForm(String form) {
        // Access the client attributes
        addCustomMessagesToAttributes();

        // Proceed with the original behavior
        return super.createForm(form);
    }

    // Helper method to add custom messages to the Freemarker context
    private void addCustomMessagesToAttributes() {
        ClientModel client = session.getContext().getClient();

        // Retrieve the 'information_message' attribute
        String informationMessage = client.getAttribute("information_message");
        if (informationMessage != null) {
            attributes.put("informationMessage", informationMessage);
        }

        // Retrieve the 'maintenance_alert_message' attribute
        String maintenanceAlertMessage = client.getAttribute("maintenance_alert_message");
        if (maintenanceAlertMessage != null) {
            attributes.put("maintenanceAlertMessage", maintenanceAlertMessage);
        }
    }
}
