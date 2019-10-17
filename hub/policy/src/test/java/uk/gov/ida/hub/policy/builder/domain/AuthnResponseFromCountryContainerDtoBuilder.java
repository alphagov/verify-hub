package uk.gov.ida.hub.policy.builder.domain;

import uk.gov.ida.saml.core.domain.AuthnResponseFromCountryContainerDto;
import uk.gov.ida.saml.core.domain.CountrySignedResponseContainer;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public class AuthnResponseFromCountryContainerDtoBuilder {

    private static final String BASE_64_SAML_RESPONSE = "samlResponse";
    private static final List<String> BASE_64_ENCRYPTED_KEYS = List.of("base64EncryptedKey");
    private static final String COUNTRY_ENTITY_ID = "countryEntityId";
    private static final URI POST_ENDPOINT = URI.create("https://post-end-point");
    private static final String RELAY_STATE = "relayState";
    private static final String IN_RESPONSE_TO = "inResponseTo";
    private static final String RESPONSE_ID = "responseId";
    private static final String SERVICE_ENTITY_ID = "serviceEntityId";

    public static AuthnResponseFromCountryContainerDtoBuilder anAuthnResponseFromCountryContainerDto() {
        return new AuthnResponseFromCountryContainerDtoBuilder();
    }

    public AuthnResponseFromCountryContainerDto build() {
        CountrySignedResponseContainer countrySignedResponseContainer = new CountrySignedResponseContainer(
                BASE_64_SAML_RESPONSE,
                BASE_64_ENCRYPTED_KEYS,
                COUNTRY_ENTITY_ID
        );
        return new AuthnResponseFromCountryContainerDto(
                countrySignedResponseContainer,
                POST_ENDPOINT,
                Optional.of(RELAY_STATE),
                IN_RESPONSE_TO,
                SERVICE_ENTITY_ID,
                RESPONSE_ID
        );
    }
}
