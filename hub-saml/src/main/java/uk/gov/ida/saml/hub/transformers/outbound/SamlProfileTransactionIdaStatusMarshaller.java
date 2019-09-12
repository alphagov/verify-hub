package uk.gov.ida.saml.hub.transformers.outbound;

import com.google.inject.Inject;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.domain.DetailedStatusCode;
import uk.gov.ida.saml.core.domain.TransactionIdaStatus;
import uk.gov.ida.saml.core.transformers.outbound.IdaStatusMarshaller;

import java.util.Map;

/**
 * This class is the same as TransactionIdaStatusMarshaller except that TransactionIdaStatus.NoMatchingServiceMatchFromHub
 * is mapped to DetailedStatusCode.SamlProfileNoMatchingServiceMatchFromHub. This is because the saml profile specifies that a
 * no-match (which has no Assertions) should have Status of Responder. TransactionIdaStatusMarshaller is kept in the package
 * for backwards compatibility (RPs receive Success:no-match at time of writing) but the goal is to deprecate it
 * (but this might take years).
 */
public class SamlProfileTransactionIdaStatusMarshaller extends IdaStatusMarshaller<TransactionIdaStatus> {

    private static final Map<TransactionIdaStatus, DetailedStatusCode> REST_TO_SAML_CODES = Map.of(
                    TransactionIdaStatus.Success, DetailedStatusCode.Success,
                    TransactionIdaStatus.NoAuthenticationContext, DetailedStatusCode.NoAuthenticationContext,
                    TransactionIdaStatus.NoMatchingServiceMatchFromHub, DetailedStatusCode.SamlProfileNoMatchingServiceMatchFromHub,
                    TransactionIdaStatus.AuthenticationFailed, DetailedStatusCode.AuthenticationFailed,
                    TransactionIdaStatus.RequesterError, DetailedStatusCode.RequesterErrorFromIdpAsSentByHub);

    @Inject
    public SamlProfileTransactionIdaStatusMarshaller(OpenSamlXmlObjectFactory samlObjectFactory) {
        super(samlObjectFactory);
    }


    @Override
    protected DetailedStatusCode getDetailedStatusCode(TransactionIdaStatus originalStatus) {
        return REST_TO_SAML_CODES.get(originalStatus);
    }
}
