package uk.gov.ida.integrationtest.hub.samlengine.apprule;

import io.dropwizard.setup.Environment;
import io.dropwizard.testing.ResourceHelpers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import ru.vyarus.dropwizard.guice.test.jupiter.ext.TestDropwizardAppExtension;
import uk.gov.ida.hub.samlengine.SamlEngineApplication;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppExtension.VERIFY_METADATA_PATH;

public class HealthCheckTest {
    @RegisterExtension
    public static TestDropwizardAppExtension samlEngineApp = SamlEngineAppExtension.forApp(SamlEngineApplication.class)
            .withDefaultConfigOverridesAnd()
            .config(ResourceHelpers.resourceFilePath("saml-engine.yml"))
            .randomPorts()
            .create();

    @AfterAll
    public static void afterAll() {
        SamlEngineAppExtension.tearDown();
    }

    @Test
    public void shouldContainBothVerifyMetadataHealthChecks(Environment environment) {
        assertThat(environment.healthChecks().getNames().stream().anyMatch(name -> name.contains(VERIFY_METADATA_PATH))).isTrue();
    }
}
