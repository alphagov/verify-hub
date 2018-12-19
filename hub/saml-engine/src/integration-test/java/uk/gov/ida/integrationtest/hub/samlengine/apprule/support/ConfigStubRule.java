package uk.gov.ida.integrationtest.hub.samlengine.apprule.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.google.common.collect.ImmutableList;
import httpstub.HttpStubRule;
import io.dropwizard.jackson.Jackson;
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.hub.samlengine.builders.CertificateDtoBuilder;
import uk.gov.ida.hub.samlengine.domain.CertificateDto;
import uk.gov.ida.hub.samlengine.domain.MatchingServiceConfigEntityDataDto;
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

        String hubSignWithSHA1StandardUri = getPath(Urls.ConfigUrls.SHOULD_SIGN_WITH_SHA1_RESOURCE, issuerEntityId);
        stubFor(get(hubSignWithSHA1StandardUri)
                .willReturn(aResponse()
                        .withStatus(OK.getStatusCode())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(objectMapper.writeValueAsString(true))
                )
        );
    }

    public void setupStubForNonExistentSigningCertificates(String issuer) {
        String signingUri = getPath(Urls.ConfigUrls.SIGNATURE_VERIFICATION_CERTIFICATES_RESOURCE, issuer);
        stubFor(get(signingUri).willReturn(aResponse().withStatus(Response.Status.NOT_FOUND.getStatusCode())));
    }

    public void setUpStubForMatchingServiceDetails(String msaEntityId) throws JsonProcessingException {
        final MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto = new MatchingServiceConfigEntityDataDto(msaEntityId, UriBuilder.fromUri("http://uri.local").build(), "rp-entity-id", true, false, false, null);
        Collection<MatchingServiceConfigEntityDataDto> matchingServices = ImmutableList.of(matchingServiceConfigEntityDataDto);
        stubFor(get(UriBuilder.fromPath(Urls.ConfigUrls.MATCHING_SERVICE_ROOT).build().toString()).willReturn(aResponse().withStatus(OK.getStatusCode()).withBody(objectMapper.writeValueAsString(matchingServices)).withHeader("Content-Type", "application/json")));
    }

    public void setUpStubForRPMetadataEnabled(String entityId) throws JsonProcessingException {
        stubFor(get(UriBuilder.fromPath(Urls.ConfigUrls.METADATA_LOCATION_RESOURCE).build(entityId).toString()).willReturn(aResponse().withStatus(OK.getStatusCode()).withBody("false").withHeader("Content-Type", "application/json")));
    }

    public UriBuilder baseUri() {
        return UriBuilder.fromUri("http://localhost").port(port());
    }

    public void reset() {
        super.resetAll();
    }
}
