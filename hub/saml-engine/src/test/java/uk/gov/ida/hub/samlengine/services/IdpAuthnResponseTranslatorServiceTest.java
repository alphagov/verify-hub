package uk.gov.ida.hub.samlengine.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.xmlsec.algorithm.DigestAlgorithm;
import org.opensaml.xmlsec.algorithm.SignatureAlgorithm;
import org.opensaml.xmlsec.algorithm.descriptors.DigestSHA256;
import org.opensaml.xmlsec.algorithm.descriptors.SignatureRSASHA1;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.impl.SignatureImpl;
import uk.gov.ida.eidas.logging.EidasAttributesLogger;
import uk.gov.ida.hub.samlengine.builders.BuilderHelper;
import uk.gov.ida.hub.samlengine.contracts.SamlAuthnResponseTranslatorDto;
import uk.gov.ida.hub.samlengine.domain.InboundResponseFromIdpDto;
import uk.gov.ida.hub.samlengine.logging.IdpAssertionMetricsCollector;
import uk.gov.ida.hub.samlengine.proxy.TransactionsConfigProxy;
import uk.gov.ida.saml.core.IdaSamlBootstrap;
import uk.gov.ida.saml.core.domain.AuthnContext;
import uk.gov.ida.saml.core.domain.FraudDetectedDetails;
import uk.gov.ida.saml.core.domain.PassthroughAssertion;
import uk.gov.ida.saml.core.domain.PersistentId;
import uk.gov.ida.saml.core.test.TestEntityIds;
import uk.gov.ida.saml.core.test.builders.AssertionBuilder;
import uk.gov.ida.saml.core.test.builders.AuthnStatementBuilder;
import uk.gov.ida.saml.core.test.builders.IPAddressAttributeBuilder;
import uk.gov.ida.saml.core.test.builders.IssuerBuilder;
import uk.gov.ida.saml.core.test.builders.MatchingDatasetAttributeStatementBuilder_1_1;
import uk.gov.ida.saml.core.test.builders.SignatureBuilder;
import uk.gov.ida.saml.core.transformers.outbound.decorators.AssertionBlobEncrypter;
import uk.gov.ida.saml.deserializers.StringToOpenSamlObjectTransformer;
import uk.gov.ida.saml.hub.domain.IdpIdaStatus;
import uk.gov.ida.saml.hub.domain.InboundResponseFromIdp;
import uk.gov.ida.saml.hub.transformers.inbound.InboundResponseFromIdpDataGenerator;
import uk.gov.ida.saml.hub.transformers.inbound.providers.DecoratedSamlResponseToIdaResponseIssuedByIdpTransformer;

import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ida.saml.core.test.builders.AttributeStatementBuilder.anAttributeStatement;
import static uk.gov.ida.saml.core.test.builders.SubjectBuilder.aSubject;
import static uk.gov.ida.saml.core.test.builders.SubjectConfirmationBuilder.aSubjectConfirmation;
import static uk.gov.ida.saml.core.test.builders.SubjectConfirmationDataBuilder.aSubjectConfirmationData;

@RunWith(MockitoJUnitRunner.class)
public class IdpAuthnResponseTranslatorServiceTest {
    @Mock
    private StringToOpenSamlObjectTransformer<Response> stringToOpenSamlResponseTransformer;
    @Mock
    private DecoratedSamlResponseToIdaResponseIssuedByIdpTransformer samlResponseToIdaResponseIssuedByIdpTransformer;
    @Mock
    private AssertionBlobEncrypter assertionBlobEncrypter;
    @Mock
    private SamlAuthnResponseTranslatorDto responseContainer;
    @Mock
    private Response samlResponse;
    @Mock
    private InboundResponseFromIdp responseFromIdp;
    @Mock
    private PassthroughAssertion authStatementAssertion;
    @Mock
    private PersistentId authnStatementPersistentId;
    @Mock
    private StringToOpenSamlObjectTransformer<Assertion> stringToAssertionTransformer;
    @Mock
    private Assertion authnStatementAssertion;
    @Mock
    private Assertion matchingDatasetAssertion;
    @Mock
    private Issuer issuer;
    @Mock
    private IdpAssertionMetricsCollector idpAssertionMetricsCollector;
    @Mock
    private PassthroughAssertion passThroughAssertion;
    @Mock
    private TransactionsConfigProxy transactionsConfigProxy;

