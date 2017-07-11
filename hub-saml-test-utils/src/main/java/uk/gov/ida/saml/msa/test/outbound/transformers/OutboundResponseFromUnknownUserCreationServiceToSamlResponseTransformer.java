package uk.gov.ida.saml.msa.test.outbound.transformers;

import com.google.common.base.Optional;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.transformers.outbound.IdaResponseToSamlResponseTransformer;
import uk.gov.ida.saml.msa.test.outbound.UnknownUserCreationIdaStatusMarshaller;
import uk.gov.ida.saml.msa.test.domain.MatchingServiceAssertion;
import uk.gov.ida.saml.msa.test.outbound.OutboundResponseFromUnknownUserCreationService;

public class OutboundResponseFromUnknownUserCreationServiceToSamlResponseTransformer extends IdaResponseToSamlResponseTransformer<OutboundResponseFromUnknownUserCreationService> {

    private final UnknownUserCreationIdaStatusMarshaller statusMarshaller;
    private final MatchingServiceAssertionToAssertionTransformer assertionTransformer;

    public OutboundResponseFromUnknownUserCreationServiceToSamlResponseTransformer(
            OpenSamlXmlObjectFactory openSamlXmlObjectFactory,
            UnknownUserCreationIdaStatusMarshaller statusMarshaller,
            MatchingServiceAssertionToAssertionTransformer assertionTransformer) {
        super(openSamlXmlObjectFactory);
        this.statusMarshaller = statusMarshaller;
        this.assertionTransformer = assertionTransformer;
    }

    @Override
    protected void transformAssertions(OutboundResponseFromUnknownUserCreationService originalResponse, Response transformedResponse) {
        Optional<MatchingServiceAssertion> assertion = originalResponse.getMatchingServiceAssertion();
        if (assertion.isPresent()) {
            Assertion transformedAssertion = assertionTransformer.transform(assertion.get());
            transformedResponse.getAssertions().add(transformedAssertion);
        }
    }

    @Override
    protected Status transformStatus(OutboundResponseFromUnknownUserCreationService originalResponse) {
        return statusMarshaller.toSamlStatus(originalResponse.getStatus());
    }

    @Override
    protected void transformDestination(OutboundResponseFromUnknownUserCreationService originalResponse, Response transformedResponse) {

    }
}
