package uk.gov.ida.hub.config.domain.builders;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.ida.hub.config.domain.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static uk.gov.ida.hub.config.domain.builders.SignatureVerificationCertificateBuilder.aSignatureVerificationCertificate;

public class MatchingServiceConfigEntityDataBuilder {
    private String entityId = "default-matching-service-entity-id";
    private EncryptionCertificate encryptionCertificate = new EncryptionCertificateBuilder().build();
    private List<SignatureVerificationCertificate> certificates = new ArrayList<>();
    private URI uri = URI.create("http://foo.bar/default-matching-service-uri");
    private boolean healthCheckEnabled;
    private URI userAccountCreationUri = URI.create("http://foo.bar/default-account-creation-uri");

    public static MatchingServiceConfigEntityDataBuilder aMatchingServiceConfigEntityData() {
        return new MatchingServiceConfigEntityDataBuilder();
    }

    public MatchingServiceConfigEntityData build() {
        if (certificates.isEmpty()) {
            certificates.add(aSignatureVerificationCertificate().build());
        }

        return new TestMatchingServiceConfigEntityData(
                entityId,
                encryptionCertificate,
                certificates,
                uri,
                userAccountCreationUri,
                healthCheckEnabled,
                false);
    }

    public MatchingServiceConfigEntityDataBuilder withEntityId(String entityId) {
        this.entityId = entityId;
        return this;
    }

    public MatchingServiceConfigEntityDataBuilder withEncryptionCertificate(EncryptionCertificate certificate) {
        this.encryptionCertificate = certificate;
        return this;
    }

    public MatchingServiceConfigEntityDataBuilder addSignatureVerificationCertificate(SignatureVerificationCertificate certificate) {
        this.certificates.add(certificate);
        return this;
    }

    public MatchingServiceConfigEntityDataBuilder withUri(URI uri) {
        this.uri = uri;
        return this;
    }

    public MatchingServiceConfigEntityDataBuilder withHealthCheckEnabled() {
        this.healthCheckEnabled = true;
        return this;
    }

    public MatchingServiceConfigEntityDataBuilder withHealthCheckDisabled() {
        this.healthCheckEnabled = false;
        return this;
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE)
    private static class TestMatchingServiceConfigEntityData extends MatchingServiceConfigEntityData {
        private TestMatchingServiceConfigEntityData(
            String entityId,
            EncryptionCertificate encryptionCertificate,
            List<SignatureVerificationCertificate> signatureVerificationCertificates,
            URI uri,
            URI userAccountCreationUri,
            boolean healthCheckEnabled,
            boolean onboarding) {

            this.entityId = entityId;
            this.encryptionCertificate = new TestX509CertificateConfiguration(encryptionCertificate.getX509());
            this.signatureVerificationCertificates = signatureVerificationCertificates.stream()
                .map(cert -> new TestX509CertificateConfiguration(cert.getX509()))
                .collect(Collectors.toList());
            this.uri = uri;
            this.userAccountCreationUri = userAccountCreationUri;
            this.healthCheckEnabled = healthCheckEnabled;
            this.onboarding = onboarding;
        }
    }
}
