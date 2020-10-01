package uk.gov.ida.hub.samlengine.factories;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.hub.samlengine.proxy.TransactionsConfigProxy;
import uk.gov.ida.saml.core.domain.AuthnResponseFromCountryContainerDto;
import uk.gov.ida.saml.core.domain.OutboundResponseFromHub;
import uk.gov.ida.saml.hub.transformers.outbound.OutboundAuthnResponseFromCountryContainerToStringFunction;
import uk.gov.ida.saml.hub.transformers.outbound.OutboundLegacyResponseFromHubToStringFunctionSHA256;
import uk.gov.ida.saml.hub.transformers.outbound.OutboundSamlProfileResponseFromHubToStringFunctionSHA256;
import uk.gov.ida.saml.hub.transformers.outbound.providers.SimpleProfileOutboundResponseFromHubToResponseTransformerProvider;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OutboundResponseFromHubToResponseTransformerFactoryTest {

    private OutboundResponseFromHubToResponseTransformerFactory outboundResponseFromHubToResponseTransformerFactory;

    @Mock
    private OutboundLegacyResponseFromHubToStringFunctionSHA256 outboundLegacyResponseFromHubToStringFunctionSHA256;

    @Mock
    private OutboundSamlProfileResponseFromHubToStringFunctionSHA256 outboundSamlProfileResponseFromHubToStringFunctionSHA256;

    @Mock
    private SimpleProfileOutboundResponseFromHubToResponseTransformerProvider simpleProfileOutboundResponseFromHubToResponseTransformerProvider;

    @Mock
    private Function<OutboundResponseFromHub, String> simpleProfileOutboundResponseFromHubToResponseTransformer;

    @Mock
    private OutboundAuthnResponseFromCountryContainerToStringFunction outboundAuthnResponseFromCountryContainerToStringFunction;

    @Mock
    private TransactionsConfigProxy transactionsConfigProxy;

    public static final String ENTITY_ID = "entity-id";

    @Before
    public void setUp() {
        outboundResponseFromHubToResponseTransformerFactory = new OutboundResponseFromHubToResponseTransformerFactory(
                simpleProfileOutboundResponseFromHubToResponseTransformerProvider,
                transactionsConfigProxy,
                outboundSamlProfileResponseFromHubToStringFunctionSHA256,
                outboundLegacyResponseFromHubToStringFunctionSHA256,
                outboundAuthnResponseFromCountryContainerToStringFunction);
    }


    @Test
    public void getShouldReturnOutboundLegacyResponseFromHubToStringFunctionSHA256WhenHubShouldSignResponseMessages() {

        when(transactionsConfigProxy.getShouldHubSignResponseMessages(ENTITY_ID)).thenReturn(true);
        when(transactionsConfigProxy.getShouldHubUseLegacySamlStandard(ENTITY_ID)).thenReturn(true);

        Function<OutboundResponseFromHub, String> transformer = outboundResponseFromHubToResponseTransformerFactory.get(ENTITY_ID);

        assertThat(transformer).isEqualTo(outboundLegacyResponseFromHubToStringFunctionSHA256);
    }

    @Test
    public void getShouldReturnOutboundSamlProfileResponseFromHubToStringFunctionSHA256WhenHubShouldSignResponseMessages() {

        when(transactionsConfigProxy.getShouldHubSignResponseMessages(ENTITY_ID)).thenReturn(true);
        when(transactionsConfigProxy.getShouldHubUseLegacySamlStandard(ENTITY_ID)).thenReturn(false);

        Function<OutboundResponseFromHub, String> transformer = outboundResponseFromHubToResponseTransformerFactory.get(ENTITY_ID);

        assertThat(transformer).isEqualTo(outboundSamlProfileResponseFromHubToStringFunctionSHA256);
    }

    @Test
    public void getShouldReturnSimpleProfileOutboundResponseFromHubToResponseTransformerProviderWhenHubShouldSignResponseMessages() {

        when(transactionsConfigProxy.getShouldHubSignResponseMessages(ENTITY_ID)).thenReturn(false);

        when(simpleProfileOutboundResponseFromHubToResponseTransformerProvider.get()).thenReturn(simpleProfileOutboundResponseFromHubToResponseTransformer);
        Function<OutboundResponseFromHub, String> transformer = outboundResponseFromHubToResponseTransformerFactory.get(ENTITY_ID);

        assertThat(transformer).isEqualTo(simpleProfileOutboundResponseFromHubToResponseTransformer);
    }

    @Test
    public void getCountryTransformerShouldReturnOutboundAuthnResponseFromCountryContainerToStringFunction() {
        Function<AuthnResponseFromCountryContainerDto, String> transformer = outboundResponseFromHubToResponseTransformerFactory.getCountryTransformer();
        assertThat(transformer).isEqualTo(outboundAuthnResponseFromCountryContainerToStringFunction);
    }
}
