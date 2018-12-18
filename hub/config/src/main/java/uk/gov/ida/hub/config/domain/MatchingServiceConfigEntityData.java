package uk.gov.ida.hub.config.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.validation.ValidationMethod;
import uk.gov.ida.hub.config.CertificateEntity;
import uk.gov.ida.hub.config.ConfigEntityData;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchingServiceConfigEntityData implements ConfigEntityData, CertificateEntity {

    @SuppressWarnings("unused") // needed to prevent guice injection
    protected MatchingServiceConfigEntityData() {
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

    @NotNull
    @JsonProperty
    protected Boolean healthCheckEnabled;

    @JsonProperty
    protected boolean onboarding;

    @NotNull
    @Valid
    @JsonProperty
    protected boolean readMetadataFromEntityId = false;

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

    public boolean getOnboarding() {
        return onboarding;
    }

    public URI getUserAccountCreationUri() {
        return userAccountCreationUri;
    }

    public Boolean getReadMetadataFromEntityId() {
        return readMetadataFromEntityId;
    }

}
