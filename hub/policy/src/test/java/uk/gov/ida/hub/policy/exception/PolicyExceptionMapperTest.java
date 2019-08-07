package uk.gov.ida.hub.policy.exception;

import org.glassfish.jersey.internal.util.collection.StringKeyIgnoreCaseMultivaluedMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.domain.SessionId;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PolicyExceptionMapperTest {

    @Mock
    private HttpServletRequest servletRequest;

    @Mock
    private UriInfo uriInfo;

    private TestExceptionMapper mapper;

    @Before
    public void setUp() {
        mapper = new TestExceptionMapper();
        mapper.setHttpServletRequest(servletRequest);
        mapper.setUriInfo(uriInfo);
    }

    @Test
    public void shouldDelegateToExceptionMapperForNewSessionResourceWithoutSessionId() {
        assertDelegateExceptionMapperIsUsedForNoContextUri(Urls.PolicyUrls.NEW_SESSION_RESOURCE);
    }

    @Test
    public void shouldDelegateToExceptionMapperForServiceNameRootWithoutSessionId() {
        assertDelegateExceptionMapperIsUsedForNoContextUri(Urls.SharedUrls.SERVICE_NAME_ROOT);
    }

    @Test
    public void shouldDelegateToExceptionMapperWhenSessionIdIsInPath() {
        when(servletRequest.getParameter(Urls.SharedUrls.SESSION_ID_PARAM)).thenReturn("");
        when(servletRequest.getParameter(Urls.SharedUrls.RELAY_STATE_PARAM)).thenReturn("");

        StringKeyIgnoreCaseMultivaluedMap<String> pathParams = new StringKeyIgnoreCaseMultivaluedMap<>();

        pathParams.add(Urls.SharedUrls.SESSION_ID_PARAM, SessionId.createNewSessionId().getSessionId());
        when(uriInfo.getPathParameters()).thenReturn(pathParams);

        String expectedMessage = "Expected message";
        Response response = mapper.toResponse(new RuntimeException(expectedMessage));

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.getEntity()).isEqualTo(expectedMessage);
    }

    @Test
    public void shouldReturnInternalServerErrorWhenThereIsNoSessionIdAndTheRequestUriIsNotAKnownNoContextPath() {
        when(servletRequest.getParameter(Urls.SharedUrls.SESSION_ID_PARAM)).thenReturn("");
        when(servletRequest.getParameter(Urls.SharedUrls.RELAY_STATE_PARAM)).thenReturn("");
        when(uriInfo.getPathParameters()).thenReturn(new StringKeyIgnoreCaseMultivaluedMap<>());
        String unknownUri = UUID.randomUUID().toString();
        when(servletRequest.getRequestURI()).thenReturn(unknownUri);

        Response response = mapper.toResponse(new RuntimeException("We don't expect to see this message"));

        assertThat(response.getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void shouldReturnResponseStatusOfNotFoundWhenANotFoundExceptionIsThrown() {
        Response response = mapper.toResponse(new NotFoundException());

        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    private void assertDelegateExceptionMapperIsUsedForNoContextUri(final String noContextUri) {
        when(servletRequest.getParameter(Urls.SharedUrls.SESSION_ID_PARAM)).thenReturn("");
        when(servletRequest.getParameter(Urls.SharedUrls.RELAY_STATE_PARAM)).thenReturn("");
        when(uriInfo.getPathParameters()).thenReturn(new StringKeyIgnoreCaseMultivaluedMap<>());
        when(servletRequest.getRequestURI()).thenReturn(noContextUri);

        String expectedMessage = "Expected message";
        Response response = mapper.toResponse(new RuntimeException(expectedMessage));

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.getEntity()).isEqualTo(expectedMessage);
    }

    private static class TestExceptionMapper extends PolicyExceptionMapper<RuntimeException> {
        @Override
        protected Response handleException(RuntimeException e) {
            return Response.ok().entity(e.getMessage()).build();
        }
    }
}
