package uk.gov.ida.hub.config.domain.remoteconfig;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RemoteServiceProviderConfig implements RemoteComponentConfig {

    @JsonProperty
    protected String id;

    @JsonProperty
    protected String name;

    @JsonProperty("encryption_certificate")
    protected RemoteCertificateConfig encryptionCertificate;

    @JsonProperty("signing_certificates")
    protected List<RemoteCertificateConfig> signingCertificates;

    @SuppressWarnings("unused")
    public RemoteServiceProviderConfig() {
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public RemoteCertificateConfig getEncryptionCertificateConfig() {
        return encryptionCertificate;
    }

    public List<RemoteCertificateConfig> getSigningCertificatesConfig() {
        return signingCertificates;
    }
}