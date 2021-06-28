package uk.gov.ida.hub.samlengine.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.ecp.Response;
import org.opensaml.xmlsec.algorithm.DigestAlgorithm;
import org.opensaml.xmlsec.algorithm.SignatureAlgorithm;
import org.opensaml.xmlsec.algorithm.descriptors.DigestSHA1;
import org.opensaml.xmlsec.algorithm.descriptors.DigestSHA256;
import org.opensaml.xmlsec.algorithm.descriptors.SignatureRSASHA1;
import org.opensaml.xmlsec.algorithm.descriptors.SignatureRSASHA256;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.impl.SignatureImpl;
import org.slf4j.LoggerFactory;
import uk.gov.ida.hub.samlengine.builders.BuilderHelper;
import uk.gov.ida.saml.core.IdaSamlBootstrap;
import uk.gov.ida.saml.core.domain.PassthroughAssertion;
import uk.gov.ida.saml.core.test.OpenSAMLExtension;
import uk.gov.ida.saml.core.test.TestEntityIds;
import uk.gov.ida.saml.core.test.builders.SignatureBuilder;
import uk.gov.ida.saml.hub.domain.AuthnRequestFromRelyingParty;
import uk.gov.ida.saml.hub.domain.IdpIdaStatus;
import uk.gov.ida.saml.hub.domain.InboundResponseFromIdp;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.ida.hub.samlengine.builders.AuthnRequestFromRelyingPartyBuilder.anAuthnRequestFromRelyingParty;
import static uk.gov.ida.hub.samlengine.logging.Role.IDP;
import static uk.gov.ida.hub.samlengine.logging.Role.SP;
import static uk.gov.ida.saml.core.test.builders.AssertionBuilder.anAssertion;
import static uk.gov.ida.saml.core.test.builders.IssuerBuilder.anIssuer;

/**
 * UnknownMethodAlgorithmLoggerTest is used for testing UnknownMethodAlgorithmLogger
 * class and ensures that its methods perform properly.
 */
@ExtendWith(OpenSAMLExtension.class)
@ExtendWith(MockitoExtension.class)
public class UnknownMethodAlgorithmLoggerTest {
    private static final SignatureAlgorithm SIGNATURE_RSA_SHA256 = new SignatureRSASHA256();
    private static final SignatureAlgorithm SIGNATURE_RSA_SHA1 = new SignatureRSASHA1();
    private static final String SIGNATURE_RSA_SHA1_ID = SIGNATURE_RSA_SHA1.getURI();
    private static final DigestAlgorithm DIGEST_SHA256 = new DigestSHA256();
    private static final DigestAlgorithm DIGEST_SHA1 = new DigestSHA1();
    private static final String DIGEST_SHA1_ID = DIGEST_SHA1.getJCAAlgorithmID();

    private static final String ID = UUID.randomUUID().toString();
    private static final String IN_RESPONSE_TO = "Anyone";
    private static final String ISSUER_IDP = TestEntityIds.STUB_IDP_ONE;
    private static final String ISSUER_SP = TestEntityIds.TEST_RP;
    private static final DateTime ISSUE_INSTANT = new DateTime();
    private static final Optional<DateTime> NOT_ON_OR_AFTER = Optional.empty();
    private static final IdpIdaStatus STATUS = IdpIdaStatus.success();
    private static final Optional<PassthroughAssertion> AUTHN_STATEMENT_ASSERTION = Optional.empty();
    private static final Optional<PassthroughAssertion> MATCHING_DATASET_ASSERTION = Optional.empty();
    private static final URI DESTINATION = URI.create(TestEntityIds.HUB_ENTITY_ID);
    private static final String AUTHN_STATEMENT = "AuthnStatement";

    private static Optional<Signature> signature;
    private static Optional<Signature> signatureWithUnknownSignatureAlgorithm;
    private static Optional<Signature> signatureWithUnknownDigestAlgorithm;
    private static Optional<Signature> signatureWithUnknownSignatureAndDigestAlgorithms;

    @Mock
    private Appender<ILoggingEvent> mockAppender;

    @Captor
    private ArgumentCaptor<LoggingEvent> captorLoggingEvent;

