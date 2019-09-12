package uk.gov.ida.saml.hub.transformers.outbound;

import com.google.inject.Inject;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.domain.DetailedStatusCode;
import uk.gov.ida.saml.core.domain.TransactionIdaStatus;
import uk.gov.ida.saml.core.transformers.outbound.IdaStatusMarshaller;

import java.util.Map;

public class TransactionIdaStatusMarshaller extends IdaStatusMarshaller<TransactionIdaStatus> {

    private static final Map<TransactionIdaStatus, DetailedStatusCode> REST_TO_SAML_CODES = Map.of(
                    TransactionIdaStatus.Success, DetailedStatusCode.Success,
                    TransactionIdaStatus.NoAuthenticationContext, DetailedStatusCode.NoAuthenticationContext,
                    TransactionIdaStatus.NoMatchingServiceMatchFromHub, DetailedStatusCode.NoMatchingServiceMatchFromHub,
                    TransactionIdaStatus.AuthenticationFailed, DetailedStatusCode.AuthenticationFailed,
                    TransactionIdaStatus.RequesterError, DetailedStatusCode.RequesterErrorFromIdpAsSentByHub);

    @Inject
    public TransactionIdaStatusMarshaller(OpenSamlXmlObjectFactory samlObjectFactory) {
        super(samlObjectFactory);
    }


    @Override
    protected DetailedStatusCode getDetailedStatusCode(TransactionIdaStatus originalStatus) {
        return REST_TO_SAML_CODES.get(originalStatus);
    }
}
