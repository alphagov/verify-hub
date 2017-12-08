package uk.gov.ida.hub.samlengine.services;

import uk.gov.ida.hub.samlengine.contracts.IdaAuthnRequestFromHubDto;
import uk.gov.ida.hub.samlengine.domain.SamlRequestDto;
import uk.gov.ida.hub.samlengine.proxy.IdpSingleSignOnServiceHelper;
import uk.gov.ida.saml.hub.domain.IdaAuthnRequestFromHub;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.URI;
import java.util.function.Function;

public class IdpAuthnRequestGeneratorService {

    private final Function<IdaAuthnRequestFromHub, String> idaRequestStringTransformer;
    private final IdpSingleSignOnServiceHelper idpSingleSignOnServiceHelper;
    private final IdaAuthnRequestTranslator idaAuthnRequestTranslator;
    private final String hubEntityId;

    @Inject
    public IdpAuthnRequestGeneratorService(IdpSingleSignOnServiceHelper idpSingleSignOnServiceHelper,
                                           Function<IdaAuthnRequestFromHub, String> idaRequestStringTransformer,
                                           IdaAuthnRequestTranslator idaAuthnRequestTranslator,
                                           @Named("HubEntityId") String hubEntityId) {
        this.idaRequestStringTransformer = idaRequestStringTransformer;
        this.idpSingleSignOnServiceHelper = idpSingleSignOnServiceHelper;
        this.idaAuthnRequestTranslator = idaAuthnRequestTranslator;
        this.hubEntityId = hubEntityId;
    }

    public SamlRequestDto generateSaml(IdaAuthnRequestFromHubDto idaAuthnRequestFromHubDto) {
        URI ssoUri = idpSingleSignOnServiceHelper.getSingleSignOn(idaAuthnRequestFromHubDto.getIdpEntityId());

        IdaAuthnRequestFromHub idaAuthnRequestFromHub = idaAuthnRequestTranslator.getIdaAuthnRequestFromHub(idaAuthnRequestFromHubDto, ssoUri, hubEntityId);

        String request = idaRequestStringTransformer.apply(idaAuthnRequestFromHub);
        return new SamlRequestDto(request, ssoUri);
    }

}
