package uk.gov.ida.hub.samlproxy.exceptions;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.exceptions.ApplicationException;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.eventsink.EventDetails;
import uk.gov.ida.eventsink.EventSinkMessageSender;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;

import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.ida.exceptions.ApplicationException.createAuditedException;
import static uk.gov.ida.exceptions.ApplicationException.createUnauditedException;
import static uk.gov.ida.eventemitter.EventDetailsKey.downstream_uri;

@RunWith(MockitoJUnitRunner.class)
public class ExceptionAuditorTest {
    private static final SessionId SESSION_ID = SessionId.createNewSessionId();

    @Mock
    public EventSinkMessageSender eventSinkMessageSender;

    public ExceptionAuditor exceptionAuditor;

    @Before
    public void setUp() throws Exception {
        exceptionAuditor = new ExceptionAuditor(eventSinkMessageSender);
    }

    @After
    public void tearDown() throws Exception {
        DateTimeFreezer.unfreezeTime();
    }

    @Test
    public void shouldAuditUnauditedException() throws Exception {
        DateTime expectedTimestamp = DateTime.now();
        DateTimeFreezer.freezeTime(expectedTimestamp);
        final UUID errorId = UUID.randomUUID();
        ApplicationException exception = createUnauditedException(
                ExceptionType.DUPLICATE_SESSION,
                errorId,
                URI.create("/some-uri")
        );

        exceptionAuditor.auditException(exception, Optional.of(SESSION_ID));

        ArgumentCaptor<EventDetails> argumentCaptor = ArgumentCaptor.forClass(EventDetails.class);
        verify(eventSinkMessageSender).audit(eq(exception), eq(errorId), eq(SESSION_ID), argumentCaptor.capture());

        final EventDetails event = argumentCaptor.getValue();
        assertThat(event.getKey()).isEqualTo(downstream_uri);
        assertThat(event.getValue()).isEqualTo(exception.getUri().get().toASCIIString());
    }

    @Test
    public void shouldNotAuditAlreadyAuditedException() throws Exception {
        final UUID errorId = UUID.randomUUID();
        ApplicationException exception =
                createAuditedException(ExceptionType.DUPLICATE_SESSION, errorId);

        exceptionAuditor.auditException(exception, Optional.of(SESSION_ID));

        verify(eventSinkMessageSender, never()).audit(
                any(Exception.class),
                any(UUID.class),
                any(SessionId.class),
                any(EventDetails.class)
        );
    }

    @Test
    public void shouldNotAuditUnauditedExceptionIfItDoesNotRequireAuditing() throws Exception {
        ApplicationException exception = createUnauditedExceptionThatShouldNotBeAudited();

        exceptionAuditor.auditException(exception, Optional.of(SESSION_ID));

        verify(eventSinkMessageSender, never()).audit(any(Exception.class), any(UUID.class), any(SessionId.class), any(EventDetails.class));
    }

    @Test
    public void shouldAuditWithNoSessionContextIfSessionIdIsNotPresent() throws Exception {
        final UUID errorId = UUID.randomUUID();
        ApplicationException exception = createUnauditedException(
                ExceptionType.DUPLICATE_SESSION,
                errorId,
                URI.create("/some-uri")
        );

        exceptionAuditor.auditException(exception, Optional.<SessionId>absent());

        verify(eventSinkMessageSender).audit(eq(exception), eq(errorId), eq(SessionId.NO_SESSION_CONTEXT_IN_ERROR), any(EventDetails.class));
    }

    private ApplicationException createUnauditedExceptionThatShouldNotBeAudited() {
        return createUnauditedException(
                ExceptionType.NETWORK_ERROR,
                UUID.randomUUID(),
                URI.create("/some-uri")
        );
    }

}
