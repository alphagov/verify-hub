package uk.gov.ida.hub.samlsoapproxy.healthcheck;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.opensaml.saml.saml2.core.AttributeQuery;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.w3c.dom.Element;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.exceptions.ApplicationException;
import uk.gov.ida.hub.samlsoapproxy.client.MatchingServiceHealthCheckClient;
import uk.gov.ida.hub.samlsoapproxy.contract.MatchingServiceConfigEntityDataDto;
import uk.gov.ida.hub.samlsoapproxy.contract.MatchingServiceHealthCheckerResponseDto;
import uk.gov.ida.hub.samlsoapproxy.contract.SamlMessageDto;
import uk.gov.ida.hub.samlsoapproxy.domain.MatchingServiceHealthCheckResponseDto;
import uk.gov.ida.hub.samlsoapproxy.logging.HealthCheckEventLogger;
import uk.gov.ida.hub.samlsoapproxy.proxy.SamlEngineProxy;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.core.validation.SamlValidationResponse;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;
import uk.gov.ida.saml.hub.transformers.inbound.MatchingServiceIdaStatus;
import uk.gov.ida.saml.security.SamlMessageSignatureValidator;

import javax.xml.namespace.QName;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.samlsoapproxy.builders.MatchingServiceConfigEntityDataDtoBuilder.aMatchingServiceConfigEntityDataDto;
import static uk.gov.ida.hub.samlsoapproxy.builders.MatchingServiceHealthCheckDetailsBuilder.aMatchingServiceHealthCheckDetails;
import static uk.gov.ida.hub.samlsoapproxy.builders.MatchingServiceHealthCheckerResponseDtoBuilder.anInboundResponseFromMatchingServiceDto;
import static uk.gov.ida.saml.hub.transformers.inbound.MatchingServiceIdaStatus.Healthy;
import static uk.gov.ida.saml.hub.transformers.inbound.MatchingServiceIdaStatus.RequesterError;

@RunWith(OpenSAMLMockitoRunner.class)
public class MatchingServiceHealthCheckerTest {
    private static final String SUPPORTED_MSA_VERSION_NUMBER = "supported-version";
    private static final String UNSUPPORTED_MSA_VERSION_NUMBER = "unsupported-version";
    private static final QName HUB_ROLE = SPSSODescriptor.DEFAULT_ELEMENT_NAME;

    private static final String DEFAULT_VERSION_VALUE = "0";
    private static final String UNDEFINED = "UNDEFINED";

    private MatchingServiceHealthChecker matchingServiceHealthChecker;
    @Mock
    private SamlEngineProxy samlEngineProxy;
    @Mock
    private MatchingServiceHealthCheckClient matchingServiceHealthCheckClient;
    @Mock
    private HealthCheckEventLogger eventLogger;
    @Mock
    private Function<Element, AttributeQuery> elementToAttributeQueryTransformer;
    @Mock
    private SamlMessageSignatureValidator matchingRequestSignatureValidator;
    @Mock
    private AttributeQuery healthCheckAttributeQuery;
    @Mock
    private Function<Element, Response> elementToResponseTransformer;

    @Before
    public void setUp() {
        List<String> supportedVersions = new ArrayList<>();
        supportedVersions.add(SUPPORTED_MSA_VERSION_NUMBER);

        final SupportedMsaVersionsRepository supportedMsaVersionsRepository = new SupportedMsaVersionsRepository();
        supportedMsaVersionsRepository.add(supportedVersions);

        when(elementToAttributeQueryTransformer.apply(any(Element.class))).thenReturn(healthCheckAttributeQuery);
        when(matchingRequestSignatureValidator.validate(healthCheckAttributeQuery, HUB_ROLE)).thenReturn(SamlValidationResponse.aValidResponse());

        mockHealthcheckResponseId("healthcheck-response-id");

        matchingServiceHealthChecker = new MatchingServiceHealthChecker(
                elementToAttributeQueryTransformer,
                elementToResponseTransformer,
                matchingRequestSignatureValidator,
                supportedMsaVersionsRepository,
                samlEngineProxy,
                matchingServiceHealthCheckClient,
                eventLogger);
    }

