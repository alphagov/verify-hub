package uk.gov.ida.hub.policy.domain.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
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

@RunWith(MockitoJUnitRunner.class)
public class IdpSelectorTest {
    private static final String IDP_ENTITY_ID = "IDP-a";
    private static final String OTHER_IDP_ENTITY_ID = "idp-b";
    private static final LevelOfAssurance REQUESTED_LOA = LevelOfAssurance.LEVEL_2;

    @Mock
    private TransactionsConfigProxy transactionsConfigProxy;
    @Mock
    private IdentityProvidersConfigProxy identityProvidersConfigProxy;

    @Before
    public void setUp() {
        IdpConfigDto idpConfigDto = new IdpConfigDto(IDP_ENTITY_ID, true, List.of(LevelOfAssurance.LEVEL_2, LevelOfAssurance.LEVEL_1));
        when(identityProvidersConfigProxy.getIdpConfig(IDP_ENTITY_ID)).thenReturn(idpConfigDto);
    }

    @Test
    public void buildIdpSelectedState_shouldReturnStateWithIdpSelectedState(){
        IdpSelectedState state = IdpSelectedStateBuilder.anIdpSelectedState().withRelayState("relay-state").withIdpEntityId(IDP_ENTITY_ID)
                .withRegistration(true).withAvailableIdentityProviders(List.of(IDP_ENTITY_ID)).withRegistration(true).build();
        when(transactionsConfigProxy.getLevelsOfAssurance(state.getRequestIssuerEntityId())).thenReturn(asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2));
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(state.getRequestIssuerEntityId(), state.isRegistering(), REQUESTED_LOA)).thenReturn(singletonList(IDP_ENTITY_ID));

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
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(state.getRequestIssuerEntityId(), state.isRegistering(), REQUESTED_LOA)).thenReturn(asList(IDP_ENTITY_ID, OTHER_IDP_ENTITY_ID));

        IdpSelectedState idpSelectedState = IdpSelector.buildIdpSelectedState(state, "idp-b", true, REQUESTED_LOA, transactionsConfigProxy, identityProvidersConfigProxy);

        assertThat(idpSelectedState).isEqualToComparingFieldByField(state);
    }

    @Test
    public void buildIdpSelectedState_shouldReturnStateWithSessionStartedState(){
        SessionStartedState state = SessionStartedStateBuilder.aSessionStartedState().build();
        when(transactionsConfigProxy.getLevelsOfAssurance(state.getRequestIssuerEntityId())).thenReturn(asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2));
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(state.getRequestIssuerEntityId(), true, REQUESTED_LOA)).thenReturn(singletonList(IDP_ENTITY_ID));

        IdpSelectedState idpSelectedState = IdpSelector.buildIdpSelectedState(state, IDP_ENTITY_ID, true, REQUESTED_LOA, transactionsConfigProxy, identityProvidersConfigProxy);

        assertThat(idpSelectedState.getRelayState()).isEqualTo(state.getRelayState());
        assertThat(idpSelectedState.getIdpEntityId()).isEqualTo(IDP_ENTITY_ID);
        assertThat(idpSelectedState.getRequestIssuerEntityId()).isEqualTo(state.getRequestIssuerEntityId());
        assertThat(idpSelectedState.getAvailableIdentityProviders()).isEqualTo(singletonList(IDP_ENTITY_ID));
        assertThat(idpSelectedState.getForceAuthentication()).isEqualTo(state.getForceAuthentication());
        assertThat(idpSelectedState.getLevelsOfAssurance()).containsSequence(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2);
        assertThat(idpSelectedState.getSessionExpiryTimestamp()).isEqualTo(state.getSessionExpiryTimestamp());
    }

    @Test(expected= StateProcessingValidationException.class)
         public void shouldRaiseAnExceptionWhenSelectedIDPDoesNotExist() {
        IdpSelectedState state = IdpSelectedStateBuilder.anIdpSelectedState().withIdpEntityId(IDP_ENTITY_ID).withAvailableIdentityProviders(List.of(IDP_ENTITY_ID)).build();

        IdpSelector.buildIdpSelectedState(state, "another-idp-entity-id", true, REQUESTED_LOA, transactionsConfigProxy, identityProvidersConfigProxy);
    }

    @Test(expected= StateProcessingValidationException.class)
    public void shouldRaiseAnExceptionWhenSelectedIDPDoesNotHaveSupportedLevelsOfAssurance() {
        IdpSelectedState state = IdpSelectedStateBuilder.anIdpSelectedState().withIdpEntityId(IDP_ENTITY_ID).withAvailableIdentityProviders(List.of(IDP_ENTITY_ID)).build();
        when(transactionsConfigProxy.getLevelsOfAssurance(state.getRequestIssuerEntityId())).thenReturn(asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2));

        IdpSelector.buildIdpSelectedState(state, IDP_ENTITY_ID, true, LevelOfAssurance.LEVEL_2, transactionsConfigProxy, identityProvidersConfigProxy);
    }

    @Test(expected= StateProcessingValidationException.class)
    public void shouldRaiseAnExceptionWhenSelectedIDPDoesNotHaveRequestedLevelOfAssurance() {
        IdpSelectedState state = IdpSelectedStateBuilder.anIdpSelectedState().withIdpEntityId(IDP_ENTITY_ID).withAvailableIdentityProviders(List.of(IDP_ENTITY_ID)).build();
        when(transactionsConfigProxy.getLevelsOfAssurance(state.getRequestIssuerEntityId())).thenReturn(asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2));

        IdpSelector.buildIdpSelectedState(state, IDP_ENTITY_ID, true, REQUESTED_LOA, transactionsConfigProxy, identityProvidersConfigProxy);
    }

    @Test(expected= StateProcessingValidationException.class)
    public void shouldRaiseAnExceptionWhenTransactionEntityDoesNotHaveRequestedLevelOfAssurance() {
        IdpSelectedState state = IdpSelectedStateBuilder.anIdpSelectedState().withIdpEntityId(IDP_ENTITY_ID).withAvailableIdentityProviders(List.of(IDP_ENTITY_ID)).build();
        when(transactionsConfigProxy.getLevelsOfAssurance(state.getRequestIssuerEntityId())).thenReturn(singletonList(LevelOfAssurance.LEVEL_1));

        IdpSelector.buildIdpSelectedState(state, IDP_ENTITY_ID, true, REQUESTED_LOA, transactionsConfigProxy, identityProvidersConfigProxy);
    }
}
