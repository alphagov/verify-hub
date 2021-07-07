package uk.gov.ida.hub.policy.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.exceptions.ApplicationException;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.logging.HubEventLogger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.ida.exceptions.ApplicationException.createAuditedException;
import static uk.gov.ida.exceptions.ApplicationException.createUnauditedException;
import static uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder.aSessionId;

@ExtendWith(MockitoExtension.class)
public class PolicyApplicationExceptionMapperTest {

    @Mock
    private HubEventLogger eventLogger;

    @Mock
    private HttpServletRequest servletRequest;

    private PolicyApplicationExceptionMapper mapper;

    @BeforeEach
    public void setUp() {
        when(servletRequest.getParameter(Urls.SharedUrls.SESSION_ID_PARAM)).thenReturn("requestId");
        mapper = new PolicyApplicationExceptionMapper(eventLogger);
        mapper.setHttpServletRequest(servletRequest);
    }

    @Test
    public void toResponse_shouldAuditErrorIfUnaudited() {
        final SessionId sessionId = aSessionId().build();
        final UUID errorId = UUID.randomUUID();
        when(servletRequest.getParameter(Urls.SharedUrls.SESSION_ID_PARAM)).thenReturn(sessionId.toString());
        ApplicationException exception = createUnauditedException(ExceptionType.IDP_DISABLED, errorId);

        mapper.toResponse(exception);

        verify(eventLogger).logErrorEvent(eq(errorId), eq(sessionId), eq("Exception of type [IDP_DISABLED] "), eq("uri-not-present")); //detailsCaptor.capture());
    }

    @Test
    public void toResponse_shouldNotAuditIfEventIsAlreadyAudited() {
        ApplicationException exception = createAuditedException(ExceptionType.IDP_DISABLED, UUID.randomUUID());

        mapper.toResponse(exception);

        verifyNoInteractions(eventLogger);
    }

    @Test
    public void toResponse_shouldReturnAnAuditedErrorStatusIfExceptionIsAudited() {
        ApplicationException exception = createAuditedException(ExceptionType.IDP_DISABLED, UUID.randomUUID());

        final Response response = mapper.toResponse(exception);

        final ErrorStatusDto errorStatus = (ErrorStatusDto) response.getEntity();
        assertThat(errorStatus.isAudited()).isEqualTo(true);
    }

    @Test
    public void toResponse_shouldReturnAnUnauditedErrorStatusIfExceptionIsNotAudited() {
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
