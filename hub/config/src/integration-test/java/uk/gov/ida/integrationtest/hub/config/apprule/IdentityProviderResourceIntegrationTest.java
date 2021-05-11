package uk.gov.ida.integrationtest.hub.config.apprule;

import io.dropwizard.testing.ResourceHelpers;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import ru.vyarus.dropwizard.guice.test.ClientSupport;
import ru.vyarus.dropwizard.guice.test.jupiter.ext.TestDropwizardAppExtension;
import uk.gov.ida.hub.config.ConfigApplication;
import uk.gov.ida.hub.config.Urls;
import uk.gov.ida.hub.config.domain.LevelOfAssurance;
import uk.gov.ida.hub.config.dto.IdpConfigDto;
import uk.gov.ida.hub.config.dto.IdpDto;
import uk.gov.ida.integrationtest.hub.config.apprule.support.ConfigAppExtension;
import uk.gov.ida.shared.utils.string.StringEncoding;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder.anIdentityProviderConfigData;
import static uk.gov.ida.hub.config.domain.builders.MatchingServiceConfigBuilder.aMatchingServiceConfig;
import static uk.gov.ida.hub.config.domain.builders.TransactionConfigBuilder.aTransactionConfigData;
import static uk.gov.ida.integrationtest.hub.config.apprule.support.ConfigAppExtension.*;
import static uk.gov.ida.integrationtest.hub.config.apprule.support.ConfigAppExtension.TRANSLATIONS_RELATIVE_PATH;

public class IdentityProviderResourceIntegrationTest {
    private static ClientSupport client;
    private static final String ENABLED_ALL_RP_IDP = "enabled-all-rp-idp";
    private static final String ENABLED_FOR_ONBOARDING_RP_IDP = "enabled-for-onboarding-rp-idp";
    private static final String DISABLED_IDP = "disabled-idp";
    private static final String ONBOARDING_TO_LOA_1_IDP = "onboarding-idp-entity-id";
    private static final String DEFAULT_RP = "default-rp-entity-id";
    private static final String ONBOARDING_RP = "onboarding-rp-entity-id";
    private static final String LOA_1_TEST_RP = "loa-1-test-rp";
    private static final String DEFAULT_MS = "default-ms-entity-id";
    private static final String ONBOARDING_MS = "onboarding-ms-entity-id";
    private static final String LOA_1_MS = "loa-1-rp-ms-entity-id";
    private static final String SOFT_DISCONNECTING_IDP_ENABLED_FOR_IDP_RESPONSE = "soft-disconnecting-idp-enabled-for-response-processing";
    private static final String SOFT_DISCONNECTING_IDP = "soft-disconnecting-idp";
    private static final String HARD_DISCONNECTING_IDP = "hard-disconnecting-idp";

    private static final DateTime expiredDateTimeWithinSessionDuration = DateTime.now().minusMinutes(89);
    private static final DateTime expiredDatetime = DateTime.now().minusDays(1);
    private static final DateTime futureDatetime = DateTime.now().plusDays(1);

