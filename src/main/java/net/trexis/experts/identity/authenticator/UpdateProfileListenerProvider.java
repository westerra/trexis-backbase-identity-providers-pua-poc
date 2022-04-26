package net.trexis.experts.identity.authenticator;

import com.finite.api.EntityApi;
import com.finite.api.model.ContactPoint;
import com.finite.api.model.ContactPoint.TypeEnum;
import com.finite.api.model.EntityProfile;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.keycloak.events.Details.PREVIOUS_EMAIL;
import static org.keycloak.events.Details.UPDATED_EMAIL;
import static org.keycloak.events.EventType.UPDATE_EMAIL;

public class UpdateProfileListenerProvider implements EventListenerProvider {

    private static final Logger log = Logger.getLogger(UpdateProfileListenerProvider.class);

    private KeycloakSession keycloakSession;
    private EntityApi entityApi;

    public UpdateProfileListenerProvider(KeycloakSession keycloakSession, EntityApi entityApi) {
        this.entityApi = entityApi;
        this.keycloakSession = keycloakSession;
        log.warn("Constructed UpdateProfileListenerProvider");
    }

    public void onEvent(Event event) {
        if (UPDATE_EMAIL == event.getType() && isNotBlank(event.getUserId())) {
            var previousEmail = event.getDetails().get(PREVIOUS_EMAIL);
            var updatedEmail = event.getDetails().get(UPDATED_EMAIL);

            log.info("Saving updated email to core");
            log.warn("previous email: " + previousEmail);
            log.warn("updated email: " + updatedEmail);

            RealmModel realm = keycloakSession.realms().getRealm(event.getRealmId());
            var user = keycloakSession.users().getUserById(event.getUserId(), realm);
            var entityId = user.getAttributes().get("entityId").stream().findFirst().orElseThrow();

            var contactPoint = new ContactPoint()
                    .name("Email")
                    .type(TypeEnum.EMAIL)
                    .value(updatedEmail);

            entityApi.putEntityProfile(entityId, new EntityProfile().addContactPointsItem(contactPoint), "trexis-backbase-identity-providers", null, false, false);
            log.warn("successfully saved email to core");
        }
    }

    public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
        // Nothing to do
    }

    public void close() {
        // Nothing to do
    }
}
