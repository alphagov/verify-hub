package uk.gov.ida.hub.policy.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.hub.policy.facade.EventSinkMessageSenderFacade;
import uk.gov.ida.shared.utils.logging.LogFormatter;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.util.UUID;

public class InvalidSessionStateExceptionMapper extends PolicyExceptionMapper<InvalidSessionStateException> {

    private static final Logger LOG = LoggerFactory.getLogger(InvalidSessionStateExceptionMapper.class);
    private final EventSinkMessageSenderFacade eventSinkMessageSenderFacade;


    @Inject
    public InvalidSessionStateExceptionMapper(EventSinkMessageSenderFacade eventSinkMessageSenderFacade) {
        super();
        this.eventSinkMessageSenderFacade = eventSinkMessageSenderFacade;
    }

    @Override
    public Response handleException(InvalidSessionStateException exception) {
        UUID errorId = UUID.randomUUID();
        LOG.warn(LogFormatter.formatLog(errorId, exception.getMessage()), exception);

        eventSinkMessageSenderFacade.audit(exception, errorId, getSessionId().get());

        ExceptionType type = getExceptionType(exception);

        ErrorStatusDto entity = ErrorStatusDto.createAuditedErrorStatus(errorId, type, exception.getMessage());
        return Response.status(Response.Status.BAD_REQUEST).entity(entity).build();
    }

    private ExceptionType getExceptionType(InvalidSessionStateException exception) {
        if (exception.getExpectedState().equals(SessionStartedState.class) &&
                exception.getActualState().equals(IdpSelectedState.class)) {
            return ExceptionType.EXPECTED_SESSION_STARTED_STATE_ACTUAL_IDP_SELECTED_STATE;
        }
        return ExceptionType.INVALID_STATE;
    }

}
