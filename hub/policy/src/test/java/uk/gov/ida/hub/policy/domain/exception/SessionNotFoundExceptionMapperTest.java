package uk.gov.ida.hub.policy.domain.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.logging.HubEventLogger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder.aSessionId;

@ExtendWith(MockitoExtension.class)
public class SessionNotFoundExceptionMapperTest {

    private static final String SESSION_ID = "42";

    @Mock
    private HubEventLogger eventLogger;

    @Mock
    private HttpServletRequest servletRequest;

    @Mock
    private UriInfo uriInfo;

    private SessionNotFoundExceptionMapper mapper;

    @BeforeEach
    public void setUp() {
        when(servletRequest.getParameter(Urls.SharedUrls.SESSION_ID_PARAM)).thenReturn(SESSION_ID);

        mapper = new SessionNotFoundExceptionMapper(uriInfo, servletRequest, eventLogger);
    }

    @Test
    public void toResponse_shouldReturnUnauditedErrorStatus() {
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
