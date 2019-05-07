package uk.gov.ida.hub.config.application;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import uk.gov.ida.hub.config.data.ConfigRepository;
import uk.gov.ida.hub.config.domain.EncryptionCertificate;
import uk.gov.ida.hub.config.domain.MatchingServiceConfig;
import uk.gov.ida.hub.config.domain.SignatureVerificationCertificate;
import uk.gov.ida.hub.config.domain.TransactionConfig;

import javax.inject.Inject;
import java.net.URI;
import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class MatchingServiceAdapterService {

    private final ConfigRepository<TransactionConfig> transactionConfigRepository;
    private final ConfigRepository<MatchingServiceConfig> matchingServiceConfigRepository;

    @Inject
    public MatchingServiceAdapterService(
            ConfigRepository<TransactionConfig> transactionConfigRepository,
            ConfigRepository<MatchingServiceConfig> matchingServiceConfigRepository) {
        this.transactionConfigRepository = transactionConfigRepository;
        this.matchingServiceConfigRepository = matchingServiceConfigRepository;
    }

    public MatchingServicePerTransaction getMatchingService(String entityId) {
        MatchingServiceConfig matchingServiceConfig = matchingServiceConfigRepository.getData(entityId).get();
        return new MatchingServicePerTransaction(entityId, matchingServiceConfig);
    }

    public List<MatchingServicePerTransaction> getMatchingServices() {
        return transactionConfigRepository.getAllData().stream()
                .filter(transaction -> transaction.isUsingMatching())
                .map(transaction -> new MatchingServicePerTransaction(transaction.getEntityId(),
                        matchingServiceConfigRepository.getData(transaction.getMatchingServiceEntityId()).get()))
                .collect(toList());
    }

    public class MatchingServicePerTransaction {
        private String transactionEntityId;
        private MatchingServiceConfig matchingServiceConfig;

        public MatchingServicePerTransaction(String transactionEntityId, MatchingServiceConfig matchingServiceConfig) {
            this.transactionEntityId = transactionEntityId;
            this.matchingServiceConfig = matchingServiceConfig;
        }

        public String getTransactionEntityId() {
            return transactionEntityId;
        }

        public String getEntityId() {
            return matchingServiceConfig.getEntityId();
        }

        public EncryptionCertificate getEncryptionCertificate() {
            return matchingServiceConfig.getEncryptionCertificate();
        }

        public Collection<SignatureVerificationCertificate> getSignatureVerificationCertificates() {
            return matchingServiceConfig.getSignatureVerificationCertificates();
        }

        public URI getUri() {
            return matchingServiceConfig.getUri();
        }

        public URI getUserAccountCreationUri() {
            return matchingServiceConfig.getUserAccountCreationUri();
        }

        public Boolean getHealthCheckEnabled() {
            return matchingServiceConfig.getHealthCheckEnabled();
        }

        public boolean isOnboarding() {
            return matchingServiceConfig.getOnboarding();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;

            if (o == null || getClass() != o.getClass()) return false;

            MatchingServicePerTransaction that = (MatchingServicePerTransaction) o;

            return new EqualsBuilder()
                    .append(transactionEntityId, that.transactionEntityId)
                    .append(matchingServiceConfig.getEntityId(), that.matchingServiceConfig.getEntityId())
                    .append(matchingServiceConfig.getEncryptionCertificate(), that.matchingServiceConfig.getEncryptionCertificate())
                    .append(matchingServiceConfig.getSignatureVerificationCertificates(), that.matchingServiceConfig.getSignatureVerificationCertificates())
                    .append(matchingServiceConfig.getUri(), that.matchingServiceConfig.getUri())
                    .append(matchingServiceConfig.getUserAccountCreationUri(), that.matchingServiceConfig.getUserAccountCreationUri())
                    .append(matchingServiceConfig.getHealthCheckEnabled(), that.matchingServiceConfig.getHealthCheckEnabled())
                    .append(matchingServiceConfig.getOnboarding(), that.matchingServiceConfig.getOnboarding())
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37)
                    .append(transactionEntityId)
                    .append(matchingServiceConfig.getEntityId())
                    .append(matchingServiceConfig.getEncryptionCertificate())
                    .append(matchingServiceConfig.getSignatureVerificationCertificates())
                    .append(matchingServiceConfig.getUri())
                    .append(matchingServiceConfig.getUserAccountCreationUri())
                    .append(matchingServiceConfig.getHealthCheckEnabled())
                    .append(matchingServiceConfig.getOnboarding())
                    .toHashCode();
        }
    }

}
