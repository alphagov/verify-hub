package uk.gov.ida.integrationtest.hub.policy.apprule.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Optional;
import httpstub.HttpStubRule;
import org.apache.http.entity.ContentType;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.builder.domain.IdpConfigDtoBuilder;
import uk.gov.ida.hub.policy.contracts.MatchingServiceConfigEntityDataDto;
import uk.gov.ida.hub.policy.domain.EidasCountryDto;
import uk.gov.ida.hub.policy.domain.IdpConfigDto;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.MatchingProcessDto;
import uk.gov.ida.hub.policy.domain.ResourceLocation;
import uk.gov.ida.hub.policy.domain.UserAccountCreationAttribute;
import uk.gov.ida.shared.utils.string.StringEncoding;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

public class ConfigStubRule extends HttpStubRule {

    private final int OK = Response.Status.OK.getStatusCode();

    public void setupStubForEnabledIdps(String transactionEntityId, boolean registering, LevelOfAssurance supportedLoa, Collection<String> enabledIdps) throws JsonProcessingException {
        if (registering) {
            setupStubForEnabledIdpsForLoa(transactionEntityId, supportedLoa, enabledIdps);
        }
        else {
            setupStubForEnabledIdpsForSignIn(transactionEntityId, enabledIdps);
        }

        setupStubForIdpConfig(enabledIdps, supportedLoa);
    }

    public void setUpStubForEnabledCountries(String rpEntityId, Collection<EidasCountryDto> enabledCountries) throws JsonProcessingException {
        register(Urls.ConfigUrls.COUNTRIES_ROOT, OK, enabledCountries);

        List<String> countryEntityIds = enabledCountries.stream().map(country -> country.getEntityId()).collect(Collectors.toList());
        UriBuilder countriesForTransactionUriBuilder = UriBuilder.fromPath(Urls.ConfigUrls.EIDAS_RP_COUNTRIES_FOR_TRANSACTION_RESOURCE);
        String countriesForTransactionPath = countriesForTransactionUriBuilder.buildFromEncoded(StringEncoding.urlEncode(rpEntityId).replace("+", "%20")).getPath();
        register(countriesForTransactionPath, OK, countryEntityIds);
    }

    public void setupStubForEidasEnabledForTransaction(String transactionEntityId, boolean eidasEnabledForTransaction) throws JsonProcessingException {
        String path = UriBuilder.fromPath(Urls.ConfigUrls.EIDAS_ENABLED_FOR_TRANSACTION_RESOURCE)
            .build(StringEncoding.urlEncode(transactionEntityId).replace("+", "%20"))
            .getPath();
        register(path, OK, eidasEnabledForTransaction);
    }

    public void setUpStubForAssertionConsumerServiceUri(String entityId) throws JsonProcessingException {
        setUpStubForAssertionConsumerServiceUri(entityId, Optional.absent());
    }

    public void setUpStubForAssertionConsumerServiceUri(String entityId, Optional<Integer> assertionConsumerServiceIndex) throws JsonProcessingException {
        UriBuilder uriBuilder = UriBuilder.fromPath(Urls.ConfigUrls.TRANSACTIONS_ASSERTION_CONSUMER_SERVICE_URI_RESOURCE);
        if (assertionConsumerServiceIndex.isPresent()) {
            uriBuilder.queryParam(Urls.ConfigUrls.ASSERTION_CONSUMER_SERVICE_INDEX_PARAM, assertionConsumerServiceIndex.get().toString());
        }

        URI uri = uriBuilder.buildFromEncoded(StringEncoding.urlEncode(entityId).replace("+", "%20"));

        register(uri.toString(), OK, new ResourceLocation(URI.create("thisIsAnRpPostEndpointUri")));
    }

    public void setUpStubToReturn404ForAssertionConsumerServiceUri(String entityId) throws JsonProcessingException {
        String uri = UriBuilder.fromPath(Urls.ConfigUrls.TRANSACTIONS_ASSERTION_CONSUMER_SERVICE_URI_RESOURCE).build(entityId).getPath();
        register(uri, Response.Status.NOT_FOUND.getStatusCode(), ErrorStatusDto.createAuditedErrorStatus(UUID.randomUUID(), ExceptionType.INVALID_ASSERTION_CONSUMER_INDEX));
    }

