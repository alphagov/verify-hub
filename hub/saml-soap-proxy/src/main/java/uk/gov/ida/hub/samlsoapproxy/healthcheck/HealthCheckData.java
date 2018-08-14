package uk.gov.ida.hub.samlsoapproxy.healthcheck;

import com.google.common.base.Optional;

class HealthCheckData {
    
    private static final String VERSION_DELIMITER = "version-";
    private static final String EIDAS_DELIMITER = "eidasenabled-";
    private static final String SHA_DELIMITER = "shouldsignwithsha1-";
    
    static HealthCheckData extractFrom(String responseId) {
        if (responseId != null && responseId.contains(VERSION_DELIMITER)) {
            String versionSubstring = responseId.trim().substring(responseId.indexOf(VERSION_DELIMITER) + VERSION_DELIMITER.length());

            String version;
            String eidasEnabled = null;
            String shouldSignWithSha1 = null;
            if (versionSubstring.contains(EIDAS_DELIMITER)) {
                version = versionSubstring.substring(0, versionSubstring.indexOf(EIDAS_DELIMITER) - 1);
                String eidasSubstring = versionSubstring.substring(versionSubstring.indexOf(EIDAS_DELIMITER) + EIDAS_DELIMITER.length());
                if (eidasSubstring.contains(SHA_DELIMITER)) {
                    eidasEnabled = eidasSubstring.substring(0, eidasSubstring.indexOf(SHA_DELIMITER) - 1);
                    shouldSignWithSha1 = eidasSubstring.substring(eidasSubstring.indexOf(SHA_DELIMITER) + SHA_DELIMITER.length());
                } else {
                    eidasEnabled = eidasSubstring;
                }
            } else {
                version = versionSubstring;
            }

            return new HealthCheckData(version, eidasEnabled, shouldSignWithSha1);
        }
        return new HealthCheckData();
    }

    private String version;
    private final String eidasEnabled;
    private final String shouldSignWithSha1;

    private HealthCheckData(String versionSubstring, String eidasEnabled, String shouldSignWithSha1) {
        this.version = versionSubstring;
        this.eidasEnabled = eidasEnabled;
        this.shouldSignWithSha1 = shouldSignWithSha1;
    }

    private HealthCheckData() {
        version = null;
        eidasEnabled = null;
        shouldSignWithSha1 = null;
    }

    Optional<String> getVersion() {
        return Optional.fromNullable(version);
    }

    Optional<String> getEidasEnabled() {
        return Optional.fromNullable(eidasEnabled);
    }

    Optional<String> getShouldSignWithSha1() {
        return Optional.fromNullable(shouldSignWithSha1);
    }
}