    @Test(expected = SamlTransformationErrorException.class)
    public void shouldNotSendHealthCheckIfSignatureFailsToValidate() {
        SamlValidationSpecificationFailure mockFailure = mock(SamlValidationSpecificationFailure.class);
        when(matchingRequestSignatureValidator.validate(any(AttributeQuery.class), eq(HUB_ROLE))).thenReturn(SamlValidationResponse.anInvalidResponse(mockFailure));
        MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto =
                aMatchingServiceConfigEntityDataDto().build();
        prepareForHealthyResponse(matchingServiceConfigEntityDataDto);


        matchingServiceHealthChecker.performHealthCheck(aMatchingServiceConfigEntityDataDto().build());
    }

    @Test
    public void shouldReturnSuccessWithMessageForHealthyMatchingService() {
        MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto =
                aMatchingServiceConfigEntityDataDto().build();
        prepareForHealthyResponse(matchingServiceConfigEntityDataDto);

        MatchingServiceHealthCheckResult result = matchingServiceHealthChecker.performHealthCheck(aMatchingServiceConfigEntityDataDto().build());

        assertThat(result.isHealthy()).isTrue();
        assertThat(result.getDetails())
                .isEqualToComparingOnlyGivenFields(aMatchingServiceHealthCheckDetails().withDetails("responded successfully").build(), "details");
    }

    @Test
    public void shouldLogExceptionsWhenAFailureOccursInGeneratingHealthCheckRequest() {
        ApplicationException unauditedException = ApplicationException.createUnauditedException(ExceptionType.INVALID_SAML, UUID.randomUUID());
        when(samlEngineProxy.generateHealthcheckAttributeQuery(any())).thenThrow(unauditedException);

        matchingServiceHealthChecker.performHealthCheck(aMatchingServiceConfigEntityDataDto().build());

        verify(eventLogger).logException(unauditedException, "Saml-engine was unable to generate saml to send to MSA: uk.gov.ida.exceptions.ApplicationException: Exception of type [INVALID_SAML] ");
    }

    @Test
    public void shouldLogExceptionsWhenAFailureOccursInTranslatingHealthCheckRequest() {
        String uri = "http://random";
        ApplicationException unauditedException = ApplicationException.createUnauditedException(ExceptionType.INVALID_SAML, UUID.randomUUID());
        when(samlEngineProxy.generateHealthcheckAttributeQuery(any())).thenReturn(new SamlMessageDto("<saml/>"));
        when(samlEngineProxy.translateHealthcheckMatchingServiceResponse(any())).thenThrow(unauditedException);
        MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto = aMatchingServiceConfigEntityDataDto().withUri(uri).build();
        when(matchingServiceHealthCheckClient.sendHealthCheckRequest(any(),
                eq(matchingServiceConfigEntityDataDto.getUri())
        ))
                .thenReturn(new MatchingServiceHealthCheckResponseDto(Optional.of("<saml/>")));

        matchingServiceHealthChecker.performHealthCheck(matchingServiceConfigEntityDataDto);

        verify(eventLogger).logException(unauditedException, MessageFormat.format("Matching service health check failed for URI {0}", uri));
    }

    @Test
    public void shouldReturnResultWhenVersionIsNotReturnedByMsa() {
        MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto =
                aMatchingServiceConfigEntityDataDto().build();
        prepareForHealthyResponse(matchingServiceConfigEntityDataDto);

        MatchingServiceHealthCheckResult result = matchingServiceHealthChecker.performHealthCheck(matchingServiceConfigEntityDataDto);

        assertThat(result.getDetails().getVersionNumber()).isEqualTo(DEFAULT_VERSION_VALUE);
    }

    @Test
    public void shouldReturnResultWhenVersionReturnedByMsaIsSupported() {
        MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto =
                aMatchingServiceConfigEntityDataDto().build();
        prepareForHealthyResponse(matchingServiceConfigEntityDataDto);
        mockHealthcheckResponseId("healthcheck-response-id-version-"+SUPPORTED_MSA_VERSION_NUMBER);

        MatchingServiceHealthCheckResult result = matchingServiceHealthChecker.performHealthCheck(matchingServiceConfigEntityDataDto);

        assertThat(result.getDetails().isVersionSupported()).isTrue();
    }

