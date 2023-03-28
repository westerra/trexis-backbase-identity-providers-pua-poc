package net.trexis.experts.identity.authenticator;

import net.trexis.experts.identity.spi.EnrollmentServiceProvider;
import net.trexis.experts.identity.spi.IngestionServiceProvider;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordForm;
import org.keycloak.services.ServicesLogger;

import javax.ws.rs.core.Response;

import static org.keycloak.authentication.AuthenticationFlowError.INVALID_CREDENTIALS;
import static org.keycloak.authentication.FlowStatus.SUCCESS;
import static org.keycloak.events.Errors.INVALID_USER_CREDENTIALS;
import static org.keycloak.events.Errors.USER_TEMPORARILY_DISABLED;
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
                if (!isLimited) {
                    log.info("User successfully logged in: " + user.getUsername() + ", rebase");
                    enrollmentServiceProvider.callRebaseService(user);

                    log.info("Starting ingestion: " + user.getUsername());
                    IngestionServiceProvider ingestionService = context.getSession().getProvider(IngestionServiceProvider.class);
                    ingestionService.callIngestionService(user);
                    log.info("Finished call to ingestion service");
                }
                context.success();
            }
        } else if(USER_TEMPORARILY_DISABLED.equalsIgnoreCase(context.getEvent().getEvent().getError())) {
            // We are setting message.summary == 'user_temporarily_disabled' So we can handle temporary user disabled case on UI, Else it will work as it is with default error case.
            Response challenge = context.form().setError(USER_TEMPORARILY_DISABLED).createLoginUsernamePassword();
            context.failure(org.keycloak.authentication.AuthenticationFlowError.USER_TEMPORARILY_DISABLED,challenge);
        }
    }
}
