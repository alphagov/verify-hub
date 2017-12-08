package uk.gov.ida.hub.config.domain;

import com.google.common.collect.ImmutableList;
import uk.gov.ida.hub.config.dto.FederationEntityType;

import javax.inject.Inject;
import java.util.Set;

public class EntityConfigDataToCertificateDtoTransformer {

    @Inject
    public EntityConfigDataToCertificateDtoTransformer() {
    }

    public ImmutableList<CertificateDetails> transform(Set<TransactionConfigEntityData> transactionConfigEntityDatas, Set<MatchingServiceConfigEntityData> matchingServiceConfigEntityDatas) {
        ImmutableList.Builder<CertificateDetails> builder = ImmutableList.builder();
        // IDP certs are now in the federation metadata and checked for expiry and OCSP status in separate sensu checks
        for (TransactionConfigEntityData transactionConfigEntityData : transactionConfigEntityDatas) {
            for (Certificate certificate : transactionConfigEntityData.getSignatureVerificationCertificates()) {
                builder.add(new CertificateDetails(transactionConfigEntityData.getEntityId(), certificate, FederationEntityType.RP));
            }
            builder.add(new CertificateDetails(transactionConfigEntityData.getEntityId(), transactionConfigEntityData.getEncryptionCertificate(), FederationEntityType.RP));
        }
        for (MatchingServiceConfigEntityData matchingServiceConfigEntityData : matchingServiceConfigEntityDatas) {
            for (Certificate certificate : matchingServiceConfigEntityData.getSignatureVerificationCertificates()) {
                builder.add(new CertificateDetails(matchingServiceConfigEntityData.getEntityId(), certificate, FederationEntityType.MS));

            }
            builder.add(new CertificateDetails(matchingServiceConfigEntityData.getEntityId(), matchingServiceConfigEntityData.getEncryptionCertificate(), FederationEntityType.MS));
        }
        return builder.build();
    }
}
