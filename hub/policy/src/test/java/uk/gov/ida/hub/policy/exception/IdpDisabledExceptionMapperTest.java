package uk.gov.ida.hub.policy.exception;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.logging.HubEventLogger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IdpDisabledExceptionMapperTest {

    private static final SessionId SESSION_ID = SessionId.createNewSessionId();
    private static final String ENTITY_ID = "my-entity";

    @Mock
    private HttpServletRequest context;

    @Mock
    private HubEventLogger eventLogger;

    private IdpDisabledExceptionMapper exceptionMapper;

    @Before
    public void setUp() {
        exceptionMapper = new IdpDisabledExceptionMapper(eventLogger);
        exceptionMapper.setHttpServletRequest(context);
        when(context.getParameter(Urls.SharedUrls.SESSION_ID_PARAM)).thenReturn(SESSION_ID.toString());
    }

    @Test
    public void toResponse_shouldReturnForbidden() {
        IdpDisabledException exception = new IdpDisabledException(ENTITY_ID);
        exceptionMapper.setHttpServletRequest(context);
        Response response = exceptionMapper.toResponse(exception);


        assertThat(response.getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
    }

    @Test
    public void toResponse_shouldLogToEventSink() {
        IdpDisabledException exception = new IdpDisabledException(ENTITY_ID);

        exceptionMapper.toResponse(exception);

        verify(eventLogger).logErrorEvent(any(UUID.class), eq(ENTITY_ID), eq(SESSION_ID));
    }

    @Test
    public void toResponse_shouldReturnErrorResponseWithAuditingTrue() {
        IdpDisabledException exception = new IdpDisabledException(ENTITY_ID);
        exceptionMapper.setHttpServletRequest(context);
        Response response = exceptionMapper.toResponse(exception);

        final ErrorStatusDto errorStatusDto = (ErrorStatusDto) response.getEntity();

        assertThat(errorStatusDto.isAudited()).isEqualTo(true);
        assertThat(errorStatusDto.getExceptionType()).isEqualTo(ExceptionType.IDP_DISABLED);
    }
}
