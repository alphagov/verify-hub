package uk.gov.ida.hub.config.exceptions;

import uk.gov.ida.hub.config.domain.IdentityProviderConfigEntityData;
import uk.gov.ida.hub.config.domain.TransactionConfigEntityData;
import uk.gov.ida.hub.config.dto.InvalidCertificateDto;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.text.MessageFormat.format;

public final class ConfigValidationException extends RuntimeException {

    private ConfigValidationException(String message) {
        super(message);
    }

    public static ConfigValidationException createFileReadError(String filePath) {
        return new ConfigValidationException(format("Error reading config service data from file ''{0}''.", filePath));
    }

    public static ConfigValidationException createDuplicateEntityIdException(String entityId) {
        return new ConfigValidationException(format("Duplicate entity id found: {0}", entityId));
    }

    public static ConfigValidationException createMissingMatchingEntityIdException(String transactionEntityId) {
        return new ConfigValidationException(format("Matching entity id not found for transaction {0}", transactionEntityId));
    }

    public static ConfigValidationException createAbsentMatchingServiceConfigException(String matchingServiceEntityId, String transactionEntityId) {
        return new ConfigValidationException(format("Matching service configuration for {0} not found, used by transaction {1}", matchingServiceEntityId, transactionEntityId));
    }

    public static ConfigValidationException createAbsentOnboardingTransactionConfigException(String transactionEntityId, String idpEntityId) {
        return new ConfigValidationException(format("IDP {0} has onboardingTransactionEntityId {1}. No transaction with entityId {1} exists.", idpEntityId, transactionEntityId));
    }

    public static ConfigValidationException createInvalidCertificatesException(List<InvalidCertificateDto> invalidCertificates) {
        return new ConfigValidationException(invalidCertificates.stream().map(certificate -> MessageFormat.format(
                "Invalid certificate found.\nEntity Id: {0}\nCertificate Type: {1}\nFederation Type: {2}\nReason: {3}\nDescription: {4}",
                certificate.getEntityId(),
                certificate.getCertificateType(),
                certificate.getFederationType(),
                certificate.getReason(),
                certificate.getDescription())).collect(Collectors.joining("\n")));
    }

    public static ConfigValidationException createIDPLevelsOfAssuranceUnsupportedException(Collection<IdentityProviderConfigEntityData> badIDPConfigs) {
        return new ConfigValidationException(badIDPConfigs.stream().map(identityProviderConfigEntityData -> MessageFormat.format(
                "Unsupported level of assurance in IDP config.\nEntity Id: {0}\nLevels: {1}\n",
                identityProviderConfigEntityData.getEntityId(),
                identityProviderConfigEntityData.getSupportedLevelsOfAssurance()))
                .collect(Collectors.joining("\n")));
    }

    public static ConfigValidationException createTransactionsRequireUnsupportedLevelOfAssurance(Collection<TransactionConfigEntityData> badTransactionConfigs) {
        return new ConfigValidationException(badTransactionConfigs.stream()
                .map(entityData -> MessageFormat.format(
                        "Unsupported level of assurance in transaction config.\nEntity Id: {0}\nLevels of assurance: {1}\n",
                        entityData.getEntityId(),
                        entityData.getLevelsOfAssurance()))
                .collect(Collectors.joining("\n")));
    }

    public static ConfigValidationException createIncompatiblePairsOfTransactionsAndIDPs(Map<TransactionConfigEntityData, List<IdentityProviderConfigEntityData>> incompatibleIdps) {
        return new ConfigValidationException(incompatibleIdps.keySet().stream().map(transactionEntity -> MessageFormat.format(
                "Transaction unsupported by IDP(s).\nTransaction: {0}\nIDP(s): {1}\n",
                transactionEntity.getEntityId(),
                incompatibleIdps.get(transactionEntity).stream().map(IdentityProviderConfigEntityData::getEntityId).collect(Collectors.toList()))
        ).collect(Collectors.joining("\n")));
    }
}
