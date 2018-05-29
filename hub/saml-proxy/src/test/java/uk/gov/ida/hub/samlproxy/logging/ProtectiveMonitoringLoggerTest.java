package uk.gov.ida.hub.samlproxy.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import org.assertj.core.data.MapEntry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.slf4j.LoggerFactory;
import uk.gov.ida.hub.samlproxy.repositories.Direction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ProtectiveMonitoringLoggerTest {

    @Mock
    private Response samlResponse;
    @Mock
    private Status status;
    @Mock
    private StatusCode statusCode;

    @Mock
    private Appender<ILoggingEvent> appender;
    @Captor
    private ArgumentCaptor<ILoggingEvent> captorLoggingEvent;

    @Before
    public void setUp() {
        Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        logger.addAppender(appender);
    }

    @After
    public void tearDown() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        logger.detachAppender(appender);
    }

    @Test
    public void shouldLogWhenNoSignatureExists() {
        ProtectiveMonitoringLogger protectiveMonitoringLogger = new ProtectiveMonitoringLogger(new ProtectiveMonitoringLogFormatter());
        when(samlResponse.getStatus()).thenReturn(status);
        when(samlResponse.getID()).thenReturn("ID");
        when(samlResponse.getInResponseTo()).thenReturn("InResponseTo");
        when(status.getStatusCode()).thenReturn(statusCode);
        when(statusCode.getValue()).thenReturn("all-good");


        protectiveMonitoringLogger.logAuthnResponse(samlResponse, Direction.OUTBOUND, false, false);

        verify(appender, times(1)).doAppend(captorLoggingEvent.capture());
        final ILoggingEvent loggingEvent = captorLoggingEvent.getValue();
        assertThat(loggingEvent.getMDCPropertyMap()).contains(
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


        protectiveMonitoringLogger.logAuthnResponse(samlResponse, Direction.OUTBOUND, true, true);

        verify(appender, times(1)).doAppend(captorLoggingEvent.capture());
        final ILoggingEvent loggingEvent = captorLoggingEvent.getValue();
        assertThat(loggingEvent.getMDCPropertyMap()).contains(
                MapEntry.entry("isSigned", "true"),
                MapEntry.entry("hasValidSignature", "true")
        );
    }

    @Test
    public void shouldLogWhenSignatureIsPresentButInvalid() {
        ProtectiveMonitoringLogger protectiveMonitoringLogger = new ProtectiveMonitoringLogger(new ProtectiveMonitoringLogFormatter());
        when(samlResponse.getStatus()).thenReturn(status);
        when(samlResponse.getID()).thenReturn("ID");
        when(samlResponse.getInResponseTo()).thenReturn("InResponseTo");
        when(status.getStatusCode()).thenReturn(statusCode);
        when(statusCode.getValue()).thenReturn("all-good");


        protectiveMonitoringLogger.logAuthnResponse(samlResponse, Direction.OUTBOUND, false, true);

        verify(appender, times(1)).doAppend(captorLoggingEvent.capture());
        final ILoggingEvent loggingEvent = captorLoggingEvent.getValue();
        assertThat(loggingEvent.getMDCPropertyMap()).contains(
                MapEntry.entry("isSigned", "true"),
                MapEntry.entry("hasValidSignature", "false")
        );
    }
}