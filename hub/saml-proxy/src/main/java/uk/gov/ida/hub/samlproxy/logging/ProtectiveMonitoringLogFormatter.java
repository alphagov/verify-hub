package uk.gov.ida.hub.samlproxy.logging;

import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.StatusDetail;
import uk.gov.ida.hub.samlproxy.repositories.Direction;
import uk.gov.ida.hub.samlproxy.repositories.SignatureStatus;
import uk.gov.ida.saml.core.extensions.StatusValue;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

public class ProtectiveMonitoringLogFormatter {
    private static final String AUTHN_REQUEST = "Protective Monitoring – Authn Request Event – {requestId: %s, " +
            "direction: %s, destination: %s, issuerId: %s, validSignature: %s}";
    private static final String AUTHN_RESPONSE = "Protective Monitoring – Authn Response Event – {responseId: %s, " +
            "inResponseTo: %s, direction: %s, destination: %s, issuerId: %s, validSignature: %s, " +
            "status: %s, subStatus: %s, statusDetails: %s}";

    public String formatAuthnRequest(AuthnRequest authnRequest, Direction direction, SignatureStatus signatureStatus) {
        Issuer issuer = authnRequest.getIssuer();
        String issuerId = issuer != null ? issuer.getValue() : "";

        return String.format(AUTHN_REQUEST,
                authnRequest.getID(),
                direction,
                authnRequest.getDestination(),
                issuerId,
                signatureStatus.valid());
    }

    public String formatAuthnResponse(Response samlResponse, Direction direction, SignatureStatus signatureStatus) {
        Issuer issuer = samlResponse.getIssuer();
        String issuerString = issuer != null ? issuer.getValue() : "";

        Status status = samlResponse.getStatus();
        StatusCode subStatusCode = status.getStatusCode().getStatusCode();
        String subStatus = subStatusCode != null ? subStatusCode.getValue() : "";

        return String.format(AUTHN_RESPONSE,
                samlResponse.getID(),
                samlResponse.getInResponseTo(),
                direction,
                samlResponse.getDestination(),
                issuerString,
                signatureStatus.valid(),
                status.getStatusCode().getValue(),
                subStatus,
                getStatusDetailValues(status));
    }

    private List<String> getStatusDetailValues(Status samlStatus) {
        Optional<StatusDetail> statusDetail = ofNullable(samlStatus.getStatusDetail());

        return statusDetail.map(x -> x.getUnknownXMLObjects().stream()
                .filter(child -> child.getElementQName().getLocalPart().equals(StatusValue.DEFAULT_ELEMENT_LOCAL_NAME))
                .map(statusDetailVal -> statusDetailVal.getDOM().getFirstChild().getTextContent()).collect(toList())
        ).orElseGet(Collections::emptyList);
    }
}
