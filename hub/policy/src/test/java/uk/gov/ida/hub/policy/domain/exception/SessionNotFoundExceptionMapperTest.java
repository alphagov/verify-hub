package uk.gov.ida.hub.policy.domain.exception;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.eventsink.EventSinkProxy;
import uk.gov.ida.hub.policy.Urls;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder.aSessionId;

@RunWith(MockitoJUnitRunner.class)
public class SessionNotFoundExceptionMapperTest {

    @Mock
    private ServiceInfoConfiguration serviceInfo;
    @Mock
    private EventSinkProxy eventSinkProxy;

    @Test
    public void toResponse_shouldReturnUnauditedErrorStatus() throws Exception {
        HttpServletRequest context = mock(HttpServletRequest.class);
        when(context.getParameter(Urls.SharedUrls.SESSION_ID_PARAM)).thenReturn("42");
        SessionNotFoundExceptionMapper mapper = new SessionNotFoundExceptionMapper(serviceInfo, eventSinkProxy);
        mapper.setHttpServletRequest(context);
        SessionNotFoundException exception = new SessionNotFoundException(aSessionId().build());

        Response response = mapper.toResponse(exception);

        assertThat(response.getEntity()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto errorStatusDto = (ErrorStatusDto)response.getEntity();
        assertThat(errorStatusDto.isAudited()).isEqualTo(false);
        assertThat(errorStatusDto.getExceptionType()).isEqualTo(ExceptionType.SESSION_NOT_FOUND);
    }
}
