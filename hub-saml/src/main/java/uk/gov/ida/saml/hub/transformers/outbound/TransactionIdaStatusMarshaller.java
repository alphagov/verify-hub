package uk.gov.ida.saml.hub.transformers.outbound;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.domain.DetailedStatusCode;
import uk.gov.ida.saml.core.domain.TransactionIdaStatus;
import uk.gov.ida.saml.core.transformers.outbound.IdaStatusMarshaller;

public class TransactionIdaStatusMarshaller extends IdaStatusMarshaller<TransactionIdaStatus> {

    private static final ImmutableMap<TransactionIdaStatus, DetailedStatusCode> REST_TO_SAML_CODES =
            ImmutableMap.<TransactionIdaStatus, DetailedStatusCode>builder()
                    .put(TransactionIdaStatus.Success, DetailedStatusCode.Success)
                    .put(TransactionIdaStatus.NoAuthenticationContext, DetailedStatusCode.NoAuthenticationContext)
                    .put(TransactionIdaStatus.NoMatchingServiceMatchFromHub, DetailedStatusCode.NoMatchingServiceMatchFromHub)
                    .put(TransactionIdaStatus.AuthenticationFailed, DetailedStatusCode.AuthenticationFailed)
                    .put(TransactionIdaStatus.RequesterError, DetailedStatusCode.RequesterErrorFromIdpAsSentByHub)
                    .build();

    @Inject
    public TransactionIdaStatusMarshaller(OpenSamlXmlObjectFactory samlObjectFactory) {
        super(samlObjectFactory);
    }


    @Override
    protected DetailedStatusCode getDetailedStatusCode(TransactionIdaStatus originalStatus) {
        return REST_TO_SAML_CODES.get(originalStatus);
    }
}
