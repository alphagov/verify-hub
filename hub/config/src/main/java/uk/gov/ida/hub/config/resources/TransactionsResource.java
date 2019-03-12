package uk.gov.ida.hub.config.resources;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableList;
import uk.gov.ida.hub.config.Urls;
import uk.gov.ida.hub.config.data.ConfigEntityDataRepository;
import uk.gov.ida.hub.config.domain.LevelOfAssurance;
import uk.gov.ida.hub.config.domain.TransactionConfigEntityData;
import uk.gov.ida.hub.config.domain.TranslationData;
import uk.gov.ida.hub.config.domain.UserAccountCreationAttribute;
import uk.gov.ida.hub.config.dto.MatchingProcessDto;
import uk.gov.ida.hub.config.dto.ResourceLocationDto;
import uk.gov.ida.hub.config.dto.TransactionSingleIdpData;
import uk.gov.ida.hub.config.dto.TransactionDisplayData;
import uk.gov.ida.hub.config.exceptions.ExceptionFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Path(Urls.ConfigUrls.TRANSACTIONS_ROOT)
@Produces(MediaType.APPLICATION_JSON)
public class TransactionsResource {

    private final ConfigEntityDataRepository<TransactionConfigEntityData> transactionConfigEntityDataRepository;
    private final ConfigEntityDataRepository<TranslationData> translationConfigEntityDataRepository;
    private final ExceptionFactory exceptionFactory;

    @Inject
    public TransactionsResource(
            ConfigEntityDataRepository<TransactionConfigEntityData> transactionConfigEntityDataRepository,
            ConfigEntityDataRepository<TranslationData> translationConfigEntityDataRepository,
            ExceptionFactory exceptionFactory) {

        this.transactionConfigEntityDataRepository = transactionConfigEntityDataRepository;
        this.translationConfigEntityDataRepository = translationConfigEntityDataRepository;
        this.exceptionFactory = exceptionFactory;
    }

    @GET
    @Path(Urls.ConfigUrls.ASSERTION_CONSUMER_SERVICE_URI_PATH)
    @Timed
    public ResourceLocationDto getAssertionConsumerServiceUri(
            @PathParam(Urls.SharedUrls.ENTITY_ID_PARAM) String entityId,
            @QueryParam(Urls.ConfigUrls.ASSERTION_CONSUMER_SERVICE_INDEX_PARAM) Optional<Integer> assertionConsumerServiceIndex) {

        final TransactionConfigEntityData configData = getTransactionConfigData(entityId);

        final Optional<URI> assertionConsumerServiceUri = configData.getAssertionConsumerServiceUri(assertionConsumerServiceIndex);
        if (!assertionConsumerServiceUri.isPresent()) {
            // we know that the index must be here because we will have pre-validated that there will be a default for the transaction
            throw exceptionFactory.createInvalidAssertionConsumerServiceIndexException(
                    entityId,
                    assertionConsumerServiceIndex.get());
        }
        return new ResourceLocationDto(assertionConsumerServiceUri.get());
    }

    @GET
    @Path(Urls.ConfigUrls.TRANSACTION_DISPLAY_DATA_PATH)
    @Timed
    public TransactionDisplayData getDisplayData(
            @PathParam(Urls.SharedUrls.ENTITY_ID_PARAM) String entityId) {

        final TransactionConfigEntityData configData = getTransactionConfigData(entityId);
        return buildTransactionDisplayData(configData);
    }

    private TransactionDisplayData buildTransactionDisplayData(TransactionConfigEntityData configData) {
        return new TransactionDisplayData(
                configData.getSimpleId().orElse(null),
                configData.getServiceHomepage(),
                configData.getLevelsOfAssurance(),
                configData.getHeadlessStartpage());
    }

    @GET
    @Path(Urls.ConfigUrls.MATCHING_PROCESS_PATH)
    @Timed
    public MatchingProcessDto getMatchingProcess(
            @PathParam(Urls.SharedUrls.ENTITY_ID_PARAM) String entityId) {

        final TransactionConfigEntityData configData = getTransactionConfigData(entityId);
        if (configData.getMatchingProcess().isPresent()) {
            return new MatchingProcessDto(
                    configData.getMatchingProcess().get().getCycle3AttributeName());
        }

        return new MatchingProcessDto(null);
    }

    @GET
    @Path(Urls.ConfigUrls.ENABLED_TRANSACTIONS_PATH)
    @Timed
    public List<TransactionDisplayData> getEnabledTransactions(){
        Set<TransactionConfigEntityData> allData = transactionConfigEntityDataRepository.getAllData();
        return allData.stream()
            .filter(TransactionConfigEntityData::isEnabled)
            .map(t -> new TransactionDisplayData(t.getSimpleId().orElse(null), t.getServiceHomepage(), t.getLevelsOfAssurance(), t.getHeadlessStartpage()))
            .collect(Collectors.toList());
    }

    @GET
    @Path(Urls.ConfigUrls.SINGLE_IDP_ENABLED_LIST_PATH)
    @Timed
    public List<TransactionSingleIdpData> getSingleIDPEnabledServiceListTransactions(){
        Set<TransactionConfigEntityData> allData = transactionConfigEntityDataRepository.getAllData();
        return allData.stream()
                .filter(TransactionConfigEntityData::isEnabled)
                .filter(TransactionConfigEntityData::isEnabledForSingleIdp)
                .map(t -> new TransactionSingleIdpData(t.getSimpleId().orElse(null),
                        t.getSingleIdpStartPage().orElse(t.getServiceHomepage()),
                        t.getLevelsOfAssurance(),
                        t.getEntityId())
                )
                .collect(Collectors.toList());
    }

