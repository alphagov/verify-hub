package uk.gov.ida.hub.policy.domain.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.event.Level;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.logging.HubEventLogger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder.aSessionId;

@ExtendWith(MockitoExtension.class)
public class StateProcessingValidationExceptionMapperTest {
    private static final String SESSION_ID = "42";

    @Mock
    private HubEventLogger eventLogger;

    @Mock
    private HttpServletRequest servletRequest;

    private StateProcessingValidationExceptionMapper mapper;

    @BeforeEach
    public void setUp() {
        mapper = new StateProcessingValidationExceptionMapper(eventLogger);
        mapper.setHttpServletRequest(servletRequest);
    }

    @Test
    public void toResponse_shouldReturnUnauditedErrorStatus() {
        when(servletRequest.getParameter(Urls.SharedUrls.SESSION_ID_PARAM)).thenReturn(SESSION_ID);
        String errorMessage = "error message";
        StateProcessingValidationException exception = new StateProcessingValidationException(errorMessage, Level.ERROR);

        final Response response = mapper.toResponse(exception);

        verify(eventLogger).logErrorEvent(any(UUID.class), eq(aSessionId().with(SESSION_ID).build()), eq(errorMessage));
        assertThat(response.getEntity()).isNotNull();
        ErrorStatusDto errorStatusDto = (ErrorStatusDto)response.getEntity();
        assertThat(errorStatusDto.isAudited()).isEqualTo(false);
        assertThat(errorStatusDto.getExceptionType()).isEqualTo(ExceptionType.STATE_PROCESSING_VALIDATION);
    }

    @Test
    public void shouldDoFormatMessage(){
        StateProcessingValidationException exception = StateProcessingValidationException.transactionLevelsOfAssuranceUnsupportedByIDP(
                "requestIssuerEntityId",
                singletonList(LevelOfAssurance.LEVEL_2),
                "idpEntityId",
                singletonList(LevelOfAssurance.LEVEL_1));
        assertThat(exception.getMessage()).isEqualTo("Transaction LevelsOfAssurance unsupported by IDP. Transaction: requestIssuerEntityId, LOAs: [LEVEL_2], IDP: idpEntityId, IDP LOAs: [LEVEL_1]");
    }
}
