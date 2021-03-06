package uk.gov.ida.hub.samlproxy.exceptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.event.Level;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.hub.shared.eventsink.EventSinkMessageSender;
import uk.gov.ida.hub.samlproxy.Urls;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.hub.exception.SamlRequestTooOldException;
import uk.gov.ida.shared.utils.logging.LevelLogger;
import uk.gov.ida.shared.utils.logging.LevelLoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SamlProxySamlTransformationErrorExceptionMapperTest {
    @Mock
    private LevelLogger levelLogger;
    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private EventSinkMessageSender eventSinkMessageSender;
    @Mock
    private LevelLoggerFactory<SamlProxySamlTransformationErrorExceptionMapper> levelLoggerFactory;

    private SamlProxySamlTransformationErrorExceptionMapper exceptionMapper;

    @BeforeEach
    public void setUp() throws Exception {
        when(levelLoggerFactory.createLevelLogger(SamlProxySamlTransformationErrorExceptionMapper.class)).thenReturn(levelLogger);
        exceptionMapper = new SamlProxySamlTransformationErrorExceptionMapper(() -> httpServletRequest, eventSinkMessageSender, levelLoggerFactory);
    }

    @Test
    public void shouldLogToEventSinkWhenExceptionHasContextAndSessionId() {
        TestSamlTransformationErrorException exception = new TestSamlTransformationErrorException("error", new RuntimeException(), Level.DEBUG);
        SessionId sessionId = SessionId.createNewSessionId();
        when(httpServletRequest.getParameter(Urls.SharedUrls.SESSION_ID_PARAM)).thenReturn(sessionId.getSessionId());
        exceptionMapper.handleException(exception);

        verify(eventSinkMessageSender).audit(eq(exception), any(UUID.class), eq(sessionId));
    }

    @Test
    public void shouldLogToEventSinkWhenExceptionHasContextAndNoSessionId() {
        TestSamlTransformationErrorException exception = new TestSamlTransformationErrorException("error", new RuntimeException(), Level.DEBUG);
        exceptionMapper.handleException(exception);

        verify(eventSinkMessageSender).audit(eq(exception), any(UUID.class), eq(SessionId.NO_SESSION_CONTEXT_IN_ERROR));
    }

    @Test
    public void shouldCreateAuditedErrorResponseForInvalidSaml() {
        Response response = exceptionMapper.handleException(new TestSamlTransformationErrorException("error", new RuntimeException(), Level.DEBUG));

        ErrorStatusDto responseEntity = (ErrorStatusDto) response.getEntity();
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(responseEntity.isAudited()).isTrue();
        assertThat(responseEntity.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML);
    }

    @Test
    public void shouldCreateAuditedErrorResponseForRequestTooOldError() {
        Response response = exceptionMapper.handleException(new SamlRequestTooOldException("error", new RuntimeException(), Level.DEBUG));

        ErrorStatusDto responseEntity = (ErrorStatusDto) response.getEntity();
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(responseEntity.isAudited()).isTrue();
        assertThat(responseEntity.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML_REQUEST_TOO_OLD);
    }

    @Test
    public void shouldLogExceptionAtCorrectLevel() {
        Level logLevel = Level.DEBUG;
        TestSamlTransformationErrorException exception = new TestSamlTransformationErrorException("error", new RuntimeException(), logLevel);
        exceptionMapper.handleException(exception);

        verify(levelLogger).log(eq(logLevel), eq(exception), any(UUID.class));
    }

    private static class TestSamlTransformationErrorException extends SamlTransformationErrorException {
        protected TestSamlTransformationErrorException(String errorMessage, Exception cause, Level logLevel) {
            super(errorMessage, cause, logLevel);
        }
    }

}
