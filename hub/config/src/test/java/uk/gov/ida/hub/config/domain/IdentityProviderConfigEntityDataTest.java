package uk.gov.ida.hub.config.domain;

import org.junit.Test;
import uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder.anIdentityProviderConfigData;

public class IdentityProviderConfigEntityDataTest {

    private final IdentityProviderConfigDataBuilder dataBuilder = anIdentityProviderConfigData();

    @Test
    public void should_defaultSupportedLevelsOfAssuranceToOnlyIncludeLOA2() throws Exception {
        IdentityProviderConfigEntityData data = dataBuilder.build();

        assertThat(data.getSupportedLevelsOfAssurance()).containsExactly(LevelOfAssurance.LEVEL_2);
    }

}