    private IdpIdaStatus.Status statusCode = IdpIdaStatus.Status.Success;
    private String statusMessage = "status message";
    private IdpIdaStatus status = new IdpIdaStatus.IdpIdaStatusFactory().create(statusCode, statusMessage);
    private String saml = "some saml";
    private String principalIpAddressSeenByIdp = "ip address";
    private String persistentIdName = "id name";
    private String responseIssuer = "responseIssuer";
    private String authStatementUnderlyingAssertionBlob = "some more saml";
    private String encryptedAuthnAssertion = "some encrypted saml";
    private String matchingDatasetUnderlyingAssertionBlob = "blob";
    private IdpAuthnResponseTranslatorService service;

    @Before
    public void setup() {
        IdaSamlBootstrap.bootstrap();
        final String idpEntityId = TestEntityIds.STUB_IDP_ONE;
        final String assertionId1 = randomUUID().toString();
        final String assertionId2 = randomUUID().toString();
        final String requestId = randomUUID().toString();
        final SignatureAlgorithm signatureAlgorithm = new SignatureRSASHA1();
        final DigestAlgorithm digestAlgorithm = new DigestSHA256();
        final Subject mdsAssertionSubject = aSubject().withSubjectConfirmation(aSubjectConfirmation().withSubjectConfirmationData(aSubjectConfirmationData().withInResponseTo(requestId).build()).build()).build();
        final AttributeStatement matchingDatasetAttributeStatement = MatchingDatasetAttributeStatementBuilder_1_1.aMatchingDatasetAttributeStatement_1_1().build();
        final Subject authnAssertionSubject = aSubject().withSubjectConfirmation(aSubjectConfirmation().withSubjectConfirmationData(aSubjectConfirmationData().withInResponseTo(requestId).build()).build()).build();
        final AttributeStatement ipAddress = anAttributeStatement().addAttribute(IPAddressAttributeBuilder.anIPAddress().build()).build();
        final Optional<Signature> signature = of(SignatureBuilder.aSignature().build());
        final SignatureImpl signatureImpl = ((SignatureImpl) signature.get());
        signatureImpl.setXMLSignature(BuilderHelper.createXMLSignature(signatureAlgorithm, digestAlgorithm));

        authnStatementAssertion = AssertionBuilder.anAssertion()
                .withId(assertionId1)
                .withIssuer(IssuerBuilder.anIssuer().withIssuerId(idpEntityId).build())
                .withSubject(authnAssertionSubject)
                .addAttributeStatement(ipAddress)
                .addAuthnStatement(AuthnStatementBuilder.anAuthnStatement().build())
                .withSignature(SignatureBuilder.aSignature()
                        .withSignatureAlgorithm(signatureAlgorithm)
                        .withDigestAlgorithm(assertionId1, digestAlgorithm).build())
                .buildUnencrypted();

        matchingDatasetAssertion = AssertionBuilder.anAssertion().withId(assertionId2)
                .withIssuer(IssuerBuilder.anIssuer().withIssuerId(idpEntityId).build())
                .withSubject(mdsAssertionSubject)
                .addAttributeStatement(matchingDatasetAttributeStatement)
                .withSignature(SignatureBuilder.aSignature()
                        .withSignatureAlgorithm(signatureAlgorithm)
                        .withDigestAlgorithm(assertionId2, digestAlgorithm).build())
                .buildUnencrypted();

        when(responseContainer.getSamlResponse()).thenReturn(saml);
        when(assertionBlobEncrypter.encryptAssertionBlob(any(), eq(authStatementUnderlyingAssertionBlob))).thenReturn(encryptedAuthnAssertion);
        when(stringToOpenSamlResponseTransformer.apply(saml)).thenReturn(samlResponse);
        when(samlResponseToIdaResponseIssuedByIdpTransformer.apply(samlResponse)).thenReturn(responseFromIdp);
        when(authStatementAssertion.getUnderlyingAssertionBlob()).thenReturn(authStatementUnderlyingAssertionBlob);
        when(authStatementAssertion.getAuthnContext()).thenReturn(Optional.empty());
        when(authStatementAssertion.getFraudDetectedDetails()).thenReturn(Optional.empty());
        when(authStatementAssertion.getPrincipalIpAddressAsSeenByIdp()).thenReturn(Optional.of(principalIpAddressSeenByIdp));
        when(authnStatementPersistentId.getNameId()).thenReturn("a name id");
        when(authnStatementPersistentId.getNameId()).thenReturn(persistentIdName);
        when(authStatementAssertion.getPersistentId()).thenReturn(authnStatementPersistentId);
        when(responseFromIdp.getIssuer()).thenReturn(responseIssuer);
        when(responseFromIdp.getStatus()).thenReturn(status);
        when(responseFromIdp.getMatchingDatasetAssertion()).thenReturn(empty());
        when(responseFromIdp.getAuthnStatementAssertion()).thenReturn(empty());
        when(responseFromIdp.getSignature()).thenReturn(signature);
        when(samlResponse.getIssuer()).thenReturn(issuer);
        when(stringToAssertionTransformer.apply(authStatementUnderlyingAssertionBlob)).thenReturn(authnStatementAssertion);
        when(stringToAssertionTransformer.apply(matchingDatasetUnderlyingAssertionBlob)).thenReturn(matchingDatasetAssertion);

        InboundResponseFromIdpDataGenerator inboundResponseFromIdpDataGenerator = new InboundResponseFromIdpDataGenerator(assertionBlobEncrypter);
        service = new IdpAuthnResponseTranslatorService(
                stringToOpenSamlResponseTransformer,
                stringToAssertionTransformer,
                samlResponseToIdaResponseIssuedByIdpTransformer,
                inboundResponseFromIdpDataGenerator,
                idpAssertionMetricsCollector,
                transactionsConfigProxy);
    }