    @Test
    public void shouldReturnResultWhenVersionReturnedByMsaIsNotSupported() {
        MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto =
                aMatchingServiceConfigEntityDataDto().build();
        prepareForHealthyResponse(matchingServiceConfigEntityDataDto);

        MatchingServiceHealthCheckResult result = matchingServiceHealthChecker.performHealthCheck(matchingServiceConfigEntityDataDto);

        assertThat(result.getDetails().isVersionSupported()).isFalse();
    }

    @Test
    public void shouldReturnDefaultVersionIfVersionFlagNotPresentInResponseIdForHealthyMatchingService() {
        MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto =
                aMatchingServiceConfigEntityDataDto().build();
        prepareForHealthyResponse(matchingServiceConfigEntityDataDto);
        mockHealthcheckResponseId("healthcheck-response-id");

        MatchingServiceHealthCheckResult result = matchingServiceHealthChecker.performHealthCheck(matchingServiceConfigEntityDataDto);

        assertThat(result.getDetails().getVersionNumber()).isEqualTo(DEFAULT_VERSION_VALUE);
    }

    @Test
    public void shouldReturnVersionIfPresentInResponseIDButNotHeaderWithMessageForHealthyMatchingService() {
        final String versionNumber = "180";
        final Optional<String> headerVersion = Optional.empty();
        MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto =
                aMatchingServiceConfigEntityDataDto().build();
        prepareForHealthyResponse(matchingServiceConfigEntityDataDto);
        mockHealthcheckResponseId("healthcheck-response-id-uuid-version-"+versionNumber);

        MatchingServiceHealthCheckResult result = matchingServiceHealthChecker.performHealthCheck(matchingServiceConfigEntityDataDto);

        assertThat(result.getDetails().getVersionNumber()).isEqualTo(versionNumber);
    }

    @Test
    public void shouldReturnVersionIfSameOneIsPresentInResponseIDAndHeaderWithMessageForHealthyMatchingService() {
        final String versionNumber = "180";
        final Optional<String> headerVersion = Optional.ofNullable(versionNumber);
        MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto =
                aMatchingServiceConfigEntityDataDto().build();
        prepareForHealthyResponse(matchingServiceConfigEntityDataDto);
        mockHealthcheckResponseId("healthcheck-response-id-uuid-version-"+versionNumber);

        MatchingServiceHealthCheckResult result = matchingServiceHealthChecker.performHealthCheck(matchingServiceConfigEntityDataDto);

        assertThat(result.getDetails().getVersionNumber()).isEqualTo(versionNumber);
    }

    @Test
    public void shouldReturnDefaultVersionIfVersionValueNotDefinedInResponseIdForHealthyMatchingService() {
        MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto =
                aMatchingServiceConfigEntityDataDto().build();
        prepareForHealthyResponse(matchingServiceConfigEntityDataDto);
        mockHealthcheckResponseId("healthcheck-response-id-version-");

        MatchingServiceHealthCheckResult result = matchingServiceHealthChecker.performHealthCheck(matchingServiceConfigEntityDataDto);

        assertThat(result.getDetails().getVersionNumber()).isEqualTo(DEFAULT_VERSION_VALUE);
    }

    @Test
    public void shouldReturnReasonableValuesForVersionEidasEnabledAndUseSha1WhenReasonableRequestIdReceived() {
        MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto =
                aMatchingServiceConfigEntityDataDto().build();
        prepareForHealthyResponse(matchingServiceConfigEntityDataDto);
        mockHealthcheckResponseId("healthcheck-response-id-version-1234-eidasenabled-true-shouldsignwithsha1-false");

        MatchingServiceHealthCheckResult result = matchingServiceHealthChecker.performHealthCheck(matchingServiceConfigEntityDataDto);

        assertThat(result.getDetails().getVersionNumber()).isEqualTo("1234");
        assertThat(result.getDetails().getEidasEnabled()).isEqualTo("true");
        assertThat(result.getDetails().getShouldSignWithSha1()).isEqualTo("false");
    }

