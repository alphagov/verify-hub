package uk.gov.ida.hub.samlengine.services;

import io.dropwizard.testing.ResourceHelpers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Response;
import uk.gov.ida.hub.samlengine.contracts.SamlAuthnResponseTranslatorDto;
import uk.gov.ida.hub.samlengine.domain.InboundResponseFromCountry;
import uk.gov.ida.hub.samlengine.validation.country.ResponseAssertionsFromCountryValidator;
import uk.gov.ida.hub.samlengine.validation.country.ResponseFromCountryValidator;
import uk.gov.ida.saml.core.IdaSamlBootstrap;
import uk.gov.ida.saml.core.transformers.AuthnContextFactory;
import uk.gov.ida.saml.core.transformers.outbound.decorators.AssertionBlobEncrypter;
import uk.gov.ida.saml.core.validators.DestinationValidator;
import uk.gov.ida.saml.deserializers.StringToOpenSamlObjectTransformer;
import uk.gov.ida.saml.deserializers.parser.SamlObjectParser;
import uk.gov.ida.saml.hub.transformers.inbound.CountryAuthenticationStatusUnmarshaller;
import uk.gov.ida.saml.hub.transformers.inbound.PassthroughAssertionUnmarshaller;
import uk.gov.ida.saml.security.AssertionDecrypter;
import uk.gov.ida.saml.security.EidasValidatorFactory;
import uk.gov.ida.saml.security.SecretKeyEncrypter;
import uk.gov.ida.saml.security.validators.ValidatedResponse;
import uk.gov.ida.saml.serializers.XmlObjectToBase64EncodedStringTransformer;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.samlengine.domain.LevelOfAssurance.LEVEL_2;

@RunWith(MockitoJUnitRunner.class)
public class CountryAuthnResponseTranslatorServiceTest {

    @Mock
    private StringToOpenSamlObjectTransformer<Response> stringToOpenSamlResponseTransformer;
    @Mock
    private ResponseFromCountryValidator responseFromCountryValidator;
    @Mock
    private ResponseAssertionsFromCountryValidator responseAssertionsFromCountryValidator;
    @Mock
    private SamlAuthnResponseTranslatorDto samlAuthnResponseTranslatorDto;
    @Mock
    private AssertionDecrypter assertionDecrypter;
    @Mock
    private SecretKeyEncrypter secretKeyEncrypter;
    @Mock
    private AssertionBlobEncrypter assertionBlobEncrypter;
    @Mock
    private EidasValidatorFactory eidasValidatorFactory;
    @Mock
    private DestinationValidator validateSamlResponseIssuedByIdpDestination;

    private String persistentIdName = "UK/GB/12345";
    private String responseIssuer = "http://localhost:56002/ServiceMetadata";
    private String identityUnderlyingAssertionBlob = "encryptedBlob";

    private CountryAuthnResponseTranslatorService service;

    private SAMLObject buildResponseFromFile() throws Exception {
        String xmlString = new String(Files.readAllBytes(Paths.get(ResourceHelpers.resourceFilePath("EIDASAMLResponse.xml"))));
        return (Response) new SamlObjectParser().getSamlObject(xmlString);
    }

    public void setupSigned() throws Exception {
        IdaSamlBootstrap.bootstrap();
        service = new CountryAuthnResponseTranslatorService(
                stringToOpenSamlResponseTransformer,
                responseFromCountryValidator,
                responseAssertionsFromCountryValidator,
                validateSamlResponseIssuedByIdpDestination,
                assertionDecrypter,
                secretKeyEncrypter,
                assertionBlobEncrypter,
                eidasValidatorFactory,
                new PassthroughAssertionUnmarshaller(new XmlObjectToBase64EncodedStringTransformer<>(), new AuthnContextFactory()),
                new CountryAuthenticationStatusUnmarshaller());

        Response eidasSAMLResponse = (Response) buildResponseFromFile();
        ValidatedResponse validateEIDASSAMLResponse = new ValidatedResponse(eidasSAMLResponse);
        List<Assertion> decryptedAssertions = eidasSAMLResponse.getAssertions();

        when(samlAuthnResponseTranslatorDto.getSamlResponse()).thenReturn("eidas");
        when(samlAuthnResponseTranslatorDto.getMatchingServiceEntityId()).thenReturn("mid");
        when(stringToOpenSamlResponseTransformer.apply("eidas")).thenReturn(eidasSAMLResponse);
        doNothing().when(responseFromCountryValidator).validate(eidasSAMLResponse);
        when(eidasValidatorFactory.getValidatedResponse(eidasSAMLResponse)).thenReturn(validateEIDASSAMLResponse);
        when(assertionDecrypter.decryptAssertions(validateEIDASSAMLResponse)).thenReturn(decryptedAssertions);
        when(assertionBlobEncrypter.encryptAssertionBlob(eq("mid"), any(String.class))).thenReturn(identityUnderlyingAssertionBlob);
    }

