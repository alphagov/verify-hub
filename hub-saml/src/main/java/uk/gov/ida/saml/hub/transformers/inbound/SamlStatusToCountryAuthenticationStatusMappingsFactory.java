package uk.gov.ida.saml.hub.transformers.inbound;

import uk.gov.ida.saml.core.domain.DetailedStatusCode;
import uk.gov.ida.saml.hub.domain.CountryAuthenticationStatus;

import java.util.Map;

public class SamlStatusToCountryAuthenticationStatusMappingsFactory {
    enum SamlStatusDefinitions {
        Success(DetailedStatusCode.Success);

        private final DetailedStatusCode statusCode;

        SamlStatusDefinitions(DetailedStatusCode statusCode) {
            this.statusCode = statusCode;
        }

        public boolean matches(String samlStatusValue) {
            return statusCode.getStatus().equals(samlStatusValue);
        }
    }

    public static Map<SamlStatusDefinitions, CountryAuthenticationStatus.Status> getSamlToCountryAuthenticationStatusMappings() {
        // Matching SAML statuses to their CountryAuthenticationStatus counterparts is dependent on the ordering of these put()
        // statements. There must be a better way of doing this.
        return Map.of(SamlStatusDefinitions.Success, CountryAuthenticationStatus.Status.Success);
    }
}