    @RegisterExtension
    public static TestDropwizardAppExtension app = ConfigAppExtension.forApp(ConfigApplication.class)
            .addTransaction(aTransactionConfigData()
                    .withEntityId(DEFAULT_RP)
                    .withMatchingServiceEntityId(DEFAULT_MS)
                    .build())
            .addMatchingService(aMatchingServiceConfig()
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
            .addMatchingService(aMatchingServiceConfig()
                    .withEntityId(ONBOARDING_MS)
                    .build())
            .addIdp(anIdentityProviderConfigData()
                    .withEntityId(ENABLED_FOR_ONBOARDING_RP_IDP)
                    .withOnboarding(singletonList(ONBOARDING_RP))
                    .withSupportedLevelsOfAssurance(asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
                    .withOnboardingLevels(asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
                    .build())
            .addTransaction(aTransactionConfigData()
                    .withEntityId(LOA_1_TEST_RP)
                    .withMatchingServiceEntityId(LOA_1_MS)
                    .build())
            .addMatchingService(aMatchingServiceConfig()
                    .withEntityId(LOA_1_MS)
                    .build())
            .addIdp(anIdentityProviderConfigData()
                    .withEntityId(ONBOARDING_TO_LOA_1_IDP)
                    .withSupportedLevelsOfAssurance(asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
                    .withOnboardingLevels(singletonList(LevelOfAssurance.LEVEL_1))
                    .withOnboarding(singletonList(LOA_1_TEST_RP))
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
                    .build())
            .addIdp(anIdentityProviderConfigData()
                    .withEntityId(SOFT_DISCONNECTING_IDP_ENABLED_FOR_IDP_RESPONSE)
                    .withEnabledForSingleIdp(true)
                    .withProvideRegistrationUntil(expiredDateTimeWithinSessionDuration)
                    .withSupportedLevelsOfAssurance(asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
                    .build())
            .writeFederationConfig()
            .withClearedCollectorRegistry()
            .withDefaultConfigOverridesAnd()
            .config(ResourceHelpers.resourceFilePath("config.yml"))
            .randomPorts()
            .create();

    @BeforeAll
    static void setup(ClientSupport clientSupport) {
        client = clientSupport;
    }

    @Test
    public void loa1TestRpAtLevel1_getIdpList_ReturnsDisconnectedIdpsForRegistration() {
        Response response = getIdpList(LOA_1_TEST_RP, LevelOfAssurance.LEVEL_1, Urls.ConfigUrls.DISCONNECTED_IDP_LIST_FOR_REGISTRATION_PATH_RESOURCE);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        List<IdpDto> idps = response.readEntity(new GenericType<List<IdpDto>>() { });
        assertThat(idps).extracting("entityId").containsOnly(
                SOFT_DISCONNECTING_IDP_ENABLED_FOR_IDP_RESPONSE
        );
    }

    @Test
    public void loa1TestRpAtLevel1_getIdpList_ReturnsOnboardingIdp() {
        Response response = getIdpList(LOA_1_TEST_RP, LevelOfAssurance.LEVEL_1, Urls.ConfigUrls.IDP_LIST_FOR_REGISTRATION_RESOURCE);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        List<IdpDto> idps = response.readEntity(new GenericType<List<IdpDto>>() { });
        assertThat(idps).extracting("entityId").containsOnly(
                ENABLED_ALL_RP_IDP,
                ONBOARDING_TO_LOA_1_IDP
        );
    }

    @Test
    public void onboardingRpAtLevel1_getIdpList_ReturnsDisconnectedIdpsForRegistration() {
        Response response = getIdpList(ONBOARDING_RP, LevelOfAssurance.LEVEL_1, Urls.ConfigUrls.DISCONNECTED_IDP_LIST_FOR_REGISTRATION_PATH_RESOURCE);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        List<IdpDto> idps = response.readEntity(new GenericType<List<IdpDto>>() { });
        assertThat(idps).extracting("entityId").containsOnly(
                SOFT_DISCONNECTING_IDP_ENABLED_FOR_IDP_RESPONSE
        );
    }

    @Test
    public void onboardingRpAtLevel1_getIdpList_ReturnsOnboardingRpIdp() {
        Response response = getIdpList(ONBOARDING_RP, LevelOfAssurance.LEVEL_1, Urls.ConfigUrls.IDP_LIST_FOR_REGISTRATION_RESOURCE);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        List<IdpDto> idps = response.readEntity(new GenericType<List<IdpDto>>() { });
        assertThat(idps).extracting("entityId").containsOnly(
                ENABLED_ALL_RP_IDP,
                ENABLED_FOR_ONBOARDING_RP_IDP
        );
    }

    @Test
    public void anyRpAtLevel2_getIdpList_ReturnsDisconnectedIdpsForRegistration() {
        Response response = getIdpList("any-rp", LevelOfAssurance.LEVEL_2, Urls.ConfigUrls.DISCONNECTED_IDP_LIST_FOR_REGISTRATION_PATH_RESOURCE);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        List<IdpDto> idps = response.readEntity(new GenericType<List<IdpDto>>() { });
        assertThat(idps).extracting("entityId").containsOnly(
                SOFT_DISCONNECTING_IDP_ENABLED_FOR_IDP_RESPONSE,
                SOFT_DISCONNECTING_IDP,
                HARD_DISCONNECTING_IDP
        );
    }

    @Test
    public void anyRpAtLevel2_getIdpList_ReturnsOnboardingIdp() {
        Response response = getIdpList("any-rp", LevelOfAssurance.LEVEL_2, Urls.ConfigUrls.IDP_LIST_FOR_REGISTRATION_RESOURCE);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        List<IdpDto> idps = response.readEntity(new GenericType<List<IdpDto>>() { });
        assertThat(idps).extracting("entityId").containsOnly(
                ENABLED_ALL_RP_IDP,
                ONBOARDING_TO_LOA_1_IDP
        );
    }

    @Test
    public void anyRp_getIdpList_returnsValidAndOnboardingIdpsForSignIn() {
        String transactionId = "any-rp";

        Response response = getIdpList(transactionId, Urls.ConfigUrls.IDP_LIST_FOR_SIGN_IN_RESOURCE);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        List<IdpDto> idps = response.readEntity(new GenericType<List<IdpDto>>() { });
        assertThat(idps).extracting("entityId").containsOnly(
                ENABLED_ALL_RP_IDP,
                ONBOARDING_TO_LOA_1_IDP,
                SOFT_DISCONNECTING_IDP_ENABLED_FOR_IDP_RESPONSE,
                SOFT_DISCONNECTING_IDP,
                HARD_DISCONNECTING_IDP
        );
    }

    @Test
    public void anyRp_getIdpList_returnsDisconnectingIdpsForSignIn_withAuthenticationEnabledSetToFalse() {
        String transactionId = "any-rp";

        Response response = getIdpList(transactionId, Urls.ConfigUrls.IDP_LIST_FOR_SIGN_IN_RESOURCE);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        List<IdpDto> idpsFromResponse = response.readEntity(new GenericType<List<IdpDto>>() { });

        List<IdpDto> disconnectingIdps = idpsFromResponse
                .stream()
                .filter(idp -> !idp.isAuthenticationEnabled())
                .collect(Collectors.toList());

        assertThat(disconnectingIdps.size()).isEqualTo(1);
        assertThat(disconnectingIdps).extracting("entityId").containsOnly(HARD_DISCONNECTING_IDP);
    }

    @Test
    public void getIdpConfigData_returnsOkAndConfigDataForEntity() {
        Response response = getIdpList(ENABLED_ALL_RP_IDP, Urls.ConfigUrls.IDP_CONFIG_DATA_RESOURCE);
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
    public void getEnabledIdentityProviderEntityIdsForIdpAuthnRequestGenerationAndLoa_returnsOkAndIdps() {
        String entityId = "not-test-rp";
        Response response = getIdpList(entityId, LevelOfAssurance.LEVEL_1, Urls.ConfigUrls.ENABLED_ID_PROVIDERS_FOR_REGISTRATION_AUTHN_REQUEST_RESOURCE);
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        List<String> providerEntityIds = response.readEntity(new GenericType<List<String>>() {
        });
        assertThat(providerEntityIds).containsOnly(
                ENABLED_ALL_RP_IDP
        );
    }

    @Test
    public void getEnabledIdentityProviderEntityIdsForIdpResponseProcessingAndLoa_returnsOkAndIdps() {
        String entityId = "not-test-rp";
        Response response = getIdpList(entityId, LevelOfAssurance.LEVEL_1, Urls.ConfigUrls.ENABLED_ID_PROVIDERS_FOR_REGISTRATION_AUTHN_RESPONSE_RESOURCE);
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        List<String> providerEntityIds = response.readEntity(new GenericType<List<String>>() {
        });
        assertThat(providerEntityIds).containsOnly(
                ENABLED_ALL_RP_IDP,
                SOFT_DISCONNECTING_IDP_ENABLED_FOR_IDP_RESPONSE
        );
    }

    @Test
    public void getEnabledIdentityProviderEntityIdsForSignIn_returnsOkAndIdps() {
        String entityId = "not-test-rp";
        Response response = getIdpList(entityId, LevelOfAssurance.LEVEL_1, Urls.ConfigUrls.ENABLED_ID_PROVIDERS_FOR_SIGN_IN_RESOURCE);
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        List<String> providerEntityIds = response.readEntity(new GenericType<List<String>>() {
        });
        assertThat(providerEntityIds).containsOnly(
                ENABLED_ALL_RP_IDP,
                ONBOARDING_TO_LOA_1_IDP,
                SOFT_DISCONNECTING_IDP_ENABLED_FOR_IDP_RESPONSE,
                SOFT_DISCONNECTING_IDP,
                HARD_DISCONNECTING_IDP
        );
    }

    @Test
    public void getEnabledIdentityProviderEntityIdsForSingleIdp_returnsOkAndIdps() {
        String entityId = "not-test-rp";
        Response response = getIdpList(entityId, Urls.ConfigUrls.IDP_LIST_FOR_SINGLE_IDP_RESOURCE);
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        List<IdpDto> returnedIdps = response.readEntity(new GenericType<List<IdpDto>>() {
        });
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(returnedIdps.size()).isEqualTo(1);
        assertThat(returnedIdps).extracting("entityId").contains(ENABLED_ALL_RP_IDP);
    }

    private Response getIdpList(String entityId, String path) {
        URI uri = UriBuilder.fromPath(path).buildFromEncoded(StringEncoding.urlEncode(entityId).replace("+", "%20"));
        return client.targetMain(uri.toString()).request().buildGet().invoke();
    }

    private Response getIdpList(String entityId, LevelOfAssurance levelOfAssurance, String path) {
        URI uri = UriBuilder.fromPath(path).buildFromEncoded(StringEncoding.urlEncode(entityId).replace("+", "%20"), levelOfAssurance.toString());
        return client.targetMain(uri.toString()).request().buildGet().invoke();
    }
}
