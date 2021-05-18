package uk.gov.ida.hub.samlengine.services;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.Response;
import org.slf4j.event.Level;
import uk.gov.ida.hub.samlengine.builders.InboundResponseFromMatchingServiceBuilder;
import uk.gov.ida.hub.samlengine.contracts.InboundResponseFromMatchingServiceDto;
import uk.gov.ida.hub.samlengine.domain.SamlResponseContainerDto;
import uk.gov.ida.saml.core.domain.AuthnContext;
import uk.gov.ida.saml.core.domain.FraudDetectedDetails;
import uk.gov.ida.saml.core.domain.PassthroughAssertion;
import uk.gov.ida.saml.core.domain.PersistentId;
import uk.gov.ida.saml.core.transformers.outbound.decorators.AssertionBlobEncrypter;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.deserializers.StringToOpenSamlObjectTransformer;
import uk.gov.ida.saml.hub.domain.InboundResponseFromMatchingService;
import uk.gov.ida.saml.hub.transformers.inbound.MatchingServiceIdaStatus;
import uk.gov.ida.saml.hub.transformers.inbound.providers.DecoratedSamlResponseToInboundResponseFromMatchingServiceTransformer;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.ida.saml.core.test.TestEntityIds.TEST_RP;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MatchingServiceResponseTranslatorServiceTest {

    @Mock
    private StringToOpenSamlObjectTransformer<Response> responseUnmarshaller;
    @Mock
    private DecoratedSamlResponseToInboundResponseFromMatchingServiceTransformer responseToInboundResponseFromMatchingServiceTransformer;
    @Mock
    private AssertionBlobEncrypter assertionBlobEncrypter;

    private MatchingServiceResponseTranslatorService matchingServiceResponseTranslatorService;

    @BeforeEach
    public void setUp() {
        matchingServiceResponseTranslatorService = new MatchingServiceResponseTranslatorService(responseUnmarshaller, responseToInboundResponseFromMatchingServiceTransformer, assertionBlobEncrypter);
    }

    @Test
    public void handle_shouldNotifyPolicyWhenSamlStringCannotBeConvertedToAnElement() {
        Assertions.assertThrows(SamlTransformationErrorException.class, () -> {
            final SamlResponseContainerDto samlResponse = new SamlResponseContainerDto("Woooo!", TEST_RP);
            when(responseUnmarshaller.apply(samlResponse.getSamlResponse())).thenThrow(new SamlTransformationErrorException("not xml", Level.ERROR));
            matchingServiceResponseTranslatorService.translate(samlResponse);
            // event sink logging is tested in SamlTransformationErrorExceptionMapperTest

        });
    }

    @Test
    public void populateReturnDtoCorrectly_handleMatchResponse() {
        final String inResponseTo = "inResponseTo";
        final String issuer = "issuer";
        final Optional<AuthnContext> authnContext = Optional.of(AuthnContext.LEVEL_2);
        final Optional<FraudDetectedDetails> fraudDetectedDetails = Optional.empty();
        final String encryptedAssertion = "encryptedAssertion";
        final MatchingServiceIdaStatus status = MatchingServiceIdaStatus.MatchingServiceMatch;
        final SamlResponseContainerDto samlResponse = new SamlResponseContainerDto("saml", TEST_RP);
        setUpForTranslate(authnContext, fraudDetectedDetails, encryptedAssertion, inResponseTo, issuer, samlResponse.getSamlResponse(), status);

        final InboundResponseFromMatchingServiceDto inboundResponseFromMatchingServiceDto = matchingServiceResponseTranslatorService.translate(samlResponse);

        assertThat(inboundResponseFromMatchingServiceDto.getInResponseTo()).isEqualTo(inResponseTo);
        assertThat(inboundResponseFromMatchingServiceDto.getEncryptedMatchingServiceAssertion().isPresent()).isTrue();
        assertThat(inboundResponseFromMatchingServiceDto.getEncryptedMatchingServiceAssertion().get()).isEqualTo(encryptedAssertion);
        assertThat(inboundResponseFromMatchingServiceDto.getIssuer()).isEqualTo(issuer);
        assertThat(inboundResponseFromMatchingServiceDto.getLevelOfAssurance().isPresent()).isTrue();
        assertThat(inboundResponseFromMatchingServiceDto.getLevelOfAssurance().get().name()).isEqualTo(authnContext.get().name());
        assertThat(inboundResponseFromMatchingServiceDto.getStatus()).isEqualTo(status);
    }

    @Test
    public void populateReturnDtoCorrectly_handleNoMatchResponse() {
        final String inResponseTo = "inResponseTo";
        final String issuer = "issuer";
        final Optional<AuthnContext> authnContext = Optional.of(AuthnContext.LEVEL_2);
        final Optional<FraudDetectedDetails> fraudDetectedDetails = Optional.empty();
        final String underlyingAssertionBlob = "underlyingAssertionBlob";
        final MatchingServiceIdaStatus status = MatchingServiceIdaStatus.NoMatchingServiceMatchFromMatchingService;
        final SamlResponseContainerDto samlResponse = new SamlResponseContainerDto("saml", TEST_RP);
        setUpForTranslate(authnContext, fraudDetectedDetails, underlyingAssertionBlob, inResponseTo, issuer, samlResponse.getSamlResponse(), status);

        final InboundResponseFromMatchingServiceDto inboundResponseFromMatchingServiceDto = matchingServiceResponseTranslatorService.translate(samlResponse);

        assertThat(inboundResponseFromMatchingServiceDto.getInResponseTo()).isEqualTo(inResponseTo);
        assertThat(inboundResponseFromMatchingServiceDto.getEncryptedMatchingServiceAssertion().isPresent()).isTrue();
        assertThat(inboundResponseFromMatchingServiceDto.getEncryptedMatchingServiceAssertion().get()).isEqualTo(underlyingAssertionBlob);
        assertThat(inboundResponseFromMatchingServiceDto.getIssuer()).isEqualTo(issuer);
        assertThat(inboundResponseFromMatchingServiceDto.getLevelOfAssurance().isPresent()).isTrue();
        assertThat(inboundResponseFromMatchingServiceDto.getLevelOfAssurance().get().name()).isEqualTo(authnContext.get().name());
        assertThat(inboundResponseFromMatchingServiceDto.getStatus()).isEqualTo(status);
    }

    @Test
    public void populateReturnDtoCorrectly_handleRequesterError() {
        final String inResponseTo = "inResponseTo";
        final String issuer = "issuer";
        final Optional<AuthnContext> authnContext = Optional.empty();
        final Optional<FraudDetectedDetails> fraudDetectedDetails = Optional.empty();
        final String underlyingAssertionBlob = null;
        final MatchingServiceIdaStatus status = MatchingServiceIdaStatus.RequesterError;
        final SamlResponseContainerDto samlResponse = new SamlResponseContainerDto("saml", TEST_RP);
        setUpForTranslate(authnContext, fraudDetectedDetails, underlyingAssertionBlob, inResponseTo, issuer, samlResponse.getSamlResponse(), status);

        final InboundResponseFromMatchingServiceDto inboundResponseFromMatchingServiceDto = matchingServiceResponseTranslatorService.translate(samlResponse);

        assertThat(inboundResponseFromMatchingServiceDto.getInResponseTo()).isEqualTo(inResponseTo);
        assertThat(inboundResponseFromMatchingServiceDto.getEncryptedMatchingServiceAssertion()).isNotPresent();
        assertThat(inboundResponseFromMatchingServiceDto.getIssuer()).isEqualTo(issuer);
        assertThat(inboundResponseFromMatchingServiceDto.getLevelOfAssurance()).isNotPresent();
        assertThat(inboundResponseFromMatchingServiceDto.getStatus()).isEqualTo(status);
    }

    @Test
    public void populateReturnDtoCorrectly_handleUserAccountCreatedResponse() {
        final String inResponseTo = "inResponseTo";
        final String issuer = "issuer";
        final Optional<AuthnContext> authnContext = Optional.of(AuthnContext.LEVEL_2);
        final Optional<FraudDetectedDetails> fraudDetectedDetails = Optional.empty();
        final String underlyingAssertionBlob = "underlyingAssertionBlob";
        final MatchingServiceIdaStatus status = MatchingServiceIdaStatus.UserAccountCreated;
        final SamlResponseContainerDto samlResponse = new SamlResponseContainerDto("saml", TEST_RP);
        setUpForTranslate(authnContext, fraudDetectedDetails, underlyingAssertionBlob, inResponseTo, issuer, samlResponse.getSamlResponse(), status);

        final InboundResponseFromMatchingServiceDto inboundResponseFromMatchingServiceDto = matchingServiceResponseTranslatorService.translate(samlResponse);

        assertThat(inboundResponseFromMatchingServiceDto.getInResponseTo()).isEqualTo(inResponseTo);
        assertThat(inboundResponseFromMatchingServiceDto.getEncryptedMatchingServiceAssertion().isPresent()).isTrue();
        assertThat(inboundResponseFromMatchingServiceDto.getEncryptedMatchingServiceAssertion().get()).isEqualTo(underlyingAssertionBlob);
        assertThat(inboundResponseFromMatchingServiceDto.getIssuer()).isEqualTo(issuer);
        assertThat(inboundResponseFromMatchingServiceDto.getLevelOfAssurance().isPresent()).isTrue();
        assertThat(inboundResponseFromMatchingServiceDto.getLevelOfAssurance().get().name()).isEqualTo(authnContext.get().name());
        assertThat(inboundResponseFromMatchingServiceDto.getStatus()).isEqualTo(status);
    }

    private void setUpForTranslate(Optional<AuthnContext> authnContext, Optional<FraudDetectedDetails> fraudDetectedDetails, String encryptedAssertion, String inResponseTo, String issuer, String samlResponse, MatchingServiceIdaStatus status) {
        final PassthroughAssertion assertion = new PassthroughAssertion(new PersistentId("persistentId"),
                authnContext,
                encryptedAssertion,
                fraudDetectedDetails,
                Optional.of("principalIpAddressAsSeenByIdp"));
        final InboundResponseFromMatchingService inboundResponseFromMatchingService = InboundResponseFromMatchingServiceBuilder
                .anInboundResponseFromMatchingService()
                .withInResponseTo(inResponseTo)
                .withIssuerId(issuer)
                .withMatchingServiceAssertion(assertion)
                .withStatus(status)
                .build();
        Response response = mock(Response.class);
        Issuer responseIssuer = mock(Issuer.class);
        when(response.getIssuer()).thenReturn(responseIssuer);
        when(responseUnmarshaller.apply(samlResponse)).thenReturn(response);
        when(responseToInboundResponseFromMatchingServiceTransformer.transform(response)).thenReturn(inboundResponseFromMatchingService);
        when(assertionBlobEncrypter.encryptAssertionBlob(eq(TEST_RP), any())).thenReturn(encryptedAssertion);
    }

}
