package uk.gov.ida.hub.samlengine.factories;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.hub.samlengine.proxy.TransactionsConfigProxy;
import uk.gov.ida.saml.core.domain.OutboundResponseFromHub;
import uk.gov.ida.saml.hub.transformers.outbound.OutboundLegacyResponseFromHubToStringFunction;
import uk.gov.ida.saml.hub.transformers.outbound.OutboundSamlProfileResponseFromHubToStringFunction;
import uk.gov.ida.saml.hub.transformers.outbound.providers.SimpleProfileOutboundResponseFromHubToResponseTransformerProvider;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OutboundResponseFromHubToResponseTransformerFactoryTest {

    private OutboundResponseFromHubToResponseTransformerFactory outboundResponseFromHubToResponseTransformerFactory;

    @Mock
    private OutboundLegacyResponseFromHubToStringFunction outboundLegacyResponseFromHubToStringFunction;

    @Mock
    private OutboundSamlProfileResponseFromHubToStringFunction outboundSamlProfileResponseFromHubToStringFunction;

    @Mock
    private SimpleProfileOutboundResponseFromHubToResponseTransformerProvider simpleProfileOutboundResponseFromHubToResponseTransformerProvider;

    @Mock
    private Function<OutboundResponseFromHub, String> simpleProfileOutboundResponseFromHubToResponseTransformer;

    @Mock
    private TransactionsConfigProxy transactionsConfigProxy;

    public static final String ENTITY_ID = "entity-id";

    @Before
    public void setUp() {
        outboundResponseFromHubToResponseTransformerFactory = new OutboundResponseFromHubToResponseTransformerFactory(
                outboundLegacyResponseFromHubToStringFunction,
                outboundSamlProfileResponseFromHubToStringFunction,
                simpleProfileOutboundResponseFromHubToResponseTransformerProvider,
                transactionsConfigProxy);
    }

    @Test
    public void toResponse_shouldReturnOutboundLegacyResponseFromHubToStringFunctionWhenHubShouldSignResponseMessages() throws Exception {
        when(transactionsConfigProxy.getShouldHubSignResponseMessages(ENTITY_ID)).thenReturn(true);
        when(transactionsConfigProxy.getShouldHubUseLegacySamlStandard(ENTITY_ID)).thenReturn(true);

        Function<OutboundResponseFromHub, String> transformer = outboundResponseFromHubToResponseTransformerFactory.get(ENTITY_ID);

        assertThat(transformer).isEqualTo(outboundLegacyResponseFromHubToStringFunction);
    }

    @Test
    public void toResponse_shouldReturnOutboundSamlProfileResponseFromHubToStringFunctionWhenHubShouldSignResponseMessages() throws Exception {
        when(transactionsConfigProxy.getShouldHubSignResponseMessages(ENTITY_ID)).thenReturn(true);
        when(transactionsConfigProxy.getShouldHubUseLegacySamlStandard(ENTITY_ID)).thenReturn(false);

        Function<OutboundResponseFromHub, String> transformer = outboundResponseFromHubToResponseTransformerFactory.get(ENTITY_ID);

        assertThat(transformer).isEqualTo(outboundSamlProfileResponseFromHubToStringFunction);
    }

    @Test
    public void toResponse_shouldReturnSimpleProfileOutboundResponseFromHubToResponseTransformerProviderWhenHubShouldSignResponseMessages() throws Exception {
        when(transactionsConfigProxy.getShouldHubSignResponseMessages(ENTITY_ID)).thenReturn(false);

        when(simpleProfileOutboundResponseFromHubToResponseTransformerProvider.get()).thenReturn(simpleProfileOutboundResponseFromHubToResponseTransformer);
        Function<OutboundResponseFromHub, String> transformer = outboundResponseFromHubToResponseTransformerFactory.get(ENTITY_ID);

        assertThat(transformer).isEqualTo(simpleProfileOutboundResponseFromHubToResponseTransformer);
    }
}