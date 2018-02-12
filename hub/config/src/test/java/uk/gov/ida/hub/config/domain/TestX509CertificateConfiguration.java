package uk.gov.ida.hub.config.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

/* Used to allow X509CertificateConfiguration to serialize correctly
* when used in integration tests - see ConfigAppRule.java */
public class TestX509CertificateConfiguration extends X509CertificateConfiguration {
    @JsonProperty
    @SuppressWarnings("unused")
    private final String cert;

    public TestX509CertificateConfiguration(String cert) {
        super(cert);
        this.cert = cert;
    }
}
