package net.trexis.experts.identity.authenticator;

import org.keycloak.forms.login.freemarker.FreeMarkerLoginFormsProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.theme.FreeMarkerUtil;

import javax.ws.rs.core.Response;

public class CustomLoginFormsProvider extends FreeMarkerLoginFormsProvider {

    public CustomLoginFormsProvider(KeycloakSession session, FreeMarkerUtil freeMarkerUtil) {
        // Pass both KeycloakSession and FreeMarkerUtil to the superclass constructor
        super(session, freeMarkerUtil);
    }

    @Override
    public Response createResponse(UserModel.RequiredAction action) {
        // Access the client attributes
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

        // Proceed with the original behavior
        return super.createResponse(action);
    }

}
