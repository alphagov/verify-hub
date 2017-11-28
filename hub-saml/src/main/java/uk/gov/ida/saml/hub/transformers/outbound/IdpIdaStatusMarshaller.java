package uk.gov.ida.saml.hub.transformers.outbound;

import com.google.common.collect.ImmutableMap;
import org.opensaml.saml.saml2.core.StatusDetail;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.domain.DetailedStatusCode;
import uk.gov.ida.saml.core.extensions.StatusValue;
import uk.gov.ida.saml.core.transformers.outbound.IdaStatusMarshaller;
import uk.gov.ida.saml.hub.domain.IdpIdaStatus;

import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.of;

public class IdpIdaStatusMarshaller extends IdaStatusMarshaller<IdpIdaStatus> {

    private static final ImmutableMap<IdpIdaStatus, DetailedStatusCode> REST_TO_SAML_CODES =
            ImmutableMap.<IdpIdaStatus, DetailedStatusCode>builder()
                    .put(IdpIdaStatus.success(), DetailedStatusCode.Success)
                    .put(IdpIdaStatus.noAuthenticationContext(), DetailedStatusCode.NoAuthenticationContext)
                    .put(IdpIdaStatus.authenticationFailed(), DetailedStatusCode.AuthenticationFailed)
                    .put(IdpIdaStatus.requesterError(), DetailedStatusCode.RequesterErrorFromIdp)
                    .put(IdpIdaStatus.authenticationCancelled(), DetailedStatusCode.NoAuthenticationContext)
                    .put(IdpIdaStatus.authenticationPending(), DetailedStatusCode.NoAuthenticationContext)
                    .put(IdpIdaStatus.upliftFailed(), DetailedStatusCode.NoAuthenticationContext)
                    .build();

    private static final ImmutableMap<IdpIdaStatus, String> REST_TO_STATUS_DETAIL =
            ImmutableMap.<IdpIdaStatus, String>builder()
                    .put(IdpIdaStatus.authenticationCancelled(), StatusValue.CANCEL)
                    .put(IdpIdaStatus.authenticationPending(), StatusValue.PENDING)
                    .put(IdpIdaStatus.upliftFailed(), StatusValue.UPLIFT_FAILED)
                    .build();

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
