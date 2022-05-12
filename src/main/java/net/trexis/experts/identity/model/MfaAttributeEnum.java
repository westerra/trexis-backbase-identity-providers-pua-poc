package net.trexis.experts.identity.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MfaAttributeEnum {

    TRUE("true"),
    FALSE("false"),
    ALWAYS_TRUE("alwaysTrue"),
    ALWAYS_FALSE("alwaysFalse");

    private String value;

    MfaAttributeEnum(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @JsonCreator
    public static MfaAttributeEnum fromValue(String value) {
        for (MfaAttributeEnum b : MfaAttributeEnum.values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
}
