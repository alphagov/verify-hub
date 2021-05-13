package uk.gov.ida.hub.policy.exception;

import com.google.common.base.Strings;
import com.google.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.domain.SessionId;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

public abstract class PolicyExceptionMapper<TException extends Exception> implements ExceptionMapper<TException> {

    private static final Logger LOG = LoggerFactory.getLogger(PolicyExceptionMapper.class);

    private final Collection<String> noContextPaths;

    private Provider<UriInfo> uriInfoProvider;

    private Provider<HttpServletRequest> servletRequestProvider;

    public PolicyExceptionMapper(
            Provider<UriInfo> uriInfoProvider,
            Provider<HttpServletRequest> servletRequestProvider) {
        this.uriInfoProvider = uriInfoProvider;
        this.servletRequestProvider = servletRequestProvider;
        noContextPaths = new ArrayList<>();
        noContextPaths.add(Urls.SharedUrls.SERVICE_NAME_ROOT);
        noContextPaths.add(Urls.PolicyUrls.NEW_SESSION_RESOURCE);
    }

    @Override
    public final Response toResponse(TException exception) {
        if (exception instanceof NotFoundException) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else if (noSessionIdInQueryStringOrPathParam() && inARequestWhereWeExpectContext()) {
            LOG.error(MessageFormat.format("No Session Id found for request to: {0}", servletRequestProvider.get().getRequestURI()), exception);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return handleException(exception);
    }

    protected abstract Response handleException(TException exception);

    protected Optional<SessionId> getSessionId() {
        // Are there any uris in Policy that contain the session id as a query string rather than as part of the path or is this just here coz it was copied from shared-rest   ?
        String parameter = servletRequestProvider.get().getParameter(Urls.SharedUrls.SESSION_ID_PARAM);
        if (Strings.isNullOrEmpty(parameter)) {
            parameter = servletRequestProvider.get().getParameter(Urls.SharedUrls.RELAY_STATE_PARAM);
        }
        if (Strings.isNullOrEmpty(parameter)) {
            MultivaluedMap<String, String> pathParameters = uriInfoProvider.get().getPathParameters();
            parameter = pathParameters.getFirst(Urls.SharedUrls.SESSION_ID_PARAM);
        }
        if (Strings.isNullOrEmpty(parameter)) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(new SessionId(parameter));
        }
    }

    private boolean inARequestWhereWeExpectContext() {
        return !noContextPaths.contains(servletRequestProvider.get().getRequestURI());
    }

    private boolean noSessionIdInQueryStringOrPathParam() {
        return !getSessionId().isPresent();
    }
}