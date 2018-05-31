package uk.gov.ida.saml.hub.transformers.outbound;

import uk.gov.ida.saml.core.domain.OutboundResponseFromHub;

import java.util.function.Function;

/*
 * VCU-43
 * Remove when SHA1 signing is definitively disabled.
 */
public class OutboundLegacyResponseFromHubToStringFunctionSHA256 implements Function<OutboundResponseFromHub, String> {
    private Function<OutboundResponseFromHub, String> transformer;

    public OutboundLegacyResponseFromHubToStringFunctionSHA256(Function<OutboundResponseFromHub,String> transformer) {
        this.transformer = transformer;
    }

    @Override
    public String apply(OutboundResponseFromHub outboundResponseFromHub) {
        return transformer.apply(outboundResponseFromHub);
    }
}