    private static void verifyLog(
            final Appender<ILoggingEvent> mockAppender,
            final ArgumentCaptor<LoggingEvent> captorLoggingEvent,
            final int expectedNumOfInvocations,
            final String expectedLogMessage) {
        verify(mockAppender, times(expectedNumOfInvocations)).doAppend(captorLoggingEvent.capture());
        final LoggingEvent loggingEvent = captorLoggingEvent.getValue();
        assertThat(loggingEvent.getLevel()).isEqualTo(Level.INFO);
        assertThat(loggingEvent.getFormattedMessage()).isEqualTo(expectedLogMessage);
    }

    @BeforeEach
    public void setUp() throws Exception {
        final Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        logger.addAppender(mockAppender);
        logger.setLevel(Level.INFO);

        signature = Optional.of(SignatureBuilder.aSignature().build());
        SignatureImpl signatureImpl = ((SignatureImpl) signature.get());
        signatureImpl.setXMLSignature(BuilderHelper.createXMLSignature(SIGNATURE_RSA_SHA256, DIGEST_SHA256));

        signatureWithUnknownSignatureAlgorithm = Optional.of(SignatureBuilder.aSignature().withSignatureAlgorithm(SIGNATURE_RSA_SHA1).build());
        SignatureImpl signatureWithUnknownSignatureAlgorithmImpl = ((SignatureImpl) signatureWithUnknownSignatureAlgorithm.get());
        signatureWithUnknownSignatureAlgorithmImpl.setXMLSignature(BuilderHelper.createXMLSignature(SIGNATURE_RSA_SHA1, DIGEST_SHA256));

        signatureWithUnknownDigestAlgorithm = Optional.of(SignatureBuilder.aSignature().withDigestAlgorithm(ID, DIGEST_SHA1).build());
        SignatureImpl signatureWithUnknownDigestAlgorithmImpl = ((SignatureImpl) signatureWithUnknownDigestAlgorithm.get());
        signatureWithUnknownDigestAlgorithmImpl.setXMLSignature(BuilderHelper.createXMLSignature(SIGNATURE_RSA_SHA256, DIGEST_SHA1));

        signatureWithUnknownSignatureAndDigestAlgorithms = Optional.of(SignatureBuilder.aSignature().withSignatureAlgorithm(SIGNATURE_RSA_SHA1).withDigestAlgorithm(ID, DIGEST_SHA1).build());
        SignatureImpl signatureWithUnknownSignatureAndDigestAlgorithmsImpl = ((SignatureImpl) signatureWithUnknownSignatureAndDigestAlgorithms.get());
        signatureWithUnknownSignatureAndDigestAlgorithmsImpl.setXMLSignature(BuilderHelper.createXMLSignature(SIGNATURE_RSA_SHA1, DIGEST_SHA1));
    }

    @AfterEach
    public void tearDown() throws Exception {
        final Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        logger.detachAppender(mockAppender);
    }

    @Test
    public void shouldNotReportStrongAlgorithmsInIDPResponse() throws Exception {
        final InboundResponseFromIdp inboundResponseFromIdp = new InboundResponseFromIdp(
                ID,
                IN_RESPONSE_TO,
                ISSUER_IDP,
                ISSUE_INSTANT,
                NOT_ON_OR_AFTER,
                STATUS,
                signature,
                MATCHING_DATASET_ASSERTION,
                DESTINATION,
                AUTHN_STATEMENT_ASSERTION);

        UnknownMethodAlgorithmLogger.probeResponseForMethodAlgorithm(inboundResponseFromIdp);
        verify(mockAppender, times(0)).doAppend(captorLoggingEvent.capture());
    }

    @Test
    public void shouldReportUnknownSignatureAlgorithmInIDPResponse() throws Exception {
        InboundResponseFromIdp inboundResponseFromIdp = new InboundResponseFromIdp(
                ID,
                IN_RESPONSE_TO,
                ISSUER_IDP,
                ISSUE_INSTANT,
                NOT_ON_OR_AFTER,
                STATUS,
                signatureWithUnknownSignatureAlgorithm,
                MATCHING_DATASET_ASSERTION,
                DESTINATION,
                AUTHN_STATEMENT_ASSERTION);

        UnknownMethodAlgorithmLogger.probeResponseForMethodAlgorithm(inboundResponseFromIdp);

        verifyLog(mockAppender, captorLoggingEvent, 1,
                String.format(UnknownMethodAlgorithmLogger.SIGNATURE_ALGORITHM_MESSAGE,
                        IDP, SIGNATURE_RSA_SHA1_ID, Response.DEFAULT_ELEMENT_LOCAL_NAME));
    }

