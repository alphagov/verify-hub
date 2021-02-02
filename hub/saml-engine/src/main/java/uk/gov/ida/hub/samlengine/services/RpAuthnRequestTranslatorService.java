package uk.gov.ida.hub.samlengine.services;

import io.prometheus.client.Gauge;
import org.opensaml.saml.saml2.core.AuthnRequest;
import uk.gov.ida.hub.samlengine.contracts.SamlRequestWithAuthnRequestInformationDto;
import uk.gov.ida.hub.samlengine.contracts.TranslatedAuthnRequestDto;
import uk.gov.ida.hub.samlengine.logging.MdcHelper;
import uk.gov.ida.hub.samlengine.logging.UnknownMethodAlgorithmLogger;
import uk.gov.ida.saml.deserializers.StringToOpenSamlObjectTransformer;
import uk.gov.ida.saml.hub.domain.AuthnRequestFromRelyingParty;
import uk.gov.ida.saml.hub.transformers.inbound.AuthnRequestToIdaRequestFromRelyingPartyTransformer;

import javax.inject.Inject;
import javax.inject.Named;

public class RpAuthnRequestTranslatorService {

    private final StringToOpenSamlObjectTransformer<AuthnRequest> stringToAuthnRequestTransformer;
    private final AuthnRequestToIdaRequestFromRelyingPartyTransformer authnRequestToIdaRequestFromRelyingPartyTransformer;
    private final Gauge vspVersionGauge;

    @Inject
    public RpAuthnRequestTranslatorService(
        StringToOpenSamlObjectTransformer<AuthnRequest> stringToAuthnRequestTransformer,
        AuthnRequestToIdaRequestFromRelyingPartyTransformer authnRequestToIdaRequestFromRelyingPartyTransformer,
        @Named("VspVersionGauge") Gauge vspVersionGauge
    ) {
        this.stringToAuthnRequestTransformer = stringToAuthnRequestTransformer;
        this.authnRequestToIdaRequestFromRelyingPartyTransformer = authnRequestToIdaRequestFromRelyingPartyTransformer;
        this.vspVersionGauge = vspVersionGauge;
    }

    public TranslatedAuthnRequestDto translate(SamlRequestWithAuthnRequestInformationDto samlRequestWithAuthnRequestInformationDto) {
        AuthnRequest authnRequest = stringToAuthnRequestTransformer.apply(samlRequestWithAuthnRequestInformationDto.getSamlMessage());

        MdcHelper.addContextToMdc(authnRequest.getID(), authnRequest.getIssuer().getValue());

        AuthnRequestFromRelyingParty authnRequestFromRelyingParty = authnRequestToIdaRequestFromRelyingPartyTransformer.apply(authnRequest);

        if (authnRequestFromRelyingParty.getVerifyServiceProviderVersion().isPresent()) {
            vspVersionGauge
                .labels(
                        authnRequestFromRelyingParty.getIssuer(),
                        authnRequestFromRelyingParty.getVerifyServiceProviderVersion().get())
                .set(1.0);
        };

        UnknownMethodAlgorithmLogger.probeAuthnRequestForMethodAlgorithm(authnRequestFromRelyingParty);

        return new TranslatedAuthnRequestDto(
            authnRequestFromRelyingParty.getId(),
            authnRequestFromRelyingParty.getIssuer(),
            authnRequestFromRelyingParty.getForceAuthentication(),
            authnRequestFromRelyingParty.getAssertionConsumerServiceUrl(),
            authnRequestFromRelyingParty.getAssertionConsumerServiceIndex());
    }
}
