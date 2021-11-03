package net.trexis.experts.identity.model;

import java.util.List;

public class IngestionResult {

    private Boolean success;
    private List<String> messages;

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }

    @Override
    public String toString() {
        return "Result{" +
                "success=" + success +
                ", messages=" + messages +
                '}';
    }
}
