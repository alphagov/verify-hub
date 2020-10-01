package uk.gov.ida.integrationtest.hub.config.apprule;

import helpers.JerseyClientConfigurationBuilder;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.util.Duration;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.ida.hub.config.Urls;
import uk.gov.ida.hub.config.domain.LevelOfAssurance;
import uk.gov.ida.hub.config.domain.UserAccountCreationAttribute;
import uk.gov.ida.hub.config.dto.MatchingProcessDto;
import uk.gov.ida.hub.config.dto.ResourceLocationDto;
import uk.gov.ida.hub.config.dto.TransactionDisplayData;
import uk.gov.ida.hub.config.dto.TransactionSingleIdpData;
import uk.gov.ida.integrationtest.hub.config.apprule.support.ConfigAppRule;
import uk.gov.ida.shared.utils.string.StringEncoding;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.config.domain.builders.AssertionConsumerServiceBuilder.anAssertionConsumerService;
import static uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder.anIdentityProviderConfigData;
import static uk.gov.ida.hub.config.domain.builders.MatchingProcessBuilder.aMatchingProcess;
import static uk.gov.ida.hub.config.domain.builders.MatchingServiceConfigBuilder.aMatchingServiceConfig;
import static uk.gov.ida.hub.config.domain.builders.TransactionConfigBuilder.aTransactionConfigData;

public class TransactionsResourceIntegrationTest {
    public static Client client;
    private static final String ENTITY_ID = "test-entity-id";
    private static final String NO_EIDAS_ENTITY_ID = "no-eidas-test-entity-id";
    private static final String SIMPLE_ID = "test-simple-id";
    private static final String MS_ENTITY_ID = "ms-entity-id";
    private static final String NO_EIDAS_MS_ENTITY_ID = "no-eidas-ms-entity-id";
    private static final String TEST_URI = "http://foo.bar/test-uri";
    private static final String SERVICE_HOMEPAGE = "http://foo.bar/service-homepage";
    private static final String HEADLESS_START_PAGE = "http://foo.bar/service-headless-start-page";
    private static final String SINGLE_IDP_START_PAGE = "http://foo.bar/service-single-idp-start-page";
    private static final String ANOTHER_ENTITY_ID = "another-test-entity-id";
    private static final String ANOTHER_SIMPLE_ID = "another-test-simple-id";
    private static final String PROXY_NODE_ENTITY_ID = "proxy-node-entity-id";
    private static final String NON_PROXY_NODE_ENTITY_ID = "non-proxy-node-entity-id";

