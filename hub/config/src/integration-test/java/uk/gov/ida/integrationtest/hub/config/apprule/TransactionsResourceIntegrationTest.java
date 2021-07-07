package uk.gov.ida.integrationtest.hub.config.apprule;

import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.ida.hub.config.ConfigApplication;
import uk.gov.ida.hub.config.Urls;
import uk.gov.ida.hub.config.domain.LevelOfAssurance;
import uk.gov.ida.hub.config.domain.UserAccountCreationAttribute;
import uk.gov.ida.hub.config.dto.MatchingProcessDto;
import uk.gov.ida.hub.config.dto.ResourceLocationDto;
import uk.gov.ida.hub.config.dto.TransactionDisplayData;
import uk.gov.ida.hub.config.dto.TransactionSingleIdpData;
import uk.gov.ida.integrationtest.hub.config.apprule.support.ConfigAppExtension;
import uk.gov.ida.integrationtest.hub.config.apprule.support.ConfigAppExtension.ConfigClient;
import uk.gov.ida.integrationtest.hub.config.apprule.support.ConfigAppExtension.ConfigAppExtensionBuilder;
import uk.gov.ida.shared.utils.string.StringEncoding;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
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

@ExtendWith(DropwizardExtensionsSupport.class)
public class TransactionsResourceIntegrationTest {
    private static final String ENTITY_ID = "test-entity-id";
    private static final String SIMPLE_ID = "test-simple-id";
    private static final String MS_ENTITY_ID = "ms-entity-id";
    private static final String TEST_URI = "http://foo.bar/test-uri";
    private static final String SERVICE_HOMEPAGE = "http://foo.bar/service-homepage";
    private static final String HEADLESS_START_PAGE = "http://foo.bar/service-headless-start-page";
    private static final String SINGLE_IDP_START_PAGE = "http://foo.bar/service-single-idp-start-page";
    private static final String ANOTHER_ENTITY_ID = "another-test-entity-id";
    private static final String ANOTHER_SIMPLE_ID = "another-test-simple-id";

    private static final ConfigAppExtension app = ConfigAppExtensionBuilder.forApp(ConfigApplication.class)
            .addTransaction(aTransactionConfigData()
                    .withEntityId(ENTITY_ID)
                    .withSimpleId(SIMPLE_ID)
                    .withEnabledForSingleIdp(true)
                    .withServiceHomepage(URI.create(SERVICE_HOMEPAGE))
                    .withLevelsOfAssurance(Collections.singletonList(LevelOfAssurance.LEVEL_2))
                    .withMatchingProcess(aMatchingProcess().withCycle3AttributeName("NationalInsuranceNumber").build())
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
            .addMatchingService(aMatchingServiceConfig()
                    .withEntityId(MS_ENTITY_ID)
                    .build())
            .addIdp(anIdentityProviderConfigData()
                    .withEntityId("idp-entity-id")
                    .withOnboarding(asList(ENTITY_ID))
                    .build())
            .build();

    private ConfigClient client;

    @BeforeEach
    void setup() { client = app.getClient(); }

    @AfterAll
    static void tearDown() { app.tearDown(); }

    @Test
    public void getAssertionConsumerServiceUri_returnsOkAndUri() {
        Response response = getForEntityIdAndPathAndQueryParam(
                ENTITY_ID,
                Urls.ConfigUrls.TRANSACTIONS_ASSERTION_CONSUMER_SERVICE_URI_RESOURCE,
                Urls.ConfigUrls.ASSERTION_CONSUMER_SERVICE_INDEX_PARAM,
                0
        );
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.readEntity(ResourceLocationDto.class).getTarget()).isEqualTo(URI.create(TEST_URI));
    }

    @Test
    public void getAssertionConsumerServiceUri_returnsNotFoundWhenIndexIsNotFound() {
        Response response = getForEntityIdAndPathAndQueryParam(
                ENTITY_ID,
                Urls.ConfigUrls.TRANSACTIONS_ASSERTION_CONSUMER_SERVICE_URI_RESOURCE,
                Urls.ConfigUrls.ASSERTION_CONSUMER_SERVICE_INDEX_PARAM,
                3
        );
        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void getDisplayData_returnsOkAndDisplayData() {
        Response response = getForEntityIdAndPath(ENTITY_ID, Urls.ConfigUrls.TRANSACTION_DISPLAY_DATA_RESOURCE);
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
        Response response = getForEntityIdAndPath(ENTITY_ID, Urls.ConfigUrls.MATCHING_PROCESS_RESOURCE);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.readEntity(MatchingProcessDto.class)).isEqualToComparingFieldByField(new MatchingProcessDto("NationalInsuranceNumber"));
    }

