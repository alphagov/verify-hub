package uk.gov.ida.hub.samlengine.logging;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import org.joda.time.DateTime;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.SubjectConfirmation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IdpAssertionMetricsCollector {

    private static final String NOT_ON_OR_AFTER = "notOnOrAfter.";
    private final MetricRegistry metricRegistry;

    private final Map<String, Long> notOnOrAfterLatestMax = new HashMap<>();

    public IdpAssertionMetricsCollector(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    public void update(Assertion assertion) {
        String idpName = replaceNonAlphaNumericCharacters(assertion);
        String metricName = NOT_ON_OR_AFTER + idpName;
        Long maxNotOnOrAfter = findMaxNotOnOrAfter(assertion.getSubject().getSubjectConfirmations(), notOnOrAfterLatestMax.get(metricName));
        notOnOrAfterLatestMax.put(metricName, maxNotOnOrAfter);
        updateMetricRegistry(metricName);
    }

    private String replaceNonAlphaNumericCharacters(Assertion assertion) {
        return assertion.getIssuer().getValue().replaceAll("[^A-Za-z0-9]", "_");
    }

    private void updateMetricRegistry(String metricName) {
        if (!metricRegistry.getGauges().containsKey(metricName)) {
            Gauge<Long> notOnOrAfterGauge = () -> notOnOrAfterLatestMax.get(metricName);
            metricRegistry.register(metricName, notOnOrAfterGauge);
        }
    }

    private Long findMaxNotOnOrAfter(List<SubjectConfirmation> subjectConfirmations, Long currentNotOnOrAfter) {
        DateTime now = DateTime.now();
        Long maxNotOnOrAfter = subjectConfirmations.stream()
                .map(subjectConfirmation -> subjectConfirmation.getSubjectConfirmationData().getNotOnOrAfter().getMillis() - now.getMillis())
                .map(millis -> millis / (60 * 1000))
                .max(Long::compareTo)
                .get();
        return (currentNotOnOrAfter != null && currentNotOnOrAfter > maxNotOnOrAfter) ? currentNotOnOrAfter : maxNotOnOrAfter;
    }
}
