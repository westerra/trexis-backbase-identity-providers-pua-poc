package net.trexis.experts.identity.model;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IngestionResult {

    private Boolean success;
    private List<String> messages;

    @Override
    public String toString() {
        return "Result{" +
                "success=" + success +
                ", messages=" + messages +
                '}';
    }
}
