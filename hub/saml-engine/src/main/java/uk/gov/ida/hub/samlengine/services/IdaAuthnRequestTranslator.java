package uk.gov.ida.hub.samlengine.services;

import org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration;
import uk.gov.ida.hub.samlengine.contracts.IdaAuthnRequestFromHubDto;
import uk.gov.ida.saml.core.domain.AuthnContext;
import uk.gov.ida.saml.hub.domain.IdaAuthnRequestFromHub;

import javax.inject.Inject;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration.EXACT;
import static org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration.MINIMUM;
import static uk.gov.ida.saml.hub.domain.IdaAuthnRequestFromHub.createRequestToSendFromHub;

public class IdaAuthnRequestTranslator {

    @Inject
    public IdaAuthnRequestTranslator() {
    }

    public IdaAuthnRequestFromHub getIdaAuthnRequestFromHub(IdaAuthnRequestFromHubDto idaAuthnRequestFromHubDto, URI ssoUri, String hubEntityId) {
        List<AuthnContext> levelsOfAssurance = idaAuthnRequestFromHubDto.getLevelsOfAssurance();
        AuthnContextComparisonTypeEnumeration comparisonType;

        if (idaAuthnRequestFromHubDto.getUseExactComparisonType()) {
            comparisonType = EXACT;
        } else {
            comparisonType = MINIMUM;
            if (levelsOfAssurance.size() == 1) {
                levelsOfAssurance = Arrays.asList(levelsOfAssurance.get(0), levelsOfAssurance.get(0));
            }
        }

        return createRequestToSendFromHub(
                idaAuthnRequestFromHubDto.getId(),
                levelsOfAssurance,
                idaAuthnRequestFromHubDto.getForceAuthentication(),
                idaAuthnRequestFromHubDto.getSessionExpiryTimestamp(),
                ssoUri,
                comparisonType,
                hubEntityId);
    }
}
