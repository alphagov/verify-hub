package uk.gov.ida.saml.hub.transformers.inbound;

import com.google.common.collect.ImmutableMap;
import uk.gov.ida.saml.core.domain.DetailedStatusCode;
import uk.gov.ida.saml.core.extensions.StatusValue;
import uk.gov.ida.saml.hub.domain.IdpIdaStatus;

import java.util.List;
import java.util.Optional;


public class SamlStatusToIdpIdaStatusMappingsFactory {
    enum SamlStatusDefinitions {
        Success(DetailedStatusCode.Success, Optional.empty()),
        AuthenticationCancelled(DetailedStatusCode.NoAuthenticationContext, Optional.of(StatusValue.CANCEL)),
        AuthenticationPending(DetailedStatusCode.NoAuthenticationContext, Optional.of(StatusValue.PENDING)),
        NoAuthenticationContext(DetailedStatusCode.NoAuthenticationContext, Optional.empty()),
        AuthenticationFailed(DetailedStatusCode.AuthenticationFailed, Optional.empty()),
        RequesterErrorFromIdp(DetailedStatusCode.RequesterErrorFromIdp, Optional.empty()),
        RequesterErrorRequestDeniedFromIdp(DetailedStatusCode.RequesterErrorRequestDeniedFromIdp, Optional.empty()),
        UpliftFailed(DetailedStatusCode.NoAuthenticationContext, Optional.of(StatusValue.UPLIFT_FAILED));

        private final DetailedStatusCode statusCode;
        private final Optional<String> statusDetailValue;

        SamlStatusDefinitions(DetailedStatusCode statusCode, Optional<String> statusDetailValue) {
            this.statusCode = statusCode;
            this.statusDetailValue = statusDetailValue;
        }

        public boolean matches(String samlStatusValue, Optional<String> samlSubStatusValue, List<String> statusDetailValues) {
            boolean statusCodesMatch = statusCode.getStatus().equals(samlStatusValue) && statusCode.getSubStatus().equals(samlSubStatusValue);
            boolean statusDetailsMatch = statusDetailValue.map(statusDetailValues::contains).orElse(true);
            return statusCodesMatch && statusDetailsMatch;
        }
    }

    public ImmutableMap<SamlStatusDefinitions, IdpIdaStatus.Status> getSamlToIdpIdaStatusMappings() {
        // Matching SAML statuses to their IdpIdaStatus counterparts is dependent on the ordering of these put()
        // statements. There must be a better way of doing this.
        return ImmutableMap.<SamlStatusDefinitions, IdpIdaStatus.Status>builder()
                .put(SamlStatusDefinitions.Success, IdpIdaStatus.Status.Success)
                .put(SamlStatusDefinitions.AuthenticationCancelled, IdpIdaStatus.Status.AuthenticationCancelled)
                .put(SamlStatusDefinitions.AuthenticationPending, IdpIdaStatus.Status.AuthenticationPending)
                .put(SamlStatusDefinitions.UpliftFailed, IdpIdaStatus.Status.UpliftFailed)
                .put(SamlStatusDefinitions.NoAuthenticationContext, IdpIdaStatus.Status.NoAuthenticationContext)
                .put(SamlStatusDefinitions.AuthenticationFailed, IdpIdaStatus.Status.AuthenticationFailed)
                .put(SamlStatusDefinitions.RequesterErrorFromIdp, IdpIdaStatus.Status.RequesterError)
                .put(SamlStatusDefinitions.RequesterErrorRequestDeniedFromIdp, IdpIdaStatus.Status.RequesterError)
                .build();
    }
}
