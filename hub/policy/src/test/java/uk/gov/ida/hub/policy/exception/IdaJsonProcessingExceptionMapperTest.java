package uk.gov.ida.hub.policy.exception;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.junit.Before;
import org.junit.Test;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class IdaJsonProcessingExceptionMapperTest {

    private IdaJsonProcessingExceptionMapper mapper;

    @Before
    public void setUp() {
        mapper = new IdaJsonProcessingExceptionMapper();
    }

    @Test
    public void toResponse_shouldReturnServerErrorWhenExceptionThrownGeneratingJson() {
        assertThat(mapper.toResponse(mock(JsonGenerationException.class)).getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void toResponse_shouldReturnServerErrorWhenDeserialisationLacksAppropriateConstructor() {
        assertThat(mapper.toResponse(new JsonMappingException("No suitable constructor found")).getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void toResponse_shouldReturnBadRequestAndErrorStatusDtoWhenErrorDeemedToBeFromClient() {
        String clientErrorMessage = "This is a client error";
        Response response = mapper.toResponse(new JsonMappingException(clientErrorMessage));
        ErrorStatusDto errorStatus = (ErrorStatusDto) response.getEntity();

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(errorStatus.isAudited()).isEqualTo(false);
        assertThat(errorStatus.getClientMessage()).isEqualTo(clientErrorMessage);
        assertThat(errorStatus.getExceptionType()).isEqualTo(ExceptionType.JSON_PARSING);
    }
}
