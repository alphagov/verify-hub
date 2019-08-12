package uk.gov.ida.hub.config.resources;

import com.codahale.metrics.annotation.Timed;
import uk.gov.ida.hub.config.ConfigConfiguration;
import uk.gov.ida.hub.config.Urls;
import uk.gov.ida.hub.config.application.CertificateService;
import uk.gov.ida.hub.config.domain.Certificate;
import uk.gov.ida.hub.config.domain.CertificateConfigurable;
import uk.gov.ida.hub.config.domain.OCSPCertificateChainValidityChecker;
import uk.gov.ida.hub.config.dto.CertificateDto;
import uk.gov.ida.hub.config.dto.CertificateHealthCheckDto;
import uk.gov.ida.hub.config.dto.InvalidCertificateDto;
import uk.gov.ida.hub.config.exceptions.CertificateDisabledException;
import uk.gov.ida.hub.config.exceptions.ExceptionFactory;
import uk.gov.ida.hub.config.exceptions.NoCertificateFoundException;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static uk.gov.ida.hub.config.dto.CertificateDto.aCertificateDto;

@Path(Urls.ConfigUrls.CERTIFICATES_ROOT)
@Produces(MediaType.APPLICATION_JSON)
public class CertificatesResource {
    private final ExceptionFactory exceptionFactory;
    private final ConfigConfiguration configuration;
    private final OCSPCertificateChainValidityChecker ocspCertificateChainValidityChecker;
    private final CertificateService<? extends CertificateConfigurable<?>> certificateService;


    @Inject
    public CertificatesResource(
            ExceptionFactory exceptionFactory,
            ConfigConfiguration configuration,
            OCSPCertificateChainValidityChecker ocspCertificateChainValidityChecker,
            CertificateService<? extends CertificateConfigurable<?>> certificateService
    ) {
        this.exceptionFactory = exceptionFactory;
        this.configuration = configuration;
        this.ocspCertificateChainValidityChecker = ocspCertificateChainValidityChecker;
        this.certificateService = certificateService;
    }

    @GET
    @Path(Urls.ConfigUrls.ENCRYPTION_CERTIFICATE_PATH)
    @Timed
    public CertificateDto getEncryptionCertificate(@PathParam(Urls.SharedUrls.ENTITY_ID_PARAM) String entityId) {
        try {
            Certificate certificate = certificateService.encryptionCertificateFor(entityId);
            Optional<String> base64Encoded = certificate.getBase64Encoded();

            return certificate.getBase64Encoded()
                    .map(base64 -> aCertificateDto(
                        entityId,
                        base64Encoded.get(),
                        CertificateDto.KeyUse.Encryption,
                        certificate.getFederationEntityType()))
                    .orElseThrow(() ->exceptionFactory.createNoDataForEntityException(entityId));

        } catch(NoCertificateFoundException ncfe) {
            throw exceptionFactory.createNoDataForEntityException(entityId);
        } catch (CertificateDisabledException cde) {
            throw exceptionFactory.createDisabledTransactionException(entityId);
        }
    }

    @GET
    @Path(Urls.ConfigUrls.SIGNATURE_VERIFICATION_CERTIFICATE_PATH)
    @Timed
    public Collection<CertificateDto> getSignatureVerificationCertificates(@PathParam(Urls.SharedUrls.ENTITY_ID_PARAM) String entityId) {
        try {
            List<Certificate> certificates = certificateService.signatureVerificationCertificatesFor(entityId);
            return certificates.stream()
                    .map(cert -> aCertificateDto(
                            entityId,
                            cert.getBase64Encoded().orElseThrow(NoCertificateFoundException::new),
                            CertificateDto.KeyUse.Signing,
                            cert.getFederationEntityType()))
                    .collect(toList());
        } catch (NoCertificateFoundException ncfe) {
            throw exceptionFactory.createNoDataForEntityException(entityId);
        }
    }

    /**
     * This checks expiry dates of RP & MSA certificates and returns details of all certs
     */
    @GET
    @Path(Urls.ConfigUrls.CERTIFICATES_HEALTH_CHECK_PATH)
    public Response getHealthCheck() {
        return Response.ok(getCertHealthCheckDtos()).build();
    }

    /**
     * This performs OCSP checking of RP & MSA certificates
     */
    @GET
    @Path(Urls.ConfigUrls.INVALID_CERTIFICATES_CHECK_PATH)
    public Response invalidCertificatesCheck() {
        Collection<Certificate> certificates = certificateService.getAllCertificates();
        Set<InvalidCertificateDto> invalidCertificateDtos = ocspCertificateChainValidityChecker.check(certificates);
        return Response.ok(invalidCertificateDtos).build();
    }

    private List<CertificateHealthCheckDto> getCertHealthCheckDtos() {
        return certificateService.getAllCertificates()
                .stream()
                .map(cert -> new CertificateHealthCheckDto(cert, configuration.getCertificateWarningPeriod()))
                .collect(toList());
    }

}
