package uk.gov.ida.hub.samlproxy.exceptions;

import com.google.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.event.Level;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.eventsink.EventSinkMessageSender;
import uk.gov.ida.hub.samlproxy.Urls;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.hub.exception.SamlDuplicateRequestIdException;
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

@RunWith(MockitoJUnitRunner.class)
public class SamlProxySamlTransformationErrorExceptionMapperTest {
    @Mock
    private LevelLogger levelLogger;
    @Mock
    private Provider<HttpServletRequest> contextProvider;
    @Mock
    private javax.servlet.http.HttpServletRequest httpServletRequest;
    @Mock
    private EventSinkMessageSender eventSinkMessageSender;
    @Mock
    private LevelLoggerFactory<SamlProxySamlTransformationErrorExceptionMapper> levelLoggerFactory;

    private SamlProxySamlTransformationErrorExceptionMapper exceptionMapper;

    @Before
    public void setUp() throws Exception {
        when(levelLoggerFactory.createLevelLogger(SamlProxySamlTransformationErrorExceptionMapper.class)).thenReturn(levelLogger);
        exceptionMapper = new SamlProxySamlTransformationErrorExceptionMapper(contextProvider, eventSinkMessageSender, levelLoggerFactory);
        when(contextProvider.get()).thenReturn(httpServletRequest);
    }

    @Test
    public void shouldLogToEventSinkWhenExceptionHasContextAndSessionId() throws Exception {
        TestSamlTransformationErrorException exception = new TestSamlTransformationErrorException("error", new RuntimeException(), Level.DEBUG);
        SessionId sessionId = SessionId.createNewSessionId();
        when(httpServletRequest.getParameter(Urls.SharedUrls.SESSION_ID_PARAM)).thenReturn(sessionId.getSessionId());
        exceptionMapper.handleException(exception);

        verify(eventSinkMessageSender).audit(eq(exception), any(UUID.class), eq(sessionId));
    }

    @Test
    public void shouldLogToEventSinkWhenExceptionHasContextAndNoSessionId() throws Exception {
        TestSamlTransformationErrorException exception = new TestSamlTransformationErrorException("error", new RuntimeException(), Level.DEBUG);
        exceptionMapper.handleException(exception);

        verify(eventSinkMessageSender).audit(eq(exception), any(UUID.class), eq(SessionId.NO_SESSION_CONTEXT_IN_ERROR));
    }

    @Test
    public void shouldCreateAuditedErrorResponseForInvalidSaml() throws Exception {
        Response response = exceptionMapper.handleException(new TestSamlTransformationErrorException("error", new RuntimeException(), Level.DEBUG));

        ErrorStatusDto responseEntity = (ErrorStatusDto) response.getEntity();
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(responseEntity.isAudited()).isTrue();
        assertThat(responseEntity.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML);
    }

    @Test
    public void shouldCreateAuditedErrorResponseForRequestTooOldError() throws Exception {
        Response response = exceptionMapper.handleException(new SamlRequestTooOldException("error", new RuntimeException(), Level.DEBUG));

        ErrorStatusDto responseEntity = (ErrorStatusDto) response.getEntity();
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(responseEntity.isAudited()).isTrue();
        assertThat(responseEntity.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML_REQUEST_TOO_OLD);
    }

    @Test
    public void shouldCreateAuditedErrorResponseForDuplicateRequestIdError() throws Exception {
        Response response = exceptionMapper.handleException(new SamlDuplicateRequestIdException("error", new RuntimeException(), Level.DEBUG));

        ErrorStatusDto responseEntity = (ErrorStatusDto) response.getEntity();
        assertThat(response.getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        assertThat(responseEntity.isAudited()).isTrue();
        assertThat(responseEntity.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML_DUPLICATE_REQUEST_ID);
    }

    @Test
    public void shouldLogExceptionAtCorrectLevel() throws Exception {
        Level logLevel = Level.DEBUG;
        TestSamlTransformationErrorException exception = new TestSamlTransformationErrorException("error", new RuntimeException(), logLevel);
        exceptionMapper.handleException(exception);

        verify(levelLogger).log(eq(logLevel), eq(exception), any(UUID.class));
    }

    private class TestSamlTransformationErrorException extends SamlTransformationErrorException {
        protected TestSamlTransformationErrorException(String errorMessage, Exception cause, Level logLevel) {
            super(errorMessage, cause, logLevel);
        }
    }

}
