package uk.gov.ida.hub.policy.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.shared.utils.logging.LogFormatter;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.util.UUID;

public class EidasNotSupportedExceptionMapper extends PolicyExceptionMapper<EidasNotSupportedException> {
    private static final Logger LOG = LoggerFactory.getLogger(EidasNotSupportedExceptionMapper.class);
    private final HubEventLogger eventLogger;

    @Inject
    public EidasNotSupportedExceptionMapper(HubEventLogger eventLogger) {
        super();
        this.eventLogger = eventLogger;
    }

    @Override
    public Response handleException(EidasNotSupportedException exception) {
        UUID errorId = UUID.randomUUID();
        LOG.warn(LogFormatter.formatLog(errorId, exception.getMessage()), exception);

        eventLogger.logErrorEvent(errorId, getSessionId().orElse(SessionId.SESSION_ID_DOES_NOT_EXIST_YET), exception.getMessage());

        ErrorStatusDto entity = ErrorStatusDto.createAuditedErrorStatus(errorId, ExceptionType.EIDAS_DISABLED, exception.getMessage());
        return Response.status(Response.Status.BAD_REQUEST).entity(entity).build();
    }
}
