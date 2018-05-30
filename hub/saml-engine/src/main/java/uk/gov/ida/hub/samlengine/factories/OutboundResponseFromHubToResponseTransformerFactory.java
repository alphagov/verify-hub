package uk.gov.ida.hub.samlengine.factories;

import uk.gov.ida.hub.samlengine.proxy.TransactionsConfigProxy;
import uk.gov.ida.saml.core.domain.OutboundResponseFromHub;
import uk.gov.ida.saml.hub.transformers.outbound.OutboundLegacyResponseFromHubToStringFunction;
import uk.gov.ida.saml.hub.transformers.outbound.OutboundLegacyResponseFromHubToStringFunctionSHA256;
import uk.gov.ida.saml.hub.transformers.outbound.OutboundSamlProfileResponseFromHubToStringFunction;
import uk.gov.ida.saml.hub.transformers.outbound.OutboundSamlProfileResponseFromHubToStringFunctionSHA256;
import uk.gov.ida.saml.hub.transformers.outbound.providers.SimpleProfileOutboundResponseFromHubToResponseTransformerProvider;

import javax.inject.Inject;
import java.util.function.Function;

public class OutboundResponseFromHubToResponseTransformerFactory {

    private final OutboundLegacyResponseFromHubToStringFunction outboundLegacyResponseFromHubToStringFunction;
    private final OutboundLegacyResponseFromHubToStringFunctionSHA256 outboundLegacyResponseFromHubToStringFunctionSHA256;
    private final OutboundSamlProfileResponseFromHubToStringFunction outboundSamlProfileResponseFromHubToStringFunction;
    private final OutboundSamlProfileResponseFromHubToStringFunctionSHA256 outboundSamlProfileResponseFromHubToStringFunctionSHA256;
    private final SimpleProfileOutboundResponseFromHubToResponseTransformerProvider simpleProfileOutboundResponseFromHubToResponseTransformerProvider;
    private final TransactionsConfigProxy transactionsConfigProxy;

    @Inject
    public OutboundResponseFromHubToResponseTransformerFactory(OutboundLegacyResponseFromHubToStringFunction outboundLegacyResponseFromHubToStringFunction,
                                                               OutboundSamlProfileResponseFromHubToStringFunction outboundSamlProfileResponseFromHubToStringFunction,
                                                               SimpleProfileOutboundResponseFromHubToResponseTransformerProvider simpleProfileOutboundResponseFromHubToResponseTransformerProvider,
                                                               TransactionsConfigProxy transactionsConfigProxy,
                                                               OutboundSamlProfileResponseFromHubToStringFunctionSHA256 outboundSamlProfileResponseFromHubToStringFunctionSHA256,
                                                               OutboundLegacyResponseFromHubToStringFunctionSHA256 outboundLegacyResponseFromHubToStringFunctionSHA256) {
        this.outboundLegacyResponseFromHubToStringFunction = outboundLegacyResponseFromHubToStringFunction;
        this.outboundLegacyResponseFromHubToStringFunctionSHA256 = outboundLegacyResponseFromHubToStringFunctionSHA256;
        this.outboundSamlProfileResponseFromHubToStringFunction = outboundSamlProfileResponseFromHubToStringFunction;
        this.outboundSamlProfileResponseFromHubToStringFunctionSHA256 = outboundSamlProfileResponseFromHubToStringFunctionSHA256;
        this.simpleProfileOutboundResponseFromHubToResponseTransformerProvider = simpleProfileOutboundResponseFromHubToResponseTransformerProvider;
        this.transactionsConfigProxy = transactionsConfigProxy;
    }

    /*
     * VCU-43
     * Additional function refs making signing algorithm configurable per transaction.
     * Remove when SHA1 signing is definitively disabled.
     */
    public Function<OutboundResponseFromHub, String> get(String authnRequestIssuerEntityId) {
        if (transactionsConfigProxy.getShouldHubSignResponseMessages(authnRequestIssuerEntityId)) {
            if (transactionsConfigProxy.getShouldHubUseLegacySamlStandard(authnRequestIssuerEntityId)) {
                return transactionsConfigProxy.getShouldSignWithSHA1(authnRequestIssuerEntityId) ?
                        outboundLegacyResponseFromHubToStringFunction :
                        outboundLegacyResponseFromHubToStringFunctionSHA256;
            } else {
                return transactionsConfigProxy.getShouldSignWithSHA1(authnRequestIssuerEntityId) ?
                        outboundSamlProfileResponseFromHubToStringFunction :
                        outboundSamlProfileResponseFromHubToStringFunctionSHA256;
            }
        }
        return simpleProfileOutboundResponseFromHubToResponseTransformerProvider.get();
    }
}
