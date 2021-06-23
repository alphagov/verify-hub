package uk.gov.ida.integrationtest.hub.samlengine.apprule;

import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppExtension;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppExtension.SamlEngineAppExtensionBuilder;


import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppExtension.VERIFY_METADATA_PATH;

@ExtendWith(DropwizardExtensionsSupport.class)
public class HealthCheckTest {

    public static SamlEngineAppExtension samlEngineApp = new SamlEngineAppExtensionBuilder().build();

    @AfterAll
    public static void afterAll() {
        samlEngineApp.tearDown();
    }

    @Test
    public void shouldContainBothVerifyMetadataHealthChecks() {
        assertThat(samlEngineApp.getEnvironment().healthChecks().getNames().stream().anyMatch(name -> name.contains(VERIFY_METADATA_PATH))).isTrue();
    }
}
