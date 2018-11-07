package uk.gov.ida.saml.hub.transformers.outbound;

import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.domain.OutboundResponseFromHub;
import uk.gov.ida.saml.core.domain.TransactionIdaStatus;
import uk.gov.ida.saml.core.transformers.outbound.IdaResponseToSamlResponseTransformer;
import uk.gov.ida.saml.core.transformers.outbound.IdaStatusMarshaller;

public class OutboundResponseFromHubToSamlResponseTransformer extends IdaResponseToSamlResponseTransformer<OutboundResponseFromHub> {

    private final IdaStatusMarshaller<TransactionIdaStatus> statusMarshaller;
    private final AssertionFromIdpToAssertionTransformer assertionTransformer;
    private final EncryptedAssertionUnmarshaller encryptedAssertionUnmarshaller;

    public OutboundResponseFromHubToSamlResponseTransformer(
            IdaStatusMarshaller<TransactionIdaStatus> statusMarshaller,
            OpenSamlXmlObjectFactory openSamlXmlObjectFactory,
            AssertionFromIdpToAssertionTransformer assertionTransformer,
            EncryptedAssertionUnmarshaller encryptedAssertionUnmarshaller) {

        super(openSamlXmlObjectFactory);

        this.statusMarshaller = statusMarshaller;
        this.assertionTransformer = assertionTransformer;
        this.encryptedAssertionUnmarshaller = encryptedAssertionUnmarshaller;
    }

    @Override
    protected void transformAssertions(OutboundResponseFromHub originalResponse, Response transformedResponse) {
        for (String assertionString: originalResponse.getAssertions()) {
            try {
                transformedResponse.getAssertions().add(assertionTransformer.transform(assertionString));
            } catch (ClassCastException ex) {
                transformedResponse.getEncryptedAssertions().add(encryptedAssertionUnmarshaller.transform(assertionString));
            }
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
}
