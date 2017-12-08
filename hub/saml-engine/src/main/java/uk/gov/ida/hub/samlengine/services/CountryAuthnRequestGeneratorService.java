package uk.gov.ida.hub.samlengine.services;

import uk.gov.ida.hub.samlengine.contracts.IdaAuthnRequestFromHubDto;
import uk.gov.ida.hub.samlengine.domain.SamlRequestDto;
import uk.gov.ida.hub.samlengine.proxy.CountrySingleSignOnServiceHelper;
import uk.gov.ida.saml.hub.domain.EidasAuthnRequestFromHub;

import javax.inject.Inject;
import java.net.URI;
import java.util.function.Function;

public class CountryAuthnRequestGeneratorService {

    private final Function<EidasAuthnRequestFromHub, String> eidasRequestStringTransformer;
    private final CountrySingleSignOnServiceHelper countrySingleSignOnServiceHelper;
    private final EidasAuthnRequestTranslator eidasAuthnRequestTranslator;
    private final String hubEidasEntityId;

    @Inject
    public CountryAuthnRequestGeneratorService(CountrySingleSignOnServiceHelper countrySingleSignOnServiceHelper,
                                               Function<EidasAuthnRequestFromHub, String> eidasRequestStringTransformer,
                                               EidasAuthnRequestTranslator eidasAuthnRequestTranslator,
                                               String hubEidasEntityId) {
        this.eidasRequestStringTransformer = eidasRequestStringTransformer;
        this.countrySingleSignOnServiceHelper = countrySingleSignOnServiceHelper;
        this.eidasAuthnRequestTranslator = eidasAuthnRequestTranslator;
        this.hubEidasEntityId = hubEidasEntityId;
    }

    public SamlRequestDto generateSaml(IdaAuthnRequestFromHubDto idaAuthnRequestFromHubDto) {
        URI ssoUri = idaAuthnRequestFromHubDto.getoverriddenSsoUrl() != null ? idaAuthnRequestFromHubDto.getoverriddenSsoUrl() :
                countrySingleSignOnServiceHelper.getSingleSignOn(idaAuthnRequestFromHubDto.getIdpEntityId());

        EidasAuthnRequestFromHub eidasAuthnRequestFromHub = eidasAuthnRequestTranslator.getEidasAuthnRequestFromHub(idaAuthnRequestFromHubDto, ssoUri, hubEidasEntityId);

        String request = eidasRequestStringTransformer.apply(eidasAuthnRequestFromHub);
        return new SamlRequestDto(request, ssoUri);
    }
}
