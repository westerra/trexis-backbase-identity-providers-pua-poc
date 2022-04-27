package net.trexis.experts.identity.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OtpChoiceRepresentation {

    private String addressId;
    private String address;
    private String channel;
    private boolean selected;
}