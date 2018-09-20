package uk.gov.ida.saml.hub.transformers.inbound;

import com.google.common.collect.ImmutableMap;
import org.opensaml.saml.saml2.core.Status;
import uk.gov.ida.saml.hub.domain.CountryAuthenticationStatus;
import uk.gov.ida.saml.hub.transformers.inbound.SamlStatusToCountryAuthenticationStatusMappingsFactory.SamlStatusDefinitions;

public class SamlStatusToCountryAuthenticationStatusCodeMapper {

    private static final ImmutableMap<SamlStatusDefinitions, CountryAuthenticationStatus.Status> STATUS_MAPPINGS =
            SamlStatusToCountryAuthenticationStatusMappingsFactory.getSamlToCountryAuthenticationStatusMappings();

    public static CountryAuthenticationStatus.Status map(Status samlStatus) {
        final String statusCodeValue = getStatusCodeValue(samlStatus);

        return STATUS_MAPPINGS.keySet().stream()
                .filter(k -> k.matches(statusCodeValue))
                .findFirst()
                .map(STATUS_MAPPINGS::get)
                .orElse(CountryAuthenticationStatus.Status.Failure);
    }

    private static String getStatusCodeValue(final Status status) {
        return status.getStatusCode().getValue();
    }
}
