package uk.gov.ida.hub.samlproxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.jetty.setup.ServletEnvironment;
import io.dropwizard.setup.Environment;
import org.junit.Before;
import org.junit.Test;
import uk.gov.ida.hub.samlproxy.filters.SessionIdQueryParamLoggingFilter;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SamlProxyApplicationTest {
    private final Environment environment = mock(Environment.class);
    private final JerseyEnvironment jersey = mock(JerseyEnvironment.class);
    private final SamlProxyApplication application = new SamlProxyApplication();
    private final SamlProxyConfiguration config = new SamlProxyConfiguration();
    private final ObjectMapper objectMapper = mock(ObjectMapper.class);
    private final ServletEnvironment servletEnvironment = mock(ServletEnvironment.class);
    private final FilterRegistration.Dynamic dynamic = mock(FilterRegistration.Dynamic.class);

    @Before
    public void setUp() throws Exception {
        when(environment.jersey()).thenReturn(jersey);
        when(environment.getObjectMapper()).thenReturn(objectMapper);
        when(environment.servlets()).thenReturn(servletEnvironment);
        when(servletEnvironment.addFilter("Logging SessionId registration Filter", SessionIdQueryParamLoggingFilter.class)).thenReturn(dynamic);
        doNothing().when(dynamic).addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
    }

    @Test
    public void shouldReturnEventEmitterConfigurationAsNullByDefault() throws Exception {
        application.run(config, environment);

        assertThat(config.getEventEmitterConfiguration()).isNull();
    }
}
