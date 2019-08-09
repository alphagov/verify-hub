package uk.gov.ida.hub.samlsoapproxy.healthcheck;

import org.opensaml.saml.saml2.core.AttributeQuery;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import uk.gov.ida.exceptions.ApplicationException;
import uk.gov.ida.hub.samlsoapproxy.client.MatchingServiceHealthCheckClient;
import uk.gov.ida.hub.samlsoapproxy.contract.MatchingServiceConfigEntityDataDto;
import uk.gov.ida.hub.samlsoapproxy.contract.MatchingServiceHealthCheckerRequestDto;
import uk.gov.ida.hub.samlsoapproxy.contract.MatchingServiceHealthCheckerResponseDto;
import uk.gov.ida.hub.samlsoapproxy.contract.SamlMessageDto;
import uk.gov.ida.hub.samlsoapproxy.domain.MatchingServiceHealthCheckResponseDto;
import uk.gov.ida.hub.samlsoapproxy.logging.HealthCheckEventLogger;
import uk.gov.ida.hub.samlsoapproxy.proxy.SamlEngineProxy;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.core.validation.SamlValidationResponse;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;
import uk.gov.ida.saml.hub.transformers.inbound.MatchingServiceIdaStatus;
import uk.gov.ida.saml.security.SamlMessageSignatureValidator;
import uk.gov.ida.shared.utils.xml.XmlUtils;

import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.text.MessageFormat.format;
import static uk.gov.ida.hub.samlsoapproxy.healthcheck.MatchingServiceHealthCheckResult.healthy;
import static uk.gov.ida.hub.samlsoapproxy.healthcheck.MatchingServiceHealthCheckResult.unhealthy;

/**
 * Checks the health of Matching Services by sending AttributeQueries
 * from saml-soap-proxy to the matching services.
 */
public class MatchingServiceHealthChecker {
    private static final Logger LOG = LoggerFactory.getLogger(MatchingServiceHealthChecker.class);
    private static final String UNDEFINED_VERSION = "0";
    private static final String UNDEFINED = "UNDEFINED";

    private final Function<Element, AttributeQuery> elementToAttributeQueryTransformer;
    private final Function<Element, Response> elementToResponseTransformer;
    private final SamlMessageSignatureValidator matchingRequestSignatureValidator;
    private final SupportedMsaVersionsRepository supportedMsaVersionsRepository;
    private final SamlEngineProxy samlEngineProxy;
    private final MatchingServiceHealthCheckClient matchingServiceHealthCheckClient;
    private final HealthCheckEventLogger eventLogger;

    @Inject
    public MatchingServiceHealthChecker(
            final Function<Element, AttributeQuery> elementToAttributeQueryTransformer,
            final Function<Element, Response> elementToResponseTransformer,
            @Named("matchingRequestSignatureValidator") SamlMessageSignatureValidator matchingRequestSignatureValidator,
            final SupportedMsaVersionsRepository supportedMsaVersionsRepository,
            final SamlEngineProxy samlEngineProxy,
            final MatchingServiceHealthCheckClient matchingServiceHealthCheckClient,
            HealthCheckEventLogger eventLogger) {
        this.elementToAttributeQueryTransformer = elementToAttributeQueryTransformer;
        this.elementToResponseTransformer = elementToResponseTransformer;
        this.matchingRequestSignatureValidator = matchingRequestSignatureValidator;
        this.supportedMsaVersionsRepository = supportedMsaVersionsRepository;
        this.matchingServiceHealthCheckClient = matchingServiceHealthCheckClient;
        this.samlEngineProxy = samlEngineProxy;
        this.eventLogger = eventLogger;
    }

    public MatchingServiceHealthCheckResult performHealthCheck(final MatchingServiceConfigEntityDataDto configEntity) {
        MatchingServiceHealthCheckerRequestDto matchingServiceHealthCheckerRequestDto =
                new MatchingServiceHealthCheckerRequestDto(configEntity.getTransactionEntityId(), configEntity.getEntityId());

        MatchingServiceHealthCheckResponseDto responseDto;
        try {
            SamlMessageDto samlMessageDto = samlEngineProxy.generateHealthcheckAttributeQuery(matchingServiceHealthCheckerRequestDto);

            final Element matchingServiceHealthCheckRequest = XmlUtils.convertToElement(samlMessageDto.getSamlMessage());
            validateRequestSignature(matchingServiceHealthCheckRequest);
            responseDto = matchingServiceHealthCheckClient.sendHealthCheckRequest(matchingServiceHealthCheckRequest,
                    configEntity.getUri()
            );

            return buildMatchingServiceHealthCheckResult(configEntity, responseDto.getResponse());
        } catch (ApplicationException e) {
            final String message = format("Saml-engine was unable to generate saml to send to MSA: {0}", e);
            eventLogger.logException(e, message);
            return logAndCreateUnhealthyResponse(configEntity, message);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            final String message = format("Unable to convert saml request to XML element: {0}", e);
            return logAndCreateUnhealthyResponse(configEntity, message);
        }
    }

