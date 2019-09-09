package uk.gov.ida.hub.config.domain.filters;

import java.util.Optional;
import java.util.function.Predicate;

import org.junit.Before;
import org.junit.Test;
import uk.gov.ida.hub.config.domain.IdentityProviderConfig;
import uk.gov.ida.hub.config.domain.LevelOfAssurance;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class IdpPredicateFactoryTest {

    private IdpPredicateFactory idpPredicateFactory;
    private static final String TRANSACTION_ENTITY = "TRANSACTION_ENTITY";
    private static final LevelOfAssurance LEVEL_OF_ASSURANCE = LevelOfAssurance.LEVEL_1;

    @Before
    public void setUp() throws Exception {
        idpPredicateFactory = new IdpPredicateFactory();
    }

    @Test
    public void createPredicatesForTransactionEntityAndLoA_shouldNotIncludeExtraPredicate() throws Exception {
        Set<Predicate<IdentityProviderConfig>> predicates = idpPredicateFactory.createPredicatesForTransactionEntityAndLoa(TRANSACTION_ENTITY, LEVEL_OF_ASSURANCE);

        assertThat(predicates).hasSize(4);
        assertThat(predicates.stream().filter(predicate -> predicate instanceof EnabledIdpPredicate)).hasSize(1);
        assertThat(predicates.stream().filter(predicate -> predicate instanceof OnboardingIdpPredicate)).hasSize(1);
        assertThat(predicates.stream().filter(predicate -> predicate instanceof SupportedLoaIdpPredicate)).hasSize(1);
        assertThat(predicates.stream().filter(predicate -> predicate instanceof NewUserIdpPredicate)).hasSize(1);
    }

    @Test
    public void createPredicatesForTransactionEntity_shouldNotIncludeExtraPredicate() throws Exception {
        Set<Predicate<IdentityProviderConfig>> predicates = idpPredicateFactory.createPredicatesForTransactionEntity(Optional.of(TRANSACTION_ENTITY));

        assertThat(predicates).hasSize(2);
    }

    @Test
    public void createPredicatesForTransactionEntity_shouldIncludeEnabledPredicateWhenTransactionEntityIdIsProvided() throws Exception {
        Set<Predicate<IdentityProviderConfig>> predicates = idpPredicateFactory.createPredicatesForTransactionEntity(Optional.of(TRANSACTION_ENTITY));

        assertThat(predicates.stream().filter(predicate -> predicate instanceof EnabledIdpPredicate)).hasSize(1);
    }

    @Test
    public void createPredicatesForTransactionEntity_shouldIncludeEnabledPredicateWhenTransactionEntityIdIsNotProvided() throws Exception {
        Set<Predicate<IdentityProviderConfig>> predicates = idpPredicateFactory.createPredicatesForTransactionEntity(null);

        assertThat(predicates.stream().filter(predicate -> predicate instanceof EnabledIdpPredicate)).hasSize(1);
    }

    @Test
    public void createPredicatesForTransactionEntity_shouldIncludeTransactionEntityPredicateWhenTransactionEntityIdIsProvided() throws Exception {
        Set<Predicate<IdentityProviderConfig>> predicates =
                idpPredicateFactory.createPredicatesForTransactionEntity(Optional.of(TRANSACTION_ENTITY));

        Predicate<Predicate> findEnabled = input -> {
            if (!(input instanceof OnboardingForTransactionEntityPredicate)) {
                return false;
            }

            return ((OnboardingForTransactionEntityPredicate) input).getTransactionEntity().equals(TRANSACTION_ENTITY);
        };

        assertThat(predicates.stream().filter(findEnabled)).hasSize(1);
    }

    @Test
    public void createPredicatesForTransactionEntity_shouldNotIncludeTransactionEntityPredicateWhenTransactionEntityIdIsNotProvided() throws Exception {
        Set<Predicate<IdentityProviderConfig>> predicates = idpPredicateFactory.createPredicatesForTransactionEntity(null);

        Predicate<Predicate> findEnabled = input -> {
            if (!(input instanceof OnboardingForTransactionEntityPredicate)) {
                return false;
            }

            return ((OnboardingForTransactionEntityPredicate) input).getTransactionEntity().equals(TRANSACTION_ENTITY);
        };

        assertThat(predicates.stream().filter(findEnabled)).isEmpty();
    }
}
