package uk.gov.ida.integrationtest.hub.samlengine.apprule.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import io.dropwizard.jackson.Jackson;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.hub.samlengine.builders.CertificateDtoBuilder;
import uk.gov.ida.hub.samlengine.domain.CertificateDto;
import uk.gov.ida.shared.utils.string.StringEncoding;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.ArrayList;
import java.util.Collection;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;

public class ConfigStubExtension implements BeforeAllCallback, AfterAllCallback {

    private ObjectMapper objectMapper = Jackson.newObjectMapper();
    private WireMockServer wireMockServer;

    public void setupCertificatesForEntity(String issuer) throws JsonProcessingException {
        CertificateDto signingCertificate = CertificateDtoBuilder.aCertificateDto().withIssuerId(issuer).withKeyUse(CertificateDto.KeyUse.Signing).build();
        CertificateDto encryptionCertificate = CertificateDtoBuilder.aCertificateDto().withIssuerId(issuer).withKeyUse(CertificateDto.KeyUse.Encryption).build();
        setupCertificatesForEntity(issuer, signingCertificate, encryptionCertificate);
    }

    public void setupCertificatesForEntity(String issuer, String signingCertString, String encryptionCertString) throws JsonProcessingException {
        CertificateDto signingCertificate = CertificateDtoBuilder.aCertificateDto().withIssuerId(issuer).withCertificate(signingCertString).withKeyUse(CertificateDto.KeyUse.Signing).build();
        CertificateDto encryptionCertificate = CertificateDtoBuilder.aCertificateDto().withIssuerId(issuer).withCertificate(encryptionCertString).withKeyUse(CertificateDto.KeyUse.Encryption).build();
        setupCertificatesForEntity(issuer, signingCertificate, encryptionCertificate);
    }

    private void setupCertificatesForEntity(String issuer, CertificateDto signingCertificate, CertificateDto encryptionCertificate) throws JsonProcessingException {
        Collection<CertificateDto> signingCertificates = new ArrayList<>();
        signingCertificates.add(signingCertificate);
        wireMockServer.stubFor(get(urlPathEqualTo(getPath(Urls.ConfigUrls.SIGNATURE_VERIFICATION_CERTIFICATES_RESOURCE, issuer)))
                .willReturn(
                        aResponse()
                        .withStatus(200)
                                .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(objectMapper.writeValueAsString(signingCertificates)
                        )
                )
        );

        String encryptionCertificateUri = getPath(Urls.ConfigUrls.ENCRYPTION_CERTIFICATES_RESOURCE, issuer);
        wireMockServer.stubFor(get(urlPathEqualTo(encryptionCertificateUri))
                .willReturn(
                        aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", APPLICATION_JSON)
                                .withBody(objectMapper.writeValueAsString(encryptionCertificate)
                                )
                )
        );
    }

    private String getPath(String encryptionCertificatesResource, String param) {
        return UriBuilder
                .fromPath(encryptionCertificatesResource)
                .buildFromEncoded( StringEncoding.urlEncode(param).replace("+", "%20"))
                .toString();
    }

    public void signResponsesAndUseLegacyStandard(String issuerEntityId) throws JsonProcessingException {
        shouldHubSignResponseMessages(issuerEntityId, true, true);
    }

    public void signResponsesAndUseSamlStandard(String issuerEntityId) throws JsonProcessingException {
        shouldHubSignResponseMessages(issuerEntityId, true, false);
    }

    public void doNotSignResponseMessages(String issuerEntityId) throws JsonProcessingException {
        shouldHubSignResponseMessages(issuerEntityId, false, false);
    }

    private void shouldHubSignResponseMessages(String issuerEntityId, Boolean shouldHubSignResponseMessages, Boolean shouldHubUseLegacySamlStandard) throws JsonProcessingException {
        String hubSignUri = getPath(Urls.ConfigUrls.SHOULD_HUB_SIGN_RESPONSE_MESSAGES_RESOURCE, issuerEntityId);
        wireMockServer.stubFor(get(hubSignUri)
                .willReturn(aResponse()
                        .withStatus(OK.getStatusCode())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(objectMapper.writeValueAsString(shouldHubSignResponseMessages))
                )
        );

        String hubSamlStandardUri = getPath(Urls.ConfigUrls.SHOULD_HUB_USE_LEGACY_SAML_STANDARD_RESOURCE, issuerEntityId);
        wireMockServer.stubFor(get(hubSamlStandardUri)
                .willReturn(aResponse()
                        .withStatus(OK.getStatusCode())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(objectMapper.writeValueAsString(shouldHubUseLegacySamlStandard))
                )
        );
    }

    public void setupStubForNonExistentSigningCertificates(String issuer) {
        String signingUri = getPath(Urls.ConfigUrls.SIGNATURE_VERIFICATION_CERTIFICATES_RESOURCE, issuer);
        wireMockServer.stubFor(get(signingUri).willReturn(aResponse().withStatus(Response.Status.NOT_FOUND.getStatusCode())));
    }

    public UriBuilder baseUri() {
        return UriBuilder.fromUri("http://localhost").port(wireMockServer.port());
    }

    public void reset() {
        wireMockServer.resetAll();
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        wireMockServer.stop();
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();
    }
}