    @Test
    public void shouldReturnDisabledValuesForVersionEidasEnabledAndUseSha1WhenReasonableRequestIdReceived() {
        MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto =
                aMatchingServiceConfigEntityDataDto().build();
        prepareForHealthyResponse(matchingServiceConfigEntityDataDto);
        mockHealthcheckResponseId("healthcheck-response-id-version-1234-eidasenabled-false-shouldsignwithsha1-true");

        MatchingServiceHealthCheckResult result = matchingServiceHealthChecker.performHealthCheck(matchingServiceConfigEntityDataDto);

        assertThat(result.getDetails().getVersionNumber()).isEqualTo("1234");
        assertThat(result.getDetails().getEidasEnabled()).isEqualTo("false");
        assertThat(result.getDetails().getShouldSignWithSha1()).isEqualTo("true");
    }

    @Test
    public void shouldReturnDefaultVersionIfVersionValueNotDefinedInResponseIdWithOtherFlagsForHealthyMatchingService() {
        MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto =
                aMatchingServiceConfigEntityDataDto().build();
        prepareForHealthyResponse(matchingServiceConfigEntityDataDto);
        mockHealthcheckResponseId("healthcheck-response-id-version--eidasenabled-true-shouldsignwithsha1-false");

        MatchingServiceHealthCheckResult result = matchingServiceHealthChecker.performHealthCheck(matchingServiceConfigEntityDataDto);

        assertThat(result.getDetails().getVersionNumber()).isEqualTo(DEFAULT_VERSION_VALUE);
    }

    @Test
    public void shouldReturnUndefinedEidasEnabledValueWhenEidasEnabledFlagNotPresentInMsaResponseId() {
        MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto =
                aMatchingServiceConfigEntityDataDto().build();
        prepareForHealthyResponse(matchingServiceConfigEntityDataDto);
        mockHealthcheckResponseId("healthcheck-response-id-version-1234");

        MatchingServiceHealthCheckResult result = matchingServiceHealthChecker.performHealthCheck(matchingServiceConfigEntityDataDto);

        assertThat(result.getDetails().getEidasEnabled()).isEqualTo(UNDEFINED);
    }

    @Test
    public void shouldReturnUndefinedEidasEnabledValueWhenNothingSpecifiedInMsaResponseId() {
        MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto =
                aMatchingServiceConfigEntityDataDto().build();
        prepareForHealthyResponse(matchingServiceConfigEntityDataDto);
        mockHealthcheckResponseId("healthcheck-response-id-version-1234-eidasenabled--shouldsignwithsha1-true");

        MatchingServiceHealthCheckResult result = matchingServiceHealthChecker.performHealthCheck(matchingServiceConfigEntityDataDto);

        assertThat(result.getDetails().getEidasEnabled()).isEqualTo(UNDEFINED);
    }

    @Test
    public void shouldReturnUndefinedShouldSignWithSha1ValueWhenShouldSignWithSha1FlagNotPresentInMsaResponseId() {
        MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto =
                aMatchingServiceConfigEntityDataDto().build();
        prepareForHealthyResponse(matchingServiceConfigEntityDataDto);
        mockHealthcheckResponseId("healthcheck-response-id-version-1234");

        MatchingServiceHealthCheckResult result = matchingServiceHealthChecker.performHealthCheck(matchingServiceConfigEntityDataDto);

        assertThat(result.getDetails().getShouldSignWithSha1()).isEqualTo(UNDEFINED);
    }

    @Test
    public void shouldReturnUndefinedShouldSignWithSha1ValueWhenNothingSpecifiedInMsaResponseId() {
        MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto =
                aMatchingServiceConfigEntityDataDto().build();
        prepareForHealthyResponse(matchingServiceConfigEntityDataDto);
        mockHealthcheckResponseId("healthcheck-response-id-version-1234-eidasenabled-true-shouldsignwithsha1-");

        MatchingServiceHealthCheckResult result = matchingServiceHealthChecker.performHealthCheck(matchingServiceConfigEntityDataDto);

        assertThat(result.getDetails().getShouldSignWithSha1()).isEqualTo(UNDEFINED);
    }

