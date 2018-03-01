package uk.gov.ida.saml.hub.transformers.inbound;

import net.shibboleth.utilities.java.support.xml.SerializeSupport;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusMessage;
import uk.gov.ida.saml.hub.domain.IdpIdaStatus;

import java.util.Optional;

import static java.text.MessageFormat.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;

public class IdpIdaStatusUnmarshaller {

    private final SamlStatusToIdaStatusCodeMapper idpStatusMapper;
    private final IdpIdaStatus.IdpIdaStatusFactory statusFactory;

    public IdpIdaStatusUnmarshaller(
            final IdpIdaStatus.IdpIdaStatusFactory statusFactory,
            final SamlStatusToIdpIdaStatusMappingsFactory statusMappingsFactory) {

        this.idpStatusMapper = new SamlStatusToIdaStatusCodeMapper(
                statusMappingsFactory.getSamlToIdpIdaStatusMappings()
        );
        this.statusFactory = statusFactory;
    }

    public IdpIdaStatus fromSaml(final Status samlStatus) {
        final IdpIdaStatus.Status status = getStatus(samlStatus);
        final Optional<String> message = getStatusMessage(samlStatus);
        return statusFactory.create(status, message);
    }

    private IdpIdaStatus.Status getStatus(final Status samlStatus) {
        return idpStatusMapper.map(samlStatus).orElseThrow(() -> new IllegalStateException(
                format("Could not map status to a IdpIdaStatus: {0}", SerializeSupport.nodeToString(samlStatus.getDOM()))
        ));
    }

    private Optional<String> getStatusMessage(final Status samlStatus) {
        final StatusMessage statusMessage = samlStatus.getStatusMessage();
        return statusMessage != null ? of(statusMessage.getMessage()) : empty();
    }

}
