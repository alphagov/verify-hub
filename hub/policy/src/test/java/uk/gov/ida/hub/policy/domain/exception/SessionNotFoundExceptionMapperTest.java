package uk.gov.ida.hub.policy.domain.exception;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.logging.HubEventLogger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder.aSessionId;

@RunWith(MockitoJUnitRunner.class)
public class SessionNotFoundExceptionMapperTest {

    private static final String SESSION_ID = "42";

    @Mock
    private HubEventLogger eventLogger;

    private SessionNotFoundExceptionMapper mapper;

    @Before
    public void setUp() {
        HttpServletRequest context = mock(HttpServletRequest.class);
        when(context.getParameter(Urls.SharedUrls.SESSION_ID_PARAM)).thenReturn(SESSION_ID);

        mapper = new SessionNotFoundExceptionMapper(eventLogger);
        mapper.setHttpServletRequest(context);
    }

    @Test
    public void toResponse_shouldReturnUnauditedErrorStatus() throws Exception {
        SessionNotFoundException exception = new SessionNotFoundException(aSessionId().with(SESSION_ID).build());

        Response response = mapper.toResponse(exception);

        verify(eventLogger).logErrorEvent(any(UUID.class), eq(aSessionId().with(SESSION_ID).build()), eq("Session: 42 not found."));
        assertThat(response.getEntity()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto errorStatusDto = (ErrorStatusDto)response.getEntity();
        assertThat(errorStatusDto.isAudited()).isEqualTo(false);
        assertThat(errorStatusDto.getExceptionType()).isEqualTo(ExceptionType.SESSION_NOT_FOUND);
    }
}
