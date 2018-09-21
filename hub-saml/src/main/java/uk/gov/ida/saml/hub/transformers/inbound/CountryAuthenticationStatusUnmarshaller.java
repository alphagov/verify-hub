package uk.gov.ida.saml.hub.transformers.inbound;

import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusMessage;
import uk.gov.ida.saml.hub.domain.CountryAuthenticationStatus;
import uk.gov.ida.saml.hub.domain.CountryAuthenticationStatus.CountryAuthenticationStatusFactory;

import java.util.Optional;

public class CountryAuthenticationStatusUnmarshaller {

    private static final CountryAuthenticationStatusFactory authenticationStatusFactory = new CountryAuthenticationStatusFactory();
    private static final SamlStatusToCountryAuthenticationStatusCodeMapper statusMapper = new SamlStatusToCountryAuthenticationStatusCodeMapper();

    private CountryAuthenticationStatusUnmarshaller() { }

    public static CountryAuthenticationStatus fromSaml(final Status samlStatus) {
        final CountryAuthenticationStatus.Status status = getStatus(samlStatus);
        final String message = getStatusMessage(samlStatus).orElse(null);
        return authenticationStatusFactory.create(status, message);
    }

    private static CountryAuthenticationStatus.Status getStatus(final Status samlStatus) {
        return statusMapper.map(samlStatus).get();
    }

    private static Optional<String> getStatusMessage(final Status samlStatus) {
        final StatusMessage statusMessage = samlStatus.getStatusMessage();
        return statusMessage != null ? Optional.of(statusMessage.getMessage()) : Optional.empty();
    }
}