    @ClassRule
    public static ConfigAppRule configAppRule = new ConfigAppRule()
            .addTransaction(aTransactionConfigData()
                    .withEntityId(ENTITY_ID)
                    .withSimpleId(SIMPLE_ID)
                    .withEnabledForSingleIdp(true)
                    .withServiceHomepage(URI.create(SERVICE_HOMEPAGE))
                    .withLevelsOfAssurance(Collections.singletonList(LevelOfAssurance.LEVEL_2))
                    .withMatchingProcess(aMatchingProcess().withCycle3AttributeName("NationalInsuranceNumber").build())
                    .withEidasEnabled(true)
                    .addUserAccountCreationAttribute(UserAccountCreationAttribute.FIRST_NAME)
                    .addAssertionConsumerService(
                            anAssertionConsumerService()
                                    .isDefault(true)
                                    .withUri(URI.create(TEST_URI))
                                    .withIndex(0)
                                    .build()
                    )
                    .withMatchingServiceEntityId(MS_ENTITY_ID)
                    .withUsingMatching(true)
                    .withHeadlessStartPage(URI.create(HEADLESS_START_PAGE))
                    .withSingleIdpStartPage(URI.create(SINGLE_IDP_START_PAGE))
                    .build())
            .addTransaction(aTransactionConfigData()
                    .withEntityId(ANOTHER_ENTITY_ID)
                    .withSimpleId(ANOTHER_SIMPLE_ID)
                    .withEnabledForSingleIdp(true)
                    .withUsingMatching(false)
                    .withMatchingServiceEntityId(MS_ENTITY_ID)
                    .withServiceHomepage(URI.create(SERVICE_HOMEPAGE))
                    .withLevelsOfAssurance(Collections.singletonList(LevelOfAssurance.LEVEL_2))
                    .build())
            .addTransaction(aTransactionConfigData()
                    .withEntityId(NO_EIDAS_ENTITY_ID)
                    .withMatchingServiceEntityId(NO_EIDAS_MS_ENTITY_ID)
                    .build())
            .addTransaction(aTransactionConfigData()
                    .withIsEidasProxyNode(true)
                    .withEntityId(PROXY_NODE_ENTITY_ID)
                    .withSimpleId(SIMPLE_ID)
                    .withServiceHomepage(URI.create(SERVICE_HOMEPAGE))
                    .withLevelsOfAssurance(Collections.singletonList(LevelOfAssurance.LEVEL_2))
                    .withMatchingServiceEntityId(MS_ENTITY_ID)
                    .withUsingMatching(false)
                    .build())
            .addTransaction(aTransactionConfigData()
                    .withIsEidasProxyNode(false)
                    .withEntityId(NON_PROXY_NODE_ENTITY_ID)
                    .withSimpleId(SIMPLE_ID)
                    .withServiceHomepage(URI.create(SERVICE_HOMEPAGE))
                    .withLevelsOfAssurance(Collections.singletonList(LevelOfAssurance.LEVEL_2))
                    .withMatchingServiceEntityId(MS_ENTITY_ID)
                    .withUsingMatching(false)
                    .build())
            .addMatchingService(aMatchingServiceConfig()
                    .withEntityId(MS_ENTITY_ID)
                    .build())
            .addMatchingService(aMatchingServiceConfig()
                    .withEntityId(NO_EIDAS_MS_ENTITY_ID)
                    .build())
            .addIdp(anIdentityProviderConfigData()
                    .withEntityId("idp-entity-id")
                    .withOnboarding(asList(ENTITY_ID, NO_EIDAS_ENTITY_ID))
                    .build());

    @BeforeClass
    public static void setUp() {
        configAppRule.newApplication();
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder.aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        client = new JerseyClientBuilder(configAppRule.getEnvironment()).using(jerseyClientConfiguration).build(TransactionsResourceIntegrationTest.class.getSimpleName());
    }

