package net.trexis.experts.identity.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnrollmentResult {

    private Boolean limited;

    @Override
    public String toString() {
        return "Result{" +
                "limited=" + limited +
                '}';
    }
}
