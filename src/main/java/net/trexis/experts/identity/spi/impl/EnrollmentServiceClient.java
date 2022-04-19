package net.trexis.experts.identity.spi.impl;

import net.trexis.experts.identity.model.EnrollmentResult;
import org.jboss.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.MalformedURLException;
import java.net.URI;

public class EnrollmentServiceClient {

    private static final Logger log = Logger.getLogger(EnrollmentServiceClient.class);
    private WebTarget target;
    private EnrollmentServiceProperties properties;

    public EnrollmentServiceClient(EnrollmentServiceProperties properties) {
        this.properties = properties;
        Client client = ClientBuilder.newClient();
        URI uri = UriBuilder
                .fromPath(properties.getBasepath())
                .host(properties.getHost())
                .port(properties.getPort())
                .scheme(properties.getScheme())
                .build();
        try {
            log.info("Path to enrollment: "  + uri.toURL().toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        target = client.target(uri.toString());
    }


    public void startRebase(String userId) {
        log.info("Start rebase process for user: " + userId);
        if (target != null) {
            log.info("Making a get call for user: ");
            Response response = null;
            try {
                response = target.path(properties.getRebasePath() + "/" + userId)
                        .request(MediaType.APPLICATION_JSON)
                        .put(Entity.entity(null, MediaType.APPLICATION_JSON));
            } catch (Exception e) {
                log.error("There was an error calling enrollment service: " + e.getMessage());
            }
            if (response == null || response.getStatus()!=200) {
                log.info("Unexpected response: " + response);
            }
            log.info("Response: " + response.getEntity());
        }
    }

    //Return true if user is limited
    public boolean evaluateLimited(String userId) {
        log.info("Start evaluate limited process for user: " + userId);
        if (target != null) {
            log.info("Making a get call for user: ");
            Response response = null;
            try {
                response = target.path(properties.getEvaluateLimitedPath() + "/" + userId)
                        .request(MediaType.APPLICATION_JSON)
                        .put(Entity.entity(null, MediaType.APPLICATION_JSON));
            } catch (Exception e) {
                log.error("There was an error calling enrollment service: " + e.getMessage());
            }
            if (response == null || response.getStatus()!=200) {
                log.info("Unexpected response: " + response);
                return true;
            } else {
                log.info("Response: " + response.getEntity());
                try{
                    EnrollmentResult enrollmentResult = response.readEntity(EnrollmentResult.class);
                    log.info("User limited state: " + enrollmentResult.getLimited());
                    return enrollmentResult.getLimited();
                } catch (Exception ex){
                    log.error("Unable to cast response to enrollment result");
                    log.info(response.readEntity(String.class));
                    return true;
                }

            }
        } else {
            return true;
        }
    }
}
