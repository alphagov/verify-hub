package uk.gov.ida.integrationtest.hub.samlengine.apprule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import httpstub.HttpStubRule;
import httpstub.RecordedRequest;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppRule;

import java.io.IOException;

import static io.dropwizard.testing.ConfigOverride.config;
import static org.assertj.core.api.Assertions.assertThat;

public class SplunkLoggerProxyTest {

    private static final String TOKEN = "SomeToken";
    private static final String SOURCE = "SplunkLoggerTest";
    private static final String SOURCE_TYPE = "IntegrationTest";
    private static final String INDEX = "SplunkIndex";
    private static final String SPLUNK_URL = "http://splunk.example.com";
    private static final String SPLUNK_EVENT_ENDPOINT = "/services/collector/event/1.0";

    @ClassRule
    public static HttpStubRule proxyStub = new HttpStubRule();

    @ClassRule
    public static SamlEngineAppRule samlEngineAppRule = new SamlEngineAppRule(
        proxyStub.baseUri().build().getHost(),
        Integer.toString(proxyStub.getPort()),
        config("logging.appenders[0].type", "splunk"),
        config("logging.appenders[0].url", SPLUNK_URL),
        config("logging.appenders[0].token", TOKEN),
        config("logging.appenders[0].source", SOURCE),
        config("logging.appenders[0].sourceType", SOURCE_TYPE),
        config("logging.appenders[0].index", INDEX)
    );

    @AfterClass
    public static void clearSystemProxyProperties() {
        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");
    }

    @Test
    public void shouldSendLogsViaProxyIfConfigured() throws IOException {
        RecordedRequest lastRequest = proxyStub.getLastRequest();
        JsonNode requestBody = new ObjectMapper().readTree(lastRequest.getEntityBytes());

        assertThat(lastRequest.getUrl()).isEqualTo(SPLUNK_URL + SPLUNK_EVENT_ENDPOINT);
        assertThat(lastRequest.getHeader("Authorization")).contains(TOKEN);
        assertThat(requestBody.get("source").asText()).isEqualTo(SOURCE);
        assertThat(requestBody.get("sourcetype").asText()).isEqualTo(SOURCE_TYPE);
        assertThat(requestBody.get("index").asText()).isEqualTo(INDEX);
    }
}
