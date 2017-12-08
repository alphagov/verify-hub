package uk.gov.ida.hub.config.data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import uk.gov.ida.hub.config.domain.IdentityProviderConfigEntityData;
import uk.gov.ida.hub.config.domain.LevelOfAssurance;
import uk.gov.ida.hub.config.domain.TransactionConfigEntityData;
import uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder;
import uk.gov.ida.hub.config.domain.builders.TransactionConfigEntityDataBuilder;
import uk.gov.ida.hub.config.exceptions.ConfigValidationException;

import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.Assert.fail;

public class LevelsOfAssuranceConfigValidatorTest {

    private LevelsOfAssuranceConfigValidator levelsOfAssuranceConfigValidator;

    private final IdentityProviderConfigEntityData loa1And2Idp = IdentityProviderConfigDataBuilder.anIdentityProviderConfigData()
            .withSupportedLevelsOfAssurance(ImmutableList.of(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
            .build();
    private final IdentityProviderConfigEntityData loa1Idp = IdentityProviderConfigDataBuilder.anIdentityProviderConfigData()
            .withSupportedLevelsOfAssurance(ImmutableList.of(LevelOfAssurance.LEVEL_1))
            .build();
    private final IdentityProviderConfigEntityData loa2Idp = IdentityProviderConfigDataBuilder.anIdentityProviderConfigData()
            .withSupportedLevelsOfAssurance(ImmutableList.of(LevelOfAssurance.LEVEL_2))
            .build();
    private final IdentityProviderConfigEntityData loa1To3Idp = IdentityProviderConfigDataBuilder.anIdentityProviderConfigData()
            .withSupportedLevelsOfAssurance(ImmutableList.of(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2, LevelOfAssurance.LEVEL_3))
            .build();

    private final TransactionConfigEntityData loa1OnlyTransaction = TransactionConfigEntityDataBuilder.aTransactionConfigData()
            .withLevelsOfAssurance(asList(LevelOfAssurance.LEVEL_1))
            .build();
    private final TransactionConfigEntityData loa3OnlyTransaction = TransactionConfigEntityDataBuilder.aTransactionConfigData()
            .withLevelsOfAssurance(asList(LevelOfAssurance.LEVEL_3))
            .build();
    private final TransactionConfigEntityData loa2And1Transaction = TransactionConfigEntityDataBuilder.aTransactionConfigData()
            .withLevelsOfAssurance(asList(LevelOfAssurance.LEVEL_2, LevelOfAssurance.LEVEL_1))
            .build();
    private final TransactionConfigEntityData loa1And2Transaction = TransactionConfigEntityDataBuilder.aTransactionConfigData()
            .withLevelsOfAssurance(asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
            .build();
    final TransactionConfigEntityData loa2Transaction = TransactionConfigEntityDataBuilder.aTransactionConfigData()
            .withLevelsOfAssurance(asList(LevelOfAssurance.LEVEL_2))
            .build();

    @Before
    public void setUp() {
        levelsOfAssuranceConfigValidator = new LevelsOfAssuranceConfigValidator();
    }

    @Test
    public void checkIDPConfigIsWithinVerifyPolicyLevelsOfAssurance() {
        Set<IdentityProviderConfigEntityData> identityProviderConfig = ImmutableSet.of(loa1And2Idp);
        levelsOfAssuranceConfigValidator.validateAllIDPsSupportLOA1orLOA2(identityProviderConfig);
    }

    @Test
    public void shouldAllowSingleLevelOfAssurance() {
        Set<IdentityProviderConfigEntityData> identityProviderConfig = ImmutableSet.of(loa1Idp);
        levelsOfAssuranceConfigValidator.validateAllIDPsSupportLOA1orLOA2(identityProviderConfig);
    }

    @Test(expected = ConfigValidationException.class)
    public void checkValidationThrowsExceptionWhenIDPSupportsAnLOAThatIsOutsideVerifyPolicyLevelsOfAssurance() {
        Set<IdentityProviderConfigEntityData> identityProviderConfig = ImmutableSet.of(loa1To3Idp);
        levelsOfAssuranceConfigValidator.validateAllIDPsSupportLOA1orLOA2(identityProviderConfig);
    }

    @Test
    public void shouldAllowLOA1TransactionConfiguration() {
        Set<TransactionConfigEntityData> transactionsConfig = ImmutableSet.of(loa1And2Transaction);
        try {
            levelsOfAssuranceConfigValidator.validateAllTransactionsAreLOA1OrLOA2(transactionsConfig);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void shouldAllowLOA2TransactionConfiguration() {
        Set<TransactionConfigEntityData> transactionsConfig = ImmutableSet.of(loa2Transaction);
        try {
            levelsOfAssuranceConfigValidator.validateAllTransactionsAreLOA1OrLOA2(transactionsConfig);
        } catch(Exception e) {
            fail();
        }
    }

    @Test (expected = ConfigValidationException.class)
    public void shouldThrowWhenTransactionIsLoa1Only() {
        levelsOfAssuranceConfigValidator.validateAllTransactionsAreLOA1OrLOA2(ImmutableSet.of(loa1OnlyTransaction));
    }

    @Test (expected = ConfigValidationException.class)
    public void shouldThrowWhenTransactionIsNotLoa1OrLoa2() {
        levelsOfAssuranceConfigValidator.validateAllTransactionsAreLOA1OrLOA2(ImmutableSet.of(loa3OnlyTransaction));
    }

    @Test (expected = ConfigValidationException.class)
    public void shouldThrowWhenTransactionLOAsAreInWrongOrder() {
        levelsOfAssuranceConfigValidator.validateAllTransactionsAreLOA1OrLOA2(ImmutableSet.of(loa2And1Transaction));
    }

    @Test (expected = ConfigValidationException.class)
    public void shouldThrowWhenOneIdpDoesNotSupportATransaction(){
        Set<IdentityProviderConfigEntityData> identityProviderConfig = ImmutableSet.of(loa1Idp, loa2Idp);
        Set<TransactionConfigEntityData> transactionsConfig = ImmutableSet.of(loa2Transaction);

        levelsOfAssuranceConfigValidator.validateAllTransactionsAreSupportedByIDPs(identityProviderConfig, transactionsConfig);
    }

    @Test
    public void shouldNotThrowWhenAllIdpsSupportATransaction(){
        Set<IdentityProviderConfigEntityData> identityProviderConfig = ImmutableSet.of(loa2Idp, loa1And2Idp);
        Set<TransactionConfigEntityData> transactionsConfig = ImmutableSet.of(loa2Transaction);

        try {
            levelsOfAssuranceConfigValidator.validateAllTransactionsAreSupportedByIDPs(identityProviderConfig, transactionsConfig);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void shouldNotThrowWhenAnIdpsIsOnboardingAndDoesNotSupportATransaction(){
        Set<TransactionConfigEntityData> transactionsConfig = ImmutableSet.of(loa1And2Transaction);

        final IdentityProviderConfigEntityData onboardingIdp = IdentityProviderConfigDataBuilder.anIdentityProviderConfigData()
                .withOnboarding(ImmutableList.of("some-other-transaction-id"))
                .withSupportedLevelsOfAssurance(ImmutableList.of(LevelOfAssurance.LEVEL_3))
                .build();

        Set<IdentityProviderConfigEntityData> identityProviderConfig = ImmutableSet.of(onboardingIdp, loa1And2Idp);

        try {
            levelsOfAssuranceConfigValidator.validateAllTransactionsAreSupportedByIDPs(identityProviderConfig, transactionsConfig);
        } catch (Exception e) {
            fail();
        }
    }
}
