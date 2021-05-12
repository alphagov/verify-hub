package uk.gov.ida.hub.policy.domain.exception;

import com.google.inject.servlet.RequestScoped;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.exception.PolicyExceptionMapper;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.shared.utils.logging.LevelLogger;
import uk.gov.ida.shared.utils.logging.LevelLoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import java.util.UUID;

import static uk.gov.ida.common.ErrorStatusDto.createUnauditedErrorStatus;
import static uk.gov.ida.common.ExceptionType.STATE_PROCESSING_VALIDATION;

@Provider
@RequestScoped
public class StateProcessingValidationExceptionMapper extends PolicyExceptionMapper<StateProcessingValidationException> {

    private static final LevelLogger LOG = new LevelLoggerFactory<StateProcessingValidationExceptionMapper>().createLevelLogger(StateProcessingValidationExceptionMapper.class);

    private final HubEventLogger eventLogger;

    @Inject
    public StateProcessingValidationExceptionMapper(@Context UriInfo uriInfo, @Context HttpServletRequest request, HubEventLogger eventLogger) {
        super(uriInfo, request);
        this.eventLogger = eventLogger;
    }

    @Override
    public Response handleException(StateProcessingValidationException exception) {
        UUID errorId = UUID.randomUUID();

        LOG.log(exception.getLevel(), exception, errorId);

        eventLogger.logErrorEvent(errorId, getSessionId().orElse(SessionId.SESSION_ID_DOES_NOT_EXIST_YET), exception.getMessage());

        ErrorStatusDto entity = createUnauditedErrorStatus(errorId, STATE_PROCESSING_VALIDATION);
        return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(entity).build();
    }

    protected SessionId createSessionId(String parameter) {
        return new SessionId(parameter);
    }
}
