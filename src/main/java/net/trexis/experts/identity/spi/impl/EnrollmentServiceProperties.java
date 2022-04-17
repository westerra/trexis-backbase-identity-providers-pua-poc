package net.trexis.experts.identity.spi.impl;

public class EnrollmentServiceProperties {

    private String host;
    private int port;
    private String scheme;
    private String basepath;
    private String rebasePath;
    private String evaluateLimitedPath;

    public EnrollmentServiceProperties(String host, int port, String scheme, String basepath, String rebasePath, String evaluateLimitedPath) {
        this.host = host;
        this.port = port;
        this.scheme = scheme;
        this.basepath = basepath;
        this.rebasePath = rebasePath;
        this.evaluateLimitedPath = evaluateLimitedPath;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getBasepath() {
        return basepath;
    }

    public void setBasepath(String basepath) {
        this.basepath = basepath;
    }

    public String getRebasePath() {
        return rebasePath;
    }

    public void setRebasePath(String rebasePath) {
        this.rebasePath = rebasePath;
    }

    public String getEvaluateLimitedPath() {
        return evaluateLimitedPath;
    }

    public void setEvaluatePath(String evaluateLimitedPath) {
        this.evaluateLimitedPath = evaluateLimitedPath;
    }
}
