package uk.gov.ida.hub.samlengine.exceptions;

import net.shibboleth.utilities.java.support.resolver.ResolverException;

public class UnableToResolveSigningCertsForHubException extends RuntimeException {
    public UnableToResolveSigningCertsForHubException(ResolverException e) {
        super(e);
    }
}
