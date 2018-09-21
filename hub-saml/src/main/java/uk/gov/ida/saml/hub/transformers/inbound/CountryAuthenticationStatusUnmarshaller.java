package uk.gov.ida.saml.hub.transformers.inbound;

import uk.gov.ida.saml.hub.domain.CountryAuthenticationStatus;

public class CountryAuthenticationStatusUnmarshaller extends AuthenticationStatusUnmarshallerBase<CountryAuthenticationStatus.Status, CountryAuthenticationStatus> {
    public CountryAuthenticationStatusUnmarshaller() {
        super(new SamlStatusToCountryAuthenticationStatusCodeMapper(), new CountryAuthenticationStatus.CountryAuthenticationStatusFactory());
    }
}
