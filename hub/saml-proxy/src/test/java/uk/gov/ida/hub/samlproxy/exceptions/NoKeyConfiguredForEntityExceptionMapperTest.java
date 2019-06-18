package uk.gov.ida.hub.samlproxy.exceptions;

import com.google.inject.Provider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.event.Level;

import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.hub.shared.eventsink.EventSinkMessageSender;
import uk.gov.ida.hub.samlproxy.Urls;
import uk.gov.ida.saml.metadata.exceptions.NoKeyConfiguredForEntityException;
import uk.gov.ida.shared.utils.logging.LevelLoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.util.UUID;

import static org.junit.Assert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NoKeyConfiguredForEntityExceptionMapperTest {

    @Mock
    private LevelLoggerFactory<NoKeyConfiguredForEntityExceptionMapper> levelLoggerFactory;
    @Mock
    private uk.gov.ida.shared.utils.logging.LevelLogger levelLogger;
    @Mock
    private EventSinkMessageSender eventSinkMessageSender;
    @Mock
    private Provider<HttpServletRequest> context;
    @Mock
    private javax.servlet.http.HttpServletRequest httpServletRequest;

    NoKeyConfiguredForEntityExceptionMapper mapper;
    NoKeyConfiguredForEntityException exception;

    @Before
    public void setup() {
        when(context.get()).thenReturn(httpServletRequest);
        when(httpServletRequest.getParameter(Urls.SharedUrls.SESSION_ID_PARAM)).thenReturn("sessionId");
        when(levelLoggerFactory.createLevelLogger(NoKeyConfiguredForEntityExceptionMapper.class)).thenReturn(levelLogger);

        mapper = new NoKeyConfiguredForEntityExceptionMapper(context, levelLoggerFactory, eventSinkMessageSender);
        exception = new NoKeyConfiguredForEntityException("entityId");
    }

    @Test
    public void assertThatLogIsCreatedAtErrorLevelAndAuditIsSentToEventSink() {
        mapper.toResponse(exception);

        verify(levelLogger).log(Level.ERROR, exception);
        verify(eventSinkMessageSender).audit(any(NoKeyConfiguredForEntityException.class), any(UUID.class), any(SessionId.class));
    }

    @Test
    public void assertThatResponseIsInCorrectFormat() {
        Response response = mapper.toResponse(exception);

        assertThat(response.getEntity(), instanceOf(ErrorStatusDto.class));
    }
}
