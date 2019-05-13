package uk.gov.ida.hub.config.domain.remoteconfig;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

public class RemoteServiceProviderConfig {

    @Valid
    @NotNull
    @JsonProperty
    protected String name;

    @Valid
    @NotNull
    @JsonProperty
    protected RemoteCertificateConfig encryptionCertificate;

    @Valid
    @NotNull
    @JsonProperty
    protected List<RemoteCertificateConfig> signingCertificates;


    @SuppressWarnings("unused")
    protected RemoteServiceProviderConfig() {
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