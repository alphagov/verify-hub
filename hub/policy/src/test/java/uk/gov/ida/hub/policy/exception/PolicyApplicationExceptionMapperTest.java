package uk.gov.ida.hub.policy.exception;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.eventsink.EventDetails;
import uk.gov.ida.exceptions.ApplicationException;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.facade.EventSinkMessageSenderFacade;
import uk.gov.ida.shared.utils.logging.LevelLogger;
import uk.gov.ida.shared.utils.logging.LevelLoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ida.eventsink.EventDetailsKey.downstream_uri;
import static uk.gov.ida.exceptions.ApplicationException.createAuditedException;
import static uk.gov.ida.exceptions.ApplicationException.createUnauditedException;
import static uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder.aSessionId;

@RunWith(MockitoJUnitRunner.class)
public class PolicyApplicationExceptionMapperTest {

    @Mock
    private EventSinkMessageSenderFacade eventSinkMessageSenderFacade;
    @Mock
    private HttpServletRequest servletRequest;
    @Mock
    private LevelLoggerFactory<PolicyApplicationExceptionMapper> levelLoggerFactory;
    @Mock
    private LevelLogger levelLogger;

    @Captor
    ArgumentCaptor<EventDetails> detailsCaptor;

    private PolicyApplicationExceptionMapper mapper;

    @Before
    public void setUp() throws Exception {
        when(levelLoggerFactory.createLevelLogger(PolicyApplicationExceptionMapper.class)).thenReturn(levelLogger);
        mapper = new PolicyApplicationExceptionMapper(eventSinkMessageSenderFacade);
        mapper.setHttpServletRequest(servletRequest);
    }

    @Test
    public void toResponse_shouldAuditErrorIfUnaudited() throws Exception {
        final SessionId sessionId = aSessionId().build();
        final UUID errorId = UUID.randomUUID();
        when(servletRequest.getParameter(Urls.SharedUrls.SESSION_ID_PARAM)).thenReturn(sessionId.toString());
        ApplicationException exception = createUnauditedException(ExceptionType.IDP_DISABLED, errorId);

        mapper.toResponse(exception);

        verify(eventSinkMessageSenderFacade).audit(eq(exception), eq(errorId), eq(sessionId), detailsCaptor.capture());

        assertThat(detailsCaptor.getValue().getKey()).isEqualTo(downstream_uri);
        assertThat(detailsCaptor.getValue().getValue()).isEqualTo("uri-not-present");
    }

    @Test
    public void toResponse_shouldNotAuditIfEventIsAlreadyAudited() throws Exception {
        when(servletRequest.getParameter(Urls.SharedUrls.SESSION_ID_PARAM)).thenReturn("requestId");
        ApplicationException exception = createAuditedException(ExceptionType.IDP_DISABLED, UUID.randomUUID());

        mapper.toResponse(exception);

        verify(eventSinkMessageSenderFacade, never()).audit(
                any(Exception.class),
                any(UUID.class),
                any(SessionId.class),
                any(EventDetails.class)
        );
    }

    @Test
    public void toResponse_shouldReturnAnAuditedErrorStatusIfExceptionIsAudited() throws Exception {
        when(servletRequest.getParameter(Urls.SharedUrls.SESSION_ID_PARAM)).thenReturn("requestId");
        ApplicationException exception = createAuditedException(ExceptionType.IDP_DISABLED, UUID.randomUUID());

        final Response response = mapper.toResponse(exception);

        final ErrorStatusDto errorStatus = (ErrorStatusDto) response.getEntity();
        assertThat(errorStatus.isAudited()).isEqualTo(true);
    }

    @Test
    public void toResponse_shouldReturnAnUnauditedErrorStatusIfExceptionIsNotAudited() throws Exception {
        when(servletRequest.getParameter(Urls.SharedUrls.SESSION_ID_PARAM)).thenReturn("requestId");
        ApplicationException exception = createUnauditedExceptionThatShouldNotBeAudited();

        final Response response = mapper.toResponse(exception);

        final ErrorStatusDto errorStatus = (ErrorStatusDto) response.getEntity();
        assertThat(errorStatus.isAudited()).isEqualTo(false);
    }

    private ApplicationException createUnauditedExceptionThatShouldNotBeAudited() {
        return createUnauditedException(
                ExceptionType.NETWORK_ERROR,
                UUID.randomUUID(),
                URI.create("/some-uri")
        );
    }
}