    @Test
    public void getMatchingProcess_returnsNotFoundForEntityThatDoesNotExist() {
        Response response = getForEntityIdAndPath("not-found", Urls.ConfigUrls.MATCHING_PROCESS_RESOURCE);
        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void getEnabledTransactions_returnsOkAndEnabledTransactions() {
        Response response = client.targetMain("/config/transactions" + Urls.ConfigUrls.ENABLED_TRANSACTIONS_PATH);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        List<TransactionDisplayData> transactionDisplayItems =
                response.readEntity(new GenericType<>() {
                });
        for (TransactionDisplayData transactionDisplayItem : transactionDisplayItems) {
            List<LevelOfAssurance> loas = transactionDisplayItem.getLoaList();
            assertThat(loas != null);
        }
    }

    @Test
    public void getLevelsOfAssurance_returnsNotFoundForEntityThatDoesNotExist() {
        Response response = getForEntityIdAndPath("not-found", Urls.ConfigUrls.LEVELS_OF_ASSURANCE_RESOURCE);
        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void getMatchingServiceEntityId_returnsOkAndMatchServiceEntityId() {
        Response response = getForEntityIdAndPath(ENTITY_ID, Urls.ConfigUrls.MATCHING_SERVICE_ENTITY_ID_RESOURCE);
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.readEntity(String.class)).isEqualTo(MS_ENTITY_ID);
    }

    @Test
    public void getMatchingServiceEntityId_returnsNotFoundForEntityIdThatDoesNotExist() {
        Response response = getForEntityIdAndPath("not-found", Urls.ConfigUrls.MATCHING_SERVICE_ENTITY_ID_RESOURCE);
        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void getUserAccountCreationAttributes_returnsOkAndUserAccountCreationAtttributes() {
        Response response = getForEntityIdAndPath(ENTITY_ID, Urls.ConfigUrls.USER_ACCOUNT_CREATION_ATTRIBUTES_RESOURCE);
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.readEntity(List.class)).isNotEmpty();
    }

    @Test
    public void getUserAccountCreationAttributes_returnsNotFoundForEntityThatDoesNotExist() {
        Response response = getForEntityIdAndPath("not-found", Urls.ConfigUrls.USER_ACCOUNT_CREATION_ATTRIBUTES_RESOURCE);
        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void getShouldHubSignResponseMessages_returnsOkAndMessages() {
        Response response = getForEntityIdAndPath(ENTITY_ID, Urls.ConfigUrls.SHOULD_HUB_SIGN_RESPONSE_MESSAGES_RESOURCE);
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.readEntity(boolean.class)).isTrue();
    }

    @Test
    public void getShouldHubSignResponseMessages_returnsNotFound() {
        Response response = getForEntityIdAndPath("not-found", Urls.ConfigUrls.SHOULD_HUB_SIGN_RESPONSE_MESSAGES_RESOURCE);
        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void getShouldHubUseLegacySamlStandard_returnsOkAndMessages() {
        Response response = getForEntityIdAndPath(ENTITY_ID, Urls.ConfigUrls.SHOULD_HUB_USE_LEGACY_SAML_STANDARD_RESOURCE);
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.readEntity(boolean.class)).isFalse();
    }

    @Test
    public void getShouldHubUseLegacySamlStandard_returnsNotFound() {
        Response response = getForEntityIdAndPath("not-found", Urls.ConfigUrls.SHOULD_HUB_USE_LEGACY_SAML_STANDARD_RESOURCE);
        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void getShouldReturnOkWhenUsingMatchingIsTrue() {
        Response response = getForEntityIdAndPath(ENTITY_ID, Urls.ConfigUrls.MATCHING_ENABLED_FOR_TRANSACTION_RESOURCE);
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.readEntity(boolean.class)).isTrue();
    }

    @Test
    public void getShouldReturnOkWhenUsingMatchingIsFalse() {
        Response response = getForEntityIdAndPath(ANOTHER_ENTITY_ID, Urls.ConfigUrls.MATCHING_ENABLED_FOR_TRANSACTION_RESOURCE);
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.readEntity(boolean.class)).isFalse();
    }

    @Test
    public void getShouldReturnNotFoundUsingMatchingWhenEntityIdDoesNotExist() {
        Response response = getForEntityIdAndPath("not-found", Urls.ConfigUrls.MATCHING_ENABLED_FOR_TRANSACTION_RESOURCE);
        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void getSingleIDPEnabledServiceListTransactions_returnsOkAndEnabledAndSingleIdpEnabledTransactions() {
        Response response = client.targetMain("/config/transactions" + Urls.ConfigUrls.SINGLE_IDP_ENABLED_LIST_PATH);
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

    private Response getForEntityIdAndPath(String entityId, String path) {
        URI uri = UriBuilder.fromPath(path).buildFromEncoded(StringEncoding.urlEncode(entityId).replace("+", "%20"));
        return client.targetMain(uri.toString());
    }

    private Response getForEntityIdAndPathAndQueryParam(String entityId, String path, String queryParamName, Object queryParamValue) {
        URI uri = UriBuilder.fromPath(path).queryParam(queryParamName, queryParamValue).buildFromEncoded(StringEncoding.urlEncode(entityId).replace("+", "%20"));
        return client.targetMain(uri);
    }
}
