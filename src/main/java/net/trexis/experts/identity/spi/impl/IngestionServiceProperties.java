package net.trexis.experts.identity.spi.impl;

public class IngestionServiceProperties {

    private String host;
    private int port;
    private String scheme;
    private String basepath;
    private String ingestionPath;

    public IngestionServiceProperties(String host, int port, String scheme, String basepath, String ingestionPath) {
        this.host = host;
        this.port = port;
        this.scheme = scheme;
        this.basepath = basepath;
        this.ingestionPath = ingestionPath;
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

    public String getIngestionPath() {
        return ingestionPath;
    }

    public void setIngestionPath(String ingestionPath) {
        this.ingestionPath = ingestionPath;
    }
}
