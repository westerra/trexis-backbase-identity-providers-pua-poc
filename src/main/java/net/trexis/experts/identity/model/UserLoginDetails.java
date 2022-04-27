package net.trexis.experts.identity.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class UserLoginDetails {

    @SerializedName("ipAddress")
    private String ipAddress;

    // event time for example:- LOGIN time, LOGOUT time
    @SerializedName("time")
    private Long time;
}
