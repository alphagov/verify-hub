package uk.gov.ida.hub.config.data;

import org.junit.Before;
import org.junit.Test;
import uk.gov.ida.hub.config.domain.IdentityProviderConfig;
import uk.gov.ida.hub.config.domain.LevelOfAssurance;
import uk.gov.ida.hub.config.domain.TransactionConfig;
import uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder;
import uk.gov.ida.hub.config.domain.builders.TransactionConfigBuilder;
import uk.gov.ida.hub.config.exceptions.ConfigValidationException;

import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.fail;

public class LevelsOfAssuranceConfigValidatorTest {

    private LevelsOfAssuranceConfigValidator levelsOfAssuranceConfigValidator;

    private final IdentityProviderConfig loa1And2Idp = IdentityProviderConfigDataBuilder.anIdentityProviderConfigData()
            .withSupportedLevelsOfAssurance(List.of(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
            .build();
    private final IdentityProviderConfig loa1Idp = IdentityProviderConfigDataBuilder.anIdentityProviderConfigData()
            .withSupportedLevelsOfAssurance(List.of(LevelOfAssurance.LEVEL_1))
            .build();
    private final IdentityProviderConfig loa2Idp = IdentityProviderConfigDataBuilder.anIdentityProviderConfigData()
            .withSupportedLevelsOfAssurance(List.of(LevelOfAssurance.LEVEL_2))
            .build();
    private final IdentityProviderConfig loa1To3Idp = IdentityProviderConfigDataBuilder.anIdentityProviderConfigData()
            .withSupportedLevelsOfAssurance(List.of(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2, LevelOfAssurance.LEVEL_3))
            .build();

    private final TransactionConfig loa1OnlyTransaction = TransactionConfigBuilder.aTransactionConfigData()
            .withLevelsOfAssurance(singletonList(LevelOfAssurance.LEVEL_1))
            .build();
    private final TransactionConfig loa3OnlyTransaction = TransactionConfigBuilder.aTransactionConfigData()
            .withLevelsOfAssurance(singletonList(LevelOfAssurance.LEVEL_3))
            .build();
    private final TransactionConfig loa2And1Transaction = TransactionConfigBuilder.aTransactionConfigData()
            .withLevelsOfAssurance(asList(LevelOfAssurance.LEVEL_2, LevelOfAssurance.LEVEL_1))
            .build();
    private final TransactionConfig loa1And2Transaction = TransactionConfigBuilder.aTransactionConfigData()
            .withLevelsOfAssurance(asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
            .build();
    final TransactionConfig loa2Transaction = TransactionConfigBuilder.aTransactionConfigData()
            .withLevelsOfAssurance(singletonList(LevelOfAssurance.LEVEL_2))
            .build();

    @Before
    public void setUp() {
        levelsOfAssuranceConfigValidator = new LevelsOfAssuranceConfigValidator();
    }

    @Test
    public void checkIDPConfigIsWithinVerifyPolicyLevelsOfAssurance() {
        Set<IdentityProviderConfig> identityProviderConfig = Set.of(loa1And2Idp);
        levelsOfAssuranceConfigValidator.validateAllIDPsSupportLOA1orLOA2(identityProviderConfig);
    }

    @Test
    public void shouldAllowSingleLevelOfAssurance() {
        Set<IdentityProviderConfig> identityProviderConfig = Set.of(loa1Idp);
        levelsOfAssuranceConfigValidator.validateAllIDPsSupportLOA1orLOA2(identityProviderConfig);
    }

    @Test(expected = ConfigValidationException.class)
    public void checkValidationThrowsExceptionWhenIDPSupportsAnLOAThatIsOutsideVerifyPolicyLevelsOfAssurance() {
        Set<IdentityProviderConfig> identityProviderConfig = Set.of(loa1To3Idp);
        levelsOfAssuranceConfigValidator.validateAllIDPsSupportLOA1orLOA2(identityProviderConfig);
    }

    @Test
    public void shouldAllowLOA1TransactionConfiguration() {
        Set<TransactionConfig> transactionsConfig = Set.of(loa1And2Transaction);
        try {
            levelsOfAssuranceConfigValidator.validateAllTransactionsAreLOA1OrLOA2(transactionsConfig);
        } catch (Exception e) {
            fail("Expected exception not thrown");
        }
    }

    @Test
    public void shouldAllowLOA2AndLOA1TransactionConfiguration() {
        Set<TransactionConfig> transactionsConfig = Set.of(loa2And1Transaction);
        try {
            levelsOfAssuranceConfigValidator.validateAllTransactionsAreLOA1OrLOA2(transactionsConfig);
        } catch (Exception e) {
            fail("Expected exception not thrown");
        }
    }

    @Test
    public void shouldAllowLOA2TransactionConfiguration() {
        Set<TransactionConfig> transactionsConfig = Set.of(loa2Transaction);
        try {
            levelsOfAssuranceConfigValidator.validateAllTransactionsAreLOA1OrLOA2(transactionsConfig);
        } catch (Exception e) {
            fail("Expected exception not thrown");
        }
    }

    @Test(expected = ConfigValidationException.class)
    public void shouldThrowWhenTransactionIsLoa1Only() {
        levelsOfAssuranceConfigValidator.validateAllTransactionsAreLOA1OrLOA2(Set.of(loa1OnlyTransaction));
    }

    @Test(expected = ConfigValidationException.class)
    public void shouldThrowWhenTransactionIsNotLoa1OrLoa2() {
        levelsOfAssuranceConfigValidator.validateAllTransactionsAreLOA1OrLOA2(Set.of(loa3OnlyTransaction));
    }

    @Test(expected = ConfigValidationException.class)
    public void shouldThrowWhenOneIdpDoesNotSupportATransaction() {
        Set<IdentityProviderConfig> identityProviderConfig = Set.of(loa1Idp, loa2Idp);
        Set<TransactionConfig> transactionsConfig = Set.of(loa2Transaction);

        levelsOfAssuranceConfigValidator.validateAllTransactionsAreSupportedByIDPs(identityProviderConfig, transactionsConfig);
    }

    @Test
    public void shouldNotThrowWhenAllIdpsSupportATransaction() {
        Set<IdentityProviderConfig> identityProviderConfig = Set.of(loa2Idp, loa1And2Idp);
        Set<TransactionConfig> transactionsConfig = Set.of(loa2Transaction);

        try {
            levelsOfAssuranceConfigValidator.validateAllTransactionsAreSupportedByIDPs(identityProviderConfig, transactionsConfig);
        } catch (Exception e) {
            fail("Expected exception not thrown");
        }
    }

    @Test
    public void shouldNotThrowWhenAnIdpsIsOnboardingAndDoesNotSupportATransaction() {
        Set<TransactionConfig> transactionsConfig = Set.of(loa1And2Transaction);

        final IdentityProviderConfig onboardingIdp = IdentityProviderConfigDataBuilder.anIdentityProviderConfigData()
                .withOnboarding(List.of("some-other-transaction-id"))
                .withSupportedLevelsOfAssurance(List.of(LevelOfAssurance.LEVEL_3))
                .build();

        Set<IdentityProviderConfig> identityProviderConfig = Set.of(onboardingIdp, loa1And2Idp);

        try {
            levelsOfAssuranceConfigValidator.validateAllTransactionsAreSupportedByIDPs(identityProviderConfig, transactionsConfig);
        } catch (Exception e) {
            fail("Expected exception not thrown");
        }
    }
}
