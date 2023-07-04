package net.trexis.experts.identity.authenticator;

import com.finite.api.EntityApi;
import com.finite.api.model.ContactPoint;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import static com.finite.api.model.ContactPoint.TypeEnum.EMAIL;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.keycloak.events.Details.PREVIOUS_EMAIL;
import static org.keycloak.events.Details.UPDATED_EMAIL;
import static org.keycloak.events.EventType.UPDATE_EMAIL;

public class UpdateProfileListenerProvider implements EventListenerProvider {

    private static final Logger log = Logger.getLogger(UpdateProfileListenerProvider.class);
    private static final String X_TRACE_ID = "trexis-backbase-identity-providers";

    private final KeycloakSession keycloakSession;
    private final EntityApi entityApi;
    private final String entityIdentifierClaim;
    private final String primaryEmailName;

    public UpdateProfileListenerProvider(KeycloakSession keycloakSession, EntityApi entityApi,
                                         String entityIdentifierClaim, String primaryEmailName) {
        this.entityApi = entityApi;
        this.keycloakSession = keycloakSession;
        this.entityIdentifierClaim = entityIdentifierClaim;
        this.primaryEmailName = primaryEmailName;
    }

    public void onEvent(Event event) {
        if (UPDATE_EMAIL == event.getType() && isNotBlank(event.getUserId())) {
            persistEmailToCore(event);
        }
    }

    private void persistEmailToCore(Event event) {
        var previousEmail = event.getDetails().get(PREVIOUS_EMAIL);
        var updatedEmail = event.getDetails().get(UPDATED_EMAIL);
        log.debug("Updating email: " + previousEmail + " -> " + updatedEmail);

        RealmModel realm = keycloakSession.realms().getRealm(event.getRealmId());
        var user = keycloakSession.users().getUserById(event.getUserId(), realm);
        user.getAttributes().get(entityIdentifierClaim).stream()
                .findAny()
                .ifPresent(entityId ->  {
                    var contactPoint = new ContactPoint()
                            .name(primaryEmailName)
                            .type(EMAIL)
                            .value(updatedEmail);

                    var existingEntityProfile = entityApi.getEntityProfile(entityId, null, false, false, X_TRACE_ID, null);

                    var updatedContactPoints = existingEntityProfile.getContactPoints().stream()
                            .map(existingContactPoint -> {
                                if (existingContactPoint.getType() == EMAIL && primaryEmailName.equalsIgnoreCase(existingContactPoint.getName())) {
                                    existingContactPoint.setValue(updatedEmail);
                                }
                                return existingContactPoint;
                            })
                            .collect(Collectors.toList());
                    existingEntityProfile.setContactPoints(updatedContactPoints);

                    entityApi.putEntityProfile(entityId, existingEntityProfile, X_TRACE_ID, null, false, false);
                    log.info("successfully saved email to core");
                });
    }

    public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
        // Can be implemented to look at admin events,
        // such as manually updating user profile in identity console
    }

    public void close() {
        // Nothing to do
    }
}