package net.trexis.experts.identity.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.UserModel;

import com.backbase.identity.authenticators.otp.OtpAuthenticatorConfiguration;
import com.backbase.identity.authenticators.otp.exception.OtpDeliveryException;
import com.backbase.identity.authenticators.otp.model.OtpChannel;
import com.backbase.identity.authenticators.otp.model.OtpChoice;
import com.backbase.identity.authenticators.otp.model.Recipient;

/**
 * @author Trexis
 */
public class OtpChannelService {
  private static final Logger log = Logger.getLogger(OtpChannelService.class);

  private final OtpAuthenticatorConfiguration otpAuthenticatorConfiguration;

  public OtpChannelService(OtpAuthenticatorConfiguration otpAuthenticatorConfiguration) {
    this.otpAuthenticatorConfiguration = otpAuthenticatorConfiguration;
  }

  public List<OtpChoice> getAvailableOtpChoices(AuthenticationFlowContext context) {

    List<OtpChoice> availableChoices = new ArrayList<>();
    List<OtpChannel> otpChannels = otpAuthenticatorConfiguration.getOtpChannels();
    UserModel user = context.getUser();

    Iterator<OtpChannel> otpChannelIter = otpChannels.iterator();

    while (otpChannelIter.hasNext()) {
      OtpChannel otpChannel = otpChannelIter.next();
      log.debugv("OTP Channel: " + otpChannel.getChannel());
      Iterator<String> channelIdentityAttributesIter = otpChannel.getIdentityAttributes().keySet().iterator();

      while (channelIdentityAttributesIter.hasNext()) {
        String attribute = channelIdentityAttributesIter.next();
        log.debugv("Attribute: " + attribute);
        if (StringUtils.isNotBlank(attribute)) {
          List<OtpChoice> existingChoices = user.getAttributes().entrySet().stream()
              .filter(entry -> {
                log.debugv("Entry: " + entry.getKey().toLowerCase());
                return attribute.equalsIgnoreCase(entry.getKey().toLowerCase()) && !entry.getValue().isEmpty();
              })
              .map(entry -> buildOtpChoice(otpChannel, entry.getValue().get(0), otpChannel.getIdentityAttributes().get(attribute)))
              .collect(Collectors.toList());
          if (!existingChoices.isEmpty())
            availableChoices.add(existingChoices.get(0));
        }
      }
    }

    return availableChoices;
  }

  public List<Recipient> getRecipients(OtpChoice otpChoice, String otp, UserModel user) {
    OtpChannel selectedChannel = findMatchingChannel(otpChoice);
    Recipient recipient = new Recipient();
    recipient.setRef(user.getUsername());
    recipient.setFrom(selectedChannel.getFrom());
    recipient.setData(Collections.singletonMap("otp", otp));
    recipient.setContentId("0");
    String var10001 = otpChoice.getChannel();
    recipient.setTo(Collections.singletonList(var10001 + ":" + otpChoice.getAddress()));
    return Collections.singletonList(recipient);
 }

 private OtpChannel findMatchingChannel(OtpChoice otpChoice) {
    return otpAuthenticatorConfiguration.getOtpChannels().stream().filter(otpChannel ->
      channelMatchesChoice(otpChannel, otpChoice)
    ).findFirst().orElseThrow(() -> new OtpDeliveryException("Could not find matching channel"));
  }

  private boolean channelMatchesChoice(OtpChannel otpChannel, OtpChoice otpChoice) {
    return otpChannel.getIdentityAttributes().values().stream().anyMatch((choiceId) -> {
       return otpChoice.getAddressId().equals("id-" + choiceId);
    });
  }

  private OtpChoice buildOtpChoice(OtpChannel otpChannel, String address, String id) {
    OtpChoice otpChoice = new OtpChoice();
    otpChoice.setChannel(otpChannel.getChannel());
    otpChoice.setAddress(address);
    otpChoice.setAddressId(getAddressId(id));
    return otpChoice;
  }

  private String getAddressId(String id) {
    return "id-" + id;
  }
}
