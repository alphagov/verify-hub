package uk.gov.ida.hub.config.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.UUID;

import static java.text.MessageFormat.format;
import static uk.gov.ida.common.ErrorStatusDto.createUnauditedErrorStatus;

public class ExceptionFactory {
    private static final Logger LOG = LoggerFactory.getLogger(ExceptionFactory.class);

    @Inject
    public ExceptionFactory() {
    }

    public NotFoundException createNoDataForEntityException(String entityId) {
        final String message = format("''{0}'' - No data is configured for this entity.", entityId);
        LOG.error(message);
        return new NotFoundException(message);
    }
    
    public NotFoundException createNoTranslationForLocaleException(String locale) {
        final String message = format("''{0}'' - No translation is configured for this locale.", locale);
        LOG.error(message);
        return new NotFoundException(message);
    }

    public WebApplicationException createInvalidAssertionConsumerServiceIndexException(String entityId, Integer assertionConsumerServiceIndex) {
        LOG.error(format("Invalid assertion consumer service index ''{0}'' for ''{1}''.", assertionConsumerServiceIndex, entityId));
        final ErrorStatusDto entity = createUnauditedErrorStatus(UUID.randomUUID(), ExceptionType.INVALID_ASSERTION_CONSUMER_INDEX);
        final Response response = Response
                .status(Response.Status.NOT_FOUND)
                .entity(entity)
                .build();
        return new WebApplicationException(response);
    }

    public WebApplicationException createDisabledTransactionException(String entityId) {
        LOG.error(format("''{0}'' - This Transaction is disabled.", entityId));
        return createDisabledEntityException(ExceptionType.TRANSACTION_DISABLED);
    }

    public WebApplicationException createDisabledIdentityProviderException(String entityId) {
        LOG.error(format("''{0}'' - This Transaction is disabled.", entityId));
        return createDisabledEntityException(ExceptionType.IDP_DISABLED);
    }

    private WebApplicationException createDisabledEntityException(ExceptionType exceptionType) {
        final ErrorStatusDto entity = createUnauditedErrorStatus(UUID.randomUUID(), exceptionType);
        final Response response = Response
            .status(Response.Status.FORBIDDEN)
            .entity(entity)
            .build();
        return new WebApplicationException(response);
    }
}
