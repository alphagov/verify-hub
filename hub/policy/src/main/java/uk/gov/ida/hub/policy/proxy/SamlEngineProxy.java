package uk.gov.ida.hub.policy.proxy;

import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.annotations.SamlEngine;
import uk.gov.ida.hub.policy.contracts.AttributeQueryContainerDto;
import uk.gov.ida.hub.policy.contracts.AttributeQueryRequestDto;
import uk.gov.ida.hub.policy.contracts.AuthnResponseFromCountryContainerDto;
import uk.gov.ida.hub.policy.contracts.AuthnResponseFromHubContainerDto;
import uk.gov.ida.hub.policy.contracts.EidasAttributeQueryRequestDto;
import uk.gov.ida.hub.policy.contracts.InboundResponseFromMatchingServiceDto;
import uk.gov.ida.hub.policy.contracts.RequestForErrorResponseFromHubDto;
import uk.gov.ida.hub.policy.contracts.SamlAuthnResponseTranslatorDto;
import uk.gov.ida.hub.policy.contracts.SamlMessageDto;
import uk.gov.ida.hub.policy.contracts.SamlRequestDto;
import uk.gov.ida.hub.policy.contracts.SamlRequestWithAuthnRequestInformationDto;
import uk.gov.ida.hub.policy.contracts.SamlResponseContainerDto;
import uk.gov.ida.hub.policy.contracts.SamlResponseWithAuthnRequestInformationDto;
import uk.gov.ida.hub.policy.domain.IdaAuthnRequestFromHubDto;
import uk.gov.ida.hub.policy.domain.InboundResponseFromCountry;
import uk.gov.ida.hub.policy.domain.InboundResponseFromIdpDto;
import uk.gov.ida.hub.policy.domain.ResponseFromHub;
import uk.gov.ida.jerseyclient.JsonClient;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

@Singleton
public class SamlEngineProxy {

    private final JsonClient jsonClient;
    private final URI samlEngineUri;

    @Inject
    public SamlEngineProxy(JsonClient jsonClient, @SamlEngine URI samlEngineUri) {
        this.jsonClient = jsonClient;
        this.samlEngineUri = samlEngineUri;
    }

    public SamlResponseWithAuthnRequestInformationDto translate(String samlMessage) {
        URI uri = UriBuilder
                .fromUri(samlEngineUri)
                .path(Urls.SamlEngineUrls.TRANSLATE_RP_AUTHN_REQUEST_RESOURCE)
                .build();
        return jsonClient.post(new SamlRequestWithAuthnRequestInformationDto(samlMessage), uri, SamlResponseWithAuthnRequestInformationDto.class);
    }

    public SamlRequestDto generateIdpAuthnRequestFromHub(IdaAuthnRequestFromHubDto authnRequestFromHub) {
        URI uri = UriBuilder
                .fromUri(samlEngineUri)
                .path(Urls.SamlEngineUrls.GENERATE_IDP_AUTHN_REQUEST_RESOURCE)
                .build();
        return jsonClient.post(authnRequestFromHub, uri, SamlRequestDto.class);
    }

    public SamlRequestDto generateCountryAuthnRequestFromHub(IdaAuthnRequestFromHubDto authnRequestFromHub) {
        URI uri = UriBuilder
                .fromUri(samlEngineUri)
                .path(Urls.SamlEngineUrls.GENERATE_COUNTRY_AUTHN_REQUEST_RESOURCE)
                .build();
        return jsonClient.post(authnRequestFromHub, uri, SamlRequestDto.class);
    }

    public InboundResponseFromIdpDto translateAuthnResponseFromIdp(SamlAuthnResponseTranslatorDto samlResponseDto) {
        URI uri = UriBuilder
                .fromUri(samlEngineUri)
                .path(Urls.SamlEngineUrls.TRANSLATE_IDP_AUTHN_RESPONSE_RESOURCE)
                .build();
        return jsonClient.post(samlResponseDto, uri, InboundResponseFromIdpDto.class);
    }

    public InboundResponseFromCountry translateAuthnResponseFromCountry(SamlAuthnResponseTranslatorDto samlResponseDto) {
        URI uri = UriBuilder
            .fromUri(samlEngineUri)
            .path(Urls.SamlEngineUrls.TRANSLATE_COUNTRY_AUTHN_RESPONSE_RESOURCE)
            .build();
        return jsonClient.post(samlResponseDto, uri, InboundResponseFromCountry.class);
    }

    public AuthnResponseFromHubContainerDto generateRpAuthnResponse(ResponseFromHub responseFromHub) {
        URI uri = UriBuilder
                .fromUri(samlEngineUri)
                .path(Urls.SamlEngineUrls.GENERATE_RP_AUTHN_RESPONSE_RESOURCE)
                .build();
        return jsonClient.post(responseFromHub, uri, AuthnResponseFromHubContainerDto.class);
    }

    public AuthnResponseFromHubContainerDto generateRpAuthnResponseWrappingCountrySaml(AuthnResponseFromCountryContainerDto authnResponseFromCountryContainerDto) {
        URI uri = UriBuilder
                .fromUri(samlEngineUri)
                .path(Urls.SamlEngineUrls.GENERATE_RP_AUTHN_RESPONSE_WRAPPING_COUNTRY_RESPONSE_RESOURCE)
                .build();
        return jsonClient.post(authnResponseFromCountryContainerDto, uri, AuthnResponseFromHubContainerDto.class);
    }

    public AttributeQueryContainerDto generateAttributeQuery(AttributeQueryRequestDto attributeQueryRequestDto) {
        URI uri = UriBuilder
                .fromUri(samlEngineUri)
                .path(Urls.SamlEngineUrls.GENERATE_ATTRIBUTE_QUERY_RESOURCE)
                .build();

        return jsonClient.post(attributeQueryRequestDto, uri, AttributeQueryContainerDto.class);
    }

    public AttributeQueryContainerDto generateEidasAttributeQuery(EidasAttributeQueryRequestDto eidasAttributeQueryRequestDto) {
        URI uri = UriBuilder
                .fromUri(samlEngineUri)
                .path(Urls.SamlEngineUrls.GENERATE_COUNTRY_ATTRIBUTE_QUERY_RESOURCE)
                .build();

        return jsonClient.post(eidasAttributeQueryRequestDto, uri, AttributeQueryContainerDto.class);
    }

    public InboundResponseFromMatchingServiceDto translateMatchingServiceResponse(SamlResponseContainerDto samlResponse) {
        URI uri = UriBuilder
                .fromUri(samlEngineUri)
                .path(Urls.SamlEngineUrls.TRANSLATE_MATCHING_SERVICE_RESPONSE_RESOURCE)
                .build();
        return jsonClient.post(samlResponse, uri, InboundResponseFromMatchingServiceDto.class);
    }

    public SamlMessageDto generateErrorResponseFromHub(RequestForErrorResponseFromHubDto requestForErrorResponseFromHubDto) {
        URI uri = UriBuilder
                .fromUri(samlEngineUri)
                .path(Urls.SamlEngineUrls.GENERATE_RP_ERROR_RESPONSE_RESOURCE)
                .build();
        return jsonClient.post(requestForErrorResponseFromHubDto, uri, SamlMessageDto.class);
    }
}