    @Test
    public void shouldExtractAuthnStatementAssertionDetails() {
        when(responseFromIdp.getAuthnStatementAssertion()).thenReturn(of(authStatementAssertion));

        InboundResponseFromIdpDto result = translateAndCheckCommonFields();

        checkAuthnStatementValues(result);
    }

    @Test
    public void shouldExtractLevelOfAssurance() {
        AuthnContext authnContext = AuthnContext.LEVEL_1;
        when(authStatementAssertion.getAuthnContext()).thenReturn(Optional.of(authnContext));
        when(responseFromIdp.getAuthnStatementAssertion()).thenReturn(of(authStatementAssertion));

        InboundResponseFromIdpDto result = translateAndCheckCommonFields();

        checkAuthnStatementValues(result);
        assertThat(result.getLevelOfAssurance().get().name()).isEqualTo(authnContext.name());
    }

    @Test
    public void shouldExtractFraudDetails() {
        String fraudIndicator = "fraud indicator";
        String fraudEventId = "fraud event id";
        FraudDetectedDetails fraudDetectedDetails = Mockito.mock(FraudDetectedDetails.class);
        when(fraudDetectedDetails.getFraudIndicator()).thenReturn(fraudIndicator);
        when(fraudDetectedDetails.getIdpFraudEventId()).thenReturn(fraudEventId);
        when(authStatementAssertion.getFraudDetectedDetails()).thenReturn(Optional.of(fraudDetectedDetails));
        when(responseFromIdp.getAuthnStatementAssertion()).thenReturn(of(authStatementAssertion));

        InboundResponseFromIdpDto result = translateAndCheckCommonFields();

        checkAuthnStatementValues(result);
        assert(result.getFraudIndicator().get()).equals(fraudIndicator);
        assert(result.getIdpFraudEventId().get()).equals(fraudEventId);
    }

    @Test
    public void shouldHandleNoAssertions() {
        InboundResponseFromIdpDto result = translateAndCheckCommonFields();

        assertThat(result.getEncryptedAuthnAssertion().isPresent()).isFalse();
        assertThat(result.getEncryptedMatchingDatasetAssertion().isPresent()).isFalse();
        assertThat(result.getLevelOfAssurance().isPresent()).isFalse();
        assertThat(result.getPersistentId().isPresent()).isFalse();
        assertThat(result.getFraudIndicator().isPresent()).isFalse();
        assertThat(result.getIdpFraudEventId().isPresent()).isFalse();
    }

