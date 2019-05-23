package uk.gov.ida.hub.config.domain.remoteconfig;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RemoteMatchingServiceConfig {

    @JsonProperty("entity_id")
    protected String entityId;

    @JsonProperty
    protected String name;

    @JsonProperty("encryption_certificate")
    protected RemoteCertificateConfig encryptionCertificate;

    @JsonProperty("signing_certificates")
    protected List<RemoteCertificateConfig> signingCertificates;


    @SuppressWarnings("unused")
    protected RemoteMatchingServiceConfig() {
    }

    public RemoteMatchingServiceConfig(String name, String entityId, RemoteCertificateConfig encryptionCertificate,
                                       List<RemoteCertificateConfig> signingCertificates) {
        this.name = name;
        this.entityId = entityId;
        this.encryptionCertificate = encryptionCertificate;
        this.signingCertificates = signingCertificates;
    }

    public String getEntityId(){
        return entityId;
    }

    public String getName() {
        return name;
    }

    public RemoteCertificateConfig getEncryptionCertificate() {
        return encryptionCertificate;
    }

    public List<RemoteCertificateConfig> getSigningCertificates() {
        return signingCertificates;
    }
}