    @Test
    public void shouldReportUnknownDigestAlgorithmInIDPResponse() throws Exception {
        InboundResponseFromIdp inboundResponseFromIdp = new InboundResponseFromIdp(
                ID,
                IN_RESPONSE_TO,
                ISSUER_IDP,
                ISSUE_INSTANT,
                NOT_ON_OR_AFTER,
                STATUS,
                signatureWithUnknownDigestAlgorithm,
                MATCHING_DATASET_ASSERTION,
                DESTINATION,
                AUTHN_STATEMENT_ASSERTION);

        UnknownMethodAlgorithmLogger.probeResponseForMethodAlgorithm(inboundResponseFromIdp);

        verifyLog(mockAppender, captorLoggingEvent, 1,
                String.format(UnknownMethodAlgorithmLogger.DIGEST_ALGORITHM_MESSAGE,
                        IDP, DIGEST_SHA1_ID, Response.DEFAULT_ELEMENT_LOCAL_NAME));
    }

    @Test
    public void shouldReportUnknownSignatureAndDigestAlgorithmsInIDPResponse() throws Exception {
        InboundResponseFromIdp inboundResponseFromIdp = new InboundResponseFromIdp(
                ID,
                IN_RESPONSE_TO,
                ISSUER_IDP,
                ISSUE_INSTANT,
                NOT_ON_OR_AFTER,
                STATUS,
                signatureWithUnknownSignatureAndDigestAlgorithms,
                MATCHING_DATASET_ASSERTION,
                DESTINATION,
                AUTHN_STATEMENT_ASSERTION);

        UnknownMethodAlgorithmLogger.probeResponseForMethodAlgorithm(inboundResponseFromIdp);

        verifyLog(mockAppender, captorLoggingEvent, 1,
                String.format(UnknownMethodAlgorithmLogger.SIGNATURE_AND_DIGEST_ALGORITHMS_MESSAGE,
                        IDP, SIGNATURE_RSA_SHA1_ID, DIGEST_SHA1_ID, Response.DEFAULT_ELEMENT_LOCAL_NAME));
    }

    @Test
    public void shouldNotReportStrongAlgorithmsInIDPAssertion() throws Exception {
        Assertion authnStatementAssertion = anAssertion()
                .withIssuer(anIssuer().withIssuerId(ISSUER_IDP).build())
                .buildUnencrypted();

        UnknownMethodAlgorithmLogger.probeAssertionForMethodAlgorithm(authnStatementAssertion, AUTHN_STATEMENT);

        verify(mockAppender, times(0)).doAppend(captorLoggingEvent.capture());
    }

    @Test
    public void shouldReportUnknownSignatureAlgorithmInIDPAssertion() throws Exception {
        Assertion authnStatementAssertion = anAssertion()
                .withIssuer(anIssuer().withIssuerId(ISSUER_IDP).build())
                .withSignature(signatureWithUnknownSignatureAlgorithm.get())
                .buildUnencrypted();

        UnknownMethodAlgorithmLogger.probeAssertionForMethodAlgorithm(authnStatementAssertion, AUTHN_STATEMENT);

        verifyLog(mockAppender, captorLoggingEvent, 1,
                String.format(UnknownMethodAlgorithmLogger.SIGNATURE_ALGORITHM_MESSAGE,
                        IDP, SIGNATURE_RSA_SHA1_ID, AUTHN_STATEMENT + Assertion.DEFAULT_ELEMENT_LOCAL_NAME));

    }

    @Test
    public void shouldReportUnknownDigestAlgorithmInIDPAssertion() throws Exception {
        Assertion authnStatementAssertion = anAssertion()
                .withId(ID)
                .withIssuer(anIssuer().withIssuerId(ISSUER_IDP).build())
                .withSignature(signatureWithUnknownDigestAlgorithm.get())
                .buildUnencrypted();

        UnknownMethodAlgorithmLogger.probeAssertionForMethodAlgorithm(authnStatementAssertion, AUTHN_STATEMENT);

        verifyLog(mockAppender, captorLoggingEvent, 1,
                String.format(UnknownMethodAlgorithmLogger.DIGEST_ALGORITHM_MESSAGE,
                        IDP, DIGEST_SHA1_ID, AUTHN_STATEMENT + Assertion.DEFAULT_ELEMENT_LOCAL_NAME));
    }

