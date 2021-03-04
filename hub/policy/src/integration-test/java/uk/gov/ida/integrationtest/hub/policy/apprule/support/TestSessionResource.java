package uk.gov.ida.integrationtest.hub.policy.apprule.support;

import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.State;
import uk.gov.ida.hub.policy.domain.state.AuthnFailedErrorState;
import uk.gov.ida.hub.policy.domain.state.AwaitingCycle3DataState;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedState;
import uk.gov.ida.hub.policy.domain.state.NonMatchingJourneySuccessState;
import uk.gov.ida.hub.policy.domain.state.SuccessfulMatchState;
import uk.gov.ida.integrationtest.hub.policy.rest.Cycle3DTO;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static uk.gov.ida.hub.policy.Urls.SharedUrls.SESSION_ID_PARAM;
import static uk.gov.ida.hub.policy.Urls.SharedUrls.SESSION_ID_PARAM_PATH;

/**
 * This resource should be used only for the purpose of creating session
 * in a specific state that the tests needs.
 * Ideal situation would be to hit the relevant resources so the state transition happens naturally adhering to domain logic,
 * however it is too complex to get to that state and requires a lot of dependency stubbing that makes the test too complex to follow.
 * Fixing the flow and dependencies is a different problem.
 */

@Path(Urls.PolicyUrls.POLICY_ROOT + "test")
@Produces(MediaType.APPLICATION_JSON)
public class TestSessionResource {

    public static final String SUCCESSFUL_MATCH_STATE = "/successful-match-state";
    public static final String IDP_SELECTED_STATE = "/idp-selected-state";
    public static final String AWAITING_CYCLE_3_DATA_STATE = "/awaiting-cycle-3-data-state";
    public static final String GET_SESSION_STATE_NAME = "/session-state-name" + SESSION_ID_PARAM_PATH;
    public static final String AUTHN_FAILED_STATE = "/session-authn-failed-state";
    public static final String NON_MATCHING_JOURNEY_SUCCESS_STATE = "/session-non-matching-journey-success-state";

    private TestSessionRepository testSessionRepository;

    @Inject
    public TestSessionResource(TestSessionRepository testSessionRepository) {
        this.testSessionRepository = testSessionRepository;
    }

    @Path(SUCCESSFUL_MATCH_STATE)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSessionInSuccessfulMatchState(TestSessionDto testSessionDto) {
        testSessionRepository.createSession(testSessionDto.getSessionId(),
                new SuccessfulMatchState(
                        testSessionDto.getRequestId(),
                        testSessionDto.getSessionExpiryTimestamp(),
                        testSessionDto.getIdentityProviderEntityId(),
                        testSessionDto.getMatchingServiceAssertion(),
                        testSessionDto.getRelayState().orElse(null),
                        testSessionDto.getRequestIssuerId(),
                        testSessionDto.getAssertionConsumerServiceUri(),
                        testSessionDto.getSessionId(),
                        testSessionDto.getLevelsOfAssurance().get(testSessionDto.getLevelsOfAssurance().size()-1),
                        testSessionDto.isRegistering())
                        );
        return Response.ok().build();
    }

    @Path(IDP_SELECTED_STATE)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSessionInIdpSelectedState(TestSessionDto testSessionDto) {
        testSessionRepository.createSession(testSessionDto.getSessionId(),
                new IdpSelectedState(testSessionDto.getRequestId(),
                        testSessionDto.getIdentityProviderEntityId(),
                        testSessionDto.getLevelsOfAssurance(),
                        testSessionDto.getUseExactComparisonType(),
                        testSessionDto.getForceAuthentication().orElse(null),
                        testSessionDto.getAssertionConsumerServiceUri(),
                        testSessionDto.getRequestIssuerId(),
                        testSessionDto.getRelayState().orElse(null),
                        testSessionDto.getSessionExpiryTimestamp(),
                        testSessionDto.isRegistering(),
                        testSessionDto.getRequestedLoa(),
                        testSessionDto.getSessionId(),
                        testSessionDto.getAvailableIdentityProviders()
                )
        );
        return Response.ok().build();
    }

    @Path(AWAITING_CYCLE_3_DATA_STATE)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSessionInAwaitingCycle3DataState(Cycle3DTO dto) {
        testSessionRepository.createSession(
                dto.getSessionId(),
                new AwaitingCycle3DataState(dto.getRequestId(),
                        dto.getIdentityProviderEntityId(),
                        dto.getSessionExpiryTimestamp(),
                        dto.getRequestIssuerId(),
                        dto.getEncryptedMatchingDatasetAssertion(),
                        dto.getAuthnStatementAssertion(),
                        dto.getRelayState().orElse(null),
                        dto.getAssertionConsumerServiceUri(),
                        dto.getMatchingServiceEntityId(),
                        dto.getSessionId(),
                        dto.getPersistentId(),
                        dto.getLevelOfAssurance(),
                        dto.isRegistering()));

        return Response.ok().build();
    }

    @Path(AUTHN_FAILED_STATE)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSessionInAuthnFailedState(TestSessionDto testSessionDto) {
        testSessionRepository.createSession(testSessionDto.getSessionId(),
                new AuthnFailedErrorState(testSessionDto.getRequestId(),
                        testSessionDto.getRequestIssuerId(),
                        testSessionDto.getSessionExpiryTimestamp(),
                        testSessionDto.getAssertionConsumerServiceUri(),
                        testSessionDto.getRelayState().orElse(null),
                        testSessionDto.getSessionId(),
                        testSessionDto.getIdentityProviderEntityId(),
                        testSessionDto.getForceAuthentication().orElse(null))
        );
        return Response.ok().build();
    }

    @Path(NON_MATCHING_JOURNEY_SUCCESS_STATE)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSessionInNonMatchingJourneySuccessState(TestSessionDto testSessionDto) {
        testSessionRepository.createSession(testSessionDto.getSessionId(),
                new NonMatchingJourneySuccessState(
                        testSessionDto.getRequestId(),
                        testSessionDto.getRequestIssuerId(),
                        testSessionDto.getSessionExpiryTimestamp(),
                        testSessionDto.getAssertionConsumerServiceUri(),
                        testSessionDto.getSessionId(),
                        testSessionDto.getRelayState().orElse(null),
                        testSessionDto.getEncryptedAssertions()
                )
        );
        return Response.ok().build();
    }

    @Path(GET_SESSION_STATE_NAME)
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getSessionStateName(@PathParam(SESSION_ID_PARAM) SessionId sessionId) {
        State session = testSessionRepository.getSession(sessionId);
        return Response.ok(session.getClass().getName()).build();
    }
}