    public void setupUnsigned() throws Exception {
        IdaSamlBootstrap.bootstrap();
        service = new CountryAuthnResponseTranslatorService(
                stringToOpenSamlResponseTransformer,
                responseFromCountryValidator,
                responseAssertionsFromCountryValidator,
                validateSamlResponseIssuedByIdpDestination,
                assertionDecrypter,
                secretKeyEncrypter,
                assertionBlobEncrypter,
                eidasValidatorFactory,
                new PassthroughAssertionUnmarshaller(new XmlObjectToBase64EncodedStringTransformer<>(), new AuthnContextFactory()),
                new CountryAuthenticationStatusUnmarshaller());

        Response eidasSAMLResponse = (Response) buildResponseFromFile();
        ValidatedResponse validateEIDASSAMLResponse = new ValidatedResponse(eidasSAMLResponse);
        List<Assertion> decryptedAssertions = eidasSAMLResponse.getAssertions();
        decryptedAssertions.get(0).setSignature(null);
        List<String> reEncryptedKeys = List.of("key");

        when(samlAuthnResponseTranslatorDto.getSamlResponse()).thenReturn("eidas");
        when(samlAuthnResponseTranslatorDto.getMatchingServiceEntityId()).thenReturn("mid");
        when(stringToOpenSamlResponseTransformer.apply("eidas")).thenReturn(eidasSAMLResponse);
        doNothing().when(responseFromCountryValidator).validate(eidasSAMLResponse);
        when(eidasValidatorFactory.getValidatedResponse(eidasSAMLResponse)).thenReturn(validateEIDASSAMLResponse);
        when(assertionDecrypter.decryptAssertions(validateEIDASSAMLResponse)).thenReturn(decryptedAssertions);
        when(assertionDecrypter.getReEncryptedKeys(validateEIDASSAMLResponse, secretKeyEncrypter,"mid" )).thenReturn(reEncryptedKeys);
        when(assertionBlobEncrypter.encryptAssertionBlob(eq("mid"), any(String.class))).thenReturn(identityUnderlyingAssertionBlob);
    }

    @Test
    public void shouldExtractAuthnStatementAssertionDetailsSignedAssertions() throws Exception {
        setupSigned();
        InboundResponseFromCountry result = service.translate(samlAuthnResponseTranslatorDto);

        assertThat(result.getIssuer()).isEqualTo(responseIssuer);

        assertThat(result.getStatus().isPresent()).isTrue();
        assertThat(result.getStatus().get()).isEqualTo("Success");

        assertThat(result.getStatusMessage().isPresent()).isFalse();

        assertThat(result.getLevelOfAssurance().isPresent()).isTrue();
        assertThat(result.getLevelOfAssurance().get()).isEqualTo(LEVEL_2);

        assertThat(result.getPersistentId().isPresent()).isTrue();
        assertThat(result.getPersistentId().get()).isEqualTo(persistentIdName);

        assertThat(result.getEncryptedIdentityAssertionBlob().isPresent()).isTrue();
        assertThat(result.getEncryptedIdentityAssertionBlob().get()).isEqualTo(identityUnderlyingAssertionBlob);

        assertThat(result.getCountrySignedResponseContainer().isPresent()).isFalse();
    }

    @Test public void shouldExtractAuthnStatementAssertionDetailsUnsignedAssertions() throws Exception {
        setupUnsigned();
        InboundResponseFromCountry result = service.translate(samlAuthnResponseTranslatorDto);
        assertThat(result.getCountrySignedResponseContainer().isPresent()).isTrue();
        assertThat(result.getCountrySignedResponseContainer().get().getBase64encryptedKeys()).isEqualTo(List.of("key"));
    }
}
