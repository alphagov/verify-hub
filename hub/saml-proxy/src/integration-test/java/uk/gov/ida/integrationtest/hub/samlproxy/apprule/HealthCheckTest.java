package uk.gov.ida.integrationtest.hub.samlproxy.apprule;

import io.dropwizard.setup.Environment;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import ru.vyarus.dropwizard.guice.test.jupiter.ext.TestDropwizardAppExtension;
import uk.gov.ida.hub.samlproxy.SamlProxyApplication;
import uk.gov.ida.hub.samlproxy.support.ResourceHelpers;
import uk.gov.ida.integrationtest.hub.samlproxy.apprule.support.SamlProxyAppExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.samlproxy.SamlProxyModule.VERIFY_METADATA_HEALTH_CHECK;

public class HealthCheckTest {
    @RegisterExtension
    public static TestDropwizardAppExtension samlProxyApp = SamlProxyAppExtension.forApp(SamlProxyApplication.class)
            .withDefaultConfigOverridesAnd()
            .config(ResourceHelpers.resourceFilePath("saml-proxy.yml"))
            .randomPorts()
            .create();

    @AfterAll
    public static void tearDown() {
        SamlProxyAppExtension.tearDown();
    }

    @Test
    public void shouldContainBothVerifyMetadataHealthChecks(Environment environment) {
        assertThat(environment.healthChecks().getNames().contains(VERIFY_METADATA_HEALTH_CHECK)).isTrue();
    }
}