    private void mockHealthcheckResponseId(String version) {
        Response response = mock(Response.class);
        when(elementToResponseTransformer.apply(any())).thenReturn(response);
        when(response.getID()).thenReturn(version);
    }

    @Test
    public void shouldReturnFailureWithMessageForUnhealthyMatchingService() {
        MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto =
                aMatchingServiceConfigEntityDataDto().build();
        prepareForResponse(matchingServiceConfigEntityDataDto, RequesterError);

        MatchingServiceHealthCheckResult result = matchingServiceHealthChecker.performHealthCheck(matchingServiceConfigEntityDataDto);

        assertThat(result.isHealthy()).isEqualTo(false);
        assertThat(result.getDetails().getDetails()).isEqualTo("responded with non-healthy status");
    }

    @Test
    public void shouldReturnFailureWithMessageForMatchingServiceThatCannotBeTransformed() {
        MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto =
                aMatchingServiceConfigEntityDataDto().build();
        when(samlEngineProxy.generateHealthcheckAttributeQuery(any())).thenReturn(new SamlMessageDto("<saml/>"));
        when(matchingServiceHealthCheckClient.sendHealthCheckRequest(any(),
                eq(matchingServiceConfigEntityDataDto.getUri())
        ))
                .thenReturn(new MatchingServiceHealthCheckResponseDto(Optional.of("<saml/>")));
        when(samlEngineProxy.translateHealthcheckMatchingServiceResponse(any()))
                .thenThrow(ApplicationException.createAuditedException(ExceptionType.INVALID_SAML, UUID.randomUUID()));

        MatchingServiceHealthCheckResult result = matchingServiceHealthChecker.performHealthCheck(matchingServiceConfigEntityDataDto);

        assertThat(result.isHealthy()).isEqualTo(false);
        assertThat(result.getDetails().getDetails()).isEqualTo("responded with non-healthy status");
    }

    @Test
    public void shouldReturnFailureWithMessageFromMatchingServiceThatCannotBeParsed() {
        MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto =
                aMatchingServiceConfigEntityDataDto().build();
        when(samlEngineProxy.generateHealthcheckAttributeQuery(any())).thenReturn(new SamlMessageDto("samSamSaml"));

        MatchingServiceHealthCheckResult result = matchingServiceHealthChecker.performHealthCheck(matchingServiceConfigEntityDataDto);

        assertThat(result.isHealthy()).isEqualTo(false);
        assertThat(result.getDetails().getDetails()).isEqualTo("Unable to convert saml request to XML element: org.xml.sax.SAXParseException; lineNumber: 1; columnNumber: 1; Content is not allowed in prolog.");
    }

    @Test
    public void shouldReturnFailureWithMessageForMatchingServiceThatCannotBeGeneratedBySamlEngine() {
        MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto =
                aMatchingServiceConfigEntityDataDto().build();
        when(samlEngineProxy.generateHealthcheckAttributeQuery(any())).thenThrow(ApplicationException.createAuditedException(ExceptionType.INVALID_INPUT, UUID.randomUUID()));

        MatchingServiceHealthCheckResult result = matchingServiceHealthChecker.performHealthCheck(matchingServiceConfigEntityDataDto);

        assertThat(result.isHealthy()).isEqualTo(false);
        assertThat(result.getDetails().getDetails()).isEqualTo("Saml-engine was unable to generate saml to send to MSA: uk.gov.ida.exceptions.ApplicationException: Exception of type [INVALID_INPUT] ");
    }

    @Test
    public void shouldExecuteHealthCheckForMatchingServiceWithHealthCheckEnabled() {
        MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto =
                aMatchingServiceConfigEntityDataDto()
                        .withHealthCheckEnabled()
                        .build();
        prepareForHealthyResponse(matchingServiceConfigEntityDataDto);

        matchingServiceHealthChecker.performHealthCheck(matchingServiceConfigEntityDataDto);

        verify(matchingServiceHealthCheckClient, times(1)).sendHealthCheckRequest(any(),
                eq(matchingServiceConfigEntityDataDto.getUri())
        );
    }

