package uk.gov.ida.integrationtest.hub.samlengine.apprule;

import com.fasterxml.jackson.core.JsonProcessingException;
import helpers.JerseyClientConfigurationBuilder;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.util.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.opensaml.saml.saml2.core.StatusCode;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.hub.samlengine.contracts.AuthnResponseFromHubContainerDto;
import uk.gov.ida.hub.samlengine.contracts.ResponseFromHubDto;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.ConfigStubRule;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppRule;
import uk.gov.ida.saml.core.domain.TransactionIdaStatus;
import uk.gov.ida.saml.deserializers.parser.SamlObjectParser;
import uk.gov.ida.saml.deserializers.validators.Base64StringDecoder;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

import static io.dropwizard.testing.ConfigOverride.config;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.integrationtest.hub.samlengine.builders.AuthnResponseFromHubContainerDtoBuilder.anAuthnResponseFromHubContainerDto;
import static uk.gov.ida.integrationtest.hub.samlengine.builders.ResponseFromHubDtoBuilder.aResponseFromHubDto;

public class RpAuthnResponseGeneratorResourceTest {

    private static String TEST_SAML_MESSAGE_RESOURCE = Urls.SamlEngineUrls.SAML_ENGINE_ROOT + "/test";
    private static Client client;

    @ClassRule
    public static ConfigStubRule configStub = new ConfigStubRule();

    @ClassRule
    public static SamlEngineAppRule samlEngineAppRule = new SamlEngineAppRule(
            config("configUri", configStub.baseUri().build().toASCIIString())
    );

    @BeforeClass
    public static void setUp() throws Exception {
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder.aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        client = new JerseyClientBuilder(samlEngineAppRule.getEnvironment()).using(jerseyClientConfiguration).build(RpAuthnResponseGeneratorResourceTest.class.getSimpleName());
    }

    @Before
    public void before() {
        DateTimeFreezer.freezeTime();
    }

    @After
    public void after() {
        DateTimeFreezer.unfreezeTime();
    }

