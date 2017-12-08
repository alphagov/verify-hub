package uk.gov.ida.hub.policy.domain.exception;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.event.Level;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.proxy.EventSinkProxy;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class StateProcessingValidationExceptionMapperTest {

    @Mock
    private ServiceInfoConfiguration serviceInfo;
    @Mock
    private EventSinkProxy eventSinkProxy;

    @Test
    public void toResponse_shouldReturnUnauditedErrorStatus() throws Exception {
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        when(httpServletRequest.getParameter(Urls.SharedUrls.SESSION_ID_PARAM)).thenReturn("42");
        StateProcessingValidationExceptionMapper mapper = new StateProcessingValidationExceptionMapper(serviceInfo, eventSinkProxy);
        mapper.setHttpServletRequest(httpServletRequest);
        StateProcessingValidationException exception = new StateProcessingValidationException("error message", Level.ERROR);

        final Response response = mapper.toResponse(exception);

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
