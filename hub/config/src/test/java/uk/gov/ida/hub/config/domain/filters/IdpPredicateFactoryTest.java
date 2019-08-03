package uk.gov.ida.hub.config.domain.filters;

import java.util.Optional;
import java.util.function.Predicate;

import com.google.common.collect.Collections2;
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

        Predicate<Predicate> findEnabled = input -> input instanceof EnabledIdpPredicate;
        Predicate<Predicate> findOnboarding = input -> input instanceof OnboardingIdpPredicate;
        Predicate<Predicate> supportedLoa = input -> input instanceof SupportedLoaIdpPredicate;
        Predicate<Predicate> findNewUserIdp = input -> input instanceof NewUserIdpPredicate;

        assertThat(predicates).hasSize(4);
        assertThat(Collections2.filter(predicates, findEnabled::test)).hasSize(1);
        assertThat(Collections2.filter(predicates, findOnboarding::test)).hasSize(1);
        assertThat(Collections2.filter(predicates, supportedLoa::test)).hasSize(1);
        assertThat(Collections2.filter(predicates, findNewUserIdp::test)).hasSize(1);
    }

    @Test
    public void createPredicatesForTransactionEntity_shouldNotIncludeExtraPredicate() throws Exception {
        Set<Predicate<IdentityProviderConfig>> predicates = idpPredicateFactory.createPredicatesForTransactionEntity(Optional.of(TRANSACTION_ENTITY));

        assertThat(predicates).hasSize(2);
    }

    @Test
    public void createPredicatesForTransactionEntity_shouldIncludeEnabledPredicateWhenTransactionEntityIdIsProvided() throws Exception {
        Set<Predicate<IdentityProviderConfig>> predicates = idpPredicateFactory.createPredicatesForTransactionEntity(Optional.of(TRANSACTION_ENTITY));

        Predicate<Predicate> findEnabled = input -> input instanceof EnabledIdpPredicate;

        assertThat(Collections2.filter(predicates, findEnabled::test)).hasSize(1);
    }

    @Test
    public void createPredicatesForTransactionEntity_shouldIncludeEnabledPredicateWhenTransactionEntityIdIsNotProvided() throws Exception {
        Set<Predicate<IdentityProviderConfig>> predicates = idpPredicateFactory.createPredicatesForTransactionEntity(null);

        Predicate<Predicate> findEnabled = input -> input instanceof EnabledIdpPredicate;

        assertThat(Collections2.filter(predicates, findEnabled::test)).hasSize(1);
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

        assertThat(Collections2.filter(predicates, findEnabled::test)).hasSize(1);
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

        assertThat(Collections2.filter(predicates, findEnabled::test)).isEmpty();
    }
}
