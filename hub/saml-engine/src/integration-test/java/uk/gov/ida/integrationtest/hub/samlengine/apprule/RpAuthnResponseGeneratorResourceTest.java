package uk.gov.ida.integrationtest.hub.samlengine.apprule;

import com.fasterxml.jackson.core.JsonProcessingException;
import helpers.JerseyClientConfigurationBuilder;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.util.Duration;
import net.shibboleth.utilities.java.support.codec.Base64Support;
import net.shibboleth.utilities.java.support.xml.XMLParserException;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.opensaml.saml.saml2.core.StatusCode;
import org.xml.sax.SAXException;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.hub.samlengine.contracts.AuthnResponseFromHubContainerDto;
import uk.gov.ida.hub.samlengine.contracts.ResponseFromHubDto;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.ConfigStubRule;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppRule;
import uk.gov.ida.saml.core.domain.TransactionIdaStatus;
import uk.gov.ida.saml.core.test.TestEntityIds;
import uk.gov.ida.saml.core.test.builders.AssertionBuilder;
import uk.gov.ida.saml.deserializers.parser.SamlObjectParser;
import uk.gov.ida.saml.serializers.XmlObjectToBase64EncodedStringTransformer;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URI;

import static io.dropwizard.testing.ConfigOverride.config;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.integrationtest.hub.samlengine.builders.ResponseFromHubDtoBuilder.aResponseFromHubDto;

public class RpAuthnResponseGeneratorResourceTest {

    private static Client client;

    @ClassRule
    public static ConfigStubRule configStub = new ConfigStubRule();

    @Rule
    public SamlEngineAppRule samlEngineAppRule = new SamlEngineAppRule(
            config("configUri", configStub.baseUri().build().toASCIIString())
    );

    @Before
    public void setUp() throws Exception {
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder.aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        if (client == null ) {
            client = new JerseyClientBuilder(samlEngineAppRule.getEnvironment()).using(jerseyClientConfiguration).build(RpAuthnResponseGeneratorResourceTest.class.getSimpleName());
        }
    }

    @Before
    public void before() {
        configStub.reset();
        DateTimeFreezer.freezeTime();
    }

    @After
    public void after() {
        DateTimeFreezer.unfreezeTime();
    }

