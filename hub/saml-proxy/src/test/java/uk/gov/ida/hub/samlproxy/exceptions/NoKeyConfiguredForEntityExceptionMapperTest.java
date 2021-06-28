package uk.gov.ida.hub.samlproxy.exceptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.event.Level;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.hub.samlproxy.Urls;
import uk.gov.ida.hub.shared.eventsink.EventSinkMessageSender;
import uk.gov.ida.saml.metadata.exceptions.NoKeyConfiguredForEntityException;
import uk.gov.ida.shared.utils.logging.LevelLogger;
import uk.gov.ida.shared.utils.logging.LevelLoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class NoKeyConfiguredForEntityExceptionMapperTest {

    @Mock
    private LevelLoggerFactory<NoKeyConfiguredForEntityExceptionMapper> levelLoggerFactory;
    @Mock
    private LevelLogger levelLogger;
    @Mock
    private EventSinkMessageSender eventSinkMessageSender;
    @Mock
    private HttpServletRequest httpServletRequest;

    NoKeyConfiguredForEntityExceptionMapper mapper;
    NoKeyConfiguredForEntityException exception;

    @BeforeEach
    public void setup() {
        when(httpServletRequest.getParameter(Urls.SharedUrls.SESSION_ID_PARAM)).thenReturn("sessionId");
        when(levelLoggerFactory.createLevelLogger(NoKeyConfiguredForEntityExceptionMapper.class)).thenReturn(levelLogger);

        mapper = new NoKeyConfiguredForEntityExceptionMapper(() -> httpServletRequest, levelLoggerFactory, eventSinkMessageSender);
        exception = new NoKeyConfiguredForEntityException("entityId");
    }

    @Test
    public void assertThatLogIsCreatedAtErrorLevelAndAuditIsSentToEventSink() {
        mapper.toResponse(exception);

        verify(levelLogger).log(Level.WARN, exception);
        verify(eventSinkMessageSender).audit(any(NoKeyConfiguredForEntityException.class), any(UUID.class), any(SessionId.class));
    }

    @Test
    public void assertThatResponseIsInCorrectFormat() {
        Response response = mapper.toResponse(exception);

        assertThat(response.getEntity()).isInstanceOf(ErrorStatusDto.class);
    }
}
