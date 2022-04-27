package net.trexis.experts.identity.model;

public class EnrollmentResult {

    private Boolean limited;

    public Boolean getLimited() {
        return limited;
    }

    public void setLimited(Boolean limited) {
        this.limited = limited;
    }

    @Override
    public String toString() {
        return "Result{" +
                "limited=" + limited +
                '}';
    }
}
