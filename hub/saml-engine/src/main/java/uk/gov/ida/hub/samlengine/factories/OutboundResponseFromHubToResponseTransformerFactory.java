package uk.gov.ida.hub.samlengine.factories;

import uk.gov.ida.hub.samlengine.proxy.TransactionsConfigProxy;
import uk.gov.ida.saml.core.domain.AuthnResponseFromCountryContainerDto;
import uk.gov.ida.saml.core.domain.OutboundResponseFromHub;
import uk.gov.ida.saml.hub.transformers.outbound.OutboundAuthnResponseFromCountryContainerToStringFunction;
import uk.gov.ida.saml.hub.transformers.outbound.OutboundLegacyResponseFromHubToStringFunctionSHA256;
import uk.gov.ida.saml.hub.transformers.outbound.OutboundSamlProfileResponseFromHubToStringFunctionSHA256;
import uk.gov.ida.saml.hub.transformers.outbound.providers.SimpleProfileOutboundResponseFromHubToResponseTransformerProvider;

import javax.inject.Inject;
import java.util.function.Function;

public class OutboundResponseFromHubToResponseTransformerFactory {

    private final OutboundLegacyResponseFromHubToStringFunctionSHA256 outboundLegacyResponseFromHubToStringFunctionSHA256;
    private final OutboundSamlProfileResponseFromHubToStringFunctionSHA256 outboundSamlProfileResponseFromHubToStringFunctionSHA256;
    private final SimpleProfileOutboundResponseFromHubToResponseTransformerProvider simpleProfileOutboundResponseFromHubToResponseTransformerProvider;
    private final TransactionsConfigProxy transactionsConfigProxy;
    private final OutboundAuthnResponseFromCountryContainerToStringFunction outboundAuthnResponseFromCountryContainerToStringFunction;

    @Inject
    public OutboundResponseFromHubToResponseTransformerFactory(SimpleProfileOutboundResponseFromHubToResponseTransformerProvider simpleProfileOutboundResponseFromHubToResponseTransformerProvider,
                                                               TransactionsConfigProxy transactionsConfigProxy,
                                                               OutboundSamlProfileResponseFromHubToStringFunctionSHA256 outboundSamlProfileResponseFromHubToStringFunctionSHA256,
                                                               OutboundLegacyResponseFromHubToStringFunctionSHA256 outboundLegacyResponseFromHubToStringFunctionSHA256,
                                                               OutboundAuthnResponseFromCountryContainerToStringFunction outboundAuthnResponseFromCountryContainerToStringFunction) {
        this.outboundLegacyResponseFromHubToStringFunctionSHA256 = outboundLegacyResponseFromHubToStringFunctionSHA256;
        this.outboundSamlProfileResponseFromHubToStringFunctionSHA256 = outboundSamlProfileResponseFromHubToStringFunctionSHA256;
        this.simpleProfileOutboundResponseFromHubToResponseTransformerProvider = simpleProfileOutboundResponseFromHubToResponseTransformerProvider;
        this.outboundAuthnResponseFromCountryContainerToStringFunction = outboundAuthnResponseFromCountryContainerToStringFunction;
        this.transactionsConfigProxy = transactionsConfigProxy;
    }

    public Function<OutboundResponseFromHub, String> get(String authnRequestIssuerEntityId) {
        if (transactionsConfigProxy.getShouldHubSignResponseMessages(authnRequestIssuerEntityId)) {
            return transactionsConfigProxy.getShouldHubUseLegacySamlStandard(authnRequestIssuerEntityId) ?
                    outboundLegacyResponseFromHubToStringFunctionSHA256 :
                    outboundSamlProfileResponseFromHubToStringFunctionSHA256;
        }
        return simpleProfileOutboundResponseFromHubToResponseTransformerProvider.get();
    }

    public Function<AuthnResponseFromCountryContainerDto, String> getCountryTransformer() {
        return outboundAuthnResponseFromCountryContainerToStringFunction;
    }
}
