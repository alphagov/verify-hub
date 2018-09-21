package uk.gov.ida.saml.hub.transformers.inbound;

import net.shibboleth.utilities.java.support.xml.SerializeSupport;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusMessage;
import uk.gov.ida.saml.core.domain.IdaStatus;
import uk.gov.ida.saml.hub.domain.AuthenticationStatusFactory;

import java.util.Optional;

import static java.text.MessageFormat.format;

public abstract class AuthenticationStatusUnmarshallerBase<T extends Enum, U extends IdaStatus> {

    private final SamlStatusToAuthenticationStatusCodeMapper<T> statusMapper;
    private final AuthenticationStatusFactory<T, U> statusFactory;

    public AuthenticationStatusUnmarshallerBase(
            final SamlStatusToAuthenticationStatusCodeMapper<T> statusMapper,
            final AuthenticationStatusFactory<T, U> authenticationStatusFactory) {

        this.statusMapper = statusMapper;
        this.statusFactory = authenticationStatusFactory;
    }

    public U fromSaml(final Status samlStatus) {
        final T status = getStatus(samlStatus);
        final String message = getStatusMessage(samlStatus).orElse(null);
        return statusFactory.create(status, message);
    }

    private T getStatus(final Status samlStatus) {
        return statusMapper.map(samlStatus).orElseThrow(() -> new IllegalStateException(
                format("Could not map status to an IdaStatus: {0}", SerializeSupport.nodeToString(samlStatus.getDOM()))
        ));
    }

    private Optional<String> getStatusMessage(final Status samlStatus) {
        final StatusMessage statusMessage = samlStatus.getStatusMessage();
        return statusMessage != null ? Optional.of(statusMessage.getMessage()) : Optional.empty();
    }
}
