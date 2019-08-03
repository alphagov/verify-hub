package uk.gov.ida.hub.samlengine.logging;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.opensaml.saml.saml2.core.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotOnOrAfterLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotOnOrAfterLogger.class);
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("dd/MM/YYYY HH:mm:ss z");

    public static void logAssertionNotOnOrAfter(final Assertion assertion, String typeOfAssertion) {
        final String idp = assertion.getIssuer().getValue();
        assertion.getSubject().getSubjectConfirmations()
                .forEach(subjectConfirmation -> {
                    DateTime notOnOrAfter = subjectConfirmation.getSubjectConfirmationData().getNotOnOrAfter();
                    LOGGER.info(String.format("NotOnOrAfter in %s from %s is set to %s", typeOfAssertion, idp, notOnOrAfter.toString(dateTimeFormatter)));
                });
    }
}
