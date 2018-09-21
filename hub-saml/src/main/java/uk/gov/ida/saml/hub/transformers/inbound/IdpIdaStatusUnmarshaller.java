package uk.gov.ida.saml.hub.transformers.inbound;

import uk.gov.ida.saml.hub.domain.IdpIdaStatus;

public class IdpIdaStatusUnmarshaller extends AuthenticationStatusUnmarshallerBase<IdpIdaStatus.Status, IdpIdaStatus> {
    public IdpIdaStatusUnmarshaller() {
        super(new SamlStatusToIdaStatusCodeMapper(), new IdpIdaStatus.IdpIdaStatusFactory());
    }
}
