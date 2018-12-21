package uk.gov.ida.integrationtest.hub.samlproxy.apprule;

import helpers.JerseyClientConfigurationBuilder;
import httpstub.HttpStubRule;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.util.Duration;
import org.apache.xml.security.exceptions.Base64DecodingException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.opensaml.xmlsec.algorithm.DigestAlgorithm;
import org.opensaml.xmlsec.algorithm.SignatureAlgorithm;
import org.opensaml.xmlsec.algorithm.descriptors.DigestSHA256;
import org.opensaml.xmlsec.algorithm.descriptors.SignatureRSASHA1;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.common.shared.security.PrivateKeyFactory;
import uk.gov.ida.common.shared.security.PublicKeyFactory;
import uk.gov.ida.common.shared.security.X509CertificateFactory;
import uk.gov.ida.hub.samlproxy.Urls;
import uk.gov.ida.hub.samlproxy.contracts.AuthnResponseFromHubContainerDto;
import uk.gov.ida.hub.samlproxy.controllogic.SamlMessageSenderHandler;
import uk.gov.ida.hub.samlproxy.domain.AuthnRequestFromHubContainerDto;
import uk.gov.ida.hub.samlproxy.domain.LevelOfAssurance;
import uk.gov.ida.integrationtest.hub.samlproxy.apprule.support.PolicyStubRule;
import uk.gov.ida.integrationtest.hub.samlproxy.apprule.support.SamlProxyAppRule;
import uk.gov.ida.saml.core.domain.OutboundResponseFromHub;
import uk.gov.ida.saml.core.domain.TransactionIdaStatus;
import uk.gov.ida.saml.core.test.HardCodedKeyStore;
import uk.gov.ida.saml.core.transformers.outbound.decorators.ResponseAssertionSigner;
import uk.gov.ida.saml.hub.api.HubTransformersFactory;
import uk.gov.ida.saml.hub.domain.IdaAuthnRequestFromHub;
import uk.gov.ida.saml.security.IdaKeyStore;
import uk.gov.ida.saml.security.IdaKeyStoreCredentialRetriever;
import uk.gov.ida.saml.security.SignatureFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.HUB_TEST_PRIVATE_ENCRYPTION_KEY;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.HUB_TEST_PRIVATE_SIGNING_KEY;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.HUB_TEST_PUBLIC_ENCRYPTION_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.HUB_TEST_PUBLIC_SIGNING_CERT;
import static uk.gov.ida.saml.core.test.TestEntityIds.HUB_ENTITY_ID;
import static uk.gov.ida.saml.core.test.builders.ResponseForHubBuilder.anAuthnResponse;
import static uk.gov.ida.saml.hub.test.builders.IdaAuthnRequestBuilder.anIdaAuthnRequest;

public class SamlMessageSenderApiResourceTest {

    private static final SignatureAlgorithm SIGNATURE_ALGORITHM = new SignatureRSASHA1();
    private static final DigestAlgorithm DIGEST_ALGORITHM = new DigestSHA256();

    private static Client client;

    @ClassRule
    public static PolicyStubRule policyStubRule = new PolicyStubRule();

    @ClassRule
    public static HttpStubRule eventSinkStubRule = new HttpStubRule();

    @ClassRule
    public static SamlProxyAppRule samlProxyAppRule = new SamlProxyAppRule(
            ConfigOverride.config("policyUri", policyStubRule.baseUri().build().toASCIIString()),
            ConfigOverride.config("eventSinkUri", eventSinkStubRule.baseUri().build().toASCIIString()));


    @Before
    public void setUp() throws Exception {
        eventSinkStubRule.register(Urls.HubSupportUrls.HUB_SUPPORT_EVENT_SINK_RESOURCE, Response.Status.OK.getStatusCode());
    }

    @BeforeClass
    public static void setUpClient() throws Exception {
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder.aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        client =  new JerseyClientBuilder(samlProxyAppRule.getEnvironment()).using(jerseyClientConfiguration).build
                (SamlMessageSenderApiResourceTest.class.getSimpleName());
    }

