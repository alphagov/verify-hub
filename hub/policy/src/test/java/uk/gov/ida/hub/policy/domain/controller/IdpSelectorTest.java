package uk.gov.ida.hub.policy.domain.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.ida.hub.policy.builder.state.IdpSelectedStateBuilder;
import uk.gov.ida.hub.policy.builder.state.SessionStartedStateBuilder;
import uk.gov.ida.hub.policy.domain.IdpConfigDto;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.exception.StateProcessingValidationException;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.hub.policy.proxy.IdentityProvidersConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class IdpSelectorTest {
    private static final String IDP_ENTITY_ID = "IDP-a";
    private static final String OTHER_IDP_ENTITY_ID = "idp-b";
    private static final LevelOfAssurance REQUESTED_LOA = LevelOfAssurance.LEVEL_2;

    @Mock
    private TransactionsConfigProxy transactionsConfigProxy;
    @Mock
    private IdentityProvidersConfigProxy identityProvidersConfigProxy;

    @BeforeEach
    public void setUp() {
        IdpConfigDto idpConfigDto = new IdpConfigDto(IDP_ENTITY_ID, true, List.of(LevelOfAssurance.LEVEL_2, LevelOfAssurance.LEVEL_1));
        when(identityProvidersConfigProxy.getIdpConfig(IDP_ENTITY_ID)).thenReturn(idpConfigDto);
    }

    @Test
    public void buildIdpSelectedState_shouldReturnStateWithIdpSelectedState(){
        IdpSelectedState state = IdpSelectedStateBuilder.anIdpSelectedState().withRelayState("relay-state").withIdpEntityId(IDP_ENTITY_ID)
                .withRegistration(true).withAvailableIdentityProviders(List.of(IDP_ENTITY_ID)).withRegistration(true).build();
        when(transactionsConfigProxy.getLevelsOfAssurance(state.getRequestIssuerEntityId())).thenReturn(asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2));
        when(identityProvidersConfigProxy.getEnabledIdentityProvidersForAuthenticationRequestGeneration(state.getRequestIssuerEntityId(), state.isRegistering(), REQUESTED_LOA)).thenReturn(singletonList(IDP_ENTITY_ID));

        IdpSelectedState idpSelectedState = IdpSelector.buildIdpSelectedState(state, IDP_ENTITY_ID, true, REQUESTED_LOA, transactionsConfigProxy, identityProvidersConfigProxy);

        assertThat(idpSelectedState).isEqualToComparingFieldByField(state);
    }

    @Test
    public void buildIdpSelectedState_shouldReturnStateWithNewIdpForIdpSelectedState(){
        IdpSelectedState state = IdpSelectedStateBuilder.anIdpSelectedState().withRelayState("relay-state").withIdpEntityId("idp-b")
                .withAvailableIdentityProviders(List.of(IDP_ENTITY_ID, OTHER_IDP_ENTITY_ID)).withRegistration(true).build();
        IdpConfigDto idpConfigDto = new IdpConfigDto(IDP_ENTITY_ID, true, List.of(LevelOfAssurance.LEVEL_2, LevelOfAssurance.LEVEL_1));
        when(identityProvidersConfigProxy.getIdpConfig("idp-b")).thenReturn(idpConfigDto);
        when(transactionsConfigProxy.getLevelsOfAssurance(state.getRequestIssuerEntityId())).thenReturn(asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2));
        when(identityProvidersConfigProxy.getEnabledIdentityProvidersForAuthenticationRequestGeneration(state.getRequestIssuerEntityId(), state.isRegistering(), REQUESTED_LOA)).thenReturn(asList(IDP_ENTITY_ID, OTHER_IDP_ENTITY_ID));

        IdpSelectedState idpSelectedState = IdpSelector.buildIdpSelectedState(state, "idp-b", true, REQUESTED_LOA, transactionsConfigProxy, identityProvidersConfigProxy);

        assertThat(idpSelectedState).isEqualToComparingFieldByField(state);
    }

    @Test
    public void buildIdpSelectedState_shouldReturnStateWithSessionStartedState(){
        SessionStartedState state = SessionStartedStateBuilder.aSessionStartedState().build();
        when(transactionsConfigProxy.getLevelsOfAssurance(state.getRequestIssuerEntityId())).thenReturn(asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2));
        when(identityProvidersConfigProxy.getEnabledIdentityProvidersForAuthenticationRequestGeneration(state.getRequestIssuerEntityId(), true, REQUESTED_LOA)).thenReturn(singletonList(IDP_ENTITY_ID));

        IdpSelectedState idpSelectedState = IdpSelector.buildIdpSelectedState(state, IDP_ENTITY_ID, true, REQUESTED_LOA, transactionsConfigProxy, identityProvidersConfigProxy);

        assertThat(idpSelectedState.getRelayState()).isEqualTo(state.getRelayState());
        assertThat(idpSelectedState.getIdpEntityId()).isEqualTo(IDP_ENTITY_ID);
        assertThat(idpSelectedState.getRequestIssuerEntityId()).isEqualTo(state.getRequestIssuerEntityId());
        assertThat(idpSelectedState.getAvailableIdentityProviders()).isEqualTo(singletonList(IDP_ENTITY_ID));
        assertThat(idpSelectedState.getForceAuthentication()).isEqualTo(state.getForceAuthentication());
        assertThat(idpSelectedState.getLevelsOfAssurance()).containsSequence(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2);
        assertThat(idpSelectedState.getSessionExpiryTimestamp()).isEqualTo(state.getSessionExpiryTimestamp());
    }

    @Test
    public void buildIdpSelectedState_shouldReturnStateWithCorrectSequenceOfLOAsWhenIDPSupportsRegistration() {
        SessionStartedState state = SessionStartedStateBuilder.aSessionStartedState().build();
        when(transactionsConfigProxy.getLevelsOfAssurance(state.getRequestIssuerEntityId())).thenReturn(asList(LevelOfAssurance.LEVEL_2, LevelOfAssurance.LEVEL_1));
        when(identityProvidersConfigProxy.getEnabledIdentityProvidersForAuthenticationRequestGeneration(state.getRequestIssuerEntityId(), true, REQUESTED_LOA)).thenReturn(singletonList(IDP_ENTITY_ID));
        when(identityProvidersConfigProxy.isIDPEnabledForRegistration(IDP_ENTITY_ID, state.getRequestIssuerEntityId(), REQUESTED_LOA)).thenReturn(true);
        IdpSelectedState idpSelectedState = IdpSelector.buildIdpSelectedState(state, IDP_ENTITY_ID, true, REQUESTED_LOA, transactionsConfigProxy, identityProvidersConfigProxy);

        assertThat(idpSelectedState.getRelayState()).isEqualTo(state.getRelayState());
        assertThat(idpSelectedState.getIdpEntityId()).isEqualTo(IDP_ENTITY_ID);
        assertThat(idpSelectedState.getRequestIssuerEntityId()).isEqualTo(state.getRequestIssuerEntityId());
        assertThat(idpSelectedState.getAvailableIdentityProviders()).isEqualTo(singletonList(IDP_ENTITY_ID));
        assertThat(idpSelectedState.getForceAuthentication()).isEqualTo(state.getForceAuthentication());
        assertThat(idpSelectedState.getLevelsOfAssurance()).containsSequence(LevelOfAssurance.LEVEL_2, LevelOfAssurance.LEVEL_1);
        assertThat(idpSelectedState.getSessionExpiryTimestamp()).isEqualTo(state.getSessionExpiryTimestamp());
    }

    @Test
    public void buildIdpSelectedState_shouldReturnStateWithCorrectSequenceOfLOAsWhenIDPDoesNotSupportRegistration() {
        SessionStartedState state = SessionStartedStateBuilder.aSessionStartedState().build();
        when(transactionsConfigProxy.getLevelsOfAssurance(state.getRequestIssuerEntityId())).thenReturn(asList(LevelOfAssurance.LEVEL_2, LevelOfAssurance.LEVEL_1));
        when(identityProvidersConfigProxy.getEnabledIdentityProvidersForAuthenticationRequestGeneration(state.getRequestIssuerEntityId(), true, REQUESTED_LOA)).thenReturn(singletonList(IDP_ENTITY_ID));
        when(identityProvidersConfigProxy.isIDPEnabledForRegistration(IDP_ENTITY_ID, state.getRequestIssuerEntityId(), REQUESTED_LOA)).thenReturn(false);
        IdpSelectedState idpSelectedState = IdpSelector.buildIdpSelectedState(state, IDP_ENTITY_ID, true, REQUESTED_LOA, transactionsConfigProxy, identityProvidersConfigProxy);

        assertThat(idpSelectedState.getRelayState()).isEqualTo(state.getRelayState());
        assertThat(idpSelectedState.getIdpEntityId()).isEqualTo(IDP_ENTITY_ID);
        assertThat(idpSelectedState.getRequestIssuerEntityId()).isEqualTo(state.getRequestIssuerEntityId());
        assertThat(idpSelectedState.getAvailableIdentityProviders()).isEqualTo(singletonList(IDP_ENTITY_ID));
        assertThat(idpSelectedState.getForceAuthentication()).isEqualTo(state.getForceAuthentication());
        assertThat(idpSelectedState.getLevelsOfAssurance()).containsSequence(LevelOfAssurance.LEVEL_2);
        assertThat(idpSelectedState.getSessionExpiryTimestamp()).isEqualTo(state.getSessionExpiryTimestamp());
    }

    @Test
    public void shouldRaiseAnExceptionWhenSelectedIDPDoesNotExist() {
        Assertions.assertThrows(StateProcessingValidationException.class, () -> {
            IdpSelectedState state = IdpSelectedStateBuilder.anIdpSelectedState().withIdpEntityId(IDP_ENTITY_ID).withAvailableIdentityProviders(List.of(IDP_ENTITY_ID)).build();

            IdpSelector.buildIdpSelectedState(state, "another-idp-entity-id", true, REQUESTED_LOA, transactionsConfigProxy, identityProvidersConfigProxy);
        });
    }

    @Test
    public void shouldRaiseAnExceptionWhenSelectedIDPDoesNotHaveSupportedLevelsOfAssurance() {
        Assertions.assertThrows(StateProcessingValidationException.class, () -> {
            IdpSelectedState state = IdpSelectedStateBuilder.anIdpSelectedState().withIdpEntityId(IDP_ENTITY_ID).withAvailableIdentityProviders(List.of(IDP_ENTITY_ID)).build();
            when(transactionsConfigProxy.getLevelsOfAssurance(state.getRequestIssuerEntityId())).thenReturn(asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2));

            IdpSelector.buildIdpSelectedState(state, IDP_ENTITY_ID, true, LevelOfAssurance.LEVEL_2, transactionsConfigProxy, identityProvidersConfigProxy);
        });
    }

    @Test
    public void shouldRaiseAnExceptionWhenSelectedIDPDoesNotHaveRequestedLevelOfAssurance() {
        Assertions.assertThrows(StateProcessingValidationException.class, () -> {
            IdpSelectedState state = IdpSelectedStateBuilder.anIdpSelectedState().withIdpEntityId(IDP_ENTITY_ID).withAvailableIdentityProviders(List.of(IDP_ENTITY_ID)).build();
            when(transactionsConfigProxy.getLevelsOfAssurance(state.getRequestIssuerEntityId())).thenReturn(asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2));

            IdpSelector.buildIdpSelectedState(state, IDP_ENTITY_ID, true, REQUESTED_LOA, transactionsConfigProxy, identityProvidersConfigProxy);
        });
    }

    @Test
    public void shouldRaiseAnExceptionWhenTransactionEntityDoesNotHaveRequestedLevelOfAssurance() {
        Assertions.assertThrows(StateProcessingValidationException.class, () -> {
            IdpSelectedState state = IdpSelectedStateBuilder.anIdpSelectedState().withIdpEntityId(IDP_ENTITY_ID).withAvailableIdentityProviders(List.of(IDP_ENTITY_ID)).build();
            when(transactionsConfigProxy.getLevelsOfAssurance(state.getRequestIssuerEntityId())).thenReturn(singletonList(LevelOfAssurance.LEVEL_1));

            IdpSelector.buildIdpSelectedState(state, IDP_ENTITY_ID, true, REQUESTED_LOA, transactionsConfigProxy, identityProvidersConfigProxy);
        });
    }
}
