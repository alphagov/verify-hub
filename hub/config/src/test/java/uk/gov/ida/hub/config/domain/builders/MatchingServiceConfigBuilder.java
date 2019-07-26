package uk.gov.ida.hub.config.domain.builders;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import uk.gov.ida.hub.config.domain.MatchingServiceConfig;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static uk.gov.ida.saml.core.test.TestCertificateStrings.HUB_TEST_PUBLIC_ENCRYPTION_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.HUB_TEST_PUBLIC_SIGNING_CERT;

public class MatchingServiceConfigBuilder {
    private String entityId = "default-matching-service-entity-id";
    private String encryptionCertificate = HUB_TEST_PUBLIC_ENCRYPTION_CERT;
    private List<String> certificates = new ArrayList<>();
    private URI uri = URI.create("http://foo.bar/default-matching-service-uri");
    private boolean healthCheckEnabled;
    private URI userAccountCreationUri = URI.create("http://foo.bar/default-account-creation-uri");
    private boolean selfService;

    public static MatchingServiceConfigBuilder aMatchingServiceConfig() {
        return new MatchingServiceConfigBuilder();
    }

    public MatchingServiceConfig build() {
        if (certificates.isEmpty()) {
            certificates.add(HUB_TEST_PUBLIC_SIGNING_CERT);
        }

        return new TestMatchingServiceConfig(
                entityId,
                encryptionCertificate,
                certificates,
                uri,
                userAccountCreationUri,
                healthCheckEnabled,
                false,
                selfService);
    }

    public MatchingServiceConfigBuilder withEntityId(String entityId) {
        this.entityId = entityId;
        return this;
    }

    public MatchingServiceConfigBuilder withEncryptionCertificate(String certificate) {
        this.encryptionCertificate = certificate;
        return this;
    }

    public MatchingServiceConfigBuilder addSignatureVerificationCertificate(String certificate) {
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

    public MatchingServiceConfigBuilder withSelfService(boolean selfService) {
        this.selfService = selfService;
        return this;
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE)
    private static class TestMatchingServiceConfig extends MatchingServiceConfig {
        private TestMatchingServiceConfig(
            String entityId,
            String encryptionCertificate,
            List<String> signatureVerificationCertificates,
            URI uri,
            URI userAccountCreationUri,
            boolean healthCheckEnabled,
            boolean onboarding,
            boolean selfService) {

            this.entityId = entityId;
            this.encryptionCertificate = encryptionCertificate;
            this.signatureVerificationCertificates = signatureVerificationCertificates;
            this.uri = uri;
            this.userAccountCreationUri = userAccountCreationUri;
            this.healthCheckEnabled = healthCheckEnabled;
            this.onboarding = onboarding;
            this.selfService = selfService;
        }
    }
}
