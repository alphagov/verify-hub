package uk.gov.ida.hub.config.application;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import uk.gov.ida.hub.config.data.ConfigEntityDataRepository;
import uk.gov.ida.hub.config.domain.EncryptionCertificate;
import uk.gov.ida.hub.config.domain.MatchingServiceConfigEntityData;
import uk.gov.ida.hub.config.domain.SignatureVerificationCertificate;
import uk.gov.ida.hub.config.domain.TransactionConfigEntityData;

import javax.inject.Inject;
import java.net.URI;
import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class MatchingServiceAdapterService {

    private final ConfigEntityDataRepository<TransactionConfigEntityData> transactionConfigEntityDataRepository;
    private final ConfigEntityDataRepository<MatchingServiceConfigEntityData> matchingServiceConfigEntityDataRepository;

    @Inject
    public MatchingServiceAdapterService(
            ConfigEntityDataRepository<TransactionConfigEntityData> transactionConfigEntityDataRepository,
            ConfigEntityDataRepository<MatchingServiceConfigEntityData> matchingServiceConfigEntityDataRepository) {
        this.transactionConfigEntityDataRepository = transactionConfigEntityDataRepository;
        this.matchingServiceConfigEntityDataRepository = matchingServiceConfigEntityDataRepository;
    }

    public MatchingServicePerTransaction getMatchingService(String entityId) {
        MatchingServiceConfigEntityData matchingServiceConfigEntityData = matchingServiceConfigEntityDataRepository.getData(entityId).get();
        return new MatchingServicePerTransaction(entityId, matchingServiceConfigEntityData);
    }

    public List<MatchingServicePerTransaction> getMatchingServices() {
        return transactionConfigEntityDataRepository.getAllData().stream()
                .map(transaction -> new MatchingServicePerTransaction(transaction.getEntityId(),
                        matchingServiceConfigEntityDataRepository.getData(transaction.getMatchingServiceEntityId()).get()))
                .collect(toList());
    }

    public class MatchingServicePerTransaction {
        private String transactionEntityId;
        private MatchingServiceConfigEntityData matchingServiceConfigEntityData;

        public MatchingServicePerTransaction(String transactionEntityId, MatchingServiceConfigEntityData matchingServiceConfigEntityData) {
            this.transactionEntityId = transactionEntityId;
            this.matchingServiceConfigEntityData = matchingServiceConfigEntityData;
        }

        public String getTransactionEntityId() {
            return transactionEntityId;
        }

        public String getEntityId() {
            return matchingServiceConfigEntityData.getEntityId();
        }

        public EncryptionCertificate getEncryptionCertificate() {
            return matchingServiceConfigEntityData.getEncryptionCertificate();
        }

        public Collection<SignatureVerificationCertificate> getSignatureVerificationCertificates() {
            return matchingServiceConfigEntityData.getSignatureVerificationCertificates();
        }

        public URI getUri() {
            return matchingServiceConfigEntityData.getUri();
        }

        public URI getUserAccountCreationUri() {
            return matchingServiceConfigEntityData.getUserAccountCreationUri();
        }

        public Boolean getHealthCheckEnabled() {
            return matchingServiceConfigEntityData.getHealthCheckEnabled();
        }

        public boolean isOnboarding() {
            return matchingServiceConfigEntityData.getOnboarding();
        }

        public Boolean getReadMetadataFromEntityId() {
            return matchingServiceConfigEntityData.getReadMetadataFromEntityId();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;

            if (o == null || getClass() != o.getClass()) return false;

            MatchingServicePerTransaction that = (MatchingServicePerTransaction) o;

            return new EqualsBuilder()
                    .append(transactionEntityId, that.transactionEntityId)
                    .append(matchingServiceConfigEntityData.getEntityId(), that.matchingServiceConfigEntityData.getEntityId())
                    .append(matchingServiceConfigEntityData.getEncryptionCertificate(), that.matchingServiceConfigEntityData.getEncryptionCertificate())
                    .append(matchingServiceConfigEntityData.getSignatureVerificationCertificates(), that.matchingServiceConfigEntityData.getSignatureVerificationCertificates())
                    .append(matchingServiceConfigEntityData.getUri(), that.matchingServiceConfigEntityData.getUri())
                    .append(matchingServiceConfigEntityData.getUserAccountCreationUri(), that.matchingServiceConfigEntityData.getUserAccountCreationUri())
                    .append(matchingServiceConfigEntityData.getHealthCheckEnabled(), that.matchingServiceConfigEntityData.getHealthCheckEnabled())
                    .append(matchingServiceConfigEntityData.getOnboarding(), that.matchingServiceConfigEntityData.getOnboarding())
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37)
                    .append(transactionEntityId)
                    .append(matchingServiceConfigEntityData.getEntityId())
                    .append(matchingServiceConfigEntityData.getEncryptionCertificate())
                    .append(matchingServiceConfigEntityData.getSignatureVerificationCertificates())
                    .append(matchingServiceConfigEntityData.getUri())
                    .append(matchingServiceConfigEntityData.getUserAccountCreationUri())
                    .append(matchingServiceConfigEntityData.getHealthCheckEnabled())
                    .append(matchingServiceConfigEntityData.getOnboarding())
                    .toHashCode();
        }
    }

}
