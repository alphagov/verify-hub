package uk.gov.ida.saml.hub.transformers.outbound;

import uk.gov.ida.saml.core.domain.OutboundResponseFromHub;

import java.util.function.Function;

/**
 * This concrete class is necessary to convince guice to inject two Function<OutboundResponseFromHub, String> implementations.
 */
public class OutboundSamlProfileResponseFromHubToStringFunction implements Function<OutboundResponseFromHub, String> {
    private Function<OutboundResponseFromHub, String> transformer;

    public OutboundSamlProfileResponseFromHubToStringFunction(Function<OutboundResponseFromHub,String> transformer) {
        this.transformer = transformer;
    }

    @Override
    public String apply(OutboundResponseFromHub outboundResponseFromHub) {
        return transformer.apply(outboundResponseFromHub);
    }
}
