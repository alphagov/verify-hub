package uk.gov.ida.hub.samlproxy;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.hub.samlproxy.config.CountryConfiguration;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class SamlProxyConfigurationTest {

    @Test
    public void shouldReturnCountryConfigOptionalIfCountryConfigNotNull() {
        SamlProxyConfiguration samlProxyConfiguration = new SamlProxyConfiguration();
        CountryConfiguration countryConfiguration = new CountryConfiguration(null);
        samlProxyConfiguration.country = countryConfiguration;

        assertThat(samlProxyConfiguration.getCountryConfiguration()).isEqualTo(Optional.of(countryConfiguration));
    }

    @Test
    public void shouldReturnEmptyOptionalIfCountConfigNull() {
        SamlProxyConfiguration samlProxyConfiguration = new SamlProxyConfiguration();

        assertThat(samlProxyConfiguration.getCountryConfiguration()).isEqualTo(Optional.empty());
    }
}
