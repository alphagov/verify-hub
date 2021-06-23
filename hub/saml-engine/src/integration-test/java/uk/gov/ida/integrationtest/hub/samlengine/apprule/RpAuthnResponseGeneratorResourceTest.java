package uk.gov.ida.integrationtest.hub.samlengine.apprule;

import com.fasterxml.jackson.core.JsonProcessingException;
import net.shibboleth.utilities.java.support.codec.Base64Support;
import net.shibboleth.utilities.java.support.xml.XMLParserException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.opensaml.saml.saml2.core.StatusCode;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.hub.samlengine.contracts.AuthnResponseFromHubContainerDto;
import uk.gov.ida.hub.samlengine.contracts.ResponseFromHubDto;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.ConfigStubExtension;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppExtension;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppExtension.SamlEngineAppExtensionBuilder;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppExtension.SamlEngineClient;
import uk.gov.ida.saml.core.domain.TransactionIdaStatus;
import uk.gov.ida.saml.core.test.TestEntityIds;
import uk.gov.ida.saml.core.test.builders.AssertionBuilder;
import uk.gov.ida.saml.deserializers.parser.SamlObjectParser;
import uk.gov.ida.saml.serializers.XmlObjectToBase64EncodedStringTransformer;

import javax.ws.rs.core.Response;
import java.util.Collections;

import static io.dropwizard.testing.ConfigOverride.config;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.integrationtest.hub.samlengine.builders.ResponseFromHubDtoBuilder.aResponseFromHubDto;

public class RpAuthnResponseGeneratorResourceTest {

    @Order(0)
    @RegisterExtension
    public static ConfigStubExtension configStub = new ConfigStubExtension();

    @Order(1)
    @RegisterExtension
    public static SamlEngineAppExtension samlEngineApp = new SamlEngineAppExtensionBuilder()
            .withConfigOverrides(
                    config("configUri", () -> configStub.baseUri().build().toASCIIString())
            )
            .build();

    private SamlEngineClient client;

    @BeforeEach
    public void beforeEach() {
        client = samlEngineApp.getClient();
        configStub.reset();;
    }

    @AfterAll
    public static void afterAll() {
        samlEngineApp.tearDown();
    }

    @Test
    public void shouldGenerateRpAuthnResponseWithMessageSignedByHubUsingLegacySamlStandard() throws Exception {
        // Given
        ResponseFromHubDto responseFromHubDto = aResponseFromHubDto()
                .withStatus(TransactionIdaStatus.NoMatchingServiceMatchFromHub)
                .withAuthnRequestIssuerEntityId(TestEntityIds.TEST_RP)
                .withAssertions(singletonList(createAssertionString()))
                .build();
        configStub.setupCertificatesForEntity(responseFromHubDto.getAuthnRequestIssuerEntityId());
        configStub.signResponsesAndUseLegacyStandard(responseFromHubDto.getAuthnRequestIssuerEntityId());

        // When
        Response rpAuthnResponse = client.postTargetMain(
                Urls.SamlEngineUrls.GENERATE_RP_AUTHN_RESPONSE_RESOURCE,
                responseFromHubDto
        );

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
        ResponseFromHubDto responseFromHubDto = aResponseFromHubDto()
                .withAuthnRequestIssuerEntityId(TestEntityIds.TEST_RP_MS) // Using a different entity ID to avoid the internal cache
                .withAssertions(singletonList(createAssertionString()))
                .withStatus(TransactionIdaStatus.Success)
                .build();

        configStub.setupCertificatesForEntity(responseFromHubDto.getAuthnRequestIssuerEntityId());
        configStub.signResponsesAndUseSamlStandard(responseFromHubDto.getAuthnRequestIssuerEntityId());

        // When
        Response rpAuthnResponse = client.postTargetMain(
                Urls.SamlEngineUrls.GENERATE_RP_AUTHN_RESPONSE_RESOURCE,
                responseFromHubDto
        );

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
        ResponseFromHubDto responseFromHubDto = aResponseFromHubDto()
                .withAuthnRequestIssuerEntityId(TestEntityIds.HEADLESS_RP) // Using a different entity ID to avoid the internal cache
                .withAssertions(singletonList(createAssertionString()))
                .withStatus(TransactionIdaStatus.Success)
                .build();

        configStub.setupCertificatesForEntity(responseFromHubDto.getAuthnRequestIssuerEntityId());
        configStub.doNotSignResponseMessages(responseFromHubDto.getAuthnRequestIssuerEntityId());

        // When
        Response rpAuthnResponse = client.postTargetMain(
                Urls.SamlEngineUrls.GENERATE_RP_AUTHN_RESPONSE_RESOURCE,
                responseFromHubDto
        );

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
        return new XmlObjectToBase64EncodedStringTransformer<>().apply(AssertionBuilder.anAssertion().build());
    }

    private org.opensaml.saml.saml2.core.Response extractResponse(AuthnResponseFromHubContainerDto actualResult) throws org.opensaml.core.xml.io.UnmarshallingException, XMLParserException {
        return new SamlObjectParser().getSamlObject(new String(Base64Support.decode(actualResult.getSamlResponse())));
    }

    @Test
    public void shouldReturnAnErrorResponseGivenBadInput() throws JsonProcessingException {
        ResponseFromHubDto responseFromHubDto = aResponseFromHubDto().withAssertionConsumerServiceUri(null).build();
        configStub.signResponsesAndUseSamlStandard(responseFromHubDto.getAuthnRequestIssuerEntityId());

        Response rpAuthnResponse = client.postTargetMain(
                Urls.SamlEngineUrls.GENERATE_RP_AUTHN_RESPONSE_RESOURCE,
                responseFromHubDto
        );

        assertThat(rpAuthnResponse.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto errorStatusDto = rpAuthnResponse.readEntity(ErrorStatusDto.class);
        assertThat(errorStatusDto.getExceptionType()).isEqualTo(ExceptionType.INVALID_INPUT);
    }

    @Test
    public void shouldReturnAResponseWithNoAssertionsIfNoAssertionsProvided() throws Exception {
        // Given
        ResponseFromHubDto responseFromHubDto = aResponseFromHubDto()
            .withAuthnRequestIssuerEntityId(TestEntityIds.HEADLESS_RP_MS) // Using a different entity ID to avoid the internal cache
            .withAssertions(Collections.emptyList())
            .build();
        configStub.setupCertificatesForEntity(responseFromHubDto.getAuthnRequestIssuerEntityId());
        configStub.signResponsesAndUseSamlStandard(responseFromHubDto.getAuthnRequestIssuerEntityId());

        // When
        Response rpAuthnResponse = client.postTargetMain(
                Urls.SamlEngineUrls.GENERATE_RP_AUTHN_RESPONSE_RESOURCE,
                responseFromHubDto
        );

        // Then
        assertThat(rpAuthnResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        AuthnResponseFromHubContainerDto result = rpAuthnResponse.readEntity(AuthnResponseFromHubContainerDto
            .class);

        org.opensaml.saml.saml2.core.Response response = extractResponse(result);
        assertThat(response.getStatus().getStatusCode().getValue()).isEqualTo(StatusCode.SUCCESS);
        assertThat(response.getEncryptedAssertions()).isEmpty();
    }
}
