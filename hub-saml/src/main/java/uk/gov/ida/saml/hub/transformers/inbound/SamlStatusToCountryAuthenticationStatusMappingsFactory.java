package uk.gov.ida.saml.hub.transformers.inbound;

import com.google.common.collect.ImmutableMap;
import uk.gov.ida.saml.core.domain.DetailedStatusCode;
import uk.gov.ida.saml.hub.domain.CountryAuthenticationStatus;

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

    public static ImmutableMap<SamlStatusDefinitions, CountryAuthenticationStatus.Status> getSamlToCountryAuthenticationStatusMappings() {
        // Matching SAML statuses to their CountryAuthenticationStatus counterparts is dependent on the ordering of these put()
        // statements. There must be a better way of doing this.
        return ImmutableMap.<SamlStatusDefinitions, CountryAuthenticationStatus.Status>builder()
                .put(SamlStatusDefinitions.Success, CountryAuthenticationStatus.Status.Success)
                .build();
    }
}
