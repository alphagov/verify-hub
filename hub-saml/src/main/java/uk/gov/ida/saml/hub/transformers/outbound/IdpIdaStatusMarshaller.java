package uk.gov.ida.saml.hub.transformers.outbound;

import org.opensaml.saml.saml2.core.StatusDetail;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.domain.DetailedStatusCode;
import uk.gov.ida.saml.core.extensions.StatusValue;
import uk.gov.ida.saml.core.transformers.outbound.IdaStatusMarshaller;
import uk.gov.ida.saml.hub.domain.IdpIdaStatus;

import java.util.Map;
import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.of;

public class IdpIdaStatusMarshaller extends IdaStatusMarshaller<IdpIdaStatus> {

    private static final Map<IdpIdaStatus, DetailedStatusCode> REST_TO_SAML_CODES = Map.of(
                    IdpIdaStatus.success(), DetailedStatusCode.Success,
                    IdpIdaStatus.noAuthenticationContext(), DetailedStatusCode.NoAuthenticationContext,
                    IdpIdaStatus.authenticationFailed(), DetailedStatusCode.AuthenticationFailed,
                    IdpIdaStatus.requesterError(), DetailedStatusCode.RequesterErrorFromIdp,
                    IdpIdaStatus.authenticationCancelled(), DetailedStatusCode.NoAuthenticationContext,
                    IdpIdaStatus.authenticationPending(), DetailedStatusCode.NoAuthenticationContext,
                    IdpIdaStatus.upliftFailed(), DetailedStatusCode.NoAuthenticationContext);

    private static final Map<IdpIdaStatus, String> REST_TO_STATUS_DETAIL = Map.of(
                    IdpIdaStatus.authenticationCancelled(), StatusValue.CANCEL,
                    IdpIdaStatus.authenticationPending(), StatusValue.PENDING,
                    IdpIdaStatus.upliftFailed(), StatusValue.UPLIFT_FAILED);

    public IdpIdaStatusMarshaller(OpenSamlXmlObjectFactory samlObjectFactory) {
        super(samlObjectFactory);
    }

    @Override
    protected Optional<String> getStatusMessage(IdpIdaStatus originalStatus) {
        return originalStatus.getMessage();
    }

    @Override
    protected Optional<StatusDetail> getStatusDetail(IdpIdaStatus originalStatus) {
        if (REST_TO_STATUS_DETAIL.containsKey(originalStatus)) {
            StatusDetail statusDetail = this.samlObjectFactory.createStatusDetail();
            StatusValue statusValue = this.samlObjectFactory.createStatusValue(REST_TO_STATUS_DETAIL.get(originalStatus));
            statusDetail.getUnknownXMLObjects().add(statusValue);
            return of(statusDetail);
        }
        return empty();
    }

    @Override
    protected DetailedStatusCode getDetailedStatusCode(IdpIdaStatus originalStatus) {
        return REST_TO_SAML_CODES.get(originalStatus);
    }
}
