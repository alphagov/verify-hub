package uk.gov.ida.integrationtest.hub.samlengine.apprule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import httpstub.HttpStubRule;
import httpstub.RecordedRequest;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppRule;

import java.io.IOException;

import static io.dropwizard.testing.ConfigOverride.config;
import static javax.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;

public class SplunkLoggerTest {

    private static final String SOURCE_TYPE = "IntegrationTest";
    private static final String TOKEN = "SomeToken";
    private static final String SOURCE = "SplunkLoggerTest";

    private static HttpStubRule splunkStub = new HttpStubRule();
    public static SamlEngineAppRule samlEngineAppRule = new SamlEngineAppRule(
            config("logging.appenders[0].type", "splunk"),
            config("logging.appenders[0].url", () -> String.valueOf(splunkStub.baseUri())),
            config("logging.appenders[0].token", TOKEN),
            config("logging.appenders[0].source", SOURCE),
            config("logging.appenders[0].sourceType", SOURCE_TYPE)
    );

    @ClassRule
    public static RuleChain rules = RuleChain.outerRule(splunkStub).around(samlEngineAppRule);

    @Before
    public void setUp() {
        splunkStub.register("/services/collector/event/1.0", OK.getStatusCode());
    }

    @Test
    public void shouldSendLogsToSplunkWithCorrectContent() throws IOException {
        RecordedRequest lastRequest = splunkStub.getLastRequest();
        assertThat(lastRequest.getHeader("Authorization")).contains(TOKEN);
        JsonNode requestBody = new ObjectMapper().readTree(lastRequest.getEntityBytes());
        assertThat(requestBody.get("source").asText()).isEqualTo(SOURCE);
        assertThat(requestBody.get("sourcetype").asText()).isEqualTo(SOURCE_TYPE);
    }
}
