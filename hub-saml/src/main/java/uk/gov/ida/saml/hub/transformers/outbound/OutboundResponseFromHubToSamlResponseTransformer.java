package uk.gov.ida.saml.hub.transformers.outbound;

import org.opensaml.saml.saml2.core.EncryptedAssertion;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.domain.OutboundResponseFromHub;
import uk.gov.ida.saml.core.domain.TransactionIdaStatus;
import uk.gov.ida.saml.core.transformers.outbound.IdaResponseToSamlResponseTransformer;
import uk.gov.ida.saml.core.transformers.outbound.IdaStatusMarshaller;

import java.util.List;
import java.util.stream.Collectors;

public class OutboundResponseFromHubToSamlResponseTransformer extends IdaResponseToSamlResponseTransformer<OutboundResponseFromHub> {

    private final IdaStatusMarshaller<TransactionIdaStatus> statusMarshaller;
    private final EncryptedAssertionUnmarshaller encryptedAssertionUnmarshaller;

    public OutboundResponseFromHubToSamlResponseTransformer(
            IdaStatusMarshaller<TransactionIdaStatus> statusMarshaller,
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
}
