package uk.gov.ida.hub.policy.domain.exception;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.eventsink.EventSinkProxy;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.domain.EventSinkHubEvent;
import uk.gov.ida.hub.policy.domain.SessionId;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder.aSessionId;

@RunWith(MockitoJUnitRunner.class)
public class SessionAlreadyExistingExceptionMapperTest {

    @Mock
    private EventSinkProxy eventSinkProxy;
    @Mock
    private ServiceInfoConfiguration serviceInfo;

    @Mock
    private HttpServletRequest servletRequest;

    @Test
    public void toResponse_shouldLogToAudit() throws Exception {
        SessionId sessionId = aSessionId().build();
        when(servletRequest.getParameter(Urls.SharedUrls.SESSION_ID_PARAM)).thenReturn(sessionId.toString());
        SessionAlreadyExistingExceptionMapper mapper = new SessionAlreadyExistingExceptionMapper(serviceInfo, eventSinkProxy);
        mapper.setHttpServletRequest(servletRequest);
        SessionAlreadyExistingException exception = new SessionAlreadyExistingException("this requestid already has a session", sessionId);
        ArgumentCaptor<EventSinkHubEvent> captor = ArgumentCaptor.forClass(EventSinkHubEvent.class);
        final Response response = mapper.toResponse(exception);
        verify(eventSinkProxy).logHubEvent(captor.capture());
        assertThat(captor.getValue().getSessionId()).isEqualTo(sessionId.getSessionId());
        final ErrorStatusDto errorStatusDto = (ErrorStatusDto) response.getEntity();
        assertThat(errorStatusDto.isAudited()).isEqualTo(true);
        assertThat(errorStatusDto.getExceptionType()).isEqualTo(ExceptionType.DUPLICATE_SESSION);
    }
}
