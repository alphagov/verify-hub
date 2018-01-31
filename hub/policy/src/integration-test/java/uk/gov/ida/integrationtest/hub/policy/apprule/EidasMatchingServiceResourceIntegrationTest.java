package uk.gov.ida.integrationtest.hub.policy.apprule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import helpers.JerseyClientConfigurationBuilder;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.util.Duration;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.builder.SamlAuthnRequestContainerDtoBuilder;
import uk.gov.ida.hub.policy.contracts.AttributeQueryContainerDto;
import uk.gov.ida.hub.policy.contracts.InboundResponseFromMatchingServiceDto;
import uk.gov.ida.hub.policy.contracts.SamlResponseDto;
import uk.gov.ida.hub.policy.contracts.SamlResponseWithAuthnRequestInformationDto;
import uk.gov.ida.hub.policy.domain.EidasCountryDto;
import uk.gov.ida.hub.policy.domain.IdpIdaStatus;
import uk.gov.ida.hub.policy.domain.InboundResponseFromCountry;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.MatchingServiceIdaStatus;
import uk.gov.ida.hub.policy.domain.SamlAuthnRequestContainerDto;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.EidasAwaitingCycle3DataState;
import uk.gov.ida.hub.policy.domain.state.EidasSuccessfulMatchState;
import uk.gov.ida.hub.policy.domain.state.NoMatchState;
import uk.gov.ida.hub.policy.proxy.SamlResponseWithAuthnRequestInformationDtoBuilder;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.ConfigStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.EventSinkStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.PolicyAppRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.SamlEngineStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.SamlSoapProxyProxyStubRule;
import uk.gov.ida.integrationtest.hub.policy.apprule.support.TestSessionResourceHelper;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Arrays;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.integrationtest.hub.policy.apprule.support.TestSessionResource.GET_SESSION_STATE_NAME;
import static uk.gov.ida.integrationtest.hub.policy.builders.SamlAuthnResponseContainerDtoBuilder.aSamlAuthnResponseContainerDto;

public class EidasMatchingServiceResourceIntegrationTest {
    @ClassRule
    public static SamlEngineStubRule samlEngineStub = new SamlEngineStubRule();

    @ClassRule
    public static ConfigStubRule configStub = new ConfigStubRule();

    @ClassRule
    public static EventSinkStubRule eventSinkStub = new EventSinkStubRule();

    @ClassRule
    public static SamlSoapProxyProxyStubRule samlSoapProxyProxyStub = new SamlSoapProxyProxyStubRule();

    @ClassRule
    public static PolicyAppRule policy = new PolicyAppRule(
        ConfigOverride.config("samlEngineUri", samlEngineStub.baseUri().build().toASCIIString()),
        ConfigOverride.config("samlSoapProxyUri", samlSoapProxyProxyStub.baseUri().build().toASCIIString()),
        ConfigOverride.config("configUri", configStub.baseUri().build().toASCIIString()),
        ConfigOverride.config("eventSinkUri", eventSinkStub.baseUri().build().toASCIIString()),
        ConfigOverride.config("eidas", "true"));


    private static Client client;
    private static String TEST_SESSION_RESOURCE_PATH = Urls.PolicyUrls.POLICY_ROOT + "test";
    private static final String MSA_ENTITY_ID = "msaEntityId";
    private static final String RP_ENTITY_ID = "rpEntityId";
    private static final EidasCountryDto NETHERLANDS = new EidasCountryDto("http://netherlandsEnitity.nl", "NL", true);
    private static final EidasCountryDto SPAIN = new EidasCountryDto("http://spainEnitity.es", "ES", true);
    private static final ImmutableList<EidasCountryDto> EIDAS_COUNTRIES = ImmutableList.of(NETHERLANDS, SPAIN);

    private SamlResponseWithAuthnRequestInformationDto translatedAuthnRequest;
    private SamlAuthnRequestContainerDto rpSamlRequest;

    @BeforeClass
    public static void beforeClass() {
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder.aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        client = new JerseyClientBuilder(policy.getEnvironment())
            .using(jerseyClientConfiguration)
            .build(EidasMatchingServiceResourceIntegrationTest.class.getSimpleName());
    }

