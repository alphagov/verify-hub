package uk.gov.ida.saml.hub.transformers.outbound;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.domain.DetailedStatusCode;
import uk.gov.ida.saml.core.domain.TransactionIdaStatus;
import uk.gov.ida.saml.core.transformers.outbound.IdaStatusMarshaller;

/**
 * This class is the same as TransactionIdaStatusMarshaller except that TransactionIdaStatus.NoMatchingServiceMatchFromHub
 * is mapped to DetailedStatusCode.SamlProfileNoMatchingServiceMatchFromHub. This is because the saml profile specifies that a
 * no-match (which has no Assertions) should have Status of Responder. TransactionIdaStatusMarshaller is kept in the package
 * for backwards compatibility (RPs receive Success:no-match at time of writing) but the goal is to deprecate it
 * (but this might take years).
 */
public class SamlProfileTransactionIdaStatusMarshaller extends IdaStatusMarshaller<TransactionIdaStatus> {

    private static final ImmutableMap<TransactionIdaStatus, DetailedStatusCode> REST_TO_SAML_CODES =
            ImmutableMap.<TransactionIdaStatus, DetailedStatusCode>builder()
                    .put(TransactionIdaStatus.Success, DetailedStatusCode.Success)
                    .put(TransactionIdaStatus.NoAuthenticationContext, DetailedStatusCode.NoAuthenticationContext)
                    .put(TransactionIdaStatus.NoMatchingServiceMatchFromHub, DetailedStatusCode.SamlProfileNoMatchingServiceMatchFromHub)
                    .put(TransactionIdaStatus.AuthenticationFailed, DetailedStatusCode.AuthenticationFailed)
                    .put(TransactionIdaStatus.RequesterError, DetailedStatusCode.RequesterErrorFromIdpAsSentByHub)
                    .build();

    @Inject
    public SamlProfileTransactionIdaStatusMarshaller(OpenSamlXmlObjectFactory samlObjectFactory) {
        super(samlObjectFactory);
    }


    @Override
    protected DetailedStatusCode getDetailedStatusCode(TransactionIdaStatus originalStatus) {
        return REST_TO_SAML_CODES.get(originalStatus);
    }
}