    public void setUpStubForLevelsOfAssurance(String entityId) throws JsonProcessingException {
        String uri = UriBuilder.fromPath(Urls.ConfigUrls.LEVELS_OF_ASSURANCE_RESOURCE)
            .build(StringEncoding.urlEncode(entityId).replace("+", "%20"))
            .getPath();
        register(uri, OK, asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2));
    }

    public void setUpStubForMatchingServiceRequest(String rpEntityId, String matchingServiceEntityId) throws JsonProcessingException {
        setUpStubForMatchingServiceRequest(rpEntityId, matchingServiceEntityId, false);
    }

    public void setUpStubForMatchingServiceRequest(
        String rpEntityId,
        String matchingServiceEntityId,
        boolean isOnboarding) throws JsonProcessingException {

        String uri = UriBuilder.fromPath(Urls.ConfigUrls.MATCHING_SERVICE_ENTITY_ID_RESOURCE)
            .build(StringEncoding.urlEncode(rpEntityId).replace("+", "%20"))
            .getPath();

        register(uri, OK, ContentType.TEXT_PLAIN.toString(), matchingServiceEntityId);


        String isUsingMatchingUri = UriBuilder
            .fromPath(Urls.ConfigUrls.MATCHING_ENABLED_FOR_TRANSACTION_RESOURCE)
            .buildFromEncoded(StringEncoding.urlEncode(rpEntityId)
            .replace("+", "%20"))
            .getPath();

        register(isUsingMatchingUri, OK, ContentType.APPLICATION_JSON.toString(), "true");


        String msaUri = UriBuilder.fromPath(Urls.ConfigUrls.MATCHING_SERVICE_RESOURCE)
            .build(StringEncoding.urlEncode(matchingServiceEntityId).replace("+", "%20"))
            .getPath();

        MatchingServiceConfigEntityDataDto matchingServiceUri = new MatchingServiceConfigEntityDataDto(matchingServiceEntityId, URI.create("matchingServiceUri"), rpEntityId, false, isOnboarding, null);

        register(msaUri, OK, matchingServiceUri);
    }

    public void setUpStubForMatchingServiceEntityId(String rpEntityId, String matchingServiceEntityId) throws JsonProcessingException {
        String uri = UriBuilder.fromPath(Urls.ConfigUrls.MATCHING_SERVICE_ENTITY_ID_RESOURCE)
            .build(StringEncoding.urlEncode(rpEntityId).replace("+", "%20"))
            .getPath();
        register(uri, OK, ContentType.TEXT_PLAIN.toString(), matchingServiceEntityId);
    }


    public String setUpStubForEnteringAwaitingCycle3DataState(String rpEntityId) throws JsonProcessingException {
        String uri = UriBuilder.fromPath(Urls.ConfigUrls.MATCHING_PROCESS_RESOURCE).build(rpEntityId).getPath();
        final MatchingProcessDto cycle3Attribute = new MatchingProcessDto(Optional.of("TUFTY_CLUB_CARD"));
        register(uri, OK, cycle3Attribute);
        return cycle3Attribute.getAttributeName().get();
    }

    public void setUpStubForEnteringAwaitingCycle3DataState(String rpEntityId, MatchingProcessDto cycle3Attribute) throws JsonProcessingException {
        String uri = UriBuilder.fromPath(Urls.ConfigUrls.MATCHING_PROCESS_RESOURCE).build(rpEntityId).getPath();
        register(uri, OK, cycle3Attribute);
    }

    public void setUpStubForCycle01NoMatchCycle3Disabled(String rpEntityId) throws JsonProcessingException {
        String uri = UriBuilder.fromPath(Urls.ConfigUrls.MATCHING_PROCESS_RESOURCE).build(rpEntityId).getPath();
        final MatchingProcessDto cycle3Attribute = new MatchingProcessDto(Optional.<String>absent());
        register(uri, OK, cycle3Attribute);
    }

    public void setUpStubForUserAccountCreation(String rpEntityId, List<UserAccountCreationAttribute> userAccountCreationAttributes) throws JsonProcessingException {
        String uri = UriBuilder.fromPath(Urls.ConfigUrls.USER_ACCOUNT_CREATION_ATTRIBUTES_RESOURCE).build(rpEntityId).getPath();
        register(uri, OK, userAccountCreationAttributes);
    }

    public void setupStubForEidasCountries(List<EidasCountryDto> eidasCountryDtos) throws JsonProcessingException {
        String uri = Urls.ConfigUrls.COUNTRIES_ROOT;
        register(uri, OK, eidasCountryDtos);
    }

    public void setupStubForIdpConfig(String idpEntityId, IdpConfigDto idpConfigDto) throws JsonProcessingException {
        String uri = UriBuilder.fromPath(Urls.ConfigUrls.IDENTITY_PROVIDER_CONFIG_DATA_RESOURCE).build(idpEntityId).getPath();
        register(uri, OK, idpConfigDto);
    }

    public void setupStubForEidasRPCountries(String rpEntityId, List<String> countryEntityIds) throws JsonProcessingException {
        String uri = UriBuilder.fromPath(Urls.ConfigUrls.EIDAS_RP_COUNTRIES_FOR_TRANSACTION_RESOURCE)
            .build(StringEncoding.urlEncode(rpEntityId).replace("+", "%20"))
            .getPath();
        register(uri, OK, countryEntityIds);
    }

    private void setupStubForEnabledIdpsForLoa(String transactionEntityId, LevelOfAssurance supportedLoa, Collection<String> enabledIdps) throws JsonProcessingException {
        register(UriBuilder.fromPath(Urls.ConfigUrls.ENABLED_ID_PROVIDERS_FOR_LOA_RESOURCE).buildFromEncoded(StringEncoding.urlEncode(transactionEntityId), supportedLoa).getPath(), OK, enabledIdps);
    }

    private void setupStubForEnabledIdpsForSignIn(String transactionEntityId, Collection<String> enabledIdps) throws JsonProcessingException {
        register(UriBuilder.fromPath(Urls.ConfigUrls.ENABLED_ID_PROVIDERS_FOR_SIGN_IN_RESOURCE).buildFromEncoded(StringEncoding.urlEncode(transactionEntityId)).getPath(), OK, enabledIdps);
    }

    private void setupStubForIdpConfig(Collection<String> enabledIdps, LevelOfAssurance supportedLoa) throws JsonProcessingException {
        for(String idpEntityId:enabledIdps) {
            register(UriBuilder.fromPath(Urls.ConfigUrls.IDENTITY_PROVIDER_CONFIG_DATA_RESOURCE).build(idpEntityId).getPath(), OK, IdpConfigDtoBuilder.anIdpConfigDto().withLevelsOfAssurance(supportedLoa).build());
        }
    }
}
