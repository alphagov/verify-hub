package uk.gov.ida.hub.samlengine.logging;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.logging.async.AsyncLoggingEventAppenderFactory;
import io.dropwizard.logging.filter.ThresholdLevelFilterFactory;
import io.dropwizard.logging.layout.DropwizardLayoutFactory;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class SplunkAppenderFactoryTest {

    private static ObjectMapper mapper = new ObjectMapper();
    private static JSONObject jsonSplunkFactory;

    @Before
    public void setup() {
        jsonSplunkFactory = new JSONObject();
        jsonSplunkFactory.put("type", "splunk");
        jsonSplunkFactory.put("url", "http://example.com");
        jsonSplunkFactory.put("token", "myToken");
        jsonSplunkFactory.put("source", "mySource");
        jsonSplunkFactory.put("sourceType", "mySourceType");
    }

    @Test
    public void canBeInstantiatedFromJson() throws Exception {
        mapper.readValue(jsonSplunkFactory.toString(), SplunkAppenderFactory.class);
    }

    @Test
    public void isNotValidWithoutRequiredFields() throws Exception {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        String[] requiredFields = {"url", "token", "source", "sourceType"};

        for (String field : requiredFields) {
            JSONObject jsonWithMissingField = new JSONObject(jsonSplunkFactory, JSONObject.getNames(jsonSplunkFactory));
            jsonWithMissingField.put(field, JSONObject.NULL);

            SplunkAppenderFactory appenderFactory = mapper.readValue(jsonWithMissingField.toString(), SplunkAppenderFactory.class);
            Set<ConstraintViolation<SplunkAppenderFactory>> violations = validator.validate(appenderFactory);

            assertThat(violations.size()).isEqualTo(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("may not be null");
        }
    }

    @Test
    public void buildReturnsAnAsyncAppender() throws Exception {
        SplunkAppenderFactory splunkAppenderFactory = mapper.readValue(jsonSplunkFactory.toString(), SplunkAppenderFactory.class);
        Appender<ILoggingEvent> appender = splunkAppenderFactory.build(
            new LoggerContext(),
            "appName",
            new DropwizardLayoutFactory(),
            new ThresholdLevelFilterFactory(),
            new AsyncLoggingEventAppenderFactory()
        );

        assertThat(appender).isExactlyInstanceOf(AsyncAppender.class);
    }
}