    @Test
    public void shouldEncryptMatchingDatasetAssertion() throws Exception {
        PassthroughAssertion assertion = Mockito.mock(PassthroughAssertion.class);
        when(assertion.getUnderlyingAssertionBlob()).thenReturn(matchingDatasetUnderlyingAssertionBlob);
        when(responseFromIdp.getMatchingDatasetAssertion()).thenReturn(of(assertion));
        String expectedEncryptedBlob = "some-value";
        final String entityId = "entity-id";
        when(responseContainer.getMatchingServiceEntityId()).thenReturn(entityId);
        when(assertionBlobEncrypter.encryptAssertionBlob(entityId, matchingDatasetUnderlyingAssertionBlob)).thenReturn(expectedEncryptedBlob);
        InboundResponseFromIdpDto result = translateAndCheckCommonFields();
        assertThat(result.getEncryptedMatchingDatasetAssertion().get()).isEqualTo(expectedEncryptedBlob);
    }

    @Test
    public void shouldCallUpdateMetricsForNotOnOrAfterWhenHasAuthnStatementAssertion() {
        when(responseFromIdp.getAuthnStatementAssertion()).thenReturn(of(authStatementAssertion));

        service.translate(responseContainer);

        verify(idpAssertionMetricsCollector, times(1)).update(authnStatementAssertion);
    }

    @Test
    public void shouldCallUpdateMetricsForNotOnOrAfterWhenHasMatchingDatasetAssertion() {
        when(passThroughAssertion.getUnderlyingAssertionBlob()).thenReturn(matchingDatasetUnderlyingAssertionBlob);
        when(responseFromIdp.getMatchingDatasetAssertion()).thenReturn(Optional.of(passThroughAssertion));

        service.translate(responseContainer);

        verify(idpAssertionMetricsCollector, times(1)).update(matchingDatasetAssertion);
    }

    @Test
    public void shouldSetEidasAttributesLoggerWhenMatchingServiceEntityIsConfiguredAsAnEidasProxyNode() {
        when(responseContainer.getMatchingServiceEntityId()).thenReturn("a proxy node entity id");
        when(transactionsConfigProxy.isProxyNodeEntityId("a proxy node entity id")).thenReturn(true);
        translateAndCheckCommonFields();
        verify(samlResponseToIdaResponseIssuedByIdpTransformer).setEidasAttributesLogger(isA(EidasAttributesLogger.class));
    }

    @Test
    public void shouldNotSetEidasAttributesLoggerWhenMatchingServiceEntityIsNotConfiguredAsAnEidasProxyNode() {
        when(responseContainer.getMatchingServiceEntityId()).thenReturn("a proxy node entity id");
        when(transactionsConfigProxy.isProxyNodeEntityId("a proxy node entity id")).thenReturn(false);
        translateAndCheckCommonFields();
        verify(samlResponseToIdaResponseIssuedByIdpTransformer, never()).setEidasAttributesLogger(isA(EidasAttributesLogger.class));
    }

    private void checkAlwaysPresentFields(InboundResponseFromIdpDto result) {
        assertThat(result.getStatus()).isSameAs(status.getStatusCode());
        assertThat(result.getStatusMessage()).isSameAs(status.getMessage());
        assertThat(result.getIssuer()).isSameAs(responseIssuer);
    }

    private void checkAuthnStatementValues(InboundResponseFromIdpDto result) {
        assert(result.getEncryptedAuthnAssertion().get()).equals(encryptedAuthnAssertion);
        assert(result.getPrincipalIpAddressAsSeenByIdp().get()).equals(principalIpAddressSeenByIdp);
        assert(result.getPersistentId().get()).equals(persistentIdName);
    }

    private InboundResponseFromIdpDto translateAndCheckCommonFields() {
        InboundResponseFromIdpDto result = service.translate(responseContainer);
        checkAlwaysPresentFields(result);
        return result;
    }
}
