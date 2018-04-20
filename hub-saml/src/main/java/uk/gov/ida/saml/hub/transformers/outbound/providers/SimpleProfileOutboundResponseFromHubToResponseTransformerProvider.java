package uk.gov.ida.saml.hub.transformers.outbound.providers;

import com.google.inject.Provider;
import uk.gov.ida.saml.core.domain.OutboundResponseFromHub;
import uk.gov.ida.saml.hub.transformers.outbound.SimpleProfileOutboundResponseFromHubToSamlResponseTransformer;

import javax.inject.Inject;
import java.util.function.Function;

public class SimpleProfileOutboundResponseFromHubToResponseTransformerProvider implements
        Provider<Function<OutboundResponseFromHub, String>> {

    private final Function<OutboundResponseFromHub, String> outboundResponseFromHubToStringTransformer;

    @Inject
    public SimpleProfileOutboundResponseFromHubToResponseTransformerProvider(
            SimpleProfileOutboundResponseFromHubToSamlResponseTransformer outboundToResponseTransformer,
            ResponseToUnsignedStringTransformer responseToStringTransformer) {

        this.outboundResponseFromHubToStringTransformer = responseToStringTransformer.compose(outboundToResponseTransformer);
    }

    public Function<OutboundResponseFromHub, String> get() {
        return outboundResponseFromHubToStringTransformer;
    }
}
