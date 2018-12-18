package uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableList;
import httpstub.HttpStubRule;
import org.apache.http.client.utils.URIBuilder;
import uk.gov.ida.hub.samlsoapproxy.Urls;
import uk.gov.ida.hub.samlsoapproxy.builders.CertificateDtoBuilder;
import uk.gov.ida.hub.samlsoapproxy.builders.MatchingServiceConfigEntityDataDtoBuilder;
import uk.gov.ida.hub.samlsoapproxy.contract.MatchingServiceConfigEntityDataDto;
import uk.gov.ida.hub.samlsoapproxy.domain.CertificateDto;
import uk.gov.ida.shared.utils.string.StringEncoding;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

import static javax.ws.rs.core.Response.Status.OK;

public class ConfigStubRule extends HttpStubRule {

    public void setupStubForMatchingServiceHealthChecks(URI matchingServiceUri) throws JsonProcessingException {
        Collection<MatchingServiceConfigEntityDataDto> matchingServiceConfigEntityDataDtos = ImmutableList.of(MatchingServiceConfigEntityDataDtoBuilder.aMatchingServiceConfigEntityDataDto().withUri(matchingServiceUri).build());
        register(Urls.ConfigUrls.ENABLED_MATCHING_SERVICES_RESOURCE, Response.Status.OK.getStatusCode(), matchingServiceConfigEntityDataDtos);
    }

    public void setupStubForCertificates(String issuer) throws JsonProcessingException {
        CertificateDto signingCertificate = new CertificateDtoBuilder().withIssuerId(issuer).withKeyUse(CertificateDto.KeyUse.Signing).build();
        CertificateDto encryptionCertificate = new CertificateDtoBuilder().withIssuerId(issuer).withKeyUse(CertificateDto.KeyUse.Encryption).build();
        registerStubForCertificates(issuer, signingCertificate, encryptionCertificate);
    }

    public void setupStubForCertificates(String issuer, String signingCertString, String encryptionCertString) throws JsonProcessingException {
        CertificateDto signingCertificate = new CertificateDtoBuilder().withIssuerId(issuer).withCertificate(signingCertString).withKeyUse(CertificateDto.KeyUse.Signing).build();
        CertificateDto encryptionCertificate = new CertificateDtoBuilder().withIssuerId(issuer).withCertificate(encryptionCertString).withKeyUse(CertificateDto.KeyUse.Encryption).build();
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

    public void setUpStubForMatchingServiceHealthCheckRequest(URI msaUri, String msaEntityId) throws JsonProcessingException {
        final MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto = new MatchingServiceConfigEntityDataDto(msaEntityId, msaUri, "rp-entity-id", true, false, false, null);
        Collection<MatchingServiceConfigEntityDataDto> matchingServices = ImmutableList.of(matchingServiceConfigEntityDataDto);
        String uri = UriBuilder.fromPath(Urls.ConfigUrls.ENABLED_MATCHING_SERVICES_RESOURCE).build().getPath();
        register(uri, OK.getStatusCode(), matchingServices);
    }

    public void setUpStubForMatchingServiceDetails(String msaEntityId) throws JsonProcessingException {
        final MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto = new MatchingServiceConfigEntityDataDto(msaEntityId, UriBuilder.fromUri(msaEntityId).build(), "rp-entity-id", true, false, false, null);
        Collection<MatchingServiceConfigEntityDataDto> matchingServices = ImmutableList.of(matchingServiceConfigEntityDataDto);
        String uri = UriBuilder.fromPath(Urls.ConfigUrls.MATCHING_SERVICE_ROOT).build().getPath();
        register(uri, OK.getStatusCode(), matchingServices);
    }

    public void setUpStubForForcedMatchingServiceHealthCheckRequest(URI msaUri, String msaEntityId) throws JsonProcessingException {
        final MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto = new MatchingServiceConfigEntityDataDto(msaEntityId, msaUri, "rp-entity-id", false, false, false, null);
        Collection<MatchingServiceConfigEntityDataDto> matchingServices = ImmutableList.of(matchingServiceConfigEntityDataDto);
        String uri = UriBuilder.fromPath(Urls.ConfigUrls.ENABLED_MATCHING_SERVICES_RESOURCE).build().getPath();
        register(uri, OK.getStatusCode(), matchingServices);
    }

    public void setUpStubForTwoMatchingServiceHealthCheckRequests(URI msaUri, String msaEntityId, URI msaUri2, String msaEntityId2) throws JsonProcessingException {
        final MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto = new MatchingServiceConfigEntityDataDto(msaEntityId, msaUri, "rp-entity-id", true, false, false, null);
        final MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto2 = new MatchingServiceConfigEntityDataDto(msaEntityId2, msaUri2, "rp-entity-id2", true, false, false,null);
        Collection<MatchingServiceConfigEntityDataDto> matchingServices = ImmutableList.of(matchingServiceConfigEntityDataDto, matchingServiceConfigEntityDataDto2);
        String uri = UriBuilder.fromPath(Urls.ConfigUrls.ENABLED_MATCHING_SERVICES_RESOURCE).build().getPath();
        register(uri, OK.getStatusCode(), matchingServices);
    }

}