    @Test
    public void getAssertionConsumerServiceUri_returnsOkAndUri() {
        URI uri = configAppRule.getUri(Urls.ConfigUrls.TRANSACTIONS_ASSERTION_CONSUMER_SERVICE_URI_RESOURCE)
                .queryParam(Urls.ConfigUrls.ASSERTION_CONSUMER_SERVICE_INDEX_PARAM, 0).buildFromEncoded(StringEncoding.urlEncode(ENTITY_ID).replace("+", "%20"));
        Response response = client.target(uri).request().get();
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.readEntity(ResourceLocationDto.class).getTarget()).isEqualTo(URI.create(TEST_URI));
    }

    @Test
    public void getAssertionConsumerServiceUri_returnsNotFoundWhenIndexIsNotFound() {
        URI uri = configAppRule.getUri(Urls.ConfigUrls.TRANSACTIONS_ASSERTION_CONSUMER_SERVICE_URI_RESOURCE)
                .queryParam(Urls.ConfigUrls.ASSERTION_CONSUMER_SERVICE_INDEX_PARAM, 3).buildFromEncoded(StringEncoding.urlEncode(ENTITY_ID).replace("+", "%20"));
        Response response = client.target(uri).request().get();
        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void getDisplayData_returnsOkAndDisplayData() {
        URI uri = configAppRule.getUri(Urls.ConfigUrls.TRANSACTION_DISPLAY_DATA_RESOURCE)
                .buildFromEncoded(StringEncoding.urlEncode(ENTITY_ID).replace("+", "%20"));
        Response response = client.target(uri).request().get();
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        TransactionDisplayData expected = new TransactionDisplayData(
                SIMPLE_ID,
                URI.create(SERVICE_HOMEPAGE),
                Collections.singletonList(LevelOfAssurance.LEVEL_2),
                URI.create(HEADLESS_START_PAGE)
        );
        assertThat(response.readEntity(TransactionDisplayData.class)).isEqualToIgnoringGivenFields(expected, "otherWaysToCompleteTransaction");
    }

    @Test
    public void getMatchingProcess_returnsOKAndMatchingProcess() {
        URI uri = configAppRule.getUri(Urls.ConfigUrls.MATCHING_PROCESS_RESOURCE)
                .buildFromEncoded(StringEncoding.urlEncode(ENTITY_ID).replace("+", "%20"));
        Response response = client.target(uri).request().get();
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.readEntity(MatchingProcessDto.class)).isEqualToComparingFieldByField(new MatchingProcessDto("NationalInsuranceNumber"));
    }

    @Test
    public void getMatchingProcess_returnsNotFoundForEntityThatDoesNotExist() {
        String entityId = "not-found";
        URI uri = configAppRule.getUri(Urls.ConfigUrls.MATCHING_PROCESS_RESOURCE)
                .buildFromEncoded(StringEncoding.urlEncode(entityId).replace("+", "%20"));
        Response response = client.target(uri).request().get();
        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void getEnabledTransactions_returnsOkAndEnabledTransactions() {
        URI uri = configAppRule.getUri("/config/transactions" + Urls.ConfigUrls.ENABLED_TRANSACTIONS_PATH).build();
        Response response = client.target(uri).request().get();
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        List<TransactionDisplayData> transactionDisplayItems =
                response.readEntity(new GenericType<List<TransactionDisplayData>>() {});
        for (TransactionDisplayData transactionDisplayItem : transactionDisplayItems) {
            List<LevelOfAssurance> loas = transactionDisplayItem.getLoaList();
            assertThat(loas != null);
        }
    }

    @Test
    public void getLevelsOfAssurance_returnsNotFoundForEntityThatDoesNotExist() {
        String entityId = "not-found";
        URI uri = configAppRule.getUri(Urls.ConfigUrls.LEVELS_OF_ASSURANCE_RESOURCE)
                .buildFromEncoded(StringEncoding.urlEncode(entityId).replace("+", "%20"));
        Response response = client.target(uri).request().get();
        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void getMatchingServiceEntityId_returnsOkAndMatchServiceEntityId() {
        URI uri = configAppRule.getUri(Urls.ConfigUrls.MATCHING_SERVICE_ENTITY_ID_RESOURCE)
                .buildFromEncoded(StringEncoding.urlEncode(ENTITY_ID).replace("+", "%20"));
        Response response = client.target(uri).request().get();
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.readEntity(String.class)).isEqualTo(MS_ENTITY_ID);
    }

    @Test
    public void getMatchingServiceEntityId_returnsNotFoundForEntityIdThatDoesNotExist() {
        String entityId = "not-found";
        URI uri = configAppRule.getUri(Urls.ConfigUrls.MATCHING_SERVICE_ENTITY_ID_RESOURCE)
                .buildFromEncoded(StringEncoding.urlEncode(entityId).replace("+", "%20"));
        Response response = client.target(uri).request().get();
        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void getUserAccountCreationAttributes_returnsOkAndUserAccountCreationAtttributes() {
        URI uri = configAppRule.getUri(Urls.ConfigUrls.USER_ACCOUNT_CREATION_ATTRIBUTES_RESOURCE)
                .buildFromEncoded(StringEncoding.urlEncode(ENTITY_ID).replace("+", "%20"));
        Response response = client.target(uri).request().get();
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.readEntity(List.class)).isNotEmpty();
    }

    @Test
    public void getUserAccountCreationAttributes_returnsNotFoundForEntityThatDoesNotExist() {
        String entityId = "not-found";
        URI uri = configAppRule.getUri(Urls.ConfigUrls.USER_ACCOUNT_CREATION_ATTRIBUTES_RESOURCE)
                .buildFromEncoded(StringEncoding.urlEncode(entityId).replace("+", "%20"));
        Response response = client.target(uri).request().get();
        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void getShouldHubSignResponseMessages_returnsOkAndMessages() {
        URI uri = configAppRule.getUri(Urls.ConfigUrls.SHOULD_HUB_SIGN_RESPONSE_MESSAGES_RESOURCE)
                .buildFromEncoded(StringEncoding.urlEncode(ENTITY_ID).replace("+", "%20"));
        Response response = client.target(uri).request().get();
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.readEntity(boolean.class)).isTrue();
    }

    @Test
    public void getShouldHubSignResponseMessages_returnsNotFound() {
        String entityId = "not-found";
        URI uri = configAppRule.getUri(Urls.ConfigUrls.SHOULD_HUB_SIGN_RESPONSE_MESSAGES_RESOURCE)
                .buildFromEncoded(StringEncoding.urlEncode(entityId).replace("+", "%20"));
        Response response = client.target(uri).request().get();
        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void getShouldHubUseLegacySamlStandard_returnsOkAndMessages() {
        URI uri = configAppRule.getUri(Urls.ConfigUrls.SHOULD_HUB_USE_LEGACY_SAML_STANDARD_RESOURCE)
                .buildFromEncoded(StringEncoding.urlEncode(ENTITY_ID).replace("+", "%20"));
        Response response = client.target(uri).request().get();
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.readEntity(boolean.class)).isFalse();
    }

    @Test
    public void getShouldHubUseLegacySamlStandard_returnsNotFound() {
        String entityId = "not-found";
        URI uri = configAppRule.getUri(Urls.ConfigUrls.SHOULD_HUB_USE_LEGACY_SAML_STANDARD_RESOURCE)
                .buildFromEncoded(StringEncoding.urlEncode(entityId).replace("+", "%20"));
        Response response = client.target(uri).request().get();
        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void getShouldReturnIsEidasEnabledForTransaction() {
        URI uri = configAppRule.getUri(Urls.ConfigUrls.EIDAS_ENABLED_FOR_TRANSACTION_RESOURCE)
                .buildFromEncoded(StringEncoding.urlEncode(ENTITY_ID).replace("+", "%20"));
        Response response = client.target(uri).request().get();
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.readEntity(boolean.class)).isTrue();
    }

    @Test
    public void getShouldReturnIsEidasDisabledOrNotPresentForTransaction() {
        URI uri = configAppRule.getUri(Urls.ConfigUrls.EIDAS_ENABLED_FOR_TRANSACTION_RESOURCE)
                .buildFromEncoded(StringEncoding.urlEncode(NO_EIDAS_ENTITY_ID).replace("+", "%20"));
        Response response = client.target(uri).request().get();
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.readEntity(boolean.class)).isFalse();
    }

    @Test
    public void getShouldReturnOkWhenUsingMatchingIsTrue() {
        URI uri = configAppRule
                .getUri(Urls.ConfigUrls.MATCHING_ENABLED_FOR_TRANSACTION_RESOURCE)
                .buildFromEncoded(StringEncoding.urlEncode(ENTITY_ID).replace("+", "%20"));
        Response response = client.target(uri).request().get();
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.readEntity(boolean.class)).isTrue();
    }

    @Test
    public void getShouldReturnTrueWhenIsEidasProxyNodeIsTrue() {
        URI uri = configAppRule
                .getUri(Urls.ConfigUrls.IS_AN_EIDAS_PROXY_NODE_FOR_TRANSACTION_RESOURCE)
                .buildFromEncoded(StringEncoding.urlEncode(PROXY_NODE_ENTITY_ID).replace("+", "%20"));
        Response response = client.target(uri).request().get();
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.readEntity(boolean.class)).isTrue();
    }

    @Test
    public void getShouldReturnFalseWhenIsEidasProxyNodeIsFalse() {
        URI uri = configAppRule
                .getUri(Urls.ConfigUrls.IS_AN_EIDAS_PROXY_NODE_FOR_TRANSACTION_RESOURCE)
                .buildFromEncoded(StringEncoding.urlEncode(NON_PROXY_NODE_ENTITY_ID).replace("+", "%20"));
        Response response = client.target(uri).request().get();
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.readEntity(boolean.class)).isFalse();
    }

    @Test
    public void getShouldReturnFalseWhenIsEidasProxyNodeIsUnset() {
        URI uri = configAppRule
                .getUri(Urls.ConfigUrls.IS_AN_EIDAS_PROXY_NODE_FOR_TRANSACTION_RESOURCE)
                .buildFromEncoded(StringEncoding.urlEncode(ANOTHER_ENTITY_ID).replace("+", "%20"));
        Response response = client.target(uri).request().get();
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.readEntity(boolean.class)).isFalse();
    }

    @Test
    public void getShouldReturnFalseWhenEntityIdNotKnown() {
        String entityId = "some entity id not present in /config-service-data/..../transactions";
        URI uri = configAppRule
                .getUri(Urls.ConfigUrls.IS_AN_EIDAS_PROXY_NODE_FOR_TRANSACTION_RESOURCE)
                .buildFromEncoded(StringEncoding.urlEncode(entityId).replace("+", "%20"));
        Response response = client.target(uri).request().get();
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.readEntity(boolean.class)).isFalse();
    }

    @Test
    public void getShouldReturnOkWhenUsingMatchingIsFalse() {
        URI uri = configAppRule
                .getUri(Urls.ConfigUrls.MATCHING_ENABLED_FOR_TRANSACTION_RESOURCE)
                .buildFromEncoded(StringEncoding.urlEncode(ANOTHER_ENTITY_ID).replace("+", "%20"));
        Response response = client.target(uri).request().get();
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.readEntity(boolean.class)).isFalse();
    }

    @Test
    public void getShouldReturnNotFoundUsingMatchingWhenEntityIdDoesNotExist() {
        String entityId = "not-found";
        URI uri = configAppRule
                .getUri(Urls.ConfigUrls.MATCHING_ENABLED_FOR_TRANSACTION_RESOURCE)
                .buildFromEncoded(StringEncoding.urlEncode(entityId).replace("+", "%20"));
        Response response = client.target(uri).request().get();
        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void getSingleIDPEnabledServiceListTransactions_returnsOkAndEnabledAndSingleIdpEnabledTransactions() {
        URI uri = configAppRule.getUri("/config/transactions" + Urls.ConfigUrls.SINGLE_IDP_ENABLED_LIST_PATH).build();
        Response response = client.target(uri).request().get();
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        List<TransactionSingleIdpData> transactionDisplayItems =
                response.readEntity(new GenericType<List<TransactionSingleIdpData>>() {});
        assertThat(transactionDisplayItems.size()).isEqualTo(2);
        for (TransactionSingleIdpData transactionDisplayItem : transactionDisplayItems) {
            if (transactionDisplayItem.getEntityId().equals(ENTITY_ID)) {
                assertThat(transactionDisplayItem.getSimpleId().get()).isEqualTo(SIMPLE_ID);
                assertThat(transactionDisplayItem.getRedirectUrl()).isEqualTo(URI.create(SINGLE_IDP_START_PAGE));
            } else {
                assertThat(transactionDisplayItem.getEntityId()).isEqualTo(ANOTHER_ENTITY_ID);
                assertThat(transactionDisplayItem.getSimpleId().get()).isEqualTo(ANOTHER_SIMPLE_ID);
                assertThat(transactionDisplayItem.getRedirectUrl()).isEqualTo(URI.create(SERVICE_HOMEPAGE));
            }
        }
    }
}
