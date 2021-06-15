package uk.gov.ida.hub.config.domain;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import uk.gov.ida.hub.config.dto.FederationEntityType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.saml.core.test.PemCertificateStrings.HUB_TEST_PUBLIC_SIGNING_CERT;

public class CertificateTest {

    @Test
    public void logsInvalidCertificate(){
        Logger logger = (Logger) LoggerFactory.getLogger(Certificate.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        new Certificate("entityId", FederationEntityType.RP, "ThisIsABadCertValue", CertificateUse.SIGNING, CertificateOrigin.FEDERATION, true);

        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList.get(0).getLevel().toString()).isEqualTo(Level.ERROR.toString());
    }

    @Test
    public void doesNotLogValidCertificate(){
        Logger logger = (Logger) LoggerFactory.getLogger(Certificate.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        new Certificate("entityId", FederationEntityType.RP, HUB_TEST_PUBLIC_SIGNING_CERT, CertificateUse.SIGNING, CertificateOrigin.FEDERATION, true);

        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList.isEmpty()).isTrue();
    }

}