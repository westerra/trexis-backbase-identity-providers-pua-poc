package net.trexis.experts.identity.spi.impl;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class IngestionServiceProperties {

    private String host;
    private int port;
    private String scheme;
    private String basepath;
    private String ingestionPath;

}