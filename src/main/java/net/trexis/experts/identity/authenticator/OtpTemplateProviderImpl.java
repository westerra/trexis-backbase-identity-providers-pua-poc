package net.trexis.experts.identity.authenticator;

import com.backbase.identity.authenticators.otp.exception.OtpDeliveryException;
import com.backbase.identity.authenticators.otp.model.Content;
import com.backbase.identity.authenticators.otp.model.OtpChoice;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.keycloak.email.EmailException;
import org.keycloak.email.freemarker.FreeMarkerEmailTemplateProvider;
import org.keycloak.email.freemarker.beans.ProfileBean;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.theme.FreeMarkerUtil;

import static java.util.Optional.ofNullable;

public class OtpTemplateProviderImpl extends com.backbase.identity.authenticators.otp.OtpTemplateProvider {

    public OtpTemplateProviderImpl(KeycloakSession session, FreeMarkerUtil freeMarker) {
        super(session, freeMarker);
    }

    public List<Content> getContent(OtpChoice otpChoice, String otp, UserModel userModel) {
        Map<String, Object> otpAttributes = new HashMap<>();
        otpAttributes.put("user", new ProfileBean(userModel));
        otpAttributes.put("otp", otp);
        String contentBody = getMessageBody(otpChoice, otpAttributes);
        Content content = new Content();
        content.setContentId("0");
        content.setBody(contentBody);
        return Collections.singletonList(content);
    }

    private String getMessageBody(OtpChoice otpChoice, Map<String, Object> otpAttributes) {
        return ofNullable(getEmailTemplate(otpAttributes))
                .map(EmailTemplate::getTextBody)
                .orElseThrow(() -> new OtpDeliveryException("Could not process message template for channel " + otpChoice.getChannel()));
    }

    private FreeMarkerEmailTemplateProvider.EmailTemplate getEmailTemplate(Map<String, Object> otpAttributes) {
        try {
            return processTemplate("", Collections.emptyList(), "communications-otp-email.ftl", otpAttributes);
        } catch (EmailException var5) {
            throw new OtpDeliveryException("Unable to generate OTP message", var5);
        }
    }
}
