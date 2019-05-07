package uk.gov.ida.hub.config.domain;

import com.google.common.collect.ImmutableList;
import uk.gov.ida.hub.config.dto.FederationEntityType;

import javax.inject.Inject;
import java.util.Set;

public class EntityConfigDataToCertificateDtoTransformer {

    @Inject
    public EntityConfigDataToCertificateDtoTransformer() {
    }

    public ImmutableList<CertificateDetails> transform(Set<TransactionConfig> transactionConfigs, Set<MatchingServiceConfig> matchingServiceConfigs) {
        ImmutableList.Builder<CertificateDetails> builder = ImmutableList.builder();
        // IDP certs are now in the federation metadata and checked for expiry and OCSP status in separate sensu checks
        for (TransactionConfig transactionConfig : transactionConfigs) {
            for (Certificate certificate : transactionConfig.getSignatureVerificationCertificates()) {
                builder.add(new CertificateDetails(transactionConfig.getEntityId(), certificate, FederationEntityType.RP));
            }
            builder.add(new CertificateDetails(transactionConfig.getEntityId(), transactionConfig.getEncryptionCertificate(), FederationEntityType.RP));
        }
        for (MatchingServiceConfig matchingServiceConfig : matchingServiceConfigs) {
            for (Certificate certificate : matchingServiceConfig.getSignatureVerificationCertificates()) {
                builder.add(new CertificateDetails(matchingServiceConfig.getEntityId(), certificate, FederationEntityType.MS));

            }
            builder.add(new CertificateDetails(matchingServiceConfig.getEntityId(), matchingServiceConfig.getEncryptionCertificate(), FederationEntityType.MS));
        }
        return builder.build();
    }
}