    @Test
    public void shouldGenerateRpAuthnResponseWithMessageSignedByHub() throws Exception {
        // Given
        ResponseFromHubDto responseFromHubDto = aResponseFromHubDto().build();
        configStub.setUpStubForShouldHubSignResponseMessagesForSamlStandard(responseFromHubDto.getAuthnRequestIssuerEntityId());
        Response samlMessageResponse = postToTestSamlMessageResource(responseFromHubDto);

        assertThat(samlMessageResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        AuthnResponseFromHubContainerDto expectedResult = anAuthnResponseFromHubContainerDto()
                .withSamlResponse(samlMessageResponse.readEntity(String.class))
                .withPostEndPoint(responseFromHubDto.getAssertionConsumerServiceUri())
                .withResponseId(responseFromHubDto.getResponseId())
                .withRelayState(responseFromHubDto.getRelayState())
                .build();

        // When
        URI generateAuthnResponseEndpoint = samlEngineAppRule.getUri(Urls.SamlEngineUrls.GENERATE_RP_AUTHN_RESPONSE_RESOURCE);
        Response rpAuthnResponse = client.target(generateAuthnResponseEndpoint).request().post(Entity.entity(responseFromHubDto, MediaType.APPLICATION_JSON_TYPE));

        // Then
        assertThat(rpAuthnResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        AuthnResponseFromHubContainerDto actualResult = rpAuthnResponse.readEntity(AuthnResponseFromHubContainerDto
                .class);
        assertThat(actualResult).isEqualToComparingFieldByField(expectedResult);

        assertStatusCode(actualResult.getSamlResponse(), StatusCode.SUCCESS);
    }

    @Test
    public void shouldGenerateRpAuthnResponseWithMessageSignedByHubUsingLegacySamlStandard() throws Exception {
        // Given
        ResponseFromHubDto responseFromHubDto = aResponseFromHubDto().withStatus(TransactionIdaStatus.NoMatchingServiceMatchFromHub).build();
        configStub.setUpStubForShouldHubSignResponseMessagesForLegacySamlStandard(responseFromHubDto.getAuthnRequestIssuerEntityId());
        Response samlMessageResponse = postToTestSamlMessageResource(responseFromHubDto, TransactionIdaStatus.NoMatchingServiceMatchFromHub);

        assertThat(samlMessageResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        AuthnResponseFromHubContainerDto expectedResult = anAuthnResponseFromHubContainerDto()
                .withSamlResponse(samlMessageResponse.readEntity(String.class))
                .withPostEndPoint(responseFromHubDto.getAssertionConsumerServiceUri())
                .withResponseId(responseFromHubDto.getResponseId())
                .withRelayState(responseFromHubDto.getRelayState())
                .build();

        // When
        URI generateAuthnResponseEndpoint = samlEngineAppRule.getUri(Urls.SamlEngineUrls.GENERATE_RP_AUTHN_RESPONSE_RESOURCE);
        Response rpAuthnResponse = client.target(generateAuthnResponseEndpoint).request().post(Entity.entity(responseFromHubDto, MediaType.APPLICATION_JSON_TYPE));

        // Then
        assertThat(rpAuthnResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        AuthnResponseFromHubContainerDto actualResult = rpAuthnResponse.readEntity(AuthnResponseFromHubContainerDto
                .class);
        assertThat(actualResult).isEqualToComparingFieldByField(expectedResult);

        assertStatusCode(actualResult.getSamlResponse(), StatusCode.SUCCESS);
    }

    @Test
    public void shouldGenerateRpAuthnResponseWithMessageSignedByHubUsingSamlProfileStandard() throws Exception {
        // Given
        ResponseFromHubDto responseFromHubDto = aResponseFromHubDto().withStatus(TransactionIdaStatus.NoMatchingServiceMatchFromHub).build();
        configStub.setUpStubForShouldHubSignResponseMessagesForSamlStandard(responseFromHubDto.getAuthnRequestIssuerEntityId());
        Response samlMessageResponse = postToTestSamlMessageResource(responseFromHubDto, TransactionIdaStatus.NoMatchingServiceMatchFromHub);

        assertThat(samlMessageResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        AuthnResponseFromHubContainerDto expectedResult = anAuthnResponseFromHubContainerDto()
                .withSamlResponse(samlMessageResponse.readEntity(String.class))
                .withPostEndPoint(responseFromHubDto.getAssertionConsumerServiceUri())
                .withResponseId(responseFromHubDto.getResponseId())
                .withRelayState(responseFromHubDto.getRelayState())
                .build();

        // When
        URI generateAuthnResponseEndpoint = samlEngineAppRule.getUri(Urls.SamlEngineUrls.GENERATE_RP_AUTHN_RESPONSE_RESOURCE);
        Response rpAuthnResponse = client.target(generateAuthnResponseEndpoint).request().post(Entity.entity(responseFromHubDto, MediaType.APPLICATION_JSON_TYPE));

        // Then
        assertThat(rpAuthnResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        AuthnResponseFromHubContainerDto actualResult = rpAuthnResponse.readEntity(AuthnResponseFromHubContainerDto
                .class);
        assertThat(actualResult).isEqualToComparingFieldByField(expectedResult);

        assertStatusCode(actualResult.getSamlResponse(), StatusCode.RESPONDER);
    }

    @Test
    public void shouldGenerateRpAuthnResponseWithUnsignedMessage() throws Exception {
        // Given
        ResponseFromHubDto responseFromHubDto = aResponseFromHubDto().build();
        configStub.setUpStubForShouldHubSignResponseMessagesForSamlStandard(responseFromHubDto.getAuthnRequestIssuerEntityId());

        Response samlMessageResponse = postToTestSamlMessageResource(responseFromHubDto);
        assertThat(samlMessageResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        AuthnResponseFromHubContainerDto expectedResult = anAuthnResponseFromHubContainerDto()
                .withSamlResponse(samlMessageResponse.readEntity(String.class))
                .withPostEndPoint(responseFromHubDto.getAssertionConsumerServiceUri())
                .withResponseId(responseFromHubDto.getResponseId())
                .withRelayState(responseFromHubDto.getRelayState())
                .build();

        // When
        URI generateAuthnResponseEndpoint = samlEngineAppRule.getUri(Urls.SamlEngineUrls.GENERATE_RP_AUTHN_RESPONSE_RESOURCE);
        Response rpAuthnResponse = client.target(generateAuthnResponseEndpoint).request().post(Entity.entity(responseFromHubDto, MediaType.APPLICATION_JSON_TYPE));

        // Then
        assertThat(rpAuthnResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        AuthnResponseFromHubContainerDto actualResult = rpAuthnResponse.readEntity(AuthnResponseFromHubContainerDto.class);
        assertThat(actualResult).isEqualToComparingFieldByField(expectedResult);

        assertStatusCode(actualResult.getSamlResponse(), StatusCode.SUCCESS);
    }

    @Test
    public void shouldReturnAnErrorResponseGivenBadInput() throws JsonProcessingException {
        ResponseFromHubDto responseFromHubDto = aResponseFromHubDto().withAssertionConsumerServiceUri(null).build();
        configStub.setUpStubForShouldHubSignResponseMessagesForSamlStandard(responseFromHubDto.getAuthnRequestIssuerEntityId());

        URI generateAuthnResponseEndpoint = samlEngineAppRule.getUri(Urls.SamlEngineUrls.GENERATE_RP_AUTHN_RESPONSE_RESOURCE);
        Response rpAuthnResponse = client.target(generateAuthnResponseEndpoint).request().post(Entity.entity(responseFromHubDto, MediaType.APPLICATION_JSON_TYPE));

        assertThat(rpAuthnResponse.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto errorStatusDto = rpAuthnResponse.readEntity(ErrorStatusDto.class);
        assertThat(errorStatusDto.getExceptionType()).isEqualTo(ExceptionType.INVALID_INPUT);
    }

    private static void assertStatusCode(String samlString, String statusCode) throws javax.xml.parsers.ParserConfigurationException, org.xml.sax.SAXException, java.io.IOException, org.opensaml.core.xml.io.UnmarshallingException {
        final SamlObjectParser samlObjectParser = new SamlObjectParser();
        final Base64StringDecoder base64Decoder = new Base64StringDecoder();
        String decodedString = base64Decoder.decode(samlString);
        org.opensaml.saml.saml2.core.Response samlResponse = samlObjectParser.getSamlObject(decodedString);
        assertThat(samlResponse.getStatus().getStatusCode().getValue()).isEqualTo(statusCode);
    }

    private Response postToTestSamlMessageResource(ResponseFromHubDto dto, TransactionIdaStatus transactionIdaStatus) {
        return client.target(samlEngineAppRule.getUri(TEST_SAML_MESSAGE_RESOURCE)).queryParam("transactionIdaStatus", transactionIdaStatus).request().post(Entity.entity(dto, MediaType.APPLICATION_JSON_TYPE));
    }

    private Response postToTestSamlMessageResource(ResponseFromHubDto dto) {
        return postToTestSamlMessageResource(dto, TransactionIdaStatus.Success);
    }
}
