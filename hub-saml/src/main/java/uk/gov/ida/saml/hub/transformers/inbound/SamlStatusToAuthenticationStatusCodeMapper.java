package uk.gov.ida.saml.hub.transformers.inbound;

import org.opensaml.saml.saml2.core.Status;

import java.util.Optional;

public interface SamlStatusToAuthenticationStatusCodeMapper<T extends Enum> {
    Optional<T> map(Status samlStatus);
}
