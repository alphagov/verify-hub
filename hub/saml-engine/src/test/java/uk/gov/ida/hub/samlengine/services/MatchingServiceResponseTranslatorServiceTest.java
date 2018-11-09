package uk.gov.ida.hub.samlengine.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.Response;
import org.slf4j.event.Level;
import uk.gov.ida.hub.samlengine.builders.InboundResponseFromMatchingServiceBuilder;
import uk.gov.ida.hub.samlengine.contracts.InboundResponseFromMatchingServiceDto;
import uk.gov.ida.hub.samlengine.domain.SamlResponseDto;
import uk.gov.ida.saml.core.domain.AuthnContext;
import uk.gov.ida.saml.core.domain.FraudDetectedDetails;
import uk.gov.ida.saml.core.domain.PassthroughAssertion;
import uk.gov.ida.saml.core.domain.PersistentId;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.deserializers.StringToOpenSamlObjectTransformer;
import uk.gov.ida.saml.hub.domain.InboundResponseFromMatchingService;
import uk.gov.ida.saml.hub.transformers.inbound.MatchingServiceIdaStatus;
import uk.gov.ida.saml.hub.transformers.inbound.providers.DecoratedSamlResponseToInboundResponseFromMatchingServiceTransformer;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MatchingServiceResponseTranslatorServiceTest {

    @Mock
    private StringToOpenSamlObjectTransformer<Response> responseUnmarshaller;
    @Mock
    private DecoratedSamlResponseToInboundResponseFromMatchingServiceTransformer responseToInboundResponseFromMatchingServiceTransformer;

    private MatchingServiceResponseTranslatorService matchingServiceResponseTranslatorService;

    @Before
    public void setUp() {
        matchingServiceResponseTranslatorService = new MatchingServiceResponseTranslatorService(responseUnmarshaller, responseToInboundResponseFromMatchingServiceTransformer);
    }

    @Test(expected=SamlTransformationErrorException.class)
    public void handle_shouldNotifyPolicyWhenSamlStringCannotBeConvertedToAnElement() throws Exception {
        final SamlResponseDto samlResponse = new SamlResponseDto("Woooo!");
        when(responseUnmarshaller.apply(samlResponse.getSamlResponse())).thenThrow(new SamlTransformationErrorException("not xml", Level.ERROR));
        matchingServiceResponseTranslatorService.translate(samlResponse);
        // event sink logging is tested in SamlTransformationErrorExceptionMapperTest
    }

    @Test
    public void populateReturnDtoCorrectly_handleMatchResponse() {
        final String inResponseTo = "inResponseTo";
        final String issuer = "issuer";
        final Optional<AuthnContext> authnContext = Optional.of(AuthnContext.LEVEL_2);
        final Optional<FraudDetectedDetails> fraudDetectedDetails = Optional.empty();
        final String underlyingAssertionBlob = "underlyingAssertionBlob";
        final MatchingServiceIdaStatus status = MatchingServiceIdaStatus.MatchingServiceMatch;
        final SamlResponseDto samlResponse = new SamlResponseDto("saml");
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
    public void populateReturnDtoCorrectly_handleNoMatchResponse() {
        final String inResponseTo = "inResponseTo";
        final String issuer = "issuer";
        final Optional<AuthnContext> authnContext = Optional.of(AuthnContext.LEVEL_2);
        final Optional<FraudDetectedDetails> fraudDetectedDetails = Optional.empty();
        final String underlyingAssertionBlob = "underlyingAssertionBlob";
        final MatchingServiceIdaStatus status = MatchingServiceIdaStatus.NoMatchingServiceMatchFromMatchingService;
        final SamlResponseDto samlResponse = new SamlResponseDto("saml");
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
        final SamlResponseDto samlResponse = new SamlResponseDto("saml");
        setUpForTranslate(authnContext, fraudDetectedDetails, underlyingAssertionBlob, inResponseTo, issuer, samlResponse.getSamlResponse(), status);

        final InboundResponseFromMatchingServiceDto inboundResponseFromMatchingServiceDto = matchingServiceResponseTranslatorService.translate(samlResponse);

        assertThat(inboundResponseFromMatchingServiceDto.getInResponseTo()).isEqualTo(inResponseTo);
        assertThat(inboundResponseFromMatchingServiceDto.getEncryptedMatchingServiceAssertion().isPresent()).isFalse();
        assertThat(inboundResponseFromMatchingServiceDto.getIssuer()).isEqualTo(issuer);
        assertThat(inboundResponseFromMatchingServiceDto.getLevelOfAssurance().isPresent()).isFalse();
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
        final SamlResponseDto samlResponse = new SamlResponseDto("saml");
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

    private void setUpForTranslate(Optional<AuthnContext> authnContext, Optional<FraudDetectedDetails> fraudDetectedDetails, String underlyingAssertionBlob, String inResponseTo, String issuer, String samlResponse, MatchingServiceIdaStatus status) {
        final PassthroughAssertion assertion = new PassthroughAssertion(new PersistentId("persistentId"),
                authnContext,
                underlyingAssertionBlob,
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
    }

}
