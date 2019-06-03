package uk.gov.ida.hub.config.domain.remoteconfig;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RemoteServiceProviderConfig implements RemoteComponentConfig {

    @JsonProperty
    private String name;

    @JsonProperty("encryption_certificate")
    private RemoteCertificateConfig encryptionCertificate;

    @JsonProperty("signing_certificates")
    private List<RemoteCertificateConfig> signingCertificates;

    @JsonProperty("id")
    private int id;

    @SuppressWarnings("unused")
    protected RemoteServiceProviderConfig() {
    }

    public RemoteServiceProviderConfig(String name, RemoteCertificateConfig encryptionCertificate,
                                       List<RemoteCertificateConfig> signingCertificates) {
        this.name = name;
        this.encryptionCertificate = encryptionCertificate;
        this.signingCertificates = signingCertificates;
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

    public int getId() {
        return id;
    }
}
