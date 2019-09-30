package uk.gov.ida.saml.hub.transformers.inbound;

import uk.gov.ida.saml.core.domain.DetailedStatusCode;
import uk.gov.ida.saml.core.extensions.StatusValue;
import uk.gov.ida.saml.hub.domain.IdpIdaStatus;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;


public class SamlStatusToIdpIdaStatusMappingsFactory {

    enum SamlStatusDefinitions {
        Success(DetailedStatusCode.Success, (v) -> true),
        AuthenticationCancelled(DetailedStatusCode.NoAuthenticationContext, StatusValue.CANCEL::equals),
        AuthenticationPending(DetailedStatusCode.NoAuthenticationContext, StatusValue.PENDING::equals),
        NoAuthenticationContext(DetailedStatusCode.NoAuthenticationContext, (v) -> v == null || !isKnownNoAuthenticationContextDetail(v)),
        AuthenticationFailed(DetailedStatusCode.AuthenticationFailed, (v) -> true),
        RequesterErrorFromIdp(DetailedStatusCode.RequesterErrorFromIdp, Objects::isNull),
        RequesterErrorRequestDeniedFromIdp(DetailedStatusCode.RequesterErrorRequestDeniedFromIdp, Objects::isNull),
        UpliftFailed(DetailedStatusCode.NoAuthenticationContext, StatusValue.UPLIFT_FAILED::equals);

        private final DetailedStatusCode statusCode;
        private final Predicate<String> statusDetailValuePredicate;

        SamlStatusDefinitions(DetailedStatusCode statusCode, Predicate<String> statusDetailValuePredicate) {
            this.statusCode = statusCode;
            this.statusDetailValuePredicate = statusDetailValuePredicate;
        }

        public boolean matches(String samlStatusValue, Optional<String> samlSubStatusValue, List<String> statusDetailValues) {
            boolean statusCodesMatch = statusCode.getStatus().equals(samlStatusValue) && statusCode.getSubStatus().equals(samlSubStatusValue);
            boolean statusDetailsMatch;
            if (statusDetailValues.isEmpty()){
                statusDetailsMatch = statusDetailValuePredicate.test(null);
            } else {
                statusDetailsMatch = statusDetailValues.stream().anyMatch(statusDetailValuePredicate);
            }
            return statusCodesMatch && statusDetailsMatch;
        }
    }

    private static boolean isKnownNoAuthenticationContextDetail(String value){
        return value.equals(StatusValue.CANCEL) ||
                value.equals(StatusValue.PENDING) ||
                value.equals(StatusValue.UPLIFT_FAILED);
    }

    public static Map<SamlStatusDefinitions, IdpIdaStatus.Status> getSamlToIdpIdaStatusMappings() {
        return Map.of(
                SamlStatusDefinitions.Success, IdpIdaStatus.Status.Success,
                SamlStatusDefinitions.AuthenticationCancelled, IdpIdaStatus.Status.AuthenticationCancelled,
                SamlStatusDefinitions.AuthenticationPending, IdpIdaStatus.Status.AuthenticationPending,
                SamlStatusDefinitions.UpliftFailed, IdpIdaStatus.Status.UpliftFailed,
                SamlStatusDefinitions.NoAuthenticationContext, IdpIdaStatus.Status.NoAuthenticationContext,
                SamlStatusDefinitions.AuthenticationFailed, IdpIdaStatus.Status.AuthenticationFailed,
                SamlStatusDefinitions.RequesterErrorFromIdp, IdpIdaStatus.Status.RequesterError,
                SamlStatusDefinitions.RequesterErrorRequestDeniedFromIdp, IdpIdaStatus.Status.RequesterError);
    }
}
