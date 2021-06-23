package uk.gov.ida.integrationtest.hub.samlproxy.apprule;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.ida.integrationtest.hub.samlproxy.apprule.support.SamlProxyAppExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.samlproxy.SamlProxyModule.VERIFY_METADATA_HEALTH_CHECK;

public class HealthCheckTest {
    @RegisterExtension
    public static final SamlProxyAppExtension samlProxyApp = SamlProxyAppExtension.builder()
            .build();

    @AfterAll
    public static void tearDown() {
        samlProxyApp.tearDown();
    }

    @Test
    public void shouldContainBothVerifyMetadataHealthChecks() {
        assertThat(samlProxyApp.getEnvironment().healthChecks().getNames().contains(VERIFY_METADATA_HEALTH_CHECK)).isTrue();
    }
}