    @Test
    public void shouldGenerateRpAuthnResponseWithMessageSignedByHubUsingLegacySamlStandard() throws Exception {
        // Given
        ResponseFromHubDto responseFromHubDto = aResponseFromHubDto()
                .withStatus(TransactionIdaStatus.NoMatchingServiceMatchFromHub)
                .withAuthnRequestIssuerEntityId(TestEntityIds.TEST_RP)
                .withAssertion(createAssertionString())
                .build();
        configStub.setupCertificatesForEntity(responseFromHubDto.getAuthnRequestIssuerEntityId());
        configStub.signResponsesAndUseLegacyStandard(responseFromHubDto.getAuthnRequestIssuerEntityId());

        // When
        URI generateAuthnResponseEndpoint = samlEngineAppRule.getUri(Urls.SamlEngineUrls.GENERATE_RP_AUTHN_RESPONSE_RESOURCE);
        Response rpAuthnResponse = client.target(generateAuthnResponseEndpoint).request().post(Entity.entity(responseFromHubDto, MediaType.APPLICATION_JSON_TYPE));

        // Then
        assertThat(rpAuthnResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        AuthnResponseFromHubContainerDto result = rpAuthnResponse.readEntity(AuthnResponseFromHubContainerDto
                .class);

        org.opensaml.saml.saml2.core.Response response = extractResponse(result);
        assertThat(response.getStatus().getStatusCode().getValue()).isEqualTo(StatusCode.SUCCESS);
        assertThat(response.getEncryptedAssertions()).isNotEmpty();
        assertThat(response.getID()).isEqualTo(responseFromHubDto.getResponseId());
        assertThat(response.getInResponseTo()).isEqualTo(responseFromHubDto.getInResponseTo());
        assertThat(response.getIssuer().getValue()).isEqualTo(TestEntityIds.HUB_ENTITY_ID);
    }

    @Test
    public void shouldGenerateRpAuthnResponseWithMessageSignedByHubUsingSamlProfileStandard() throws Exception {
        // Given
        String assertion = createAssertionString();

        ResponseFromHubDto responseFromHubDto = aResponseFromHubDto()
                .withAuthnRequestIssuerEntityId(TestEntityIds.TEST_RP)
                .withAssertion(assertion)
                .withStatus(TransactionIdaStatus.Success)
                .build();

        configStub.setupCertificatesForEntity(responseFromHubDto.getAuthnRequestIssuerEntityId());
        configStub.signResponsesAndUseSamlStandard(responseFromHubDto.getAuthnRequestIssuerEntityId());

        // When
        URI generateAuthnResponseEndpoint = samlEngineAppRule.getUri(Urls.SamlEngineUrls.GENERATE_RP_AUTHN_RESPONSE_RESOURCE);
        Response rpAuthnResponse = client.target(generateAuthnResponseEndpoint).request().post(Entity.entity(responseFromHubDto, MediaType.APPLICATION_JSON_TYPE));

        // Then
        assertThat(rpAuthnResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        AuthnResponseFromHubContainerDto actualResult = rpAuthnResponse.readEntity(AuthnResponseFromHubContainerDto
                .class);

        org.opensaml.saml.saml2.core.Response response = extractResponse(actualResult);

        assertThat(response.getStatus().getStatusCode().getValue()).isEqualTo(StatusCode.SUCCESS);
        assertThat(response.getEncryptedAssertions()).isNotEmpty();
        assertThat(response.getID()).isEqualTo(responseFromHubDto.getResponseId());
        assertThat(response.getInResponseTo()).isEqualTo(responseFromHubDto.getInResponseTo());
        assertThat(response.getIssuer().getValue()).isEqualTo(TestEntityIds.HUB_ENTITY_ID);
        assertThat(response.getSignature()).isNotNull();
    }

    @Test
    public void shouldGenerateRpAuthnResponseWithUnsignedMessage() throws Exception {
        // Given
        String assertion = createAssertionString();

        ResponseFromHubDto responseFromHubDto = aResponseFromHubDto()
                .withAuthnRequestIssuerEntityId(TestEntityIds.TEST_RP)
                .withAssertion(assertion)
                .withStatus(TransactionIdaStatus.Success)
                .build();

        configStub.setupCertificatesForEntity(responseFromHubDto.getAuthnRequestIssuerEntityId());
        configStub.doNotSignResponseMessages(responseFromHubDto.getAuthnRequestIssuerEntityId());

        // When
        URI generateAuthnResponseEndpoint = samlEngineAppRule.getUri(Urls.SamlEngineUrls.GENERATE_RP_AUTHN_RESPONSE_RESOURCE);
        Response rpAuthnResponse = client.target(generateAuthnResponseEndpoint).request().post(Entity.entity(responseFromHubDto, MediaType.APPLICATION_JSON_TYPE));

        // Then
        assertThat(rpAuthnResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        AuthnResponseFromHubContainerDto result = rpAuthnResponse.readEntity(AuthnResponseFromHubContainerDto.class);

        org.opensaml.saml.saml2.core.Response response = extractResponse(result);

        assertThat(response.getStatus().getStatusCode().getValue()).isEqualTo(StatusCode.SUCCESS);
        assertThat(response.getEncryptedAssertions()).isNotEmpty();
        assertThat(response.getID()).isEqualTo(responseFromHubDto.getResponseId());
        assertThat(response.getInResponseTo()).isEqualTo(responseFromHubDto.getInResponseTo());
        assertThat(response.getSignature()).isNull();
        assertThat(response.getIssuer()).isNull();
    }

    private String createAssertionString() {
        return new XmlObjectToBase64EncodedStringTransformer<>().apply(AssertionBuilder.anAssertion().buildUnencrypted());
    }

    private org.opensaml.saml.saml2.core.Response extractResponse(AuthnResponseFromHubContainerDto actualResult) throws org.opensaml.core.xml.io.UnmarshallingException, IOException, SAXException, ParserConfigurationException, XMLParserException {
        return new SamlObjectParser().getSamlObject(new String(Base64Support.decode(actualResult.getSamlResponse())));
    }

    @Test
    public void shouldReturnAnErrorResponseGivenBadInput() throws JsonProcessingException {
        ResponseFromHubDto responseFromHubDto = aResponseFromHubDto().withAssertionConsumerServiceUri(null).build();
        configStub.signResponsesAndUseSamlStandard(responseFromHubDto.getAuthnRequestIssuerEntityId());

        URI generateAuthnResponseEndpoint = samlEngineAppRule.getUri(Urls.SamlEngineUrls.GENERATE_RP_AUTHN_RESPONSE_RESOURCE);
        Response rpAuthnResponse = client.target(generateAuthnResponseEndpoint).request().post(Entity.entity(responseFromHubDto, MediaType.APPLICATION_JSON_TYPE));

        assertThat(rpAuthnResponse.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto errorStatusDto = rpAuthnResponse.readEntity(ErrorStatusDto.class);
        assertThat(errorStatusDto.getExceptionType()).isEqualTo(ExceptionType.INVALID_INPUT);
    }
}