    @Test
    public void shouldReturnReportWhenHubFailsToPerformHealthCheck() {
        final String expectedFailureDetails = "no response";
        MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto =
                aMatchingServiceConfigEntityDataDto().build();
        when(samlEngineProxy.generateHealthcheckAttributeQuery(any())).thenReturn(new SamlMessageDto("<saml/>"));
        when(matchingServiceHealthCheckClient.sendHealthCheckRequest(any(),
                eq(matchingServiceConfigEntityDataDto.getUri())
        ))
                .thenReturn(new MatchingServiceHealthCheckResponseDto(Optional.empty()));

        MatchingServiceHealthCheckResult result = matchingServiceHealthChecker.performHealthCheck(matchingServiceConfigEntityDataDto);

        assertThat(result.isHealthy()).isFalse();
        assertThat(result.getDetails())
                .isEqualToComparingOnlyGivenFields(aMatchingServiceHealthCheckDetails().withDetails(expectedFailureDetails).build(), "details");
    }

    @Test
    public void shouldIncludeOnboardingStatusTrueWhenMsaIsOnboarding() {
        MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto =
                aMatchingServiceConfigEntityDataDto()
                        .withOnboarding(true)
                        .withHealthCheckEnabled()
                        .build();
        prepareForHealthyResponse(matchingServiceConfigEntityDataDto);

        MatchingServiceHealthCheckResult result = matchingServiceHealthChecker.performHealthCheck(matchingServiceConfigEntityDataDto);

        assertThat(result.getDetails().isOnboarding()).isTrue();
    }

    @Test
    public void shouldIncludeOnboardingStatusFalseWhenMsaIsNotOnboarding() {
        MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto =
                aMatchingServiceConfigEntityDataDto()
                        .withOnboarding(false)
                        .withHealthCheckEnabled()
                        .build();
        prepareForHealthyResponse(matchingServiceConfigEntityDataDto);

        MatchingServiceHealthCheckResult result = matchingServiceHealthChecker.performHealthCheck(matchingServiceConfigEntityDataDto);

        assertThat(result.getDetails().isOnboarding()).isFalse();
    }

    @Test
    public void shouldBase64EncodeSamlToBeSentToSamlEngine() {
        final String saml = "<samlsamlsamlsamlsamlsamlsamlsamlsaml/>";
        MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto =
                aMatchingServiceConfigEntityDataDto().build();
        prepareForHealthyResponse(matchingServiceConfigEntityDataDto);
        when(matchingServiceHealthCheckClient.sendHealthCheckRequest(any(),
                eq(matchingServiceConfigEntityDataDto.getUri())
        ))
                .thenReturn(new MatchingServiceHealthCheckResponseDto(Optional.of(saml)));

        matchingServiceHealthChecker.performHealthCheck(aMatchingServiceConfigEntityDataDto().build());

        ArgumentCaptor<SamlMessageDto> argumentCaptor = ArgumentCaptor.forClass(SamlMessageDto.class);
        verify(samlEngineProxy, times(1)).translateHealthcheckMatchingServiceResponse(argumentCaptor.capture());
        assertThat(Base64.getEncoder().encodeToString(saml.getBytes())).isEqualTo(argumentCaptor.getValue().getSamlMessage());
    }

    private void prepareForHealthyResponse(MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto) {
        prepareForResponse(matchingServiceConfigEntityDataDto, Healthy);
    }

    private void prepareForResponse(MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto, MatchingServiceIdaStatus status) {
        when(samlEngineProxy.generateHealthcheckAttributeQuery(any())).thenReturn(new SamlMessageDto("<saml/>"));
        final MatchingServiceHealthCheckerResponseDto inboundResponseFromMatchingServiceDto =
                anInboundResponseFromMatchingServiceDto().withStatus(status).build();
        when(matchingServiceHealthCheckClient.sendHealthCheckRequest(any(),
                eq(matchingServiceConfigEntityDataDto.getUri())
        ))
                .thenReturn(new MatchingServiceHealthCheckResponseDto(Optional.of("<saml/>")));
        when(samlEngineProxy.translateHealthcheckMatchingServiceResponse(any())).thenReturn(inboundResponseFromMatchingServiceDto);
    }
}
