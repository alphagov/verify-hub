package uk.gov.ida.integrationtest.hub.policy.apprule;

import helpers.JerseyClientConfigurationBuilder;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.util.Duration;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.builder.SamlAuthnRequestContainerDtoBuilder;
import uk.gov.ida.hub.policy.contracts.SamlResponseWithAuthnRequestInformationDto;
import uk.gov.ida.hub.policy.domain.IdpSelected;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SamlAuthnRequestContainerDto;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.proxy.SamlResponseWithAuthnRequestInformationDtoBuilder;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.ConfigStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.EventSinkStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.PolicyAppRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.SamlEngineStubRule;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.text.MessageFormat.format;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.common.ExceptionType.SESSION_TIMEOUT;
import static uk.gov.ida.saml.core.test.TestEntityIds.STUB_IDP_ONE;

public class SessionTimeoutIntegrationTests {
    private static final int SOME_TIMEOUT = 10;
    private static final DateTime SOME_TIME = new DateTime(2013, 5, 30, 12, 0);
    private static final String THE_TX_ID = "the-tx-id";
    private static Client client;
    private static final boolean REGISTERING = true;
    private static final LevelOfAssurance REQUESTED_LOA = LevelOfAssurance.LEVEL_2;

    @ClassRule
    public static SamlEngineStubRule samlEngineStub = new SamlEngineStubRule();

    @ClassRule
    public static ConfigStubRule configStub = new ConfigStubRule();

    @ClassRule
    public static EventSinkStubRule eventSinkStub = new EventSinkStubRule();

    @ClassRule
    public static PolicyAppRule policy = new PolicyAppRule(
            ConfigOverride.config("samlEngineUri", samlEngineStub.baseUri().build().toASCIIString()),
            ConfigOverride.config("configUri", configStub.baseUri().build().toASCIIString()),
            ConfigOverride.config("eventSinkUri", eventSinkStub.baseUri().build().toASCIIString()),
            ConfigOverride.config("timeoutPeriod", format("{0}m", SOME_TIMEOUT)));

    private SamlAuthnRequestContainerDto samlRequest;

    @BeforeClass
    public static void beforeClass() {
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder.aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        client = new JerseyClientBuilder(policy.getEnvironment()).using(jerseyClientConfiguration).build(SessionTimeoutIntegrationTests.class.getSimpleName());
    }

    @Before
    public void setUp() throws Exception {
        SamlResponseWithAuthnRequestInformationDto samlResponse = SamlResponseWithAuthnRequestInformationDtoBuilder.aSamlResponseWithAuthnRequestInformationDto().withIssuer(THE_TX_ID).build();
        samlRequest = SamlAuthnRequestContainerDtoBuilder.aSamlAuthnRequestContainerDto().build();

        samlEngineStub.setupStubForAuthnRequestTranslate(samlResponse);
        configStub.setUpStubForLevelsOfAssurance(samlResponse.getIssuer());
        eventSinkStub.setupStubForLogging();
        configStub.setUpStubForAssertionConsumerServiceUri(samlResponse.getIssuer());
        configStub.setupStubForEidasEnabledForTransaction(THE_TX_ID, false);
    }

    @After
    public void tearDown() {
        DateTimeFreezer.unfreezeTime();
    }

    @Test
    public void selectIdpShouldReturnErrorWhenSessionHasTimedOut() {
        DateTimeFreezer.freezeTime(SOME_TIME);
        SessionId sessionId = client
                .target(policy.uri(Urls.PolicyUrls.NEW_SESSION_RESOURCE)).request()
                .post(Entity.entity(samlRequest, MediaType.APPLICATION_JSON_TYPE), SessionId.class);

        DateTimeFreezer.freezeTime(SOME_TIME.plusMinutes(SOME_TIMEOUT + 1));
        URI uri = UriBuilder
                .fromPath(Urls.PolicyUrls.AUTHN_REQUEST_SELECT_IDP_RESOURCE)
                .buildFromEncoded(sessionId);

        confirmError(policy.uri(uri.getPath()), new IdpSelected(STUB_IDP_ONE, "some-ip-address", REGISTERING, REQUESTED_LOA, "this-is-an-analytics-session-id", "this-is-a-journey-type"),
                SESSION_TIMEOUT);
    }

    @Test
    public void selectIdpShouldReturnErrorWhenSessionDoesNotExistInPolicy() {
        final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        SessionId sessionId = SessionId.createNewSessionId();

        URI uri = UriBuilder
                .fromPath(Urls.PolicyUrls.AUTHN_REQUEST_SELECT_IDP_RESOURCE)
                .buildFromEncoded(sessionId);

        confirmError(policy.uri(uri.getPath()), new IdpSelected(STUB_IDP_ONE, "some-ip-address", REGISTERING, REQUESTED_LOA, "this-is-an-analytics-session-id", "this-is-a-journey-type"), ExceptionType
                .SESSION_NOT_FOUND);

        assertThatEventEmitterWritesToStandardOutput(outContent);
        System.setOut(System.out);
    }

    private void assertThatEventEmitterWritesToStandardOutput(ByteArrayOutputStream outContent) {
        Pattern p = Pattern.compile("Event ID: [0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[34][0-9a-fA-F]{3}-[89ab][0-9a-fA-F]{3}-[0-9a-fA-F]{12}, Timestamp: [0-9]+-[0-1][0-9]-[0-3][0-9]T[0-9][0-9]:[0-9][0-9]:[0-9][0-9].[0-9][0-9][0-9](Z|[+-](?:2[0-3]|[01][0-9]):[0-5][0-9]), Event Type: error_event");
        Matcher m = p.matcher(outContent.toString());
        assertThat(m.find()).isTrue();
    }

    private void confirmError(URI uri, Object entity, ExceptionType exceptionType) {
        Response response = post(uri, entity);
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(response.readEntity(ErrorStatusDto.class).getExceptionType()).isEqualTo(exceptionType);
    }

    private Response post(URI uri, Object entity) {
        return client.target(uri).request().post(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE));
    }
}
