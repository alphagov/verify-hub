package uk.gov.ida.hub.policy.domain.exception;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.event.Level;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.logging.HubEventLogger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.util.UUID;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder.aSessionId;

@RunWith(MockitoJUnitRunner.class)
public class StateProcessingValidationExceptionMapperTest {
    private static final String SESSION_ID = "42";

    @Mock
    private HubEventLogger eventLogger;

    private StateProcessingValidationExceptionMapper mapper;

    @Before
    public void setUp() {
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        when(httpServletRequest.getParameter(Urls.SharedUrls.SESSION_ID_PARAM)).thenReturn(SESSION_ID);

        mapper = new StateProcessingValidationExceptionMapper(eventLogger);
        mapper.setHttpServletRequest(httpServletRequest);
    }

    @Test
    public void toResponse_shouldReturnUnauditedErrorStatus() {
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
                asList(LevelOfAssurance.LEVEL_2),
                "idpEntityId",
                asList(LevelOfAssurance.LEVEL_1));
        assertThat(exception.getMessage()).isEqualTo("Transaction LevelsOfAssurance unsupported by IDP. Transaction: requestIssuerEntityId, LOAs: [LEVEL_2], IDP: idpEntityId, IDP LOAs: [LEVEL_1]");
    }
}
