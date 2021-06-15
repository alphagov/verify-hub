package uk.gov.ida.hub.samlengine.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensaml.saml.saml2.core.Assertion;
import org.slf4j.LoggerFactory;
import uk.gov.ida.saml.core.test.OpenSAMLExtension;
import uk.gov.ida.saml.core.test.TestEntityIds;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.ida.saml.core.test.builders.AssertionBuilder.anAssertion;
import static uk.gov.ida.saml.core.test.builders.IssuerBuilder.anIssuer;
import static uk.gov.ida.saml.core.test.builders.SubjectBuilder.aSubject;
import static uk.gov.ida.saml.core.test.builders.SubjectConfirmationBuilder.aSubjectConfirmation;
import static uk.gov.ida.saml.core.test.builders.SubjectConfirmationDataBuilder.aSubjectConfirmationData;

@ExtendWith(OpenSAMLExtension.class)
@ExtendWith(MockitoExtension.class)
public class NotOnOrAfterLoggerTest {

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("dd/MM/YYYY HH:mm:ss z");
    private static final String ISSUER_IDP = TestEntityIds.STUB_IDP_ONE;

    @Mock
    private Appender<ILoggingEvent> mockAppender;

    @Captor
    private ArgumentCaptor<LoggingEvent> captorLoggingEvent;

    @BeforeEach
    public void setUp() {
        final Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        logger.addAppender(mockAppender);
        logger.setLevel(Level.INFO);
    }

    @AfterEach
    public void tearDown() {
        final Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        logger.detachAppender(mockAppender);
    }

    @Test
    public void shouldLogNotOnOrAfterWithIdp() {

        DateTime notOnOrAfter = DateTime.now().withZone(DateTimeZone.UTC).plusHours(1);
        Assertion assertion = anAssertionWithNotOnOrAfter(notOnOrAfter);

        String typeOfAssertion = "assertionType";
        NotOnOrAfterLogger.logAssertionNotOnOrAfter(assertion, typeOfAssertion);

        String expectedMessage = String.format("NotOnOrAfter in %s from %s is set to %s", typeOfAssertion, ISSUER_IDP, notOnOrAfter.toString(dateTimeFormatter));
        verifyLog(mockAppender, captorLoggingEvent, expectedMessage);
    }

    private Assertion anAssertionWithNotOnOrAfter(DateTime notOnOrAfter) {
        return anAssertion()
                .withIssuer(anIssuer().withIssuerId(ISSUER_IDP).build())
                .withSubject(
                        aSubject()
                                .withSubjectConfirmation(
                                        aSubjectConfirmation()
                                                .withSubjectConfirmationData(
                                                        aSubjectConfirmationData()
                                                                .withNotOnOrAfter(notOnOrAfter)
                                                                .build())
                                                .build()
                                )
                                .build()
                )
                .buildUnencrypted();
    }

    private static void verifyLog(
            final Appender<ILoggingEvent> mockAppender,
            final ArgumentCaptor<LoggingEvent> captorLoggingEvent,
            final String expectedMessage) {
        verify(mockAppender, times(1)).doAppend(captorLoggingEvent.capture());
        final LoggingEvent loggingEvent = captorLoggingEvent.getValue();

        assertThat(loggingEvent.getLevel()).isEqualTo(Level.INFO);
        assertThat(loggingEvent.getFormattedMessage()).isEqualTo(expectedMessage);
    }
}
