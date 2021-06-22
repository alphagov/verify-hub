package uk.gov.ida.integrationtest.hub.policy;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import com.google.inject.util.Modules;
import io.dropwizard.setup.Environment;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.mockito.Mock;
import uk.gov.ida.hub.policy.PolicyModule;
import uk.gov.ida.hub.policy.configuration.PolicyConfiguration;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.StateController;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.controller.AuthnFailedErrorStateController;
import uk.gov.ida.hub.policy.domain.controller.AwaitingCycle3DataStateController;
import uk.gov.ida.hub.policy.domain.controller.Cycle0And1MatchRequestSentStateController;
import uk.gov.ida.hub.policy.domain.controller.Cycle3DataInputCancelledStateController;
import uk.gov.ida.hub.policy.domain.controller.Cycle3MatchRequestSentStateController;
import uk.gov.ida.hub.policy.domain.controller.FraudEventDetectedStateController;
import uk.gov.ida.hub.policy.domain.controller.IdpSelectedStateController;
import uk.gov.ida.hub.policy.domain.controller.MatchingServiceRequestErrorStateController;
import uk.gov.ida.hub.policy.domain.controller.NoMatchStateController;
import uk.gov.ida.hub.policy.domain.controller.RequesterErrorStateController;
import uk.gov.ida.hub.policy.domain.controller.SessionStartedStateController;
import uk.gov.ida.hub.policy.domain.controller.StateControllerFactory;
import uk.gov.ida.hub.policy.domain.controller.SuccessfulMatchStateController;
import uk.gov.ida.hub.policy.domain.controller.TimeoutStateController;
import uk.gov.ida.hub.policy.domain.controller.UserAccountCreatedStateController;
import uk.gov.ida.hub.policy.domain.controller.UserAccountCreationFailedStateController;
import uk.gov.ida.hub.policy.domain.controller.UserAccountCreationRequestSentStateController;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.hub.policy.proxy.IdentityProvidersConfigProxy;
import uk.gov.ida.hub.shared.eventsink.EventSinkProxy;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.RedisTestRule;
import uk.gov.ida.jerseyclient.JsonClient;

import javax.ws.rs.client.Client;
import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder.aSessionId;
import static uk.gov.ida.hub.policy.builder.state.AuthnFailedErrorStateBuilder.anAuthnFailedErrorState;
import static uk.gov.ida.hub.policy.builder.state.AwaitingCycle3DataStateBuilder.anAwaitingCycle3DataState;
import static uk.gov.ida.hub.policy.builder.state.Cycle0And1MatchRequestSentStateBuilder.aCycle0And1MatchRequestSentState;
import static uk.gov.ida.hub.policy.builder.state.Cycle3DataInputCancelledStateBuilder.aCycle3DataInputCancelledState;
import static uk.gov.ida.hub.policy.builder.state.Cycle3MatchRequestSentStateBuilder.aCycle3MatchRequestSentState;
import static uk.gov.ida.hub.policy.builder.state.FraudEventDetectedStateBuilder.aFraudEventDetectedState;
import static uk.gov.ida.hub.policy.builder.state.IdpSelectedStateBuilder.anIdpSelectedState;
import static uk.gov.ida.hub.policy.builder.state.MatchingServiceRequestErrorStateBuilder.aMatchingServiceRequestErrorState;
import static uk.gov.ida.hub.policy.builder.state.NoMatchStateBuilder.aNoMatchState;
import static uk.gov.ida.hub.policy.builder.state.RequesterErrorStateBuilder.aRequesterErrorState;
import static uk.gov.ida.hub.policy.builder.state.SessionStartedStateBuilder.aSessionStartedState;
import static uk.gov.ida.hub.policy.builder.state.SuccessfulMatchStateBuilder.aSuccessfulMatchState;
import static uk.gov.ida.hub.policy.builder.state.TimeoutStateBuilder.aTimeoutState;
import static uk.gov.ida.hub.policy.builder.state.UserAccountCreatedStateBuilder.aUserAccountCreatedState;
import static uk.gov.ida.hub.policy.builder.state.UserAccountCreationFailedStateBuilder.aUserAccountCreationFailedState;
import static uk.gov.ida.hub.policy.builder.state.UserAccountCreationRequestSentStateBuilder.aUserAccountCreationRequestSentState;
import static uk.gov.ida.integrationtest.hub.policy.builders.PolicyConfigurationBuilder.aPolicyConfiguration;

public class StateControllerFactoryTest {

    private static final int REDIS_PORT = 6382;

    @Mock
    private StateTransitionAction stateTransitionAction;

    @ClassRule
    public static ExternalResource redis = new RedisTestRule(REDIS_PORT);

    private StateControllerFactory factory;

