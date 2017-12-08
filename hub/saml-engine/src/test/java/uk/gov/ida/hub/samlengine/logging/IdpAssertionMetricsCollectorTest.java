package uk.gov.ida.hub.samlengine.logging;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensaml.saml.saml2.core.Assertion;
import uk.gov.ida.saml.core.IdaSamlBootstrap;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;

import java.util.SortedMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ida.saml.core.test.builders.AssertionBuilder.anAssertion;
import static uk.gov.ida.saml.core.test.builders.IssuerBuilder.anIssuer;
import static uk.gov.ida.saml.core.test.builders.SubjectBuilder.aSubject;
import static uk.gov.ida.saml.core.test.builders.SubjectConfirmationBuilder.aSubjectConfirmation;
import static uk.gov.ida.saml.core.test.builders.SubjectConfirmationDataBuilder.aSubjectConfirmationData;

public class IdpAssertionMetricsCollectorTest {

    @BeforeClass
    public static void setUp() {
        IdaSamlBootstrap.bootstrap();
    }

    @After
    public void tearDown() {
        DateTimeFreezer.unfreezeTime();
    }

    @Test
    public void shouldRegisterIdpNameFromAssertion() {
        MetricRegistry metricRegistry = new MetricRegistry();
        IdpAssertionMetricsCollector idpAssertionMetricsCollector = new IdpAssertionMetricsCollector(metricRegistry);
        Assertion anAssertion = anAssertion()
                .withIssuer(anIssuer().withIssuerId("testIdP").build())
                .buildUnencrypted();

        idpAssertionMetricsCollector.update(anAssertion);

        assertThat(metricRegistry.getGauges().keySet()).contains("notOnOrAfter.testIdP");
    }

    @Test
    public void shouldNotRegisterIdpAlreadyExist() {
        MetricRegistry metricRegistry = mock(MetricRegistry.class);
        SortedMap<String, Gauge> gaugeMock= mock(SortedMap.class);
        when(gaugeMock.containsKey(any())).thenReturn(true);
        when(metricRegistry.getGauges()).thenReturn(gaugeMock);
        Assertion assertion = anAssertion()
                .withIssuer(anIssuer().withIssuerId("testIdP").build())
                .buildUnencrypted();
        IdpAssertionMetricsCollector idpAssertionMetricsCollector = new IdpAssertionMetricsCollector(metricRegistry);

        idpAssertionMetricsCollector.update(assertion);

        verify(metricRegistry, times(0)).register(any(), any());
    }

    @Test
    public void shouldCollectNotOnOrAfterValueFromAssertion() {
        DateTimeFreezer.freezeTime();
        MetricRegistry metricRegistry = new MetricRegistry();
        IdpAssertionMetricsCollector idpAssertionMetricsCollector = new IdpAssertionMetricsCollector(metricRegistry);
        DateTime notOnOrAfter = DateTime.now().plusMinutes(15);
        Assertion anAssertion = anAssertion()
                .withIssuer(anIssuer().withIssuerId("testIdP").build())
                .withSubject(aSubject().withSubjectConfirmation(aSubjectConfirmation()
                        .withSubjectConfirmationData(aSubjectConfirmationData()
                                .withNotOnOrAfter(notOnOrAfter)
                                .build())
                        .build())
                        .build())
                .buildUnencrypted();

        idpAssertionMetricsCollector.update(anAssertion);

        Gauge actual = metricRegistry.getGauges().get("notOnOrAfter.testIdP");
        assertThat(actual.getValue()).isEqualTo(15L);
    }

    @Test
    public void shouldGetMaxInNotOnOrAfterFromSubjectConfirmations() {
        DateTimeFreezer.freezeTime();
        MetricRegistry metricRegistry = new MetricRegistry();
        IdpAssertionMetricsCollector idpAssertionMetricsCollector = new IdpAssertionMetricsCollector(metricRegistry);
        DateTime notOnOrAfterSmaller = DateTime.now().plusMinutes(15);
        DateTime notOnOrAfterBigger = DateTime.now().plusMinutes(30);
        Assertion anAssertion = anAssertion()
                .withIssuer(anIssuer().withIssuerId("testIdP").build())
                .withSubject(aSubject()
                        .withSubjectConfirmation(aSubjectConfirmation()
                        .withSubjectConfirmationData(aSubjectConfirmationData()
                                .withNotOnOrAfter(notOnOrAfterSmaller)
                                .build())
                        .build())
                        .withSubjectConfirmation(aSubjectConfirmation()
                        .withSubjectConfirmationData(aSubjectConfirmationData()
                                .withNotOnOrAfter(notOnOrAfterBigger)
                                .build())
                        .build())
                        .build())
                .buildUnencrypted();

        idpAssertionMetricsCollector.update(anAssertion);

        Gauge actual = metricRegistry.getGauges().get("notOnOrAfter.testIdP");
        assertThat(actual.getValue()).isEqualTo(30L);
    }
}
