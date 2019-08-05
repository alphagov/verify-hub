package uk.gov.ida.integrationtest.hub.samlproxy.apprule;

import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.ida.integrationtest.hub.samlproxy.apprule.support.SamlProxyAppRule;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.samlproxy.SamlProxyModule.COUNTRY_METADATA_HEALTH_CHECK;
import static uk.gov.ida.hub.samlproxy.SamlProxyModule.VERIFY_METADATA_HEALTH_CHECK;

public class HealthCheckWithoutCountryTest {

    @ClassRule
    public static SamlProxyAppRule samlProxyAppRule = new SamlProxyAppRule(false);

    @Test
    public void shouldContainBothVerifyAndCountryMetadataHealthChecks() {
        assertThat(samlProxyAppRule.getEnvironment().healthChecks().getNames().contains(VERIFY_METADATA_HEALTH_CHECK)).isTrue();
        assertThat(samlProxyAppRule.getEnvironment().healthChecks().getNames().contains(COUNTRY_METADATA_HEALTH_CHECK)).isFalse();
    }
}
