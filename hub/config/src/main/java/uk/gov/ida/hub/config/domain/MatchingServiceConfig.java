package uk.gov.ida.hub.config.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.dropwizard.validation.ValidationMethod;
import uk.gov.ida.hub.config.dto.FederationEntityType;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchingServiceConfig implements CertificateConfigurable<MatchingServiceConfig> {

    @Valid
    @NotNull
    @JsonProperty
    protected String entityId;

    protected String encryptionCertificate;

    protected List<String> signatureVerificationCertificates;

    @Valid
    @NotNull
    @JsonProperty
    protected URI uri;

    @Valid
    @JsonProperty
    protected URI userAccountCreationUri;

    @NotNull
    @JsonProperty
    protected boolean healthCheckEnabled = true;

    @Valid
    @JsonProperty
    protected boolean onboarding = false;

    @Valid
    @JsonProperty
    protected boolean selfService = false;

    @SuppressWarnings("unused") // needed to prevent guice injection
    protected MatchingServiceConfig() {
    }

    @JsonProperty
    protected void setEncryptionCertificate(JsonNode node){
        if (node.isTextual()){
            encryptionCertificate = node.textValue();
        }else{
            encryptionCertificate = node.get("x509").textValue();
        }
    }

    @JsonProperty
    protected void setSignatureVerificationCertificates(JsonNode node){
        signatureVerificationCertificates = new ArrayList<>();
        for (JsonNode n : node) {
            if (n.isTextual()){
                signatureVerificationCertificates.add(n.textValue());
            }else{
                signatureVerificationCertificates.add(n.get("x509").textValue());
            }
        }
    }

    public String getEntityId() {
        return entityId;
    }

    @Override
    public FederationEntityType getEntityType() {
        return FederationEntityType.MS;
    }

    public Certificate getEncryptionCertificate() {
        return new Certificate(this.entityId, getEntityType(), encryptionCertificate, CertificateType.ENCRYPTION, this.isEnabled());
    }

    public Collection<Certificate> getSignatureVerificationCertificates() {
        return signatureVerificationCertificates
                .stream()
                .map(svc -> new Certificate(this.entityId, getEntityType(), svc, CertificateType.SIGNING, this.isEnabled()))
                .collect(Collectors.toList());
    }

    public boolean getHealthCheckEnabled() {
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

    public boolean isSelfService() {
        return selfService;
    }

    @Override
    public MatchingServiceConfig override(List<String> signatureVerificationCertificateList, String encryptionCertificate) {
        MatchingServiceConfig clone = new MatchingServiceConfig();
        clone.entityId = this.entityId;
        clone.encryptionCertificate = encryptionCertificate;
        clone.signatureVerificationCertificates = signatureVerificationCertificates;
        clone.uri = this.uri;
        clone.userAccountCreationUri = this.userAccountCreationUri;
        clone.healthCheckEnabled = this.healthCheckEnabled;
        clone.onboarding = this.onboarding;
        clone.selfService = this.selfService;

        return clone;
    }
}
