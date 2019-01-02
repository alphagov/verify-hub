package uk.gov.ida.hub.samlproxy.exceptions;

import java.util.Optional;
import com.google.common.base.Strings;
import com.google.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.hub.samlproxy.Urls;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;

public abstract class AbstractContextExceptionMapper<TException extends Exception> implements ExceptionMapper<TException> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractContextExceptionMapper.class);

    private final Provider<HttpServletRequest> context;

    private final Collection<String> noContextPaths;


    public AbstractContextExceptionMapper(Provider<HttpServletRequest> context) {
        this.context = context;
        noContextPaths = new ArrayList<>();
        noContextPaths.add(Urls.SharedUrls.SERVICE_NAME_ROOT);
        noContextPaths.add(Urls.SamlProxyUrls.SAML2_SSO_RECEIVER_API_ROOT);
        noContextPaths.add(Urls.SamlProxyUrls.SAML2_SSO_RECEIVER_API_RESOURCE);
        noContextPaths.add(Urls.SamlProxyUrls.SAML2_SSO_SENDER_API_ROOT);
    }

    @Override
    public final Response toResponse(TException exception) {
        if (exception instanceof NotFoundException) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else if (noSessionIdInQueryString() && inARequestWhereWeExpectContext()) {
            LOG.error(MessageFormat.format("No Session Id found for request to: {0}", context.get().getRequestURI()), exception);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return handleException(exception);
    }

    protected abstract Response handleException(TException exception);

    protected Optional<SessionId> getSessionId() {
        String parameter = context.get().getParameter(Urls.SharedUrls.SESSION_ID_PARAM);
        if (Strings.isNullOrEmpty(parameter)) {
            parameter = context.get().getParameter(Urls.SharedUrls.RELAY_STATE_PARAM);
        }
        if (Strings.isNullOrEmpty(parameter)) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(new SessionId(parameter));
        }
    }

    private boolean inARequestWhereWeExpectContext() {
        return !noContextPaths.contains(context.get().getRequestURI());
    }

    private boolean noSessionIdInQueryString() {
        return !getSessionId().isPresent();
    }

}
