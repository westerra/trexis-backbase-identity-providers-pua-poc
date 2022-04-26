package net.trexis.experts.identity.authenticator;

import net.trexis.experts.identity.spi.EnrollmentServiceProvider;
import net.trexis.experts.identity.spi.IngestionServiceProvider;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordForm;
import org.keycloak.services.ServicesLogger;

import static org.keycloak.authentication.FlowStatus.SUCCESS;
import static org.keycloak.services.ServicesLogger.LOGGER;

public class LoginIngestionUsernamePasswordForm extends UsernamePasswordForm {

    protected static ServicesLogger log;

    static {
        log = LOGGER;
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        super.action(context);

        if (SUCCESS == context.getStatus()) {
            log.info("Authentication is successful");
            var user = context.getUser();
            if (user != null) {
                log.info("User successfully logged in: " + user.getUsername() + ", evaluate limited");
                EnrollmentServiceProvider enrollmentServiceProvider = context.getSession().getProvider(EnrollmentServiceProvider.class);
                log.info("Enrollment service provider: " + enrollmentServiceProvider);
                boolean isLimited = enrollmentServiceProvider.callEvaluateLimitedService(user);
                if(!isLimited){
                    log.info("User successfully logged in: " + user.getUsername() + ", rebase");
                    enrollmentServiceProvider.callRebaseService(user);

                    log.info("Starting ingestion: " + user.getUsername());
                    IngestionServiceProvider ingestionService = context.getSession().getProvider(IngestionServiceProvider.class);
                    ingestionService.callIngestionService(user);
                    log.info("Finished call to ingestion service");
                }
                context.success();
            }
        }
    }
}
