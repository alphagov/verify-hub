package uk.gov.ida.integrationtest.hub.samlengine.apprule.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import httpstub.HttpStubRule;
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.hub.samlengine.builders.CertificateDtoBuilder;
import uk.gov.ida.hub.samlengine.domain.CertificateDto;
import uk.gov.ida.shared.utils.string.StringEncoding;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

import static javax.ws.rs.core.Response.Status.OK;

public class ConfigStubRule extends HttpStubRule {

    public void setupStubForCertificates(String issuer) throws JsonProcessingException {
        CertificateDto signingCertificate = CertificateDtoBuilder.aCertificateDto().withIssuerId(issuer).withKeyUse(CertificateDto.KeyUse.Signing).build();
        CertificateDto encryptionCertificate = CertificateDtoBuilder.aCertificateDto().withIssuerId(issuer).withKeyUse(CertificateDto.KeyUse.Encryption).build();
        registerStubForCertificates(issuer, signingCertificate, encryptionCertificate);
    }

    public void setupStubForCertificates(String issuer, String signingCertString, String encryptionCertString) throws JsonProcessingException {
        CertificateDto signingCertificate = CertificateDtoBuilder.aCertificateDto().withIssuerId(issuer).withCertificate(signingCertString).withKeyUse(CertificateDto.KeyUse.Signing).build();
        CertificateDto encryptionCertificate = CertificateDtoBuilder.aCertificateDto().withIssuerId(issuer).withCertificate(encryptionCertString).withKeyUse(CertificateDto.KeyUse.Encryption).build();
        registerStubForCertificates(issuer, signingCertificate, encryptionCertificate);
    }

    private void registerStubForCertificates(String issuer, CertificateDto signingCertificate, CertificateDto encryptionCertificate) throws JsonProcessingException {
        String signingCertificateUri = UriBuilder.fromPath(Urls.ConfigUrls.SIGNATURE_VERIFICATION_CERTIFICATES_RESOURCE).buildFromEncoded(StringEncoding.urlEncode(issuer)).toASCIIString();
        Collection<CertificateDto> signingCertificates = new ArrayList<>();
        signingCertificates.add(signingCertificate);
        register(signingCertificateUri, OK.getStatusCode(), signingCertificates);

        String encryptionCertificateUri = UriBuilder.fromPath(Urls.ConfigUrls.ENCRYPTION_CERTIFICATES_RESOURCE).buildFromEncoded(StringEncoding.urlEncode(issuer)).toASCIIString();
        register(encryptionCertificateUri, OK.getStatusCode(), encryptionCertificate);
    }

    public void setUpStubForShouldHubSignResponseMessagesForLegacySamlStandard(String issuerEntityId) throws JsonProcessingException {
        setUpStubForShouldHubSignResponseMessages(issuerEntityId, true, true);
    }

    public void setUpStubForShouldHubSignResponseMessagesForSamlStandard(String issuerEntityId) throws JsonProcessingException {
        setUpStubForShouldHubSignResponseMessages(issuerEntityId, true, false);
    }

    private void setUpStubForShouldHubSignResponseMessages(String issuerEntityId, Boolean shouldHubSignResponseMessages, Boolean shouldHubUseLegacySamlStandard) throws JsonProcessingException {
        URI hubSignUri = UriBuilder.fromPath(Urls.ConfigUrls.SHOULD_HUB_SIGN_RESPONSE_MESSAGES_RESOURCE)
                .buildFromEncoded(StringEncoding.urlEncode(issuerEntityId).replace("+", "%20"));
        register(hubSignUri.getRawPath(), OK.getStatusCode(), shouldHubSignResponseMessages);

        URI hubSamlStandardUri = UriBuilder.fromPath(Urls.ConfigUrls.SHOULD_HUB_USE_LEGACY_SAML_STANDARD_RESOURCE)
                .buildFromEncoded(StringEncoding.urlEncode(issuerEntityId).replace("+", "%20"));
        register(hubSamlStandardUri.getRawPath(), OK.getStatusCode(), shouldHubUseLegacySamlStandard);
    }

    public void setupStubForNonExistentSigningCertificates(String issuer) {
        String encryptionCertificateUri = UriBuilder.fromPath(Urls.ConfigUrls.SIGNATURE_VERIFICATION_CERTIFICATES_RESOURCE).buildFromEncoded(StringEncoding.urlEncode(issuer)).toASCIIString();
        register(encryptionCertificateUri, Response.Status.NOT_FOUND.getStatusCode());
    }

}
