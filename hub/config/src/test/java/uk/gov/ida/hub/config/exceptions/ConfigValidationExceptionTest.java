package uk.gov.ida.hub.config.exceptions;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import uk.gov.ida.hub.config.domain.CertificateType;
import uk.gov.ida.hub.config.domain.IdentityProviderConfigEntityData;
import uk.gov.ida.hub.config.domain.TransactionConfigEntityData;
import uk.gov.ida.hub.config.dto.FederationEntityType;
import uk.gov.ida.hub.config.dto.InvalidCertificateDto;

import java.security.cert.CertPathValidatorException;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder.anIdentityProviderConfigData;
import static uk.gov.ida.hub.config.domain.builders.TransactionConfigEntityDataBuilder.aTransactionConfigData;

public class ConfigValidationExceptionTest {
    @Test
    public void createFileReadError() throws Exception {
        ConfigValidationException exception = ConfigValidationException.createFileReadError("/tmp/foo");
        assertThat(exception.getMessage()).isEqualTo("Error reading config service data from file '/tmp/foo'.");
    }

    @Test
    public void createDuplicateEntityIdException() throws Exception {
        ConfigValidationException exception = ConfigValidationException.createDuplicateEntityIdException("entity-id");
        assertThat(exception.getMessage()).isEqualTo("Duplicate entity id found: entity-id");
    }

    @Test
    public void createAbsentMatchingServiceConfigException() throws Exception {
        ConfigValidationException exception = ConfigValidationException.createAbsentMatchingServiceConfigException("msa-entity-id", "transaction-entity-id");
        assertThat(exception.getMessage()).isEqualTo("Matching service configuration for msa-entity-id not found, used by transaction transaction-entity-id");
    }

    @Test
    public void createAbsentOnboardingTransactionConfigException() throws Exception {
        ConfigValidationException exception = ConfigValidationException.createAbsentOnboardingTransactionConfigException("transaction-entity-id", "idp-entity-id");
        assertThat(exception.getMessage()).isEqualTo("IDP idp-entity-id has onboardingTransactionEntityId transaction-entity-id. No transaction with entityId transaction-entity-id exists.");
    }

    @Test
    public void createInvalidCertificatesException() throws Exception {
        InvalidCertificateDto invalidCertificateDto = new InvalidCertificateDto("entity-id", CertPathValidatorException.BasicReason.EXPIRED, CertificateType.ENCRYPTION, FederationEntityType.IDP, "description");
        ConfigValidationException exception = ConfigValidationException.createInvalidCertificatesException(asList(invalidCertificateDto));
        assertThat(exception.getMessage()).isEqualTo("Invalid certificate found.\n" +
                "Entity Id: entity-id\n" +
                "Certificate Type: ENCRYPTION\n" +
                "Federation Type: IDP\n" +
                "Reason: EXPIRED\n" +
                "Description: description");
    }

    @Test
    public void createIDPLevelsOfAssuranceUnsupportedException() throws Exception {
        ConfigValidationException exception = ConfigValidationException.createIDPLevelsOfAssuranceUnsupportedException(asList(anIdentityProviderConfigData().build()));
        assertThat(exception.getMessage()).isEqualTo("Unsupported level of assurance in IDP config.\nEntity Id: default-idp-entity-id\nLevels: [LEVEL_2]\n");
    }

    @Test
    public void createTransactionsRequireUnsupportedLevelOfAssurance() throws Exception {
        ConfigValidationException exception = ConfigValidationException.createTransactionsRequireUnsupportedLevelOfAssurance(asList(aTransactionConfigData().build()));
        assertThat(exception.getMessage()).isEqualTo("Unsupported level of assurance in transaction config.\n" +
                "Entity Id: default-transaction-entity-id\n" +
                "Levels of assurance: [LEVEL_1, LEVEL_2]\n");
    }

    @Test
    public void createIncompatiblePairsOfTransactionsAndIDPs() throws Exception {
        Map<TransactionConfigEntityData, List<IdentityProviderConfigEntityData>> incompatiblePairs = ImmutableMap.of(aTransactionConfigData().build(), asList(anIdentityProviderConfigData().build()));
        ConfigValidationException exception = ConfigValidationException.createIncompatiblePairsOfTransactionsAndIDPs(incompatiblePairs);
        assertThat(exception.getMessage()).isEqualTo("Transaction unsupported by IDP(s).\n" +
                "Transaction: default-transaction-entity-id\n" +
                "IDP(s): [default-idp-entity-id]\n");
    }

}