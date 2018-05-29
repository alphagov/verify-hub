package uk.gov.ida.hub.samlproxy.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.assertj.core.data.MapEntry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.slf4j.LoggerFactory;

import uk.gov.ida.hub.samlproxy.repositories.Direction;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ProtectiveMonitoringLoggerTest {

    @Mock
    private Response samlResponse;
    @Mock
    private Status status;
    @Mock
    private StatusCode statusCode;

    private static TestAppender testAppender = new TestAppender();

    @Before
    public void setUp() {
        testAppender.start();
        Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        logger.addAppender(testAppender);
    }

    @After
    public void tearDown() throws Exception {
        testAppender.stop();
        TestAppender.events.clear();
        Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        logger.detachAppender(testAppender);
    }

    @Test
    public void shouldLogWhenNoSignatureExists() {
        ProtectiveMonitoringLogger protectiveMonitoringLogger = new ProtectiveMonitoringLogger(new ProtectiveMonitoringLogFormatter());
        when(samlResponse.getStatus()).thenReturn(status);
        when(samlResponse.getID()).thenReturn("ID");
        when(samlResponse.getInResponseTo()).thenReturn("InResponseTo");
        when(status.getStatusCode()).thenReturn(statusCode);
        when(statusCode.getValue()).thenReturn("all-good");


        protectiveMonitoringLogger.logAuthnResponse(samlResponse, Direction.OUTBOUND, null);

        assertThat(TestAppender.events.size()).isEqualTo(1);
        assertThat(TestAppender.events.get(0).getMDCPropertyMap()).contains(
                MapEntry.entry("isSigned", "false"),
                MapEntry.entry("hasValidSignature", "false")
        );
    }

    @Test
    public void shouldLogWhenSignatureIsPresentAndCorrect() {
        ProtectiveMonitoringLogger protectiveMonitoringLogger = new ProtectiveMonitoringLogger(new ProtectiveMonitoringLogFormatter());
        when(samlResponse.getStatus()).thenReturn(status);
        when(samlResponse.getID()).thenReturn("ID");
        when(samlResponse.getInResponseTo()).thenReturn("InResponseTo");
        when(status.getStatusCode()).thenReturn(statusCode);
        when(statusCode.getValue()).thenReturn("all-good");


        protectiveMonitoringLogger.logAuthnResponse(samlResponse, Direction.OUTBOUND, true);

        assertThat(TestAppender.events.size()).isEqualTo(1);
        assertThat(TestAppender.events.get(0).getMDCPropertyMap()).contains(
                MapEntry.entry("isSigned", "true"),
                MapEntry.entry("hasValidSignature", "true")
        );
    }

    private static class TestAppender extends AppenderBase<ILoggingEvent> {
        public static List<ILoggingEvent> events = new ArrayList<>();

        @Override
        protected void append(ILoggingEvent eventObject) {
            events.add(eventObject);
        }

    }
}