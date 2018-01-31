package uk.gov.ida.hub.policy.domain.controller;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.hub.policy.builder.state.IdpSelectedStateBuilder;
import uk.gov.ida.hub.policy.builder.state.SessionStartedStateBuilder;
import uk.gov.ida.hub.policy.domain.IdpConfigDto;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.exception.StateProcessingValidationException;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedStateTransitional;
import uk.gov.ida.hub.policy.domain.state.SessionStartedStateTransitional;
import uk.gov.ida.hub.policy.proxy.IdentityProvidersConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
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
    public void setUp() throws Exception {
        IdpConfigDto idpConfigDto = new IdpConfigDto(IDP_ENTITY_ID, true, ImmutableList.of(LevelOfAssurance.LEVEL_2, LevelOfAssurance.LEVEL_1));
        when(identityProvidersConfigProxy.getIdpConfig(IDP_ENTITY_ID)).thenReturn(idpConfigDto);
    }

    @Test
    public void buildIdpSelectedState_shouldReturnStateWithIdpSelectedState(){
        IdpSelectedStateTransitional state = IdpSelectedStateBuilder.anIdpSelectedState().withRelayState("relay-state").withIdpEntityId(IDP_ENTITY_ID)
                .withRegistration(true).withAvailableIdentityProviders(ImmutableList.of(IDP_ENTITY_ID)).withRegistration(true).build();
        when(transactionsConfigProxy.getLevelsOfAssurance(state.getRequestIssuerEntityId())).thenReturn(asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2));
        when(transactionsConfigProxy.getMatchingServiceEntityId(state.getRequestIssuerEntityId())).thenReturn(state.getMatchingServiceEntityId());
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(state.getRequestIssuerEntityId(), state.isRegistering(), REQUESTED_LOA)).thenReturn(singletonList(IDP_ENTITY_ID));

        IdpSelectedStateTransitional idpSelectedState = IdpSelector.buildIdpSelectedState(state, IDP_ENTITY_ID, true, REQUESTED_LOA, transactionsConfigProxy, identityProvidersConfigProxy);

        assertThat(idpSelectedState).isEqualToComparingFieldByField(state);
    }

    @Test
    public void buildIdpSelectedState_shouldReturnStateWithNewIdpForIdpSelectedState(){
        IdpSelectedStateTransitional state = IdpSelectedStateBuilder.anIdpSelectedState().withRelayState("relay-state").withIdpEntityId("idp-b")
                .withAvailableIdentityProviders(ImmutableList.of(IDP_ENTITY_ID, OTHER_IDP_ENTITY_ID)).withRegistration(true).build();
        IdpConfigDto idpConfigDto = new IdpConfigDto(IDP_ENTITY_ID, true, ImmutableList.of(LevelOfAssurance.LEVEL_2, LevelOfAssurance.LEVEL_1));
        when(identityProvidersConfigProxy.getIdpConfig("idp-b")).thenReturn(idpConfigDto);
        when(transactionsConfigProxy.getLevelsOfAssurance(state.getRequestIssuerEntityId())).thenReturn(asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2));
        when(transactionsConfigProxy.getMatchingServiceEntityId(state.getRequestIssuerEntityId())).thenReturn(state.getMatchingServiceEntityId());
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(state.getRequestIssuerEntityId(), state.isRegistering(), REQUESTED_LOA)).thenReturn(asList(IDP_ENTITY_ID, OTHER_IDP_ENTITY_ID));

        IdpSelectedStateTransitional idpSelectedState = IdpSelector.buildIdpSelectedState(state, "idp-b", true, REQUESTED_LOA, transactionsConfigProxy, identityProvidersConfigProxy);

        assertThat(idpSelectedState).isEqualToComparingFieldByField(state);
    }

    @Test
    public void buildIdpSelectedState_shouldReturnStateWithSessionStartedState(){
        SessionStartedStateTransitional state = SessionStartedStateBuilder.aSessionStartedState().build();
        when(transactionsConfigProxy.getLevelsOfAssurance(state.getRequestIssuerEntityId())).thenReturn(asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2));
        when(transactionsConfigProxy.getMatchingServiceEntityId(state.getRequestIssuerEntityId())).thenReturn("matching-service-id");
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(state.getRequestIssuerEntityId(), true, REQUESTED_LOA)).thenReturn(singletonList(IDP_ENTITY_ID));

        IdpSelectedStateTransitional idpSelectedState = IdpSelector.buildIdpSelectedState(state, IDP_ENTITY_ID, true, REQUESTED_LOA, transactionsConfigProxy, identityProvidersConfigProxy);

        assertThat(idpSelectedState.getRelayState()).isEqualTo(state.getRelayState());
        assertThat(idpSelectedState.getIdpEntityId()).isEqualTo(IDP_ENTITY_ID);
        assertThat(idpSelectedState.getRequestIssuerEntityId()).isEqualTo(state.getRequestIssuerEntityId());
        assertThat(idpSelectedState.getAvailableIdentityProviderEntityIds()).isEqualTo(singletonList(IDP_ENTITY_ID));
        assertThat(idpSelectedState.getMatchingServiceEntityId()).isEqualTo("matching-service-id");
        assertThat(idpSelectedState.getForceAuthentication()).isEqualTo(state.getForceAuthentication());
        assertThat(idpSelectedState.getLevelsOfAssurance()).containsSequence(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2);
        assertThat(idpSelectedState.getSessionExpiryTimestamp()).isEqualTo(state.getSessionExpiryTimestamp());
    }

    @Test(expected= StateProcessingValidationException.class)
         public void shouldRaiseAnExceptionWhenSelectedIDPDoesNotExist() {
        IdpSelectedStateTransitional state = IdpSelectedStateBuilder.anIdpSelectedState().withIdpEntityId(IDP_ENTITY_ID).withAvailableIdentityProviders(ImmutableList.of(IDP_ENTITY_ID)).build();
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(state.getRequestIssuerEntityId(), state.isRegistering(), REQUESTED_LOA)).thenReturn(singletonList(IDP_ENTITY_ID));

        IdpSelector.buildIdpSelectedState(state, "another-idp-entity-id", true, REQUESTED_LOA, transactionsConfigProxy, identityProvidersConfigProxy);
    }

    @Test(expected= StateProcessingValidationException.class)
    public void shouldRaiseAnExceptionWhenSelectedIDPDoesNotHaveSupportedLevelsOfAssurance() {
        IdpSelectedStateTransitional state = IdpSelectedStateBuilder.anIdpSelectedState().withIdpEntityId(IDP_ENTITY_ID).withAvailableIdentityProviders(ImmutableList.of(IDP_ENTITY_ID)).build();
        when(transactionsConfigProxy.getLevelsOfAssurance(state.getRequestIssuerEntityId())).thenReturn(asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2));
        when(identityProvidersConfigProxy.getIdpConfig(IDP_ENTITY_ID)).thenReturn(new IdpConfigDto(IDP_ENTITY_ID, true, ImmutableList.of()));
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(state.getRequestIssuerEntityId(), state.isRegistering(), REQUESTED_LOA)).thenReturn(emptyList());

        IdpSelector.buildIdpSelectedState(state, IDP_ENTITY_ID, true, LevelOfAssurance.LEVEL_2, transactionsConfigProxy, identityProvidersConfigProxy);
    }

    @Test(expected= StateProcessingValidationException.class)
    public void shouldRaiseAnExceptionWhenSelectedIDPDoesNotHaveRequestedLevelOfAssurance() {
        IdpSelectedStateTransitional state = IdpSelectedStateBuilder.anIdpSelectedState().withIdpEntityId(IDP_ENTITY_ID).withAvailableIdentityProviders(ImmutableList.of(IDP_ENTITY_ID)).build();
        when(transactionsConfigProxy.getLevelsOfAssurance(state.getRequestIssuerEntityId())).thenReturn(asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2));
        when(identityProvidersConfigProxy.getIdpConfig(IDP_ENTITY_ID)).thenReturn(new IdpConfigDto(IDP_ENTITY_ID, true, ImmutableList.of(LevelOfAssurance.LEVEL_1)));
        when(identityProvidersConfigProxy.getIdpConfig(OTHER_IDP_ENTITY_ID)).thenReturn(new IdpConfigDto(OTHER_IDP_ENTITY_ID, true, ImmutableList.of(REQUESTED_LOA)));
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(state.getRequestIssuerEntityId(), state.isRegistering(), REQUESTED_LOA)).thenReturn(singletonList(OTHER_IDP_ENTITY_ID));

        IdpSelector.buildIdpSelectedState(state, IDP_ENTITY_ID, true, REQUESTED_LOA, transactionsConfigProxy, identityProvidersConfigProxy);
    }

    @Test(expected= StateProcessingValidationException.class)
    public void shouldRaiseAnExceptionWhenTransactionEntityDoesNotHaveRequestedLevelOfAssurance() {
        IdpSelectedStateTransitional state = IdpSelectedStateBuilder.anIdpSelectedState().withIdpEntityId(IDP_ENTITY_ID).withAvailableIdentityProviders(ImmutableList.of(IDP_ENTITY_ID)).build();
        when(transactionsConfigProxy.getLevelsOfAssurance(state.getRequestIssuerEntityId())).thenReturn(singletonList(LevelOfAssurance.LEVEL_1));
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(state.getRequestIssuerEntityId(), state.isRegistering(), REQUESTED_LOA)).thenReturn(singletonList(IDP_ENTITY_ID));

        IdpSelector.buildIdpSelectedState(state, IDP_ENTITY_ID, true, REQUESTED_LOA, transactionsConfigProxy, identityProvidersConfigProxy);
    }
}