    @After
    public void resetStubRules() {
        policyStubRule.reset();
        eventSinkStubRule.reset();
    }

    @Test
    public void sendJsonAuthnRequestFromHub_shouldRespondWithNextLocation() throws Exception {
        SessionId sessionId = SessionId.createNewSessionId();

        URI nextLocationUri = URI.create("http://blah");
        IdaAuthnRequestFromHub authnRequestFromHub = anIdaAuthnRequest()
                .withIssuer(HUB_ENTITY_ID)
                .buildFromHub();
        Function<IdaAuthnRequestFromHub, String> idaAuthnRequestFromHubToStringTransformer =
                new HubTransformersFactory().getIdaAuthnRequestFromHubToStringTransformer(
                        getKeyStore(),
                        SIGNATURE_ALGORITHM,
                        DIGEST_ALGORITHM);
        String samlString = idaAuthnRequestFromHubToStringTransformer.apply(authnRequestFromHub);
        policyStubRule.aValidAuthnRequestFromHubToIdp(sessionId, new AuthnRequestFromHubContainerDto(samlString, nextLocationUri, false));
        Response response = getResponseFromSamlProxy(Urls.SamlProxyUrls.SEND_AUTHN_REQUEST_API_RESOURCE, sessionId);
        assertThat(response.readEntity(SamlMessageSenderHandler.SamlMessage.class).getPostEndpoint()).isEqualTo(nextLocationUri.toASCIIString());
    }

    @Test
    public void getSendJsonAuthnRequestFromHub_shouldErrorWhenAValidationFailureOccurs() throws Exception {
        SessionId sessionId = SessionId.createNewSessionId();
        policyStubRule.receiveAuthnResponseFromIdp(sessionId.toString(), LevelOfAssurance.LEVEL_2);
        policyStubRule.receiveAuthnResponseFromIdpError(sessionId.toString());

        Response response = getResponseFromSamlProxy(Urls.SamlProxyUrls.SEND_AUTHN_REQUEST_API_RESOURCE, sessionId);

        assertThat(response.getStatus()).isEqualTo(500);
    }

    @Test
    public void sendUnsignedJsonAuthnResponseFromHub_shouldRespondWithNextLocation() throws Exception {
        SessionId sessionId = SessionId.createNewSessionId();
        URI nextLocationUri = URI.create("http://blah");
        String requestId = UUID.randomUUID().toString();

        Function<OutboundResponseFromHub, String> outboundResponseFromHubToStringTransformer =
                new HubTransformersFactory().getOutboundResponseFromHubToStringTransformer(
                        new HardCodedKeyStore(HUB_ENTITY_ID),
                        getKeyStore(),
                        new IdpHardCodedEntityToEncryptForLocator(),
                        SIGNATURE_ALGORITHM,
                        DIGEST_ALGORITHM);
        OutboundResponseFromHub authnResponseFromHub = anAuthnResponse()
                .withInResponseTo(requestId)
                .withIssuerId(HUB_ENTITY_ID)
                .withTransactionIdaStatus(TransactionIdaStatus.Success)
                .buildOutboundResponseFromHub();
        String samlString = outboundResponseFromHubToStringTransformer.apply(authnResponseFromHub);

        AuthnResponseFromHubContainerDto authnResponseFromHubContainerDto = new AuthnResponseFromHubContainerDto(
                samlString,
                nextLocationUri,
                java.util.Optional.empty(),
                authnResponseFromHub.getId());

        policyStubRule.anAuthnResponseFromHubToRp(sessionId, authnResponseFromHubContainerDto);

        javax.ws.rs.core.Response response = getResponseFromSamlProxy(Urls.SamlProxyUrls.SEND_RESPONSE_FROM_HUB_API_RESOURCE, sessionId);
        assertThat(response.readEntity(SamlMessageSenderHandler.SamlMessage.class).getPostEndpoint()).isEqualTo(nextLocationUri.toASCIIString());
    }

    @Test
    public void sendSignedJsonAuthnResponseFromHub_shouldRespondWithNextLocation() throws Exception {
        SessionId sessionId = SessionId.createNewSessionId();
        URI nextLocationUri = URI.create("http://blah");
        String requestId = UUID.randomUUID().toString();


        ResponseAssertionSigner responseAssertionSigner = new ResponseAssertionSigner(
                new SignatureFactory(new IdaKeyStoreCredentialRetriever(getKeyStore()), SIGNATURE_ALGORITHM, DIGEST_ALGORITHM)
        );
        Function<OutboundResponseFromHub, String> outboundResponseFromHubToStringTransformer = new HubTransformersFactory()
                .getOutboundResponseFromHubToStringTransformer(
                        new HardCodedKeyStore(HUB_ENTITY_ID),
                        getKeyStore(),
                        new IdpHardCodedEntityToEncryptForLocator(),
                        responseAssertionSigner,
                        SIGNATURE_ALGORITHM,
                        DIGEST_ALGORITHM
                );
        OutboundResponseFromHub authnResponseFromHub = anAuthnResponse()
                .withInResponseTo(requestId)
                .withIssuerId(HUB_ENTITY_ID)
                .withTransactionIdaStatus(TransactionIdaStatus.Success)
                .buildOutboundResponseFromHub();
        String samlString = outboundResponseFromHubToStringTransformer.apply(authnResponseFromHub);

        AuthnResponseFromHubContainerDto authnResponseFromHubContainerDto = new AuthnResponseFromHubContainerDto(
                samlString,
                nextLocationUri,
                java.util.Optional.empty(),
                authnResponseFromHub.getId());

        policyStubRule.anAuthnResponseFromHubToRp(sessionId, authnResponseFromHubContainerDto);

        javax.ws.rs.core.Response response = getResponseFromSamlProxy(Urls.SamlProxyUrls.SEND_RESPONSE_FROM_HUB_API_RESOURCE, sessionId);
        assertThat(response.readEntity(SamlMessageSenderHandler.SamlMessage.class).getPostEndpoint()).isEqualTo(nextLocationUri.toASCIIString());
    }

    @Test
    public void sendJsonAuthnResponseFromHub_shouldErrorWhenAValidationFailureOccurs() throws Exception {
        String responseId = "my-request";
        SessionId sessionId = SessionId.createNewSessionId();
        URI nextLocationUri = URI.create("http://blah");

        OutboundResponseFromHub authnResponseFromHub = anAuthnResponse()
                .withInResponseTo(responseId)
                .withIssuerId(HUB_ENTITY_ID)
                .withTransactionIdaStatus(TransactionIdaStatus.Success)
                .buildOutboundResponseFromHub();

        AuthnResponseFromHubContainerDto invalidAuthnResponseFromHubContainerDto = new AuthnResponseFromHubContainerDto(
                "something not valid",
                nextLocationUri,
                java.util.Optional.empty(),
                authnResponseFromHub.getId());

        policyStubRule.anAuthnResponseFromHubToRp(sessionId, invalidAuthnResponseFromHubContainerDto);
        javax.ws.rs.core.Response response = getResponseFromSamlProxy(Urls.SamlProxyUrls.SEND_RESPONSE_FROM_HUB_API_RESOURCE, sessionId);

        assertThat(response.getStatus()).isEqualTo(500);
    }

    @Test
    public void sendJsonErrorResponseFromHub_shouldRespondWithNextLocation() throws Exception {
        URI uri = URI.create("http://blah");
        String requestId = UUID.randomUUID().toString();
        final SessionId sessionId = SessionId.createNewSessionId();
        OutboundResponseFromHub authnResponseFromHub = anAuthnResponse()
                .withInResponseTo(requestId)
                .withIssuerId(HUB_ENTITY_ID)
                .withTransactionIdaStatus(TransactionIdaStatus.RequesterError)
                .buildOutboundResponseFromHub();
        Function<OutboundResponseFromHub, String> outboundResponseFromHubToStringTransformer = new HubTransformersFactory()
                .getOutboundResponseFromHubToStringTransformer(
                        new HardCodedKeyStore(HUB_ENTITY_ID),
                        getKeyStore(),
                        new IdpHardCodedEntityToEncryptForLocator(),
                        SIGNATURE_ALGORITHM,
                        DIGEST_ALGORITHM
                );

        String samlString = outboundResponseFromHubToStringTransformer.apply(authnResponseFromHub);

        AuthnResponseFromHubContainerDto authnResponseFromHubContainerDto = new AuthnResponseFromHubContainerDto(
                samlString,
                uri,
                java.util.Optional.empty(),
                authnResponseFromHub.getId());
        policyStubRule.anErrorResponseFromHubToRp(sessionId, authnResponseFromHubContainerDto);

        javax.ws.rs.core.Response response = getResponseFromSamlProxy(Urls.SamlProxyUrls.SEND_ERROR_RESPONSE_FROM_HUB_API_RESOURCE, sessionId);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.readEntity(SamlMessageSenderHandler.SamlMessage.class).getPostEndpoint()).isEqualTo(uri
                .toASCIIString());
    }

    @Test
    public void sendJsonErrorResponseFromHub_shouldErrorWhenAValidationFailureOccurs() throws Exception {
        URI uri = URI.create("http://blah");
        String requestId = UUID.randomUUID().toString();
        final SessionId sessionId = SessionId.createNewSessionId();
        OutboundResponseFromHub authnResponseFromHub = anAuthnResponse()
                .withInResponseTo(requestId)
                .withIssuerId(HUB_ENTITY_ID)
                .withTransactionIdaStatus(TransactionIdaStatus.RequesterError)
                .buildOutboundResponseFromHub();

        AuthnResponseFromHubContainerDto authnResponseFromHubContainerDto = new AuthnResponseFromHubContainerDto(
                "invalid saml",
                uri,
                java.util.Optional.empty(),
                authnResponseFromHub.getId());
        policyStubRule.anErrorResponseFromHubToRp(sessionId, authnResponseFromHubContainerDto);

        javax.ws.rs.core.Response response = getResponseFromSamlProxy(Urls.SamlProxyUrls.SEND_ERROR_RESPONSE_FROM_HUB_API_RESOURCE, sessionId);

        assertThat(response.getStatus()).isEqualTo(500);
    }

    private Response getResponseFromSamlProxy(String url, SessionId sessionId) {
        return client.target(samlProxyAppRule.getUri(url)).queryParam(Urls.SharedUrls.SESSION_ID_PARAM, sessionId.toString()).request().get();
    }

    private static IdaKeyStore getKeyStore() throws Base64DecodingException {
        List<KeyPair> encryptionKeyPairs = new ArrayList<>();
        PublicKeyFactory publicKeyFactory = new PublicKeyFactory(new X509CertificateFactory());
        PrivateKeyFactory privateKeyFactory = new PrivateKeyFactory();
        PublicKey encryptionPublicKey = publicKeyFactory.createPublicKey(HUB_TEST_PUBLIC_ENCRYPTION_CERT);
        PrivateKey encryptionPrivateKey = privateKeyFactory.createPrivateKey(Base64.getDecoder().decode(HUB_TEST_PRIVATE_ENCRYPTION_KEY.getBytes()));
        encryptionKeyPairs.add(new KeyPair(encryptionPublicKey, encryptionPrivateKey));
        PublicKey publicSigningKey = publicKeyFactory.createPublicKey(HUB_TEST_PUBLIC_SIGNING_CERT);
        PrivateKey privateSigningKey = privateKeyFactory.createPrivateKey(Base64.getDecoder().decode(HUB_TEST_PRIVATE_SIGNING_KEY.getBytes()));
        KeyPair signingKeyPair = new KeyPair(publicSigningKey, privateSigningKey);

        return new IdaKeyStore(signingKeyPair, encryptionKeyPairs);
    }

}
