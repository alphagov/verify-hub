package uk.gov.ida.saml.hub.transformers.inbound;

import com.google.common.collect.ImmutableMap;
import uk.gov.ida.saml.core.domain.TransactionIdaStatus;

public class TransactionIdaStatusUnmarshaller extends IdaStatusUnmarshaller<TransactionIdaStatus> {

    private static final ImmutableMap<IdaStatusMapperStatus, TransactionIdaStatus> SAML_TO_REST_CODES =
        ImmutableMap.<IdaStatusMapperStatus, TransactionIdaStatus>builder()
            .put(IdaStatusMapperStatus.RequesterErrorFromIdpAsSentByHub, TransactionIdaStatus.RequesterError)
            .put(IdaStatusMapperStatus.AuthenticationFailed, TransactionIdaStatus.AuthenticationFailed)
            .put(IdaStatusMapperStatus.NoAuthenticationContext, TransactionIdaStatus.NoAuthenticationContext)
            .put(IdaStatusMapperStatus.NoMatchingServiceMatchFromHub, TransactionIdaStatus.NoMatchingServiceMatchFromHub) // This line represents Success:no-match (Legacy functionality which will be deleted in the distant future)
            .put(IdaStatusMapperStatus.NoMatchingServiceMatchFromMatchingService, TransactionIdaStatus.NoMatchingServiceMatchFromHub) // This line represents Responder:no-match
            .put(IdaStatusMapperStatus.Success, TransactionIdaStatus.Success)
            .build();

    public TransactionIdaStatusUnmarshaller() {
        super(SAML_TO_REST_CODES);
    }
}
