package uk.gov.ida.saml.hub.transformers.inbound;

import com.google.common.collect.ImmutableMap;
import org.opensaml.saml.saml2.core.Status;
import uk.gov.ida.saml.hub.domain.CountryAuthenticationStatus;
import uk.gov.ida.saml.hub.transformers.inbound.SamlStatusToCountryAuthenticationStatusMappingsFactory.SamlStatusDefinitions;

import java.util.Optional;

public class SamlStatusToCountryAuthenticationStatusCodeMapper extends SamlStatusToAuthenticationStatusCodeMapper<CountryAuthenticationStatus.Status> {

    private final ImmutableMap<SamlStatusDefinitions, CountryAuthenticationStatus.Status> statusMappings;

    public SamlStatusToCountryAuthenticationStatusCodeMapper() {
        this.statusMappings = SamlStatusToCountryAuthenticationStatusMappingsFactory.getSamlToCountryAuthenticationStatusMappings();
    }

    @Override
    public Optional<CountryAuthenticationStatus.Status> map(Status samlStatus) {
        final String statusCodeValue = getStatusCodeValue(samlStatus);

        final CountryAuthenticationStatus.Status mappedStatus =
                statusMappings.keySet().stream()
                        .filter(k -> k.matches(statusCodeValue))
                        .findFirst()
                        .map(statusMappings::get)
                        .orElse(CountryAuthenticationStatus.Status.Failure);

        return Optional.of(mappedStatus);
    }
}
