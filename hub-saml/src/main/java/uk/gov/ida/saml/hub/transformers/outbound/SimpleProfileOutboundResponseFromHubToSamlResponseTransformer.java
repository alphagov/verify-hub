package uk.gov.ida.saml.hub.transformers.outbound;

import javax.inject.Inject;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.domain.OutboundResponseFromHub;
import uk.gov.ida.saml.core.transformers.outbound.IdaResponseToSamlResponseTransformer;

import java.util.Optional;

public class SimpleProfileOutboundResponseFromHubToSamlResponseTransformer extends IdaResponseToSamlResponseTransformer<OutboundResponseFromHub> {

    private final SimpleProfileTransactionIdaStatusMarshaller statusMarshaller;
    private final AssertionFromIdpToAssertionTransformer assertionTransformer;

    @Inject
    public SimpleProfileOutboundResponseFromHubToSamlResponseTransformer(
            SimpleProfileTransactionIdaStatusMarshaller statusMarshaller,
            OpenSamlXmlObjectFactory openSamlXmlObjectFactory,
            AssertionFromIdpToAssertionTransformer assertionTransformer) {

        super(openSamlXmlObjectFactory);

        this.statusMarshaller = statusMarshaller;
        this.assertionTransformer = assertionTransformer;
    }

    @Override
    protected void transformAssertions(OutboundResponseFromHub originalResponse, Response transformedResponse) {
        Optional<String> matchingServiceAssertion = originalResponse.getMatchingServiceAssertion();
        if (matchingServiceAssertion.isPresent()) {
            Assertion transformedAssertion = assertionTransformer.transform(matchingServiceAssertion.get());
            transformedResponse.getAssertions().add(transformedAssertion);
        }
    }

    @Override
    protected Status transformStatus(OutboundResponseFromHub originalResponse) {
        return statusMarshaller.toSamlStatus(originalResponse.getStatus());
    }

    @Override
    protected void transformDestination(OutboundResponseFromHub originalResponse, Response transformedResponse) {
        transformedResponse.setDestination(originalResponse.getDestination().toASCIIString());
    }

    @Override
    protected void transformIssuer(final OutboundResponseFromHub originalResponse, final Response transformedResponse) {
       // do nothing
    }
}