    @GET
    @Path(Urls.ConfigUrls.TRANSLATIONS_LOCALE_PATH)
    @Timed
    public TranslationData.Translation getTranslation(@PathParam(Urls.SharedUrls.SIMPLE_ID_PARAM) String simpleId, @PathParam(Urls.SharedUrls.LOCALE_PARAM) String locale) {
        final TranslationData translationData = getTranslationData(simpleId);
        Optional<TranslationData.Translation> translation = translationData.getTranslationsByLocale(locale);
        if (!translation.isPresent()) throw exceptionFactory.createNoTranslationForLocaleException(locale);
        return translation.get();
    }

    @GET
    @Path(Urls.ConfigUrls.LEVELS_OF_ASSURANCE_PATH)
    @Timed
    public List<LevelOfAssurance> getLevelsOfAssurance(
            @PathParam(Urls.SharedUrls.ENTITY_ID_PARAM) String entityId) {

        final TransactionConfigEntityData configData = getTransactionConfigData(entityId);
        return configData.getLevelsOfAssurance();
    }

    @GET
    @Path(Urls.ConfigUrls.MATCHING_SERVICE_ENTITY_ID_PATH)
    @Timed
    public String getMatchingServiceEntityId(@PathParam(Urls.SharedUrls.ENTITY_ID_PARAM) String entityId) {
        final TransactionConfigEntityData configData = getTransactionConfigData(entityId);
        return configData.getMatchingServiceEntityId();
    }

    @GET
    @Path(Urls.ConfigUrls.USER_ACCOUNT_CREATION_ATTRIBUTES_PATH)
    @Timed
    public List<UserAccountCreationAttribute> getUserAccountCreationAttributes(@PathParam(Urls.SharedUrls.ENTITY_ID_PARAM) String entityId) {
        final TransactionConfigEntityData configData = getTransactionConfigData(entityId);

        return configData.getUserAccountCreationAttributes().orElse(Collections.<UserAccountCreationAttribute>emptyList());
    }

    @GET
    @Path(Urls.ConfigUrls.SHOULD_HUB_SIGN_RESPONSE_MESSAGES_PATH)
    @Timed
    public boolean getShouldHubSignResponseMessages(@PathParam(Urls.SharedUrls.ENTITY_ID_PARAM) String entityId) {
        final TransactionConfigEntityData configData = getTransactionConfigData(entityId);
        return configData.getShouldHubSignResponseMessages();
    }

    @GET
    @Path(Urls.ConfigUrls.SHOULD_HUB_USE_LEGACY_SAML_STANDARD_PATH)
    @Timed
    public boolean getShouldHubUseLegacySamlStandard(@PathParam(Urls.SharedUrls.ENTITY_ID_PARAM) String entityId) {
        final TransactionConfigEntityData configData = getTransactionConfigData(entityId);
        return configData.getShouldHubUseLegacySamlStandard();
    }

    @GET
    @Path(Urls.ConfigUrls.EIDAS_ENABLED_FOR_TRANSACTION_PATH)
    @Timed
    public boolean isEidasEnabledForTransaction(@PathParam(Urls.SharedUrls.ENTITY_ID_PARAM) String entityId){
        final TransactionConfigEntityData configData = getTransactionConfigData(entityId);
        return configData.isEidasEnabled();
    }

    @GET
    @Path(Urls.ConfigUrls.EIDAS_COUNTRIES_FOR_TRANSACTION_PATH)
    @Timed
    public List<String> getEidasCountries(@PathParam(Urls.SharedUrls.ENTITY_ID_PARAM) String entityId){
        final TransactionConfigEntityData configData = getTransactionConfigData(entityId);
        Optional<List<String>> eidasCountries = configData.getEidasCountries();
        return eidasCountries.isPresent() ? eidasCountries.get() : ImmutableList.of();
    }

    @GET
    @Path(Urls.ConfigUrls.SHOULD_SIGN_WITH_SHA1_PATH)
    @Timed
    public boolean getShouldSignWithSHA1(@PathParam(Urls.SharedUrls.ENTITY_ID_PARAM) String entityId) {
        final TransactionConfigEntityData configData = getTransactionConfigData(entityId);
        return configData.getShouldSignWithSHA1();
    }
    @GET
    @Path(Urls.ConfigUrls.MATCHING_ENABLED_FOR_TRANSACTION_PATH)
    @Timed
    public boolean isUsingMatching(@PathParam(Urls.SharedUrls.ENTITY_ID_PARAM) String entityId){
        final TransactionConfigEntityData configData = getTransactionConfigData(entityId);
        return configData.isUsingMatching();
    }

    @GET
    @Path(Urls.ConfigUrls.IS_EIDAS_PROXY_NODE_ENABLED_FOR_TRANSACTION_PATH)
    @Timed
    public boolean isEidasProxyNode(@PathParam(Urls.SharedUrls.ENTITY_ID_PARAM) String entityId){
        final TransactionConfigEntityData configData = getTransactionConfigData(entityId);
        return configData.isEidasProxyNode();
    }

    private TransactionConfigEntityData getTransactionConfigData(String entityId) {
        final Optional<TransactionConfigEntityData> configData = transactionConfigEntityDataRepository.getData(entityId);
        if (!configData.isPresent()) {
            throw exceptionFactory.createNoDataForEntityException(entityId);
        }
        if (!configData.get().isEnabled()) {
            throw exceptionFactory.createDisabledTransactionException(entityId);
        }
        return configData.get();
    }

    private TranslationData getTranslationData(String simpleId) {
        final Optional<TranslationData> data = translationConfigEntityDataRepository.getData(simpleId);
        if (!data.isPresent()) throw exceptionFactory.createNoDataForEntityException(simpleId);
        return data.get();
    }
}
