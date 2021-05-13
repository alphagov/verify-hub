package uk.gov.ida.integrationtest.hub.samlproxy.apprule.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import httpstub.HttpStubRule;
import uk.gov.ida.hub.samlproxy.Urls;
import uk.gov.ida.hub.samlproxy.builders.CertificateDtoBuilder;
import uk.gov.ida.hub.samlproxy.domain.CertificateDto;
import uk.gov.ida.shared.utils.string.StringEncoding;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.ArrayList;
import java.util.Collection;

public class ConfigStubRule extends HttpStubRule {

    public void setupStubForCertificates(String issuer) throws JsonProcessingException {
        String signingCertificateUri = UriBuilder.fromPath(Urls.ConfigUrls.SIGNATURE_VERIFICATION_CERTIFICATES_RESOURCE).buildFromEncoded(StringEncoding.urlEncode(issuer)).toASCIIString();
        CertificateDto signingCertificate = CertificateDtoBuilder.aCertificateDto().withIssuerId(issuer).withKeyUse(CertificateDto.KeyUse.Signing).build();
        Collection<CertificateDto> signingCertificates = new ArrayList<>();
        signingCertificates.add(signingCertificate);
        register(signingCertificateUri, Response.Status.OK.getStatusCode(), signingCertificates);

        String encryptionCertificateUri = UriBuilder.fromPath(Urls.ConfigUrls.ENCRYPTION_CERTIFICATES_RESOURCE).buildFromEncoded(StringEncoding.urlEncode(issuer)).toASCIIString();
        CertificateDto encryptionCertificate = CertificateDtoBuilder.aCertificateDto().withIssuerId(issuer).withKeyUse(CertificateDto.KeyUse.Encryption).build();
        register(encryptionCertificateUri, Response.Status.OK.getStatusCode(), encryptionCertificate);
    }

    public void setupStubForNonExistentSigningCertificates(String issuer) {
        String encryptionCertificateUri = UriBuilder.fromPath(Urls.ConfigUrls.SIGNATURE_VERIFICATION_CERTIFICATES_RESOURCE).buildFromEncoded(StringEncoding.urlEncode(issuer)).toASCIIString();
        register(encryptionCertificateUri, Response.Status.NOT_FOUND.getStatusCode());
    }
}
