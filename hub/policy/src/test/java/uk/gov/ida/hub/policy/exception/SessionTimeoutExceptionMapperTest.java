package uk.gov.ida.hub.policy.exception;

import com.google.inject.Provider;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.logging.HubEventLogger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder.aSessionId;

@ExtendWith(MockitoExtension.class)
public class SessionTimeoutExceptionMapperTest {

    @Mock
    private HttpServletRequest servletRequest;
    @Mock
    private Provider<UriInfo> uriInfoProvider;
    @Mock
    private HubEventLogger hubEventLogger;

    @BeforeEach
    public void beforeAll() {
        when(servletRequest.getParameter(Urls.SharedUrls.SESSION_ID_PARAM)).thenReturn("42");
    }

    @Test
    public void toResponse_shouldReturnAuditedErrorStatus() {
        SessionTimeoutExceptionMapper mapper = new SessionTimeoutExceptionMapper(uriInfoProvider, () -> servletRequest, hubEventLogger);
        SessionTimeoutException exception = new SessionTimeoutException("Timeout exception", aSessionId().build(), "some entity id", DateTime.now().minusMinutes(10), "some request id");

        final Response response = mapper.toResponse(exception);

        assertThat(response.getEntity()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(400);
        final ErrorStatusDto errorStatusDto = (ErrorStatusDto) response.getEntity();
        assertThat(errorStatusDto.isAudited()).isEqualTo(true);
        assertThat(errorStatusDto.getExceptionType()).isEqualTo(ExceptionType.SESSION_TIMEOUT);
    }

    @Test
    public void toResponse_shouldLogToEventSink() {
        SessionTimeoutExceptionMapper mapper = new SessionTimeoutExceptionMapper(uriInfoProvider, () -> servletRequest, hubEventLogger);
        SessionId sessionId = aSessionId().build();
        DateTime sessionExpiryTimestamp = DateTime.now().minusMinutes(10);
        String transactionEntityId = "some entity id";
        String requestId = "some request id";
        SessionTimeoutException exception = new SessionTimeoutException("Timeout exception", sessionId, transactionEntityId, sessionExpiryTimestamp, requestId);

        mapper.toResponse(exception);

        verify(hubEventLogger)
                .logSessionTimeoutEvent(sessionId, sessionExpiryTimestamp, transactionEntityId, requestId);
    }
}
