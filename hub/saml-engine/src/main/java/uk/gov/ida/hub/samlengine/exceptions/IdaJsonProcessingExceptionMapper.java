package uk.gov.ida.hub.samlengine.exceptions;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.common.ExceptionType;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.UUID;

import static uk.gov.ida.common.ErrorStatusDto.createUnauditedErrorStatus;

//TODO move to shared library
@Provider
public class IdaJsonProcessingExceptionMapper implements ExceptionMapper<JsonProcessingException> {

    private static final Logger LOG = LoggerFactory.getLogger(IdaJsonProcessingExceptionMapper.class);

    @Override
    public Response toResponse(JsonProcessingException exception) {

        if (exception instanceof JsonGenerationException) {
            LOG.error("Error generating JSON", exception);
            return Response.serverError().build();
        }

        if (exception.getMessage().startsWith("No suitable constructor found")) {
            LOG.error("Unable to deserialize the specific type", exception);
            return Response.serverError().build();
        }

        LOG.info(exception.getLocalizedMessage());
        return Response
                .status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(createUnauditedErrorStatus(UUID.randomUUID(), ExceptionType.JSON_PARSING, exception.getOriginalMessage()))
                .build();
    }
}
