package uk.gov.ida.hub.samlproxy.exceptions;

import com.google.inject.Provider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.event.Level;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.eventsink.EventSinkMessageSender;
import uk.gov.ida.saml.metadata.exceptions.NoKeyConfiguredForEntityException;
import uk.gov.ida.shared.utils.logging.LevelLoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

import static org.mockito.Matchers.any;
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

    @Test
    public void assertThatLogIsCreatedAtErrorLevelAndAuditIsSentToEventSink() throws Exception {
        when(context.get()).thenReturn(httpServletRequest);
        when(levelLoggerFactory.createLevelLogger(NoKeyConfiguredForEntityExceptionMapper.class)).thenReturn(levelLogger);

        NoKeyConfiguredForEntityExceptionMapper mapper = new NoKeyConfiguredForEntityExceptionMapper(context, levelLoggerFactory, eventSinkMessageSender);

        NoKeyConfiguredForEntityException exception = new NoKeyConfiguredForEntityException("entityId");
        mapper.toResponse(exception);
        verify(levelLogger).log(Level.ERROR, exception);
        verify(eventSinkMessageSender).audit(any(NoKeyConfiguredForEntityException.class), any(UUID.class), any(SessionId.class));
    }
}