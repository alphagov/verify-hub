package uk.gov.ida.integrationtest.hub.config.apprule;

import helpers.JerseyClientConfigurationBuilder;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.util.Duration;
import org.joda.time.DateTime;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.ida.hub.config.Urls;
import uk.gov.ida.hub.config.domain.LevelOfAssurance;
import uk.gov.ida.hub.config.dto.IdpConfigDto;
import uk.gov.ida.hub.config.dto.IdpDto;
import uk.gov.ida.integrationtest.hub.config.apprule.support.ConfigAppRule;
import uk.gov.ida.shared.utils.string.StringEncoding;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder.anIdentityProviderConfigData;
import static uk.gov.ida.hub.config.domain.builders.MatchingServiceConfigEntityDataBuilder.aMatchingServiceConfigEntityData;
import static uk.gov.ida.hub.config.domain.builders.TransactionConfigEntityDataBuilder.aTransactionConfigData;

public class IdentityProviderResourceIntegrationTest {
    private static Client client;
    private static final String ENABLED_ALL_RP_IDP = "enabled-all-rp-idp";
    private static final String ENABLED_FOR_ONBOARDING_RP_IDP = "enabled-for-onboarding-rp-idp";
    private static final String DISABLED_IDP = "disabled-idp";
    private static final String ONBOARDING_TO_LOA_1_IDP = "onboarding-idp-entity-id";
    private static final String ONBOARDING_TO_LOA_1_IDP_USING_TEMP_LIST = "onboarding-idp-entity-id-using-temp";
    private static final String DEFAULT_RP = "default-rp-entity-id";
    private static final String ONBOARDING_RP = "onboarding-rp-entity-id";
    private static final String LOA_1_TEST_RP = "loa-1-test-rp";
    private static final String DEFAULT_MS = "default-ms-entity-id";
    private static final String ONBOARDING_MS = "onboarding-ms-entity-id";
    private static final String LOA_1_MS = "loa-1-rp-ms-entity-id";
    private static final String SOFT_DISCONNECTING_IDP = "soft-disconnecting-idp";
    private static final String HARD_DISCONNECTING_IDP = "hard-disconnecting-idp";
    
    private static final DateTime expiredDatetime = DateTime.now().minusDays(1);
    private static final DateTime futureDatetime = DateTime.now().plusDays(1);

    @ClassRule
    public static ConfigAppRule configAppRule = new ConfigAppRule()
        .addTransaction(aTransactionConfigData()
                .withEntityId(DEFAULT_RP)
                .withMatchingServiceEntityId(DEFAULT_MS)
                .build())
        .addMatchingService(aMatchingServiceConfigEntityData()
                .withEntityId(DEFAULT_MS)
                .build())
        .addIdp(anIdentityProviderConfigData()
                .withEntityId(ENABLED_ALL_RP_IDP)
                .withEnabledForSingleIdp(true)
                .withSupportedLevelsOfAssurance(asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
                .build())
        .addTransaction(aTransactionConfigData()
                .withEntityId(ONBOARDING_RP)
                .withMatchingServiceEntityId(ONBOARDING_MS)
                .build())
        .addMatchingService(aMatchingServiceConfigEntityData()
                .withEntityId(ONBOARDING_MS)
                .build())
        .addIdp(anIdentityProviderConfigData()
                .withEntityId(ENABLED_FOR_ONBOARDING_RP_IDP)
                .withOnboarding(Collections.singletonList(ONBOARDING_RP))
                .withSupportedLevelsOfAssurance(asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
                .withOnboardingLevels(asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
                .build())
        .addTransaction(aTransactionConfigData()
                .withEntityId(LOA_1_TEST_RP)
                .withMatchingServiceEntityId(LOA_1_MS)
                .build())
        .addMatchingService(aMatchingServiceConfigEntityData()
                .withEntityId(LOA_1_MS)
                .build())
        .addIdp(anIdentityProviderConfigData()
                .withEntityId(ONBOARDING_TO_LOA_1_IDP)
                .withSupportedLevelsOfAssurance(asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
                .withOnboardingLevels(Collections.singletonList(LevelOfAssurance.LEVEL_1))
                .withOnboarding(Collections.singletonList(LOA_1_TEST_RP))
                .build())
        .addIdp(anIdentityProviderConfigData()
                .withEntityId(ONBOARDING_TO_LOA_1_IDP_USING_TEMP_LIST)
                .withSupportedLevelsOfAssurance(asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
                .withOnboardingLevels(Collections.singletonList(LevelOfAssurance.LEVEL_1))
                .withOnboardingTemp(Collections.singletonList(LOA_1_TEST_RP))
                .build())
        .addIdp(anIdentityProviderConfigData()
                .withEntityId(DISABLED_IDP)
                .withEnabled(false)
                .build())
        .addIdp(anIdentityProviderConfigData()
                .withEntityId(SOFT_DISCONNECTING_IDP)
                .withEnabled(true)
                .withProvideRegistrationUntil(expiredDatetime)
                .withProvideAuthenticationUntil(futureDatetime)
                .build())
        .addIdp(anIdentityProviderConfigData()
                .withEntityId(HARD_DISCONNECTING_IDP)
                .withEnabled(true)
                .withProvideRegistrationUntil(expiredDatetime)
                .withProvideAuthenticationUntil(expiredDatetime)
                .build());

    @BeforeClass
    public static void setUp() {
        configAppRule.newApplication();
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder.aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        client = new JerseyClientBuilder(configAppRule.getEnvironment()).using(jerseyClientConfiguration).build(IdentityProviderResourceIntegrationTest.class.getSimpleName());
    }

    @Test
    public void loa1TestRpAtLevel1_getIdpList_ReturnsOnboardingIdp() {
        Response response = getIdpListForLoA(LOA_1_TEST_RP, LevelOfAssurance.LEVEL_1,  Urls.ConfigUrls.IDP_LIST_FOR_TRANSACTION_AND_LOA_RESOURCE);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        List<IdpDto> idps = response.readEntity(new GenericType<List<IdpDto>>(){});
        assertThat(idps).extracting("entityId").containsOnly(
                ENABLED_ALL_RP_IDP,
                ONBOARDING_TO_LOA_1_IDP,
                ONBOARDING_TO_LOA_1_IDP_USING_TEMP_LIST
        );
    }

    @Test
    public void onboardingRpAtLevel1_getIdpList_ReturnsOnboardingRpIdp() {
        Response response = getIdpListForLoA(ONBOARDING_RP, LevelOfAssurance.LEVEL_1,  Urls.ConfigUrls.IDP_LIST_FOR_TRANSACTION_AND_LOA_RESOURCE);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        List<IdpDto> idps = response.readEntity(new GenericType<List<IdpDto>>(){});
        assertThat(idps).extracting("entityId").containsOnly(
                ENABLED_ALL_RP_IDP,
                ENABLED_FOR_ONBOARDING_RP_IDP
        );
    }

    @Test
    public void anyRpAtLevel2_getIdpList_ReturnsOnboardingIdp() {
        Response response = getIdpListForLoA("any-rp", LevelOfAssurance.LEVEL_2, Urls.ConfigUrls.IDP_LIST_FOR_TRANSACTION_AND_LOA_RESOURCE);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        List<IdpDto> idps = response.readEntity(new GenericType<List<IdpDto>>(){});
        assertThat(idps).extracting("entityId").containsOnly(
                ENABLED_ALL_RP_IDP,
                ONBOARDING_TO_LOA_1_IDP,
                ONBOARDING_TO_LOA_1_IDP_USING_TEMP_LIST
        );
    }

    @Test
    public void anyRp_getIdpList_returnsValidAndOnboardingIdpsForSignIn() {
        String transactionId = "any-rp";

        Response response = getIdpList(transactionId, Urls.ConfigUrls.IDP_LIST_FOR_SIGN_IN_RESOURCE);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        List<IdpDto> idps = response.readEntity(new GenericType<List<IdpDto>>(){});
        assertThat(idps).extracting("entityId").containsOnly(
                ENABLED_ALL_RP_IDP,
                ONBOARDING_TO_LOA_1_IDP,
                ONBOARDING_TO_LOA_1_IDP_USING_TEMP_LIST,
                SOFT_DISCONNECTING_IDP,
                HARD_DISCONNECTING_IDP
        );
    }
    
    @Test
    public void anyRp_getIdpList_returnsDisconnectingIdpsForSignIn_withAuthenticationEnabledSetToFalse() {
        String transactionId = "any-rp";
        
        Response response = getIdpList(transactionId, Urls.ConfigUrls.IDP_LIST_FOR_SIGN_IN_RESOURCE);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        List<IdpDto> idpsFromResponse = response.readEntity(new GenericType<List<IdpDto>>(){});

        List<IdpDto> disconnectingIdps = idpsFromResponse
            .stream()
            .filter(idp -> !idp.isAuthenticationEnabled())
            .collect(Collectors.toList());

        assertThat(disconnectingIdps.size()).isEqualTo(1);
        assertThat(disconnectingIdps).extracting("entityId").containsOnly(HARD_DISCONNECTING_IDP);
    }

    @Test
    public void getIdpConfigData_returnsOkAndConfigDataForEntity() {
        String entityId = ENABLED_ALL_RP_IDP;
        Response response = getIdpList(entityId, Urls.ConfigUrls.IDP_CONFIG_DATA_RESOURCE);
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        IdpConfigDto idpConfigDto = response.readEntity(IdpConfigDto.class);
        assertThat(idpConfigDto.getSupportedLevelsOfAssurance()).contains(LevelOfAssurance.LEVEL_2);
        assertThat(idpConfigDto.getUseExactComparisonType()).isEqualTo(false);
    }

    @Test
    public void getIdpConfigData_returnsNotFoundIfIdpDoesNotExist() {
        String entityId = "not-found";
        Response response = getIdpList(entityId, Urls.ConfigUrls.IDP_CONFIG_DATA_RESOURCE);
        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    @Deprecated
    public void getIdpList_returnsOkAndIdps() {
        String transactionId = "";
        URI uri = configAppRule.getUri(Urls.ConfigUrls.IDP_LIST_RESOURCE).queryParam(Urls.SharedUrls.TRANSACTION_ENTITY_ID_PARAM, transactionId).build();
        Response response = client.target(uri).request().get();
        List<IdpDto> returnedIdps = response.readEntity(new GenericType<List<IdpDto>>(){});
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(returnedIdps).isNotEmpty();
        assertThat(returnedIdps).extracting("entityId").doesNotContain(ENABLED_FOR_ONBOARDING_RP_IDP);
    }

    @Test
    public void deprecated_getEnabledIdentityProviderEntityIds_returnsOkAndIdps() {
        String transactionId = ONBOARDING_RP;
        URI uri = configAppRule.getUri(Urls.ConfigUrls.ENABLED_IDENTITY_PROVIDERS_RESOURCE).queryParam(Urls.SharedUrls.TRANSACTION_ENTITY_ID_PARAM, transactionId).build();
        Response response = client.target(uri).request().get();
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.readEntity(Collection.class)).contains(ENABLED_FOR_ONBOARDING_RP_IDP);
    }

    @Test
    public void deprecated_getEnabledIdentityProviderEntityIdsPathParam_returnOkAndIdps() {
        String entityId = ONBOARDING_RP;
        Response response = getIdpList(entityId, Urls.ConfigUrls.ENABLED_IDENTITY_PROVIDERS_PARAM_PATH_RESOURCE);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.readEntity(Collection.class)).contains(ENABLED_FOR_ONBOARDING_RP_IDP);
    }

    @Test
    public void getEnabledIdentityProviderEntityIdsForLoa_returnsOkAndIdps() {
        String entityId = "not-test-rp";
        Response response = getIdpListForLoA(entityId, LevelOfAssurance.LEVEL_1, Urls.ConfigUrls.ENABLED_ID_PROVIDERS_FOR_LOA_RESOURCE);
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        List<String> providerEntityIds = response.readEntity(new GenericType<List<String>>(){});
        assertThat(providerEntityIds).containsOnly(
                ENABLED_ALL_RP_IDP
        );
    }

    @Test
    public void getEnabledIdentityProviderEntityIdsForSignIn_returnsOkAndIdps() {
        String entityId = "not-test-rp";
        Response response = getIdpListForLoA(entityId, LevelOfAssurance.LEVEL_1, Urls.ConfigUrls.ENABLED_ID_PROVIDERS_FOR_SIGN_IN_RESOURCE);
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        List<String> providerEntityIds = response.readEntity(new GenericType<List<String>>(){});
        assertThat(providerEntityIds).containsOnly(
                ENABLED_ALL_RP_IDP,
                ONBOARDING_TO_LOA_1_IDP,
                ONBOARDING_TO_LOA_1_IDP_USING_TEMP_LIST,
                SOFT_DISCONNECTING_IDP,
                HARD_DISCONNECTING_IDP
        );
    }

    @Test
    public void getEnabledIdentityProviderEntityIdsForSingleIdp_returnsOkAndIdps() {
        String entityId = "not-test-rp";
        Response response = getIdpList(entityId, Urls.ConfigUrls.IDP_LIST_FOR_SINGLE_IDP_RESOURCE);
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        List<IdpDto> returnedIdps = response.readEntity(new GenericType<List<IdpDto>>(){});
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(returnedIdps.size()).isEqualTo(1);
        assertThat(returnedIdps).extracting("entityId").contains(ENABLED_ALL_RP_IDP);
    }

    private Response getIdpList(String entityId, String path) {
        URI uri = configAppRule.getUri(path).buildFromEncoded(StringEncoding.urlEncode(entityId).replace("+", "%20"));
        return client.target(uri).request().get();
    }

    private Response getIdpListForLoA(String entityId, LevelOfAssurance levelOfAssurance, String path) {
        URI uri = configAppRule.getUri(path).buildFromEncoded(StringEncoding.urlEncode(entityId).replace("+", "%20"), levelOfAssurance.toString());
        return client.target(uri).request().get();
    }
}
