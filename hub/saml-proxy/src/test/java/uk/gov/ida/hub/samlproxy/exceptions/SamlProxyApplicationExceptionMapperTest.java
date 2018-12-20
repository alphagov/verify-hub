package uk.gov.ida.hub.samlproxy.exceptions;

import com.google.common.base.Optional;
import com.google.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.exceptions.ApplicationException;
import uk.gov.ida.shared.utils.logging.LevelLogger;
import uk.gov.ida.shared.utils.logging.LevelLoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ida.exceptions.ApplicationException.createAuditedException;
import static uk.gov.ida.exceptions.ApplicationException.createUnauditedException;
import static uk.gov.ida.hub.samlproxy.Urls.SharedUrls.SESSION_ID_PARAM;

@RunWith(MockitoJUnitRunner.class)
public class SamlProxyApplicationExceptionMapperTest {
    @Mock
    private Provider<HttpServletRequest> servletRequestProvider;
    @Mock
    private LevelLogger levelLogger;
    @Mock
    private ExceptionAuditor exceptionAuditor;
    @Mock
    private LevelLoggerFactory<SamlProxyApplicationExceptionMapper> levelLoggerFactory;


    private ExceptionType exceptionType = ExceptionType.INVALID_SAML;
    private SessionId sessionId = SessionId.createNewSessionId();
    private UUID errorId = UUID.randomUUID();
    private SamlProxyApplicationExceptionMapper mapper;
    public HttpServletRequest servletRequest;

    @Before
    public void setUp() throws Exception {
        servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getParameter(SESSION_ID_PARAM)).thenReturn(sessionId.toString());
        when(servletRequestProvider.get()).thenReturn(servletRequest);
        when(levelLoggerFactory.createLevelLogger(SamlProxyApplicationExceptionMapper.class)).thenReturn(levelLogger);
        mapper = new SamlProxyApplicationExceptionMapper(
                servletRequestProvider,
                exceptionAuditor,
                levelLoggerFactory);
    }

    @Test
    public void toResponse_shouldAuditException() throws Exception {
        URI exceptionUri = URI.create("/exception-uri");
        ApplicationException exception = createUnauditedException(exceptionType, errorId, exceptionUri);

        mapper.toResponse(exception);

        verify(exceptionAuditor).auditException(exception, Optional.of(sessionId));
    }

    @Test
    public void toResponse_shouldReturnAuditedErrorResponse() throws Exception {
        ApplicationException exception = createAuditedException(exceptionType, errorId);

        final Response response = mapper.toResponse(exception);

        ErrorStatusDto responseEntity = (ErrorStatusDto) response.getEntity();
        assertThat(response.getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        assertThat(responseEntity.isAudited()).isTrue();
    }

    @Test
    public void shouldLogExceptionAtCorrectLevel() throws Exception {
        ApplicationException exception = createAuditedException(exceptionType, errorId);
        mapper.toResponse(exception);

        verify(levelLogger).log(eq(exception.getExceptionType().getLevel()), eq(exception), any(UUID.class));
    }

    private ApplicationException createUnauditedExceptionThatShouldNotBeAudited() {
        return createUnauditedException(
                ExceptionType.NETWORK_ERROR,
                UUID.randomUUID(),
                URI.create("/some-uri")
        );
    }
}
