package uk.gov.ida.saml.hub.transformers.inbound;

import uk.gov.ida.saml.core.domain.TransactionIdaStatus;

import java.util.Map;

public class TransactionIdaStatusUnmarshaller extends IdaStatusUnmarshaller<TransactionIdaStatus> {

    private static final Map<IdaStatusMapperStatus, TransactionIdaStatus> SAML_TO_REST_CODES = Map.of(
            IdaStatusMapperStatus.RequesterErrorFromIdpAsSentByHub, TransactionIdaStatus.RequesterError,
            IdaStatusMapperStatus.AuthenticationFailed, TransactionIdaStatus.AuthenticationFailed,
            IdaStatusMapperStatus.NoAuthenticationContext, TransactionIdaStatus.NoAuthenticationContext,
            IdaStatusMapperStatus.NoMatchingServiceMatchFromHub, TransactionIdaStatus.NoMatchingServiceMatchFromHub, // This line represents Success:no-match (Legacy functionality which will be deleted in the distant future,
            IdaStatusMapperStatus.NoMatchingServiceMatchFromMatchingService, TransactionIdaStatus.NoMatchingServiceMatchFromHub, // This line represents Responder:no-match
            IdaStatusMapperStatus.Success, TransactionIdaStatus.Success);

    public TransactionIdaStatusUnmarshaller() {
        super(SAML_TO_REST_CODES);
    }
}
