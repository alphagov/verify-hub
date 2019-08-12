package uk.gov.ida.hub.policy.filters;

import org.jboss.logging.MDC;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

public class SessionIdQueryParamLoggingFilter implements Filter {

    @Override
    public void init(final FilterConfig filterConfig) {
        // this method intentionally left blank
    }

    @Override
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain filterChain) throws IOException, ServletException {
        String sessionId = servletRequest.getParameter("sessionId");
        if (sessionId != null) {
            MDC.put("SessionId", sessionId);
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {
        // this method intentionally left blank
    }
}