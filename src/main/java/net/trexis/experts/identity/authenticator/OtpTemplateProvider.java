package net.trexis.experts.identity.authenticator;

import com.backbase.identity.authenticators.otp.exception.OtpDeliveryException;
import com.backbase.identity.authenticators.otp.model.Content;
import com.backbase.identity.authenticators.otp.model.OtpChoice;
import org.keycloak.email.EmailException;
import org.keycloak.email.freemarker.FreeMarkerEmailTemplateProvider;
import org.keycloak.email.freemarker.beans.ProfileBean;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.theme.FreeMarkerUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OtpTemplateProvider extends com.backbase.identity.authenticators.otp.OtpTemplateProvider {

    public OtpTemplateProvider(KeycloakSession session, FreeMarkerUtil freeMarker) {
        super(session, freeMarker);
    }

    public List<Content> getContent(OtpChoice otpChoice, String otp, UserModel userModel) {
        Map<String, Object> otpAttributes = new HashMap();
        otpAttributes.put("user", new ProfileBean(userModel));
        otpAttributes.put("otp", otp);
        String contentBody = this.getMessageBody(otpChoice, otpAttributes);
        Content content = new Content();
        content.setContentId("0");
        content.setBody(contentBody);
        return Collections.singletonList(content);
    }

    private String getMessageBody(OtpChoice otpChoice, Map<String, Object> otpAttributes) {
        FreeMarkerEmailTemplateProvider.EmailTemplate emailTemplate = this.getEmailTemplate(otpChoice, otpAttributes);
        String contentBody = emailTemplate.getTextBody();
        if (contentBody == null) {
            throw new OtpDeliveryException("Could not process message template for channel " + otpChoice.getChannel());
        } else {
            return contentBody;
        }
    }

    private FreeMarkerEmailTemplateProvider.EmailTemplate getEmailTemplate(OtpChoice otpChoice, Map<String, Object> otpAttributes) {
        try {
            FreeMarkerEmailTemplateProvider.EmailTemplate emailTemplate = this.processTemplate("", Collections.emptyList(), String.format("communications-otp-email.ftl", otpChoice.getChannel()), otpAttributes);
            return emailTemplate;
        } catch (EmailException var5) {
            throw new OtpDeliveryException("Unable to generate OTP message", var5);
        }
    }
}
