package uk.gov.ida.hub.policy.filters;

import org.jboss.logging.MDC;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import java.util.Optional;

import static uk.gov.ida.common.CommonUrls.SESSION_ID_PARAM;

public class SessionIdPathParamLoggingFilter implements ContainerRequestFilter {

    @Context
    UriInfo uriInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        Optional.ofNullable(uriInfo.getPathParameters().getFirst(SESSION_ID_PARAM))
                .ifPresent(sessionId -> MDC.put("SessionId", sessionId));
    }

}
