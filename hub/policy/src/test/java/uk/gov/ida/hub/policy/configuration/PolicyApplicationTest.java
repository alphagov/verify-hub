package uk.gov.ida.hub.policy.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.setup.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.ida.hub.policy.PolicyApplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PolicyApplicationTest {

    private final Environment environment = mock(Environment.class);
    private final JerseyEnvironment jersey = mock(JerseyEnvironment.class);
    private final PolicyApplication application = new PolicyApplication();
    private final PolicyConfiguration config = new PolicyConfiguration();
    private final ObjectMapper objectMapper = mock(ObjectMapper.class);

    @BeforeEach
    public void setUp() {
        when(environment.jersey()).thenReturn(jersey);
        when(environment.getObjectMapper()).thenReturn(objectMapper);
    }

    @Test
    public void shouldReturnEventEmitterConfigurationAsNullByDefault() {
        application.run(config, environment);

        assertThat(config.getEventEmitterConfiguration()).isNull();
    }
}
