package uk.gov.ida.hub.config.domain;


import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import uk.gov.ida.common.shared.security.verification.CertificateChainValidator;
import uk.gov.ida.common.shared.security.verification.CertificateValidity;
import uk.gov.ida.hub.config.dto.FederationEntityType;
import uk.gov.ida.hub.config.truststore.TrustStoreForCertificateProvider;

import java.security.KeyStore;
import java.security.cert.CertPathValidatorException;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.config.domain.builders.TransactionConfigBuilder.aTransactionConfigData;

@RunWith(MockitoJUnitRunner.class)
public class CertificateChainConfigValidatorTest {

    @Mock
    private TrustStoreForCertificateProvider trustStoreForCertificateProvider;

    @Mock
    private CertificateChainValidator certificateChainValidator;

    @Mock
    private KeyStore trustStore;

    private CertificateChainConfigValidator validator;

    @Before
    public void setUp() {
        when(trustStoreForCertificateProvider.getTrustStoreFor(FederationEntityType.RP)).thenReturn(trustStore);

        validator = new CertificateChainConfigValidator(trustStoreForCertificateProvider, certificateChainValidator);
    }

    @Test
    public void validateLogsErrorForMissingCertificatesWhenNotSelfService() {
        Logger logger = (Logger) LoggerFactory.getLogger(CertificateChainConfigValidator.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        TransactionConfig config = aTransactionConfigData().withEncryptionCertificate("").withSelfService(false).build();
        Certificate signingCert = config.getSignatureVerificationCertificates().stream().findFirst().get();

        when(certificateChainValidator.validate(signingCert.getX509Certificate().get(), trustStore)).thenReturn(CertificateValidity.valid());

        validator.validate(Set.of(config));

        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList.get(0).getLevel().toString()).isEqualTo(Level.ERROR.toString());

    }

    @Test
    public void validateLogsWarningForX509CertsThatFailChainValidation() {
        Logger logger = (Logger) LoggerFactory.getLogger(CertificateChainConfigValidator.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        TransactionConfig config = aTransactionConfigData().withSelfService(false).build();
        Certificate encryptionCert = config.getEncryptionCertificate();
        Certificate signingCert = config.getSignatureVerificationCertificates().stream().findFirst().get();

        CertPathValidatorException certPathValidatorException = new CertPathValidatorException("Bad Cert Chain");

        when(certificateChainValidator.validate(encryptionCert.getX509Certificate().get(), trustStore)).thenReturn(CertificateValidity.invalid(certPathValidatorException));
        when(certificateChainValidator.validate(signingCert.getX509Certificate().get(), trustStore)).thenReturn(CertificateValidity.valid());

        validator.validate(Set.of(config));

        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList.get(0).getLevel().toString()).isEqualTo(Level.WARN.toString());
        assertThat(logsList.get(0).getMessage()).contains("Bad Cert Chain");

    }

}