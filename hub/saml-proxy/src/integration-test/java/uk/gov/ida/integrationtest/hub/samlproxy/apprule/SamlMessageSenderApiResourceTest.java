package uk.gov.ida.integrationtest.hub.samlproxy.apprule;

import httpstub.HttpStubExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
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
import uk.gov.ida.integrationtest.hub.samlproxy.apprule.support.PolicyStubExtension;
import uk.gov.ida.integrationtest.hub.samlproxy.apprule.support.SamlProxyAppExtension;
import uk.gov.ida.integrationtest.hub.samlproxy.apprule.support.SamlProxyAppExtension.SamlProxyClient;
import uk.gov.ida.saml.core.domain.OutboundResponseFromHub;
import uk.gov.ida.saml.core.domain.TransactionIdaStatus;
import uk.gov.ida.saml.core.test.HardCodedKeyStore;
import uk.gov.ida.saml.core.transformers.outbound.decorators.ResponseAssertionSigner;
import uk.gov.ida.saml.hub.api.HubTransformersFactory;
import uk.gov.ida.saml.hub.domain.IdaAuthnRequestFromHub;
import uk.gov.ida.saml.security.IdaKeyStore;
import uk.gov.ida.saml.security.IdaKeyStoreCredentialRetriever;
import uk.gov.ida.saml.security.SignatureFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static io.dropwizard.testing.ConfigOverride.config;
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

    @Order(0)
    @RegisterExtension
    public static final PolicyStubExtension policyStub = new PolicyStubExtension();

    @Order(0)
    @RegisterExtension
    public static HttpStubExtension eventSinkStub = new HttpStubExtension();

    @Order(1)
    @RegisterExtension
    public static final SamlProxyAppExtension samlProxyApp = SamlProxyAppExtension.builder()
            .withConfigOverrides(
                    config("policyUri", () -> policyStub.baseUri().build().toASCIIString()),
                    config("eventSinkUri", () -> eventSinkStub.baseUri().build().toASCIIString())
            )
            .build();

    private SamlProxyClient client;

    @BeforeAll
    public static void beforeClass() {
        eventSinkStub.register(Urls.HubSupportUrls.HUB_SUPPORT_EVENT_SINK_RESOURCE, Response.Status.OK.getStatusCode());
    }

    @BeforeEach
    public void beforeEach() {
        client = samlProxyApp.getClient();
    }

    @AfterEach
    public void resetStubRules() {
        policyStub.reset();
        eventSinkStub.reset();
    }

    @AfterAll
    public static void tearDown() {
        samlProxyApp.tearDown();
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
        policyStub.aValidAuthnRequestFromHubToIdp(sessionId, new AuthnRequestFromHubContainerDto(samlString, nextLocationUri, false));
        Response response = getResponseFromSamlProxy(Urls.SamlProxyUrls.SEND_AUTHN_REQUEST_API_RESOURCE, sessionId);
        assertThat(response.readEntity(SamlMessageSenderHandler.SamlMessage.class).getPostEndpoint()).isEqualTo(nextLocationUri.toASCIIString());
    }

    @Test
    public void getSendJsonAuthnRequestFromHub_shouldErrorWhenAValidationFailureOccurs() throws Exception {
        SessionId sessionId = SessionId.createNewSessionId();
        policyStub.receiveAuthnResponseFromIdp(sessionId.toString(), LevelOfAssurance.LEVEL_2);
        policyStub.receiveAuthnResponseFromIdpError(sessionId.toString());

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

        policyStub.anAuthnResponseFromHubToRp(sessionId, authnResponseFromHubContainerDto);

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

        policyStub.anAuthnResponseFromHubToRp(sessionId, authnResponseFromHubContainerDto);

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

        policyStub.anAuthnResponseFromHubToRp(sessionId, invalidAuthnResponseFromHubContainerDto);
        javax.ws.rs.core.Response response = getResponseFromSamlProxy(Urls.SamlProxyUrls.SEND_RESPONSE_FROM_HUB_API_RESOURCE, sessionId);

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
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
        policyStub.anErrorResponseFromHubToRp(sessionId, authnResponseFromHubContainerDto);

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
        policyStub.anErrorResponseFromHubToRp(sessionId, authnResponseFromHubContainerDto);

        javax.ws.rs.core.Response response = getResponseFromSamlProxy(Urls.SamlProxyUrls.SEND_ERROR_RESPONSE_FROM_HUB_API_RESOURCE, sessionId);

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    private Response getResponseFromSamlProxy(String url, SessionId sessionId) {
        return client.getTargetMain(UriBuilder.fromPath(url).queryParam(Urls.SharedUrls.SESSION_ID_PARAM, sessionId.toString()).build());
    }

    private static IdaKeyStore getKeyStore() {
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
