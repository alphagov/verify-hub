package uk.gov.ida.integrationtest.hub.policy.apprule.support;

import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.State;
import uk.gov.ida.hub.policy.domain.state.AuthnFailedErrorState;
import uk.gov.ida.hub.policy.domain.state.AwaitingCycle3DataState;
import uk.gov.ida.hub.policy.domain.state.EidasAuthnFailedErrorState;
import uk.gov.ida.hub.policy.domain.state.EidasAwaitingCycle3DataState;
import uk.gov.ida.hub.policy.domain.state.EidasSuccessfulMatchState;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.hub.policy.domain.state.SuccessfulMatchState;
import uk.gov.ida.integrationtest.hub.policy.rest.Cycle3DTO;
import uk.gov.ida.integrationtest.hub.policy.rest.EidasCycle3DTO;

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
    public static final String EIDAS_SUCCESSFUL_MATCH_STATE = "/eidas-successful-match-state";
    public static final String IDP_SELECTED_STATE = "/idp-selected-state";
    public static final String EIDAS_COUNTRY_SELECTED_STATE = "/eidas-country-selected-state";
    public static final String AWAITING_CYCLE_3_DATA_STATE = "/awaiting-cycle-3-data-state";
    public static final String EIDAS_AWAITING_CYCLE_3_DATA_STATE = "/eidas-awaiting-cycle-3-data-state";
    public static final String GET_SESSION_STATE_NAME = "/session-state-name" + SESSION_ID_PARAM_PATH;
    public static final String AUTHN_FAILED_STATE = "/session-authn-failed-state";
    public static final String EIDAS_AUTHN_FAILED_STATE = "/session-eidas-authn-failed-state";

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
                        testSessionDto.getRelayState().orNull(),
                        testSessionDto.getRequestIssuerId(),
                        testSessionDto.getAssertionConsumerServiceUri(),
                        testSessionDto.getSessionId(),
                        testSessionDto.getLevelsOfAssurance().get(testSessionDto.getLevelsOfAssurance().size()-1),
                        testSessionDto.isRegistering(),
                        testSessionDto.getTransactionSupportsEidas())
                        );
        return Response.ok().build();
    }

    @Path(EIDAS_SUCCESSFUL_MATCH_STATE)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSessionInEidasSuccessfulMatchState(TestSessionDto testSessionDto){
        testSessionRepository.createSession(testSessionDto.getSessionId(),
                new EidasSuccessfulMatchState(testSessionDto.getRequestId(),
                        testSessionDto.getSessionExpiryTimestamp(),
                        testSessionDto.getIdentityProviderEntityId(),
                        testSessionDto.getMatchingServiceAssertion(),
                        testSessionDto.getRelayState().orNull(),
                        testSessionDto.getRequestIssuerId(),
                        testSessionDto.getAssertionConsumerServiceUri(),
                        testSessionDto.getSessionId(),
                        testSessionDto.getLevelsOfAssurance().get(testSessionDto.getLevelsOfAssurance().size()-1),
                        testSessionDto.getTransactionSupportsEidas()));
        return Response.ok().build();

    }

    @Path(IDP_SELECTED_STATE)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSessionInIdpSelectedState(TestSessionDto testSessionDto) {
        testSessionRepository.createSession(testSessionDto.getSessionId(),
                new IdpSelectedState(testSessionDto.getRequestId(),
                        testSessionDto.getIdentityProviderEntityId(),
                        testSessionDto.getMatchingServiceEntityId(),
                        testSessionDto.getLevelsOfAssurance(),
                        testSessionDto.getUseExactComparisonType(),
                        testSessionDto.getForceAuthentication().orNull(),
                        testSessionDto.getAssertionConsumerServiceUri(),
                        testSessionDto.getRequestIssuerId(),
                        testSessionDto.getRelayState().orNull(),
                        testSessionDto.getSessionExpiryTimestamp(),
                        testSessionDto.isRegistering(),
                        testSessionDto.getRequestedLoa(),
                        testSessionDto.getSessionId(),
                        testSessionDto.getAvailableIdentityProviders(),
                        testSessionDto.getTransactionSupportsEidas())
        );
        return Response.ok().build();
    }

    @Path(EIDAS_COUNTRY_SELECTED_STATE)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSessionInCountrySelectingState(TestSessionDto testSessionDto) {
        testSessionRepository.createSession(testSessionDto.getSessionId(),
                new SessionStartedState(
                        testSessionDto.getRequestId(),
                        testSessionDto.getRelayState().orNull(),
                        testSessionDto.getRequestIssuerId(),
                        testSessionDto.getAssertionConsumerServiceUri(),
                        null,
                        testSessionDto.getSessionExpiryTimestamp(),
                        testSessionDto.getSessionId(),
                        testSessionDto.getTransactionSupportsEidas()));
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
                        dto.getRelayState().orNull(),
                        dto.getAssertionConsumerServiceUri(),
                        dto.getMatchingServiceEntityId(),
                        dto.getSessionId(),
                        dto.getPersistentId(),
                        dto.getLevelOfAssurance(),
                        dto.isRegistering(),
                        dto.getTransactionSupportsEidas()));

        return Response.ok().build();
    }

    @Path(EIDAS_AWAITING_CYCLE_3_DATA_STATE)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSessionInEidasAwaitingCycle3DataState(EidasCycle3DTO dto) {
        testSessionRepository.createSession(
            dto.getSessionId(),
            new EidasAwaitingCycle3DataState(
                dto.getRequestId(),
                dto.getRequestIssuerEntityId(),
                dto.getSessionExpiryTimestamp(),
                dto.getAssertionConsumerServiceUri(),
                dto.getSessionId(),
                dto.getTransactionSupportsEidas(),
                dto.getIdentityProviderEntityId(),
                dto.getMatchingServiceAdapterEntityId(),
                dto.getRelayState(),
                dto.getPersistentId(),
                dto.getLevelOfAssurance(),
                dto.getEncryptedIdentityAssertion()
            )
        );

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
                        testSessionDto.getRelayState().orNull(),
                        testSessionDto.getSessionId(),
                        testSessionDto.getIdentityProviderEntityId(),
                        testSessionDto.getForceAuthentication().orNull(),
                        testSessionDto.getTransactionSupportsEidas())
        );
        return Response.ok().build();
    }

    @Path(EIDAS_AUTHN_FAILED_STATE)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSessionInEidasAuthnFailedState(TestSessionDto testSessionDto) {
        testSessionRepository.createSession(testSessionDto.getSessionId(),
                new EidasAuthnFailedErrorState(
                        testSessionDto.getRequestId(),
                        testSessionDto.getRequestIssuerId(),
                        testSessionDto.getSessionExpiryTimestamp(),
                        testSessionDto.getAssertionConsumerServiceUri(),
                        testSessionDto.getRelayState().orNull(),
                        testSessionDto.getSessionId(),
                        testSessionDto.getIdentityProviderEntityId(),
                        testSessionDto.getLevelsOfAssurance())
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