    @Before
    public void setUp() throws Exception {
        translatedAuthnRequest = SamlResponseWithAuthnRequestInformationDtoBuilder.aSamlResponseWithAuthnRequestInformationDto().withIssuer(RP_ENTITY_ID).build();
        rpSamlRequest = SamlAuthnRequestContainerDtoBuilder.aSamlAuthnRequestContainerDto().build();

        configStub.setupStubForEidasCountries(EIDAS_COUNTRIES);
        configStub.setUpStubForMatchingServiceRequest(RP_ENTITY_ID, MSA_ENTITY_ID, true);
        configStub.setupStubForEidasEnabledForTransaction(RP_ENTITY_ID, true);
        configStub.setUpStubForLevelsOfAssurance(RP_ENTITY_ID);
        enableCountriesForRp(RP_ENTITY_ID, NETHERLANDS, SPAIN);

        eventSinkStub.setupStubForLogging();

        stubSamlEngineTranslationLOAForCountry(LevelOfAssurance.LEVEL_2, NETHERLANDS);
        stubSamlEngineGenerationOfAQR();
    }

    @Test
    public void shouldTransitionToEidasSuccessfulMatchStateWhenMatchIsReceivedForEidasCycle0And1() throws Exception {
        final SessionId sessionId = aSessionIsCreated();
        selectACountry(sessionId, NETHERLANDS);
        samlSoapProxyProxyStub.setUpStubForSendHubMatchingServiceRequest(sessionId);
        postAuthnResponseToPolicy(sessionId);

        final InboundResponseFromMatchingServiceDto inboundResponseFromMatchingServiceDto = new InboundResponseFromMatchingServiceDto(
            MatchingServiceIdaStatus.MatchingServiceMatch,
            translatedAuthnRequest.getId(),
            MSA_ENTITY_ID,
            Optional.of("assertionBlob"),
            Optional.of(LevelOfAssurance.LEVEL_2));
        samlEngineStub.setupStubForAttributeResponseTranslate(inboundResponseFromMatchingServiceDto);

        final Response response = postAttributeQueryResponseToPolicy(sessionId);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(getSessionStateName(sessionId)).isEqualTo(EidasSuccessfulMatchState.class.getName());
    }

    @Test
    public void shouldTransitionToEidasAwaitingCycle3DataStateWhenNoMatchIsReceivedForEidasCycle0And1WithCycle3Enabled() throws Exception {
        final SessionId sessionId = aSessionIsCreated();
        selectACountry(sessionId, NETHERLANDS);
        samlSoapProxyProxyStub.setUpStubForSendHubMatchingServiceRequest(sessionId);
        postAuthnResponseToPolicy(sessionId);

        final InboundResponseFromMatchingServiceDto inboundResponseFromMatchingServiceDto = new InboundResponseFromMatchingServiceDto(
            MatchingServiceIdaStatus.NoMatchingServiceMatchFromMatchingService,
            translatedAuthnRequest.getId(),
            MSA_ENTITY_ID,
            Optional.absent(),
            Optional.absent());
        samlEngineStub.setupStubForAttributeResponseTranslate(inboundResponseFromMatchingServiceDto);
        configStub.setUpStubForEnteringAwaitingCycle3DataState(RP_ENTITY_ID);

        final Response response = postAttributeQueryResponseToPolicy(sessionId);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(getSessionStateName(sessionId)).isEqualTo(EidasAwaitingCycle3DataState.class.getName());
    }

