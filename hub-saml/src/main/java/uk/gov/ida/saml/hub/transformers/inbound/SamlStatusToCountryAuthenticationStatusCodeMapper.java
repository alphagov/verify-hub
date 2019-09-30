package uk.gov.ida.saml.hub.transformers.inbound;

import org.opensaml.saml.saml2.core.Status;
import uk.gov.ida.saml.core.domain.DetailedStatusCode;
import uk.gov.ida.saml.hub.domain.CountryAuthenticationStatus;

import java.util.Optional;

public class SamlStatusToCountryAuthenticationStatusCodeMapper extends SamlStatusToAuthenticationStatusCodeMapper<CountryAuthenticationStatus.Status> {

    @Override
    public Optional<CountryAuthenticationStatus.Status> map(Status samlStatus) {
        return Optional.of(
                getStatusCodeValue(samlStatus).equals(DetailedStatusCode.Success.getStatus()) ?
                        CountryAuthenticationStatus.Status.Success :
                        CountryAuthenticationStatus.Status.Failure
        );
    }
}
