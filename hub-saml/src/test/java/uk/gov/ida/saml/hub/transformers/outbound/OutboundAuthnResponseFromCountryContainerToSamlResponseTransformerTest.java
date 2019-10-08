package uk.gov.ida.saml.hub.transformers.outbound;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.Response;
import uk.gov.ida.common.shared.security.IdGenerator;
import uk.gov.ida.saml.core.IdaConstants;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.domain.AuthnResponseFromCountryContainerDto;
import uk.gov.ida.saml.core.domain.CountrySignedResponseContainer;
import uk.gov.ida.saml.core.extensions.eidas.CountrySamlResponse;
import uk.gov.ida.saml.core.extensions.eidas.EncryptedAssertionKeys;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(OpenSAMLMockitoRunner.class)
public class OutboundAuthnResponseFromCountryContainerToSamlResponseTransformerTest {

    private OutboundAuthnResponseFromCountryContainerToSamlResponseTransformer transformer;
    private final String GENERATED_ID = "aGeneratedId";
    private final String HUB_ENTITY_ID = "http://hub-entity-id.gov.uk";
    private final String BASE_64_SAML = "base64Saml";
    private final String BASE_64_KEY_1 = "base64EncryptedKey1";
    private final String BASE_64_KEY_2 = "base64EncryptedKey2";
    private final String BASE_64_KEY_3 = "base64EncryptedKey3";
    private final String DESTINATION = "https://postendpoint.com";
    private final String RELAY_STATE = "relayState";
    private final String RESPONSE_ID = "responseID";
    private final String IN_RESPONSE_TO_ID = "responseID";
    private final String STATUS_SUCCESS_STRING = "urn:oasis:names:tc:SAML:2.0:status:Success";
    private final String COUNTRY_ENTITY_ID = "http://country-entity-id.gov.uk";
    private final String SERVICE_ENTITY_ID = "serviceEntityId";

    @Mock
    private IdGenerator idGenerator;

    @Before
    public void setUp() {
        when(idGenerator.getId()).thenReturn(GENERATED_ID);
        transformer = new OutboundAuthnResponseFromCountryContainerToSamlResponseTransformer(
                new OpenSamlXmlObjectFactory(),
                HUB_ENTITY_ID,
                idGenerator
        );
        DateTimeFreezer.freezeTime();
    }

    @After
    public void tearDown() {
        DateTimeFreezer.unfreezeTime();
    }

    @Test
    public void shouldReturnASamlResponseWithCountryResponseAndEncryptedKeysAsAttributes() {
        CountrySignedResponseContainer countrySignedResponseContainer = new CountrySignedResponseContainer(
                BASE_64_SAML,
                Arrays.asList(BASE_64_KEY_1),
                COUNTRY_ENTITY_ID
        );
        AuthnResponseFromCountryContainerDto countryResponse = new AuthnResponseFromCountryContainerDto(
                countrySignedResponseContainer,
                URI.create(DESTINATION),
                Optional.of(RELAY_STATE),
                RESPONSE_ID,
                SERVICE_ENTITY_ID,
                IN_RESPONSE_TO_ID
        );

        Response apply = transformer.apply(countryResponse);

        assertThat(apply.getIssuer().getValue()).isEqualTo(HUB_ENTITY_ID);
        assertThat(apply.getInResponseTo()).isEqualTo(IN_RESPONSE_TO_ID);
        assertThat(apply.getID()).isEqualTo(RESPONSE_ID);
        assertThat(apply.getIssueInstant()).isEqualByComparingTo(DateTime.now());
        assertThat(apply.getDestination()).isEqualTo(DESTINATION);
        assertThat(apply.getStatus().getStatusCode().getValue()).isEqualTo(STATUS_SUCCESS_STRING);
        assertThat(apply.getAssertions().get(0).getID()).isEqualTo(GENERATED_ID);
        assertThat(apply.getAssertions().get(0).getIssuer().getValue()).isEqualTo(HUB_ENTITY_ID);

        List<Attribute> attributes = apply.getAssertions().get(0).getAttributeStatements().get(0).getAttributes();

        assertThat(attributes.get(0).getName()).isEqualTo(IdaConstants.Eidas_Attributes.UnsignedAssertions.EidasSamlResponse.NAME);
        assertThat(attributes.get(0).getFriendlyName()).isEqualTo(IdaConstants.Eidas_Attributes.UnsignedAssertions.EidasSamlResponse.FRIENDLY_NAME);

        CountrySamlResponse countrySamlResponseValue = (CountrySamlResponse) attributes.get(0).getAttributeValues().get(0);
        assertThat(countrySamlResponseValue.getCountrySamlResponse()).isEqualTo(BASE_64_SAML);

        assertThat(attributes.get(1).getName()).isEqualTo(IdaConstants.Eidas_Attributes.UnsignedAssertions.EncryptedSecretKeys.NAME);
        assertThat(attributes.get(1).getFriendlyName()).isEqualTo(IdaConstants.Eidas_Attributes.UnsignedAssertions.EncryptedSecretKeys.FRIENDLY_NAME);

        EncryptedAssertionKeys encryptedAssertionKeysValue = (EncryptedAssertionKeys) attributes.get(1).getAttributeValues().get(0);
        assertThat(encryptedAssertionKeysValue.getEncryptedAssertionKeys()).isEqualTo(BASE_64_KEY_1);
    }

    @Test
    public void shouldAddMultipleEncryptedAttributeKeyValues() {
        List<String> keys = Arrays.asList(
                BASE_64_KEY_1,
                BASE_64_KEY_2,
                BASE_64_KEY_3
        );
        CountrySignedResponseContainer countrySignedResponseContainer = new CountrySignedResponseContainer(
                BASE_64_SAML,
                keys,
                COUNTRY_ENTITY_ID
        );
        AuthnResponseFromCountryContainerDto countryResponse = new AuthnResponseFromCountryContainerDto(
                countrySignedResponseContainer,
                URI.create(DESTINATION),
                Optional.of(RELAY_STATE),
                RESPONSE_ID,
                IN_RESPONSE_TO_ID,
                COUNTRY_ENTITY_ID
        );

        Response apply = transformer.apply(countryResponse);

        List<XMLObject> encryptedAssertionKeyValues = apply
                .getAssertions()
                .get(0)
                .getAttributeStatements()
                .get(0)
                .getAttributes()
                .get(1)
                .getAttributeValues();

        List<String> encryptedAssertionKeyStrings = encryptedAssertionKeyValues.stream()
                .map(key -> (EncryptedAssertionKeys) key)
                .map(key -> key.getEncryptedAssertionKeys())
                .collect(Collectors.toList());

        assertThat(encryptedAssertionKeyStrings).isEqualTo(keys);
    }
}
