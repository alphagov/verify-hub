package uk.gov.ida.hub.config.domain.builders;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import uk.gov.ida.hub.config.domain.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static uk.gov.ida.hub.config.domain.builders.AssertionConsumerServiceBuilder.anAssertionConsumerService;

public class TransactionConfigEntityDataBuilder {

    private String entityId = "default-transaction-entity-id";
    private String simpleId = "default-transaction-simple-id";
    private EncryptionCertificate encryptionCertificate = new EncryptionCertificateBuilder().build();
    private List<SignatureVerificationCertificate> signatureVerificationCertificates = new ArrayList<>();
    private String matchingServiceEntityId = "default-matching-service-entity-id";
    private List<AssertionConsumerService> assertionConsumerServices = new ArrayList<>();
    private List<UserAccountCreationAttribute> userAccountCreationAttributes = new ArrayList<>();
    private String displayName = "a default transaction display name";
    private MatchingProcess matchingProcess;
    private List<LevelOfAssurance> levelsOfAssurance = Arrays.asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2);
    private boolean enabled = true;
    private boolean shouldHubSignResponseMessages = true;
    private String otherWaysToCompleteTransaction = "default other ways to complete transaction";
    private String otherWaysDescription = "default other ways description";
    private URI serviceHomepage = URI.create("/service-uri");
    private String rpName = "Default RP name";
    private boolean eidasEnabled = false;

    public static TransactionConfigEntityDataBuilder aTransactionConfigData() {
        return new TransactionConfigEntityDataBuilder();
    }

    public TransactionConfigEntityData build() {
        if (signatureVerificationCertificates.isEmpty()) {
            signatureVerificationCertificates.add(new SignatureVerificationCertificateBuilder().build());
        }

        if (assertionConsumerServices.isEmpty()) {
            assertionConsumerServices.add(anAssertionConsumerService().isDefault(true).build());
        }

        return new TestTransactionConfigEntityData(
                entityId,
                simpleId,
                encryptionCertificate,
                signatureVerificationCertificates,
                matchingServiceEntityId,
                assertionConsumerServices,
                userAccountCreationAttributes,
                serviceHomepage,
                matchingProcess,
                enabled,
                eidasEnabled,
                shouldHubSignResponseMessages,
                levelsOfAssurance
        );
    }

    public TransactionConfigEntityDataBuilder withEncryptionCertificate(EncryptionCertificate certificate) {
        this.encryptionCertificate = certificate;
        return this;
    }

    public TransactionConfigEntityDataBuilder withEntityId(String entityId) {
        this.entityId = entityId;
        return this;
    }

    public TransactionConfigEntityDataBuilder withSimpleId(String simpleId) {
        this.simpleId = simpleId;
        return this;
    }

    public TransactionConfigEntityDataBuilder addSignatureVerificationCertificate(SignatureVerificationCertificate certificate) {
        this.signatureVerificationCertificates.add(certificate);
        return this;
    }

    public TransactionConfigEntityDataBuilder withMatchingServiceEntityId(String entityId) {
        this.matchingServiceEntityId = entityId;
        return this;
    }

    public TransactionConfigEntityDataBuilder addAssertionConsumerService(AssertionConsumerService assertionConsumerService) {
        this.assertionConsumerServices.add(assertionConsumerService);
        return this;
    }

    public TransactionConfigEntityDataBuilder addUserAccountCreationAttribute(UserAccountCreationAttribute userAccountCreationAttribute) {
        this.userAccountCreationAttributes.add(userAccountCreationAttribute);
        return this;
    }

    public TransactionConfigEntityDataBuilder withDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public TransactionConfigEntityDataBuilder withMatchingProcess(MatchingProcess matchingProcess) {
        this.matchingProcess = matchingProcess;
        return this;
    }

    public TransactionConfigEntityDataBuilder withEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public TransactionConfigEntityDataBuilder withEidasEnabled(boolean eidasEnabled) {
        this.eidasEnabled = eidasEnabled;
        return this;
    }

    public TransactionConfigEntityDataBuilder withOtherWaysToCompleteTransaction(final String otherWaysToCompleteTransaction) {
        this.otherWaysToCompleteTransaction = otherWaysToCompleteTransaction;
        return this;
    }

    public TransactionConfigEntityDataBuilder withLevelsOfAssurance(final List<LevelOfAssurance> levelsOfAssurance) {
        this.levelsOfAssurance = levelsOfAssurance;
        return this;
    }

    public TransactionConfigEntityDataBuilder withServiceHomepage(final URI serviceHomepage) {
        this.serviceHomepage = serviceHomepage;
        return this;
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE)
    private static class TestTransactionConfigEntityData extends TransactionConfigEntityData {

        private TestTransactionConfigEntityData(
                String entityId,
                String simpleId,
                EncryptionCertificate encryptionCertificate,
                List<SignatureVerificationCertificate> signatureVerificationCertificates,
                String matchingServiceEntityId,
                List<AssertionConsumerService> assertionConsumerServices,
                List<UserAccountCreationAttribute> userAccountCreationAttributes,
                URI serviceHomepage,
                MatchingProcess matchingProcess,
                boolean enabled,
                boolean eidasEnabled,
                boolean shouldHubSignResponseMessages,
                List<LevelOfAssurance> levelsOfAssurance) {
            this.serviceHomepage = serviceHomepage;
            this.entityId = entityId;
            this.simpleId = simpleId;
            this.encryptionCertificate = new TestX509CertificateConfiguration(encryptionCertificate.getX509());
            this.signatureVerificationCertificates = signatureVerificationCertificates.stream()
                    .map(cert -> new TestX509CertificateConfiguration(cert.getX509()))
                    .collect(Collectors.toList());
            this.matchingServiceEntityId = matchingServiceEntityId;
            this.assertionConsumerServices = assertionConsumerServices;
            this.userAccountCreationAttributes = userAccountCreationAttributes;
            this.matchingProcess = matchingProcess;
            this.enabled = enabled;
            this.eidasEnabled = eidasEnabled;
            this.shouldHubSignResponseMessages = shouldHubSignResponseMessages;
            this.levelsOfAssurance = levelsOfAssurance;
        }
    }
}