    @Test
    public void shouldReportUnknownSignatureAndDigestAlgorithmsInIDPAssertion() throws Exception {
        Assertion authnStatementAssertion = anAssertion()
                .withId(ID)
                .withIssuer(anIssuer().withIssuerId(ISSUER_IDP).build())
                .withSignature(signatureWithUnknownSignatureAndDigestAlgorithms.get())
                .buildUnencrypted();

        UnknownMethodAlgorithmLogger.probeAssertionForMethodAlgorithm(authnStatementAssertion, AUTHN_STATEMENT);

        verifyLog(mockAppender, captorLoggingEvent, 1,
                String.format(UnknownMethodAlgorithmLogger.SIGNATURE_AND_DIGEST_ALGORITHMS_MESSAGE,
                        IDP, SIGNATURE_RSA_SHA1_ID, DIGEST_SHA1_ID, AUTHN_STATEMENT + Assertion.DEFAULT_ELEMENT_LOCAL_NAME));
    }

    @Test
    public void shouldNotReportStrongAlgorithmsInSPAuthnRequest() throws Exception {
        AuthnRequestFromRelyingParty authnRequestFromRelyingParty = anAuthnRequestFromRelyingParty()
                .withId(ID)
                .withIssuer(ISSUER_SP)
                .withSignature(signature.get())
                .build();

        UnknownMethodAlgorithmLogger.probeAuthnRequestForMethodAlgorithm(authnRequestFromRelyingParty);

        verify(mockAppender, times(0)).doAppend(captorLoggingEvent.capture());
    }

    @Test
    public void shouldReportUnknownSignatureAlgorithmInSPAuthnRequest() throws Exception {
        AuthnRequestFromRelyingParty authnRequestFromRelyingParty = anAuthnRequestFromRelyingParty()
                .withId(ID)
                .withIssuer(ISSUER_SP)
                .withSignature(signatureWithUnknownSignatureAlgorithm.get())
                .build();

        UnknownMethodAlgorithmLogger.probeAuthnRequestForMethodAlgorithm(authnRequestFromRelyingParty);

        verifyLog(mockAppender, captorLoggingEvent, 1,
                String.format(UnknownMethodAlgorithmLogger.SIGNATURE_ALGORITHM_MESSAGE,
                        SP, SIGNATURE_RSA_SHA1_ID, AuthnRequest.DEFAULT_ELEMENT_LOCAL_NAME));
    }

    @Test
    public void shouldReportUnknownDigestAlgorithmInSPAuthnRequest() throws Exception {
        AuthnRequestFromRelyingParty authnRequestFromRelyingParty = anAuthnRequestFromRelyingParty()
                .withId(ID)
                .withIssuer(ISSUER_SP)
                .withSignature(signatureWithUnknownDigestAlgorithm.get())
                .build();

        UnknownMethodAlgorithmLogger.probeAuthnRequestForMethodAlgorithm(authnRequestFromRelyingParty);

        verifyLog(mockAppender, captorLoggingEvent, 1,
                String.format(UnknownMethodAlgorithmLogger.DIGEST_ALGORITHM_MESSAGE,
                        SP, DIGEST_SHA1_ID, AuthnRequest.DEFAULT_ELEMENT_LOCAL_NAME));
    }

    @Test
    public void shouldReportUnknownSignatureAndDigestAlgorithmsInSPAuthnRequest() throws Exception {
        AuthnRequestFromRelyingParty authnRequestFromRelyingParty = anAuthnRequestFromRelyingParty()
                .withId(ID)
                .withIssuer(ISSUER_SP)
                .withSignature(signatureWithUnknownSignatureAndDigestAlgorithms.get())
                .build();

        UnknownMethodAlgorithmLogger.probeAuthnRequestForMethodAlgorithm(authnRequestFromRelyingParty);

        verifyLog(mockAppender, captorLoggingEvent, 1,
                String.format(UnknownMethodAlgorithmLogger.SIGNATURE_AND_DIGEST_ALGORITHMS_MESSAGE,
                        SP, SIGNATURE_RSA_SHA1_ID, DIGEST_SHA1_ID, AuthnRequest.DEFAULT_ELEMENT_LOCAL_NAME));
    }
}