    @Before
    public void setup() {
        Injector injector = Guice.createInjector(
            Modules.override(new PolicyModule()
            ).with(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(EventSinkProxy.class).toInstance(mock(EventSinkProxy.class));
                    bind(IdentityProvidersConfigProxy.class).toInstance(mock(IdentityProvidersConfigProxy.class));
                    bind(Client.class).toInstance(mock(Client.class));
                    bind(Environment.class).toInstance(mock(Environment.class));
                    bind(PolicyConfiguration.class).toInstance(aPolicyConfiguration().withRedisPort(REDIS_PORT).build());
                    bind(HubEventLogger.class).toInstance(mock(HubEventLogger.class));
                    bind(JsonClient.class).annotatedWith(Names.named("samlSoapProxyClient")).toInstance(mock(JsonClient.class));
                    bind(JsonClient.class).toInstance(mock(JsonClient.class));
                }
            })
        );

        factory = new StateControllerFactory(injector);
    }

    @Test
    public void build_shouldCreateASessionStartedStateController() {
        StateController controller = factory.build(aSessionStartedState().build(), stateTransitionAction);

        assertThat(controller).isInstanceOf(SessionStartedStateController.class);
    }

    @Test
    public void build_shouldCreateAnIdpSelectedController() {
        StateController controller = factory.build(anIdpSelectedState().build(), stateTransitionAction);

        assertThat(controller).isInstanceOf(IdpSelectedStateController.class);
    }

    @Test
    public void build_shouldCreateAnCycle01MatchRequestSentController() {
        StateController controller = factory.build(aCycle0And1MatchRequestSentState().build(), stateTransitionAction);

        assertThat(controller).isInstanceOf(Cycle0And1MatchRequestSentStateController.class);
    }

    @Test
    public void build_shouldCreateASuccessfulMatchController() {
        StateController controller = factory.build(aSuccessfulMatchState().build(), stateTransitionAction);

        assertThat(controller).isInstanceOf(SuccessfulMatchStateController.class);
    }

    @Test
    public void build_shouldCreateANoMatchController() {
        StateController controller = factory.build(aNoMatchState().build(), stateTransitionAction);

        assertThat(controller).isInstanceOf(NoMatchStateController.class);

    }

    @Test
    public void build_shouldCreateAnAwaitingCycle3DataController() {
        StateController controller = factory.build(anAwaitingCycle3DataState().build(), stateTransitionAction);

        assertThat(controller).isInstanceOf(AwaitingCycle3DataStateController.class);
    }

    @Test
    public void build_shouldCreateAUserAccountCreatedController() {
        StateController controller = factory.build(aUserAccountCreatedState().build(), stateTransitionAction);

        assertThat(controller).isInstanceOf(UserAccountCreatedStateController.class);
    }

    @Test
    public void build_shouldCreateACycle3MatchRequestDataSentController() {
        StateController controller = factory.build(aCycle3MatchRequestSentState().build(), stateTransitionAction);

        assertThat(controller).isInstanceOf(Cycle3MatchRequestSentStateController.class);
    }

    @Test
    public void build_shouldCreateTimeoutStateController() {
        StateController controller = factory.build(aTimeoutState().build(), stateTransitionAction);

        assertThat(controller).isInstanceOf(TimeoutStateController.class);
    }

    @Test
    public void build_shouldCreateMatchingServiceRequestErrorController() {
        StateController controller = factory.build(aMatchingServiceRequestErrorState().build(), stateTransitionAction);

        assertThat(controller).isInstanceOf(MatchingServiceRequestErrorStateController.class);
    }

    @Test
    public void shouldCreateAUserAccountCreationRequestSentStateController() {
        StateController controller = factory.build(aUserAccountCreationRequestSentState().build(), stateTransitionAction);

        assertThat(controller).isInstanceOf(UserAccountCreationRequestSentStateController.class);
    }

    @Test
    public void build_shouldCreateAuthnFailedErrorController() {
        StateController controller = factory.build(anAuthnFailedErrorState().build(), stateTransitionAction);

        assertThat(controller).isInstanceOf(AuthnFailedErrorStateController.class);
    }

    @Test
    public void shouldCreateAFraudEventDetectedStateController() {
        StateController controller = factory.build(aFraudEventDetectedState().build(), stateTransitionAction);

        assertThat(controller).isInstanceOf(FraudEventDetectedStateController.class);
    }

    @Test
    public void shouldCreateARequesterErrorStateController() {
        StateController controller = factory.build(aRequesterErrorState().build(), stateTransitionAction);

        assertThat(controller).isInstanceOf(RequesterErrorStateController.class);
    }

    @Test
    public void build_shouldCreateCycle3DataInputCancelledController() {
        StateController controller = factory.build(aCycle3DataInputCancelledState().build(), stateTransitionAction);

        assertThat(controller).isInstanceOf(Cycle3DataInputCancelledStateController.class);
    }

    @Test
    public void build_shouldCreateUserAccountCreationFailedStateController() {
        StateController controller = factory.build(aUserAccountCreationFailedState().build(), stateTransitionAction);

        assertThat(controller).isInstanceOf(UserAccountCreationFailedStateController.class);
    }

    @Test(expected = RuntimeException.class)
    public void build_shouldThrowRuntimeExceptionIfControllerNotFound() {
        factory.build(
            new AbstractState(
                "requestId",
                "requestIssuerId",
                DateTime.now(),
                URI.create("/some-ac-service-uri"),
                aSessionId().build(),
                null
            ) {
                @Override
                public Optional<String> getRelayState() {
                    return Optional.empty();
                }
            },
            mock(StateTransitionAction.class)
        );
    }
}
