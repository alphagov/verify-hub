package uk.gov.ida.saml.core.domain;

import java.util.List;
import java.util.Optional;

public interface UnsignedAssertions {
    Optional<EidasUnsignedAssertions> getUnsignedAssertions() ;
    void setUnisgnedAssertions(EidasUnsignedAssertions unsignedAssertions);
}
