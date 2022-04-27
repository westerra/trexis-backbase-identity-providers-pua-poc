package net.trexis.experts.identity.spi.impl;

import java.net.MalformedURLException;
import java.net.URI;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import net.trexis.experts.identity.model.IngestionResult;
import org.jboss.logging.Logger;

public class IngestionServiceClient {

    private static final Logger log = Logger.getLogger(IngestionServiceClient.class);
    private final WebTarget target;
    private final IngestionServiceProperties properties;

    public IngestionServiceClient(IngestionServiceProperties properties) {
        this.properties = properties;
        Client client = ClientBuilder.newClient();
        URI uri = UriBuilder
                .fromPath(properties.getBasepath())
                .host(properties.getHost())
                .port(properties.getPort())
                .scheme(properties.getScheme())
                .build();
        try {
            log.info("Path to ingestion: " + uri.toURL());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        target = client.target(uri.toString());
    }


    public void startIngestion(String userId) {
        log.info("Start ingestion process for user: " + userId);
        if (target != null) {
            log.info("Making a get call for user: ");
            IngestionResult result = null;
            try {
                result = target.path(properties.getIngestionPath() + "/" + userId)
                        .request(MediaType.APPLICATION_JSON)
                        .get(IngestionResult.class);
            } catch (Exception e) {
                log.error("There was an error calling ingestion service: " + e.getMessage());
            }
            if (result == null) {
                log.info("Result was null");
            }
            log.info("Result: " + result);
            // TODO Audit results
        }
    }
}
