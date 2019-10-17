package uk.gov.ida.saml.hub.transformers.outbound;

import uk.gov.ida.saml.core.domain.AuthnResponseFromCountryContainerDto;

import java.util.function.Function;

public class OutboundAuthnResponseFromCountryContainerToStringFunction implements Function<AuthnResponseFromCountryContainerDto, String> {
    private Function<AuthnResponseFromCountryContainerDto, String> transformer;

    public OutboundAuthnResponseFromCountryContainerToStringFunction(Function<AuthnResponseFromCountryContainerDto, String> transformer) {
        this.transformer = transformer;
    }

    @Override
    public String apply(AuthnResponseFromCountryContainerDto authnResponseFromCountryContainerDto) {
        return transformer.apply(authnResponseFromCountryContainerDto);
    }
}
