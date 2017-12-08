package uk.gov.ida.hub.samlengine.factories;

import uk.gov.ida.hub.samlengine.proxy.TransactionsConfigProxy;
import uk.gov.ida.saml.core.domain.OutboundResponseFromHub;
import uk.gov.ida.saml.hub.transformers.outbound.OutboundLegacyResponseFromHubToStringFunction;
import uk.gov.ida.saml.hub.transformers.outbound.OutboundSamlProfileResponseFromHubToStringFunction;
import uk.gov.ida.saml.hub.transformers.outbound.providers.SimpleProfileOutboundResponseFromHubToResponseTransformerProvider;

import javax.inject.Inject;
import java.util.function.Function;

public class OutboundResponseFromHubToResponseTransformerFactory {

    private final OutboundLegacyResponseFromHubToStringFunction outboundLegacyResponseFromHubToStringFunction;
    private final OutboundSamlProfileResponseFromHubToStringFunction outboundSamlProfileResponseFromHubToStringFunction;
    private final SimpleProfileOutboundResponseFromHubToResponseTransformerProvider simpleProfileOutboundResponseFromHubToResponseTransformerProvider;
    private final TransactionsConfigProxy transactionsConfigProxy;

    @Inject
    public OutboundResponseFromHubToResponseTransformerFactory(OutboundLegacyResponseFromHubToStringFunction outboundLegacyResponseFromHubToStringFunction,
                                                               OutboundSamlProfileResponseFromHubToStringFunction outboundSamlProfileResponseFromHubToStringFunction,
                                                               SimpleProfileOutboundResponseFromHubToResponseTransformerProvider simpleProfileOutboundResponseFromHubToResponseTransformerProvider,
                                                               TransactionsConfigProxy transactionsConfigProxy) {
        this.outboundLegacyResponseFromHubToStringFunction = outboundLegacyResponseFromHubToStringFunction;
        this.outboundSamlProfileResponseFromHubToStringFunction = outboundSamlProfileResponseFromHubToStringFunction;
        this.simpleProfileOutboundResponseFromHubToResponseTransformerProvider = simpleProfileOutboundResponseFromHubToResponseTransformerProvider;
        this.transactionsConfigProxy = transactionsConfigProxy;
    }

    public Function<OutboundResponseFromHub, String> get(String authnRequestIssuerEntityId) {
        if (transactionsConfigProxy.getShouldHubSignResponseMessages(authnRequestIssuerEntityId)) {
            if (transactionsConfigProxy.getShouldHubUseLegacySamlStandard(authnRequestIssuerEntityId)) {
                return outboundLegacyResponseFromHubToStringFunction;
            } else {
                return outboundSamlProfileResponseFromHubToStringFunction;
            }
        }
        return simpleProfileOutboundResponseFromHubToResponseTransformerProvider.get();
    }
}
