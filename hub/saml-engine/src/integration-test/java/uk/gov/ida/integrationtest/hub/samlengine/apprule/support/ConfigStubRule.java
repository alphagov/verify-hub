package uk.gov.ida.integrationtest.hub.samlengine.apprule.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import io.dropwizard.jackson.Jackson;
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

public class ConfigStubRule extends WireMockClassRule {

    private ObjectMapper objectMapper;

    public ConfigStubRule() {
        super(0);
        this.objectMapper = Jackson.newObjectMapper();
        this.start();
    }

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
        stubFor(get(urlPathEqualTo(getPath(Urls.ConfigUrls.SIGNATURE_VERIFICATION_CERTIFICATES_RESOURCE, issuer)))
                .willReturn(
                        aResponse()
                        .withStatus(200)
                                .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(objectMapper.writeValueAsString(signingCertificates)
                        )
                )
        );

        String encryptionCertificateUri = getPath(Urls.ConfigUrls.ENCRYPTION_CERTIFICATES_RESOURCE, issuer);
        stubFor(get(urlPathEqualTo(encryptionCertificateUri))
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

    public void setupIssuerIsEidasProxyNode(String issuer, boolean response) {
        String hubSignUri = getPath(Urls.ConfigUrls.IS_VERIFY_PROXY_NODE_RESOURCE, issuer);
        stubFor(get(hubSignUri)
                .willReturn(aResponse()
                        .withStatus(OK.getStatusCode())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(String.valueOf(response))
                )
        );
    }

    private void shouldHubSignResponseMessages(String issuerEntityId, Boolean shouldHubSignResponseMessages, Boolean shouldHubUseLegacySamlStandard) throws JsonProcessingException {
        String hubSignUri = getPath(Urls.ConfigUrls.SHOULD_HUB_SIGN_RESPONSE_MESSAGES_RESOURCE, issuerEntityId);
        stubFor(get(hubSignUri)
                .willReturn(aResponse()
                        .withStatus(OK.getStatusCode())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(objectMapper.writeValueAsString(shouldHubSignResponseMessages))
                )
        );

        String hubSamlStandardUri = getPath(Urls.ConfigUrls.SHOULD_HUB_USE_LEGACY_SAML_STANDARD_RESOURCE, issuerEntityId);
        stubFor(get(hubSamlStandardUri)
                .willReturn(aResponse()
                        .withStatus(OK.getStatusCode())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(objectMapper.writeValueAsString(shouldHubUseLegacySamlStandard))
                )
        );
    }

    public void setupStubForNonExistentSigningCertificates(String issuer) {
        String signingUri = getPath(Urls.ConfigUrls.SIGNATURE_VERIFICATION_CERTIFICATES_RESOURCE, issuer);
        stubFor(get(signingUri).willReturn(aResponse().withStatus(Response.Status.NOT_FOUND.getStatusCode())));
    }

    public UriBuilder baseUri() {
        return UriBuilder.fromUri("http://localhost").port(port());
    }

    public void reset() {
        super.resetAll();
    }
}
