package uk.gov.ida.hub.samlengine.services;

import uk.gov.ida.hub.samlengine.contracts.IdaAuthnRequestFromHubDto;
import uk.gov.ida.saml.hub.domain.EidasAuthnRequestFromHub;

import javax.inject.Inject;
import java.net.URI;

import static uk.gov.ida.saml.hub.domain.EidasAuthnRequestFromHub.createRequestToSendFromHub;

public class EidasAuthnRequestTranslator {

    @Inject
    public EidasAuthnRequestTranslator() {
    }

    public EidasAuthnRequestFromHub getEidasAuthnRequestFromHub(IdaAuthnRequestFromHubDto idaAuthnRequestFromHubDto, URI ssoUri, String hubEidasEntityId) {
        return createRequestToSendFromHub(
                idaAuthnRequestFromHubDto.getId(),
                idaAuthnRequestFromHubDto.getLevelsOfAssurance(),
                ssoUri,
                "HUB_PROVIDER",
                hubEidasEntityId);
    }
}
