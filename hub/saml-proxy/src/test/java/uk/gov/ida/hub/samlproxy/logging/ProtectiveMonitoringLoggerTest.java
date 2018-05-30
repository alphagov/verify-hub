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
import uk.gov.ida.hub.samlproxy.repositories.SignatureStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;


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


        protectiveMonitoringLogger.logAuthnResponse(samlResponse, Direction.OUTBOUND, SignatureStatus.NO_SIGNATURE);

        verify(appender, times(1)).doAppend(captorLoggingEvent.capture());
        final ILoggingEvent loggingEvent = captorLoggingEvent.getValue();
        assertThat(loggingEvent.getMDCPropertyMap()).contains(
                MapEntry.entry("signatureStatus", "NO_SIGNATURE")
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


        protectiveMonitoringLogger.logAuthnResponse(samlResponse, Direction.OUTBOUND, SignatureStatus.VALID_SIGNATURE);

        verify(appender, times(1)).doAppend(captorLoggingEvent.capture());
        final ILoggingEvent loggingEvent = captorLoggingEvent.getValue();
        assertThat(loggingEvent.getMDCPropertyMap()).contains(
                MapEntry.entry("signatureStatus", "VALID_SIGNATURE")
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


        protectiveMonitoringLogger.logAuthnResponse(samlResponse, Direction.OUTBOUND, SignatureStatus.INVALID_SIGNATURE);

        verify(appender, times(1)).doAppend(captorLoggingEvent.capture());
        final ILoggingEvent loggingEvent = captorLoggingEvent.getValue();
        assertThat(loggingEvent.getMDCPropertyMap()).contains(
                MapEntry.entry("signatureStatus", "INVALID_SIGNATURE")
        );
    }
}