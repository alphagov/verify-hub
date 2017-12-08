package uk.gov.ida.hub.policy.exception;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.eventsink.EventSinkHubEventConstants;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.domain.EventSinkHubEvent;
import uk.gov.ida.hub.policy.proxy.EventSinkProxy;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ida.eventsink.EventDetailsKey.idp_entity_id;

@RunWith(MockitoJUnitRunner.class)
public class IdpDisabledExceptionMapperTest {

    private static final String SERVICE_NAME = "aService";
    private static final SessionId SESSION_ID = SessionId.createNewSessionId();

    @Mock
    private HttpServletRequest context;

    @Mock
    private ServiceInfoConfiguration serviceInfo;
    @Mock
    private EventSinkProxy eventSinkProxy;

    private IdpDisabledExceptionMapper exceptionMapper;

    @Before
    public void setUp() throws Exception {
        exceptionMapper = new IdpDisabledExceptionMapper(serviceInfo, eventSinkProxy);
        exceptionMapper.setHttpServletRequest(context);
        when(serviceInfo.getName()).thenReturn(SERVICE_NAME);
        when(context.getParameter(Urls.SharedUrls.SESSION_ID_PARAM)).thenReturn(SESSION_ID.toString());
    }

    @Test
    public void toResponse_shouldReturnForbidden() throws Exception {
        IdpDisabledException exception = new IdpDisabledException("my-entity");
        exceptionMapper.setHttpServletRequest(context);
        Response response = exceptionMapper.toResponse(exception);


        assertThat(response.getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
    }

    @Test
    public void toResponse_shouldLogToEventSink() throws Exception {
        IdpDisabledException exception = new IdpDisabledException("my-entity");
        ArgumentCaptor<EventSinkHubEvent> captor = ArgumentCaptor.forClass(EventSinkHubEvent.class);

        exceptionMapper.toResponse(exception);

        verify(eventSinkProxy).logHubEvent(captor.capture());
        EventSinkHubEvent value = captor.getValue();
        assertThat(value.getOriginatingService()).isEqualTo(SERVICE_NAME);
        assertThat(value.getEventType()).isEqualTo(EventSinkHubEventConstants.EventTypes.ERROR_EVENT);
        assertThat(value.getSessionId()).isEqualTo(SESSION_ID.toString());
        assertThat(value.getDetails().containsKey(idp_entity_id)).as("Details should contain IDP id").isTrue();
    }

    @Test
    public void toResponse_shouldReturnErrorResponseWithAuditingTrue() {
        IdpDisabledException exception = new IdpDisabledException("my-entity");
        exceptionMapper.setHttpServletRequest(context);
        Response response = exceptionMapper.toResponse(exception);

        final ErrorStatusDto errorStatusDto = (ErrorStatusDto) response.getEntity();

        assertThat(errorStatusDto.isAudited()).isEqualTo(true);
        assertThat(errorStatusDto.getExceptionType()).isEqualTo(ExceptionType.IDP_DISABLED);
    }
}