    @Test
    public void shouldTransitionToNoMatchStateWhenNoMatchIsReceivedForEidasCycle0And1WithCycle3Disabled() throws Exception {
        final SessionId sessionId = aSessionIsCreated();
        selectACountry(sessionId, NETHERLANDS);
        samlSoapProxyProxyStub.setUpStubForSendHubMatchingServiceRequest(sessionId);
        postAuthnResponseToPolicy(sessionId);

        final InboundResponseFromMatchingServiceDto inboundResponseFromMatchingServiceDto = new InboundResponseFromMatchingServiceDto(
            MatchingServiceIdaStatus.NoMatchingServiceMatchFromMatchingService,
            translatedAuthnRequest.getId(),
            MSA_ENTITY_ID,
            Optional.absent(),
            Optional.absent());
        samlEngineStub.setupStubForAttributeResponseTranslate(inboundResponseFromMatchingServiceDto);
        configStub.setUpStubForCycle01NoMatchCycle3Disabled(RP_ENTITY_ID);

        final Response response = postAttributeQueryResponseToPolicy(sessionId);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(getSessionStateName(sessionId)).isEqualTo(NoMatchState.class.getName());
    }

    private SessionId aSessionIsCreated() throws JsonProcessingException {
        configStub.setUpStubForAssertionConsumerServiceUri(RP_ENTITY_ID);
        samlEngineStub.setupStubForAuthnRequestTranslate(translatedAuthnRequest);
        return createASession(rpSamlRequest).readEntity(SessionId.class);
    }

    private Response createASession(final SamlAuthnRequestContainerDto samlRequest) {
        return client
            .target(policy.uri(Urls.PolicyUrls.NEW_SESSION_RESOURCE).toASCIIString())
            .request()
            .post(Entity.json(samlRequest));
    }

    private SessionId selectACountry(final SessionId sessionId, final EidasCountryDto dto) {
        TestSessionResourceHelper.selectCountryInSession(
            sessionId,
            client,
            policy.uri(UriBuilder.fromPath(Urls.PolicyUrls.COUNTRIES_RESOURCE)
                .path(Urls.PolicyUrls.COUNTRY_SET_PATH)
                .build(sessionId, dto.getSimpleId()).toString())
        );
        return sessionId;
    }

    private Response postAuthnResponseToPolicy(final SessionId sessionId) {
        URI countryResponseUri = UriBuilder.fromPath(Urls.PolicyUrls.COUNTRY_AUTHN_RESPONSE_RESOURCE).build(sessionId);
        return client
            .target(policy.uri(countryResponseUri.toASCIIString()))
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.json(aSamlAuthnResponseContainerDto().withSessionId(sessionId).build()));
    }

    private Response postAttributeQueryResponseToPolicy(final SessionId sessionId) {
        final URI attributeQueryResponseUri = UriBuilder.fromPath(Urls.PolicyUrls.ATTRIBUTE_QUERY_RESPONSE_RESOURCE).build(sessionId);
        return client
            .target(policy.uri(attributeQueryResponseUri.toASCIIString()))
            .request()
            .post(Entity.json(new SamlResponseDto("a-saml-response")));
    }

    private String getSessionStateName(final SessionId sessionId) {
        final URI sessionStateNameUri = UriBuilder.fromPath(TEST_SESSION_RESOURCE_PATH + GET_SESSION_STATE_NAME).build(sessionId);
        return client
            .target(policy.uri(sessionStateNameUri.toASCIIString()))
            .request()
            .get().readEntity(String.class);
    }

    private void enableCountriesForRp(final String rpEntityId, final EidasCountryDto... countries) throws Exception {
        configStub.setupStubForEidasRPCountries(rpEntityId, Arrays.stream(countries).map(EidasCountryDto::getEntityId).collect(toList()));
    }

    private void stubSamlEngineTranslationLOAForCountry(final LevelOfAssurance loa, final EidasCountryDto country) throws Exception {
        samlEngineStub.reset();
        InboundResponseFromCountry translationDto = new InboundResponseFromCountry(IdpIdaStatus.Status.Success, Optional.absent(), country.getEntityId(), Optional.of("BLOB"), Optional.of("PID"), Optional.of(loa));
        samlEngineStub.setupStubForCountryAuthnResponseTranslate(translationDto);
    }

    private void stubSamlEngineGenerationOfAQR() throws Exception {
        AttributeQueryContainerDto aqrDto = new AttributeQueryContainerDto("SAML", URI.create("/foo"), "id", DateTime.now(), "issuer", true);
        samlEngineStub.setupStubForEidasAttributeQueryRequestGeneration(aqrDto);
    }
}
