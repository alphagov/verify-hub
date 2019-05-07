package uk.gov.ida.hub.config.domain.builders;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import uk.gov.ida.hub.config.domain.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static uk.gov.ida.hub.config.domain.builders.SignatureVerificationCertificateBuilder.aSignatureVerificationCertificate;

public class MatchingServiceConfigBuilder {
    private String entityId = "default-matching-service-entity-id";
    private EncryptionCertificate encryptionCertificate = new EncryptionCertificateBuilder().build();
    private List<SignatureVerificationCertificate> certificates = new ArrayList<>();
    private URI uri = URI.create("http://foo.bar/default-matching-service-uri");
    private boolean healthCheckEnabled;
    private URI userAccountCreationUri = URI.create("http://foo.bar/default-account-creation-uri");

    public static MatchingServiceConfigBuilder aMatchingServiceConfig() {
        return new MatchingServiceConfigBuilder();
    }

    public MatchingServiceConfig build() {
        if (certificates.isEmpty()) {
            certificates.add(aSignatureVerificationCertificate().build());
        }

        return new TestMatchingServiceConfig(
                entityId,
                encryptionCertificate,
                certificates,
                uri,
                userAccountCreationUri,
                healthCheckEnabled,
                false);
    }

    public MatchingServiceConfigBuilder withEntityId(String entityId) {
        this.entityId = entityId;
        return this;
    }

    public MatchingServiceConfigBuilder withEncryptionCertificate(EncryptionCertificate certificate) {
        this.encryptionCertificate = certificate;
        return this;
    }

    public MatchingServiceConfigBuilder addSignatureVerificationCertificate(SignatureVerificationCertificate certificate) {
        this.certificates.add(certificate);
        return this;
    }

    public MatchingServiceConfigBuilder withUri(URI uri) {
        this.uri = uri;
        return this;
    }

    public MatchingServiceConfigBuilder withHealthCheckEnabled() {
        this.healthCheckEnabled = true;
        return this;
    }

    public MatchingServiceConfigBuilder withHealthCheckDisabled() {
        this.healthCheckEnabled = false;
        return this;
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE)
    private static class TestMatchingServiceConfig extends MatchingServiceConfig {
        private TestMatchingServiceConfig(
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
