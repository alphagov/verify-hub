package uk.gov.ida.hub.samlsoapproxy.healthcheck;

import com.google.common.base.Optional;
import org.glassfish.jersey.internal.util.Base64;
import org.opensaml.saml.saml2.core.AttributeQuery;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.metadata.AttributeAuthorityDescriptor;
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
import java.util.function.Function;

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

        MatchingServiceHealthCheckResponseDto response;
        try {
            SamlMessageDto samlMessageDto = samlEngineProxy.generateHealthcheckAttributeQuery(matchingServiceHealthCheckerRequestDto);

            final Element matchingServiceHealthCheckRequest = XmlUtils.convertToElement(samlMessageDto.getSamlMessage());
            validateRequestSignature(matchingServiceHealthCheckRequest);
            response = matchingServiceHealthCheckClient.sendHealthCheckRequest(matchingServiceHealthCheckRequest,
                        configEntity.getUri()
            );
            if(response.getResponse().isPresent()) {
                final Optional<String> msaVersion = extractVersionFromResponseId(response.getResponse().get());
                if(msaVersion.isPresent()) {
                    // if we have conflicting return values, lets trust the one from the ID a little bit more
                    response = new MatchingServiceHealthCheckResponseDto(response.getResponse(), msaVersion);
                    if (!response.getVersionNumber().get().equals(msaVersion.get())) {
                        response = new MatchingServiceHealthCheckResponseDto(response.getResponse(), msaVersion);
                        LOG.warn("MSA healthcheck response with two version numbers: {0} & {1}", response.getVersionNumber().get(), msaVersion.get());
                    }
                }
            }
        } catch (ApplicationException e) {
            final String message = format("Saml-engine was unable to generate saml to send to MSA: {0}", e);
            eventLogger.logException(e, message);
            return logAndCreateUnhealthyResponse(configEntity, message);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            final String message = format("Unable to convert saml request to XML element: {0}", e);
            return logAndCreateUnhealthyResponse(configEntity, message);
        }

        if (isHealthyResponse(response, configEntity.getUri())) {
            return healthy(generateHealthCheckDescription("responded successfully", configEntity.getUri(),
                    response.getVersionNumber(), configEntity.isOnboarding()));
        } else {
            return unhealthy(generateHealthCheckFailureDescription(response, configEntity.getUri(), configEntity.isOnboarding()));
        }
    }

    private Optional<String> extractVersionFromResponseId(String response) throws IOException, SAXException, ParserConfigurationException {
        final Response attributeQueryResponse = elementToResponseTransformer.apply(XmlUtils.convertToElement(response));
        final String responseId = attributeQueryResponse.getID();
        final String key = "version-";
        if(responseId.contains(key)) {
            return Optional.fromNullable(responseId.trim().substring(responseId.indexOf(key) + key.length()));
        }
        return Optional.absent();
    }

    private void validateRequestSignature(Element matchingServiceRequest) {
        AttributeQuery attributeQuery = elementToAttributeQueryTransformer.apply(matchingServiceRequest);
        SamlValidationResponse signatureValidationResponse = matchingRequestSignatureValidator.validate(attributeQuery, AttributeAuthorityDescriptor.DEFAULT_ELEMENT_NAME);
        if (!signatureValidationResponse.isOK()) {
            SamlValidationSpecificationFailure failure = signatureValidationResponse.getSamlValidationSpecificationFailure();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), signatureValidationResponse.getCause(), Level.ERROR);
        }
    }

    private MatchingServiceHealthCheckResult logAndCreateUnhealthyResponse(MatchingServiceConfigEntityDataDto configEntity, String message) {
        return unhealthy(generateHealthCheckDescription(
                message,
                configEntity.getUri(),
                Optional.<String>absent(), configEntity.isOnboarding()));
    }

    private MatchingServiceHealthCheckDetails generateHealthCheckDescription(
            final String message,
            final URI matchingServiceUri,
            final Optional<String> version,
            final boolean isOnboarding) {

        String versionNumber = version.isPresent() ? version.get() : UNDEFINED_VERSION;
        boolean isSupported = isMsaVersionSupported(versionNumber);
        return new MatchingServiceHealthCheckDetails(matchingServiceUri, message, versionNumber,
                isSupported, isOnboarding);
    }

    private boolean isMsaVersionSupported(final String versionNumber) {
        return supportedMsaVersionsRepository.getSupportedVersions().contains(versionNumber);
    }

    private MatchingServiceHealthCheckDetails generateHealthCheckFailureDescription(
            final MatchingServiceHealthCheckResponseDto response,
            final URI matchingServiceUri,
            final boolean isOnboarding) {

        if (!response.getResponse().isPresent()) {
            return generateHealthCheckDescription("no response", matchingServiceUri, response.getVersionNumber(), isOnboarding);
        }

        return generateHealthCheckDescription("responded with non-healthy status", matchingServiceUri,
                response.getVersionNumber(), isOnboarding);
    }

    private boolean isHealthyResponse(
            final MatchingServiceHealthCheckResponseDto responseDto,
            final URI matchingServiceUri) {

        if (!responseDto.getResponse().isPresent()) {
            return false;
        }

        String exceptionMessage = format("Matching service health check failed for URI {0}", matchingServiceUri);
        try {
            // Saml-engine expects the saml to be base64 encoded
            final SamlMessageDto samlMessageDto = new SamlMessageDto(Base64.encodeAsString(responseDto.getResponse().get()));
            final MatchingServiceHealthCheckerResponseDto responseFromMatchingService =
                    samlEngineProxy.translateHealthcheckMatchingServiceResponse(samlMessageDto);

            if (responseFromMatchingService.getStatus() != MatchingServiceIdaStatus.Healthy) {
                return false;
            }
        } catch (ApplicationException e) {
            eventLogger.logException(e, exceptionMessage);
            return false;
        } catch (RuntimeException e ) {
            LOG.warn(format("Matching service health check failed for URI {0}", matchingServiceUri), e);
            return false;
        }

        return true;
    }
}
