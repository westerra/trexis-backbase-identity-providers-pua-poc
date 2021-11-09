package net.trexis.experts.identity.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OtpChoiceRepresentation {
    String addressId;
    String address;
    String channel;
    boolean selected;
}