package uk.gov.ida.hub.policy.metrics;

import io.prometheus.client.Counter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import uk.gov.ida.hub.policy.metrics.EidasConnectorMetrics.Direction;
import uk.gov.ida.hub.policy.metrics.EidasConnectorMetrics.Status;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class EidasConnectorMetricsTest {

    @Parameterized.Parameter(0)
    public String entityId;

    @Parameterized.Parameter(1)
    public Direction direction;

    @Parameterized.Parameter(2)
    public Status status;

    @Parameterized.Parameter(3)
    public String expectedCountryCode;

    @Parameterized.Parameter(4)
    public boolean incrementsCounter;

    @Parameterized.Parameters
    public static Collection testParams() {
        return Arrays.asList(new Object[][]{
                {"https://test.gov.uk" + "/" + URLEncoder.encode("test.gov.uk", Charset.defaultCharset()), Direction.response, Status.ko, "uk", true},
                {"http://localhost", Direction.request, Status.ok, "localhost", true},
                {"https://proxy-node.gov.uk/ServiceMetadata.de", Direction.request, Status.ok, "uk", true},
                {"https://apple.com ", Direction.response, Status.ko, "com", true},
                {"http://⌘.com", Direction.response, Status.ko, "n/a", false},
                {"http://.com", Direction.response, Status.ko, "n/a", false},
                {"http://test.gov.uk", null, Status.ko, "n/a", false},
                {"http://test.gov.uk", Direction.response, null, "n/a", false},
        });
    }

    @Test
    public void testIncrementingCounterWithParameters() throws Exception {
        Counter counter = mock(Counter.class);
        Counter.Child counterChild = mock(Counter.Child.class);
        setFinalStatic(EidasConnectorMetrics.class.getDeclaredField("CONNECTOR_COUNTER"), counter);
        when(counter.labels(expectedCountryCode, direction != null ? direction.name() : null, status != null ? status.name() : null)).thenReturn(counterChild);
        EidasConnectorMetrics.increment(entityId, direction, status);
        if (incrementsCounter) {
            verify(counterChild).inc();
        } else {
            verifyNoInteractions(counter, counterChild);
        }
    }

    private static void setFinalStatic(Field field, Object newValue) throws Exception {
        field.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(null, newValue);
    }
}