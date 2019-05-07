package uk.gov.ida.hub.config.domain.filters;

import org.junit.Test;
import uk.gov.ida.hub.config.domain.IdentityProviderConfig;
import uk.gov.ida.hub.config.domain.LevelOfAssurance;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.config.domain.filters.PredicateTestHelper.*;

public class SupportedLoaIdpPredicateTest {

    @Test
    public void shouldReturnRelevantIdpsForLoa() {
        final SupportedLoaIdpPredicate loa1Predicate = new SupportedLoaIdpPredicate(LevelOfAssurance.LEVEL_1);
        final Set<IdentityProviderConfig> filteredIdps = getFilteredIdps(allIdps, loa1Predicate);

        final IdentityProviderConfig[] expectedFilteredIdps = {nonOnboardingLoa1Idp, nonOnboardingAllLevelsIdp,
                onboardingLoa1Idp, onboardingLoa2Idp, onboardingAllLevelsIdp, onboardingLoa1IdpOtherOnboardingEntity,
                onboardingLoa2IdpOtherOnboardingEntity, onboardingAllLevelsIdpOtherOnboardingEntity, 
                nonOnboardingSoftDisconnectingIdp, nonOnboardingHardDisconnectingIdp, onboardingSoftDisconnectingIdp,
                onboardingHardDisconnectingIdp};

        assertThat(filteredIdps).containsOnly(expectedFilteredIdps);
    }
}
