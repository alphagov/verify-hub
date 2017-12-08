package uk.gov.ida.hub.policy.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.hub.policy.facade.EventSinkMessageSenderFacade;
import uk.gov.ida.shared.utils.logging.LogFormatter;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.util.UUID;

public class EidasCountryNotSupportedExceptionMapper extends PolicyExceptionMapper<EidasCountryNotSupportedException> {
    private static final Logger LOG = LoggerFactory.getLogger(EidasCountryNotSupportedExceptionMapper.class);
    private final EventSinkMessageSenderFacade eventSinkMessageSenderFacade;

    @Inject
    public EidasCountryNotSupportedExceptionMapper(EventSinkMessageSenderFacade eventSinkMessageSenderFacade) {
        super();
        this.eventSinkMessageSenderFacade = eventSinkMessageSenderFacade;
    }

    @Override
    public Response handleException(EidasCountryNotSupportedException exception) {
        UUID errorId = UUID.randomUUID();
        LOG.warn(LogFormatter.formatLog(errorId, exception.getMessage()), exception);

        eventSinkMessageSenderFacade.audit(exception, errorId, getSessionId().get());

        ErrorStatusDto entity = ErrorStatusDto.createAuditedErrorStatus(errorId, ExceptionType.EIDAS_COUNTRY_NOT_SUPPORTED, exception.getMessage());
        return Response.status(Response.Status.BAD_REQUEST).entity(entity).build();
    }
}
