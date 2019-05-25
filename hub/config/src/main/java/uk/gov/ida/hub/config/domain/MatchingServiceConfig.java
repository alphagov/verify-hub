package uk.gov.ida.hub.config.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.validation.ValidationMethod;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteCertificateConfig;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteMatchingServiceConfig;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchingServiceConfig implements EntityIdentifiable, CertificateConfigurable {

    @SuppressWarnings("unused") // needed to prevent guice injection
    protected MatchingServiceConfig() {
    }

    @Valid
    @NotNull
    @JsonProperty
    protected String entityId;

    @Valid
    @NotNull
    @JsonProperty
    protected X509CertificateConfiguration encryptionCertificate;

    @Valid
    @NotNull
    @JsonProperty
    protected List<X509CertificateConfiguration> signatureVerificationCertificates;

    @Valid
    @NotNull
    @JsonProperty
    protected URI uri;

    @Valid
    @JsonProperty
    protected URI userAccountCreationUri;

    @Valid
    @JsonProperty
    protected boolean healthCheckEnabled = true;

    @Valid
    @JsonProperty
    protected boolean onboarding = false;

    @Valid
    @JsonProperty
    protected boolean selfService = false;

    public String getEntityId() {
        return entityId;
    }

    public EncryptionCertificate getEncryptionCertificate() {
        return new EncryptionCertificate(encryptionCertificate);
    }

    public Collection<SignatureVerificationCertificate> getSignatureVerificationCertificates() {
        return signatureVerificationCertificates
                .stream()
                .map(SignatureVerificationCertificate::new)
                .collect(Collectors.toList());
    }

    public Boolean getHealthCheckEnabled() {
        return healthCheckEnabled;
    }

    @ValidationMethod(message = "Matching Service url must be an absolute url.")
    @SuppressWarnings("unused") // used by the deserializer
    private boolean isUriValid() {
        return uri.isAbsolute();
    }

    public URI getUri() {
        return uri;
    }

    public URI getUserAccountCreationUri() {
        return userAccountCreationUri;
    }

    public boolean isHealthCheckEnabled() {
        return healthCheckEnabled;
    }

    public boolean isOnboarding() {
        return onboarding;
    }

    public boolean isSelfService() {
        return selfService;
    }

    public MatchingServiceConfig override(RemoteMatchingServiceConfig remoteConfig){
        MatchingServiceConfig clone = new MatchingServiceConfig();
        clone.entityId = this.entityId;
        clone.encryptionCertificate = mapRemoteCertConfig(remoteConfig.getEncryptionCertificate());
        clone.signatureVerificationCertificates = mapRemoteCertConfig(remoteConfig.getSigningCertificates());
        clone.uri = this.uri;
        clone.userAccountCreationUri = this.userAccountCreationUri;
        clone.healthCheckEnabled = this.healthCheckEnabled;
        clone.onboarding = this.onboarding;
        clone.selfService = this.selfService;

        return clone;
    }

    private List<X509CertificateConfiguration> mapRemoteCertConfig(List<RemoteCertificateConfig> certificates) {
        return certificates.stream()
                .map(this::mapRemoteCertConfig)
                .collect(Collectors.toList());
    }

    private X509CertificateConfiguration mapRemoteCertConfig(RemoteCertificateConfig cert) {
        return new X509CertificateConfiguration(cert.getValue());
    }

}


