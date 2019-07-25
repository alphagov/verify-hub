package uk.gov.ida.hub.config.resources;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableList;
import uk.gov.ida.hub.config.ConfigConfiguration;
import uk.gov.ida.hub.config.Urls;
import uk.gov.ida.hub.config.application.CertificateService;
import uk.gov.ida.hub.config.data.LocalConfigRepository;
import uk.gov.ida.hub.config.domain.Certificate;
import uk.gov.ida.hub.config.domain.CertificateConfigurable;
import uk.gov.ida.hub.config.domain.MatchingServiceConfig;
import uk.gov.ida.hub.config.domain.OCSPCertificateChainValidityChecker;
import uk.gov.ida.hub.config.domain.TransactionConfig;
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
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static uk.gov.ida.hub.config.dto.CertificateDto.aCertificateDto;
import static uk.gov.ida.hub.config.dto.CertificateHealthCheckDto.createCertificateHealthCheckDto;

@Path(Urls.ConfigUrls.CERTIFICATES_ROOT)
@Produces(MediaType.APPLICATION_JSON)
public class CertificatesResource {
    private final LocalConfigRepository<TransactionConfig> transactionDataSource;
    private final LocalConfigRepository<MatchingServiceConfig> matchingServiceDataSource;
    private final ExceptionFactory exceptionFactory;
    private final ConfigConfiguration configuration;
    private final OCSPCertificateChainValidityChecker ocspCertificateChainValidityChecker;
    private final CertificateService certificateService;


    @Inject
    public CertificatesResource(
            LocalConfigRepository<TransactionConfig> transactionDataSource,
            LocalConfigRepository<MatchingServiceConfig> matchingServiceDataSource,
            ExceptionFactory exceptionFactory,
            ConfigConfiguration configuration,
            OCSPCertificateChainValidityChecker ocspCertificateChainValidityChecker,
            CertificateService certificateService
    ) {
        this.transactionDataSource = transactionDataSource;
        this.matchingServiceDataSource = matchingServiceDataSource;
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
            return aCertificateDto(
                    entityId,
                    certificate.getX509(),
                    CertificateDto.KeyUse.Encryption,
                    certificate.getFederationEntityType()
            );
        } catch(NoCertificateFoundException ncfe) {
            throw exceptionFactory.createNoDataForEntityException(entityId);
        } catch (CertificateDisabledException cde) {
            throw exceptionFactory.createDisabledTransactionException(entityId);
        }
    }

    @GET
    @Path(Urls.ConfigUrls.SIGNATURE_VERIFICATION_CERTIFICATE_PATH)
    @Timed
    public Collection<CertificateDto> getSignatureVerificationCertificates(
            @PathParam(Urls.SharedUrls.ENTITY_ID_PARAM) String entityId) {
        try {
            List<Certificate> certificateDetails = certificateService.signatureVerificationCertificatesFor(entityId);
            return certificateDetails.stream()
                    .map(details -> aCertificateDto(
                            entityId,
                            details.getX509(),
                            CertificateDto.KeyUse.Signing,
                            details.getFederationEntityType()))
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
    public Response getHealthCheck() throws CertificateException {
        return Response.ok(getCertHealthCheckDtos()).build();
    }

    /**
     * This performs OCSP checking of RP & MSA certificates
     */
    @GET
    @Path(Urls.ConfigUrls.INVALID_CERTIFICATES_CHECK_PATH)
    public Response invalidCertificatesCheck() {
        Collection<Certificate> certificates = getCertificates(transactionDataSource.getAllData(), matchingServiceDataSource.getAllData());
        ImmutableList<InvalidCertificateDto> invalidCertificateDtos = ocspCertificateChainValidityChecker.check(certificates);
        return Response.ok(invalidCertificateDtos).build();
    }

    private List<CertificateHealthCheckDto> getCertHealthCheckDtos() throws CertificateException {
        List<CertificateHealthCheckDto> certs = new LinkedList<>();
        // IDP certs are now in the federation metadata and checked for expiry and OCSP status in separate sensu checks
        for(TransactionConfig transaction : transactionDataSource.getAllData()) {
            certs.add(createCertificateHealthCheckDto(
                    transaction.getEntityId(),
                    transaction.getEncryptionCertificate(),
                    configuration.getCertificateWarningPeriod()));
            addCertificateHealthCheckDtos(
                    certs,
                    transaction.getEntityId(),
                    transaction.getSignatureVerificationCertificates());
        }
        for(MatchingServiceConfig ms : matchingServiceDataSource.getAllData()) {
            certs.add(createCertificateHealthCheckDto(
                    ms.getEntityId(),
                    ms.getEncryptionCertificate(),
                    configuration.getCertificateWarningPeriod()));
            addCertificateHealthCheckDtos(
                    certs,
                    ms.getEntityId(),
                    ms.getSignatureVerificationCertificates());
        }
        return certs;
    }

    private void addCertificateHealthCheckDtos(
            List<CertificateHealthCheckDto> dtos,
            final String entityId,
            Collection<? extends Certificate> certificates) throws CertificateException {
        for(Certificate cert : certificates) {
            dtos.add(createCertificateHealthCheckDto(
                    entityId,
                    cert,
                    configuration.getCertificateWarningPeriod()));
        }
    }

    private Collection<Certificate> getCertificates(Set<TransactionConfig> transactionConfigs, Set<MatchingServiceConfig> matchingServiceConfigs) {
        return Stream.concat(transactionConfigs.stream(), matchingServiceConfigs.stream())
                .flatMap(this::getCertificates)
                .collect(Collectors.toSet());
    }

    private Stream<Certificate> getCertificates(CertificateConfigurable config){
        List<Certificate> certs = new ArrayList();
        certs.addAll(config.getSignatureVerificationCertificates());
        certs.add(config.getEncryptionCertificate());
        return certs.stream();
    }
}
