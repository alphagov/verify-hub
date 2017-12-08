package uk.gov.ida.hub.config.domain.builders;

import uk.gov.ida.common.shared.configuration.X509CertificateConfiguration;
import uk.gov.ida.hub.config.domain.EncryptionCertificate;
import uk.gov.ida.hub.config.domain.MatchingServiceConfigEntityData;
import uk.gov.ida.hub.config.domain.SignatureVerificationCertificate;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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

    private static class TestMatchingServiceConfigEntityData extends MatchingServiceConfigEntityData {
        private final EncryptionCertificate encCertificate;
        private final List<SignatureVerificationCertificate> sigCertificates;

        private TestMatchingServiceConfigEntityData(
            String entityId,
            EncryptionCertificate encryptionCertificate,
            List<SignatureVerificationCertificate> signatureVerificationCertificates,
            URI uri,
            URI userAccountCreationUri,
            boolean healthCheckEnabled,
            boolean onboarding) {

            this.entityId = entityId;
            this.encryptionCertificate = new X509CertificateConfiguration(null, "test", null);
            this.signatureVerificationCertificates = Collections.emptyList();
            this.uri = uri;
            this.userAccountCreationUri = userAccountCreationUri;
            this.healthCheckEnabled = healthCheckEnabled;
            this.onboarding = onboarding;

            this.encCertificate = encryptionCertificate;
            this.sigCertificates = signatureVerificationCertificates;
        }

        @Override
        public EncryptionCertificate getEncryptionCertificate() {
            return encCertificate;
        }

        @Override
        public Collection<SignatureVerificationCertificate> getSignatureVerificationCertificates() {
            return sigCertificates;
        }
    }
}
