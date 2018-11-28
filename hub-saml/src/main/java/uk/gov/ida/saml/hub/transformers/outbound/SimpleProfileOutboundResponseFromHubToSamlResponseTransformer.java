package uk.gov.ida.saml.hub.transformers.outbound;

import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.domain.OutboundResponseFromHub;
import uk.gov.ida.saml.core.transformers.outbound.IdaResponseToSamlResponseTransformer;

import javax.inject.Inject;

public class SimpleProfileOutboundResponseFromHubToSamlResponseTransformer extends IdaResponseToSamlResponseTransformer<OutboundResponseFromHub> {

    private final SimpleProfileTransactionIdaStatusMarshaller statusMarshaller;
    private final EncryptedAssertionUnmarshaller encryptedAssertionUnmarshaller;

    @Inject
    public SimpleProfileOutboundResponseFromHubToSamlResponseTransformer(
            SimpleProfileTransactionIdaStatusMarshaller statusMarshaller,
            OpenSamlXmlObjectFactory openSamlXmlObjectFactory,
            EncryptedAssertionUnmarshaller encryptedAssertionUnmarshaller) {

        super(openSamlXmlObjectFactory);

        this.statusMarshaller = statusMarshaller;
        this.encryptedAssertionUnmarshaller = encryptedAssertionUnmarshaller;
    }

    @Override
    protected void transformAssertions(OutboundResponseFromHub originalResponse, Response transformedResponse) {
        originalResponse
                .getEncryptedAssertions().stream()
                .map(encryptedAssertionUnmarshaller::transform)
                .forEach(transformedResponse.getEncryptedAssertions()::add);
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
