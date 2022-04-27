package net.trexis.experts.identity.spi.impl;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EnrollmentServiceProperties {

    private String host;
    private int port;
    private String scheme;
    private String basepath;
    private String rebasePath;
    private String evaluateLimitedPath;
}