    private MatchingServiceHealthCheckResult buildMatchingServiceHealthCheckResult(final MatchingServiceConfigEntityDataDto configEntity,
                                                                                   final Optional<String> responseBody) throws ParserConfigurationException, SAXException, IOException {

        final HealthCheckData healthCheckData = getHealthCheckData(responseBody);

        final String versionNumber = healthCheckData.getVersion()
                .filter(Predicate.not(String::isEmpty))
                .orElse(UNDEFINED_VERSION);

        final String isEidasEnabled = healthCheckData.getEidasEnabled()
                .filter(Predicate.not(String::isEmpty))
                .orElse(UNDEFINED);

        final String shouldSignWithSha1 = healthCheckData.getShouldSignWithSha1()
                .filter(Predicate.not(String::isEmpty))
                .orElse(UNDEFINED);

        if (isHealthyResponse(configEntity.getUri(), responseBody)) {
            return healthy(generateHealthCheckDescription(
                    "responded successfully",
                    configEntity.getUri(),
                    versionNumber,
                    configEntity.isOnboarding(),
                    isEidasEnabled,
                    shouldSignWithSha1)
            );
        } else {
            return unhealthy(generateHealthCheckFailureDescription(configEntity.getUri(),
                    configEntity.isOnboarding(),
                    responseBody,
                    versionNumber,
                    isEidasEnabled,
                    shouldSignWithSha1));
        }
    }

    private HealthCheckData getHealthCheckData(Optional<String> responseBody) throws IOException, SAXException, ParserConfigurationException {

        if (responseBody.isPresent()) {
            Response response = elementToResponseTransformer.apply(XmlUtils.convertToElement(responseBody.get()));
            return HealthCheckData.extractFrom(response.getID());
        }

        return HealthCheckData.extractFrom(null);
    }

    private void validateRequestSignature(Element matchingServiceRequest) {
        AttributeQuery attributeQuery = elementToAttributeQueryTransformer.apply(matchingServiceRequest);
        SamlValidationResponse signatureValidationResponse = matchingRequestSignatureValidator.validate(attributeQuery, SPSSODescriptor.DEFAULT_ELEMENT_NAME);
        if (!signatureValidationResponse.isOK()) {
            SamlValidationSpecificationFailure failure = signatureValidationResponse.getSamlValidationSpecificationFailure();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), signatureValidationResponse.getCause(), Level.ERROR);
        }
    }

    private MatchingServiceHealthCheckResult logAndCreateUnhealthyResponse(MatchingServiceConfigEntityDataDto configEntity, String message) {
        return unhealthy(generateHealthCheckDescription(
                message,
                configEntity.getUri(),
                UNDEFINED_VERSION,
                configEntity.isOnboarding(),
                UNDEFINED,
                UNDEFINED
                )
        );
    }

    private MatchingServiceHealthCheckDetails generateHealthCheckDescription(
            final String message,
            final URI matchingServiceUri,
            final String versionNumber,
            final boolean isOnboarding,
            final String isEidasEnabled,
            final String shouldSignWithSha1) {

        boolean isSupported = supportedMsaVersionsRepository.getSupportedVersions().contains(versionNumber);
        return new MatchingServiceHealthCheckDetails(
                matchingServiceUri,
                message,
                versionNumber,
                isSupported,
                isOnboarding,
                isEidasEnabled,
                shouldSignWithSha1
        );
    }

    private MatchingServiceHealthCheckDetails generateHealthCheckFailureDescription(
            final URI matchingServiceUri,
            final boolean isOnboarding,
            Optional<String> response,
            String versionNumber,
            String eidasEnabled,
            String shouldSignWithSha1) {

        if (response.isEmpty()) {
            return generateHealthCheckDescription("no response", matchingServiceUri, versionNumber, isOnboarding, eidasEnabled, shouldSignWithSha1);
        }

        return generateHealthCheckDescription("responded with non-healthy status", matchingServiceUri,
                versionNumber, isOnboarding, eidasEnabled, shouldSignWithSha1);
    }

    private boolean isHealthyResponse(
            final URI matchingServiceUri,
            Optional<String> response) {

        if (response.isEmpty()) {
            return false;
        }

        String exceptionMessage = format("Matching service health check failed for URI {0}", matchingServiceUri);
        try {
            // Saml-engine expects the saml to be base64 encoded

            String base64Response = Base64.getEncoder().encodeToString(response.get().getBytes(Charset.defaultCharset()));
            final SamlMessageDto samlMessageDto = new SamlMessageDto(base64Response);
            final MatchingServiceHealthCheckerResponseDto responseFromMatchingService =
                    samlEngineProxy.translateHealthcheckMatchingServiceResponse(samlMessageDto);

            if (responseFromMatchingService.getStatus() != MatchingServiceIdaStatus.Healthy) {
                return false;
            }
        } catch (ApplicationException e) {
            eventLogger.logException(e, exceptionMessage);
            return false;
        } catch (RuntimeException e) {
            LOG.warn(format("Matching service health check failed for URI {0}", matchingServiceUri), e);
            return false;
        }

        return true;
    }
}
