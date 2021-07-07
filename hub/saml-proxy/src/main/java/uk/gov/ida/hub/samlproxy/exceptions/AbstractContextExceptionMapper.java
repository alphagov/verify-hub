package uk.gov.ida.hub.samlproxy.exceptions;

import com.google.common.base.Strings;
import com.google.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.hub.samlproxy.Urls;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public abstract class AbstractContextExceptionMapper<TException extends Exception> implements ExceptionMapper<TException> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractContextExceptionMapper.class);

    private final Provider<HttpServletRequest> requestProvider;

    private final Collection<String> noContextPaths;


    public AbstractContextExceptionMapper(Provider<HttpServletRequest> requestProvider) {
        this.requestProvider = requestProvider;
        noContextPaths = new ArrayList<>();
        noContextPaths.add(Urls.SharedUrls.SERVICE_NAME_ROOT);
        noContextPaths.add(Urls.SamlProxyUrls.SAML2_SSO_SENDER_API_ROOT);
        noContextPaths.add(Urls.SamlProxyUrls.SAML2_SSO_RECEIVER_API_ROOT);
        noContextPaths.add(Urls.SamlProxyUrls.SAML2_SSO_RECEIVER_API_RESOURCE);
    }

    @Override
    public final Response toResponse(TException exception) {
        if (exception instanceof NotFoundException) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else if (noSessionIdInQueryString() && inARequestWhereWeExpectContext()) {
            LOG.error(MessageFormat.format("No Session Id found for request to: {0}", requestProvider.get().getRequestURI()), exception);
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorStatusDto.createUnauditedErrorStatus(UUID.randomUUID(), ExceptionType.UNKNOWN, exception.getMessage()))
                    .build();
        }

        return handleException(exception);
    }

    protected abstract Response handleException(TException exception);

    protected Optional<SessionId> getSessionId() {
        final String parameter;

        final String sessionIdParam = requestProvider.get().getParameter(Urls.SharedUrls.SESSION_ID_PARAM);
        if (!Strings.isNullOrEmpty(sessionIdParam)) {
            parameter = sessionIdParam;
        } else {
            parameter = requestProvider.get().getParameter(Urls.SharedUrls.RELAY_STATE_PARAM);
        }

        if (Strings.isNullOrEmpty(parameter)) {
            return Optional.empty();
        } else {
            return Optional.of(new SessionId(parameter));
        }
    }

    private boolean inARequestWhereWeExpectContext() {
        return !noContextPaths.contains(requestProvider.get().getRequestURI());
    }

    private boolean noSessionIdInQueryString() {
        return getSessionId().isEmpty();
    }

}
