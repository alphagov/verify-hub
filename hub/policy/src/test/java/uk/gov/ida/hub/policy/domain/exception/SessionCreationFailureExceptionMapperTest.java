package uk.gov.ida.hub.policy.domain.exception;

import com.google.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.event.Level;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder;
import uk.gov.ida.hub.policy.domain.SessionId;
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

@ExtendWith(MockitoExtension.class)
public class SessionCreationFailureExceptionMapperTest {

    private static final SessionId SESSION_ID = SessionIdBuilder.aSessionId().build();
    private static final String MESSAGE = "The session does not exist.";

    @Mock
    private HubEventLogger hubEventLogger;

    @Mock
    private HttpServletRequest servletRequest;

    @Mock
    private Provider<UriInfo> uriInfoProvider;

    private SessionCreationFailureExceptionMapper mapper;

    @BeforeEach
    public void setUp() {
        when(servletRequest.getParameter(Urls.SharedUrls.SESSION_ID_PARAM)).thenReturn(SESSION_ID.getSessionId());

        mapper = new SessionCreationFailureExceptionMapper(uriInfoProvider, () -> servletRequest, hubEventLogger);
    }

    @Test
    public void toResponse_shouldLogToAudit() {
        SessionCreationFailureException exception = new SessionCreationFailureException(MESSAGE, Level.ERROR, ExceptionType.DUPLICATE_SESSION);

        final Response response = mapper.toResponse(exception);

        verify(hubEventLogger).logErrorEvent(any(UUID.class), eq(SESSION_ID), eq(MESSAGE));
        final ErrorStatusDto errorStatusDto = (ErrorStatusDto) response.getEntity();
        assertThat(errorStatusDto.isAudited()).isEqualTo(true);
        assertThat(errorStatusDto.getExceptionType()).isEqualTo(ExceptionType.DUPLICATE_SESSION);
    }
}
