package uk.gov.ida.saml.hub.transformers.inbound;

import com.google.common.collect.ImmutableMap;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.StatusDetail;
import uk.gov.ida.saml.core.extensions.StatusValue;
import uk.gov.ida.saml.hub.domain.IdpIdaStatus;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

public class SamlStatusToIdaStatusCodeMapper {

    private final ImmutableMap<SamlStatusToIdpIdaStatusMappingsFactory.SamlStatusDefinitions, IdpIdaStatus.Status> statusMappings;

    public SamlStatusToIdaStatusCodeMapper(
            final ImmutableMap<SamlStatusToIdpIdaStatusMappingsFactory.SamlStatusDefinitions, IdpIdaStatus.Status> statusMappings) {
        this.statusMappings = statusMappings;
    }

    public Optional<IdpIdaStatus.Status> map(Status samlStatus) {
        final String statusCodeValue = getStatusCodeValue(samlStatus);
        final Optional<String> subStatusCodeValue = getSubStatusCodeValue(samlStatus);
        final List<String> statusDetailValues = getStatusDetailValues(samlStatus);

        return statusMappings.keySet().stream()
                .filter(k -> k.matches(statusCodeValue, subStatusCodeValue, statusDetailValues))
                .findFirst()
                .map(statusMappings::get);
    }

    private String getStatusCodeValue(final Status status) {
        return status.getStatusCode().getValue();
    }

    private Optional<String> getSubStatusCodeValue(final Status status) {
        return ofNullable(status.getStatusCode().getStatusCode()).map(StatusCode::getValue);
    }

    private List<String> getStatusDetailValues(Status samlStatus) {
        Optional<StatusDetail> statusDetail = ofNullable(samlStatus.getStatusDetail());

        return statusDetail.map(x -> x.getUnknownXMLObjects().stream()
                        .filter(child -> child.getElementQName().getLocalPart().equals(StatusValue.DEFAULT_ELEMENT_LOCAL_NAME))
                        .map(statusDetailVal -> statusDetailVal.getDOM().getFirstChild().getTextContent()).collect(toList())
        ).orElseGet(Collections::emptyList);
    }
}
