package uk.gov.ida.hub.config.resources;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import uk.gov.ida.hub.config.Urls;
import uk.gov.ida.hub.config.data.ConfigEntityDataRepository;
import uk.gov.ida.hub.config.domain.IdentityProviderConfigEntityData;
import uk.gov.ida.hub.config.domain.LevelOfAssurance;
import uk.gov.ida.hub.config.domain.filters.IdpEntityIdExtractor;
import uk.gov.ida.hub.config.domain.filters.IdpPredicateFactory;
import uk.gov.ida.hub.config.dto.IdpConfigDto;
import uk.gov.ida.hub.config.dto.IdpDto;
import uk.gov.ida.hub.config.exceptions.ExceptionFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Path(Urls.ConfigUrls.IDENTITY_PROVIDER_ROOT)
@Produces(MediaType.APPLICATION_JSON)
public class IdentityProviderResource {

    private final ConfigEntityDataRepository<IdentityProviderConfigEntityData> identityProviderConfigEntityDataRepository;
    private IdpPredicateFactory idpPredicateFactory;
    private final ExceptionFactory exceptionFactory;

    @Inject
    public IdentityProviderResource(
            ConfigEntityDataRepository<IdentityProviderConfigEntityData> identityProviderConfigEntityDataRepository,
            IdpPredicateFactory idpPredicateFactory,
            ExceptionFactory exceptionFactory) {

        this.identityProviderConfigEntityDataRepository = identityProviderConfigEntityDataRepository;
        this.idpPredicateFactory = idpPredicateFactory;
        this.exceptionFactory = exceptionFactory;
    }

    @GET
    @Path(Urls.ConfigUrls.IDP_LIST_PATH)
    @Timed
    @Deprecated
    public List<IdpDto> getIdpList(@QueryParam(Urls.SharedUrls.TRANSACTION_ENTITY_ID_PARAM)
                                   final Optional<String> transactionEntityId) {

        Collection<IdentityProviderConfigEntityData> matchingIdps = getIdentityProviderConfigEntityData(transactionEntityId);
        return matchingIdps.stream().map(configData ->
                new IdpDto(configData.getSimpleId(), configData.getEntityId(), configData.getSupportedLevelsOfAssurance(), configData.isAuthenticationEnabled())).collect(Collectors.toList());
    }

    @GET
    @Path(Urls.ConfigUrls.IDP_LIST_FOR_TRANSACTION_AND_LOA_PATH)
    @Timed
    public List<IdpDto> getIdpList(@PathParam(Urls.SharedUrls.TRANSACTION_ENTITY_ID_PARAM) final String transactionEntityId,
                                   @PathParam(Urls.SharedUrls.LEVEL_OF_ASSURANCE_PARAM) final LevelOfAssurance levelOfAssurance) {
        Collection<IdentityProviderConfigEntityData> matchingIdps = getIdentityProviderConfigEntityData(transactionEntityId, levelOfAssurance);
        return matchingIdps.stream()
                .map(configData ->
                        new IdpDto(
                                configData.getSimpleId(),
                                configData.getEntityId(),
                                configData.getSupportedLevelsOfAssurance(),
                                configData.isAuthenticationEnabled()))
                .collect(Collectors.toList());
    }

    @GET
    @Path(Urls.ConfigUrls.IDP_LIST_FOR_SIGN_IN_PATH)
    @Timed
    public List<IdpDto> getIdpListForSignIn(@PathParam(Urls.SharedUrls.TRANSACTION_ENTITY_ID_PARAM) final String transactionEntityId) {
        Collection<IdentityProviderConfigEntityData> matchingIdps = getIdentityProviderConfigEntityDataForSignIn(transactionEntityId);
        return matchingIdps.stream()
                .map(configData ->
                        new IdpDto(
                                configData.getSimpleId(),
                                configData.getEntityId(),
                                configData.getSupportedLevelsOfAssurance(),
                                configData.isAuthenticationEnabled()))
                .collect(Collectors.toList());
    }

    @GET
    @Path(Urls.ConfigUrls.IDP_LIST_FOR_SINGLE_IDP_PATH)
    @Timed
    public List<IdpDto> getIdpListForSingleIdp(@PathParam(Urls.SharedUrls.TRANSACTION_ENTITY_ID_PARAM) final String transactionEntityId) {
        Collection<IdentityProviderConfigEntityData> matchingIdps = getIdentityProviderConfigEntityDataForSingleIdp(transactionEntityId);
        return matchingIdps.stream()
                .map(configData ->
                        new IdpDto(
                                configData.getSimpleId(),
                                configData.getEntityId(),
                                configData.getSupportedLevelsOfAssurance(),
                                configData.isAuthenticationEnabled()))
                .collect(Collectors.toList());
    }

    @GET
    @Path(Urls.ConfigUrls.IDP_CONFIG_DATA)
    @Timed
    public IdpConfigDto getIdpConfig(@PathParam(Urls.SharedUrls.ENTITY_ID_PARAM) String entityId) {

        IdentityProviderConfigEntityData idpData = getIdentityProviderConfigData(entityId);
        return new IdpConfigDto(
                idpData.getSimpleId(),
                idpData.isEnabled(),
                idpData.getSupportedLevelsOfAssurance(),
                idpData.getUseExactComparisonType()
        );
    }

    @GET
    @Path(Urls.ConfigUrls.ENABLED_IDENTITY_PROVIDERS_PATH)
    @Timed
    @Deprecated
    public Collection<String> getEnabledIdentityProviderEntityIds(
            @QueryParam(Urls.SharedUrls.TRANSACTION_ENTITY_ID_PARAM) final Optional<String> transactionEntityId) {

        return getEnabledIdentityProviderEntityIdsPathParam(transactionEntityId);
    }

    @GET
    @Path(Urls.ConfigUrls.ENABLED_IDENTITY_PROVIDERS_PARAM_PATH)
    @Timed
    @Deprecated
    public Collection<String> getEnabledIdentityProviderEntityIdsPathParam(
            @PathParam(Urls.SharedUrls.ENTITY_ID_PARAM) final Optional<String> transactionEntityId) {

        Collection<IdentityProviderConfigEntityData> matchingIdps = getIdentityProviderConfigEntityData(transactionEntityId);

        return Collections2.transform(matchingIdps, new IdpEntityIdExtractor());
    }

    @GET
    @Path(Urls.ConfigUrls.ENABLED_ID_PROVIDERS_FOR_SIGN_IN_PATH)
    @Timed
    public Collection<String> getEnabledIdentityProviderEntityIdsForSignIn(
            @PathParam(Urls.SharedUrls.ENTITY_ID_PARAM) final String transactionEntityId) {
        return getIdpListForSignIn(transactionEntityId).stream().map(IdpDto::getEntityId).collect(Collectors.toList());
    }

    @GET
    @Path(Urls.ConfigUrls.ENABLED_ID_PROVIDERS_FOR_LOA_PATH)
    @Timed
    public Collection<String> getEnabledIdentityProviderEntityIdsForLoa(
            @PathParam(Urls.SharedUrls.ENTITY_ID_PARAM) final String transactionEntityId,
            @PathParam(Urls.SharedUrls.LEVEL_OF_ASSURANCE_PARAM) final LevelOfAssurance levelOfAssurance) {
        return getIdpList(transactionEntityId, levelOfAssurance).stream().map(IdpDto::getEntityId).collect(Collectors.toList());
    }

    private IdentityProviderConfigEntityData getIdentityProviderConfigData(String identityProviderEntityId) {
        final Optional<IdentityProviderConfigEntityData> configData = identityProviderConfigEntityDataRepository.getData(identityProviderEntityId);
        if (!configData.isPresent()) {
            throw exceptionFactory.createNoDataForEntityException(identityProviderEntityId);
        }
        if (!configData.get().isEnabled()) {
            throw exceptionFactory.createDisabledIdentityProviderException(identityProviderEntityId);
        }
        return configData.get();
    }

    @Deprecated
    private Set<IdentityProviderConfigEntityData> getIdentityProviderConfigEntityData(Optional<String> transactionEntityId) {
        Set<Predicate<IdentityProviderConfigEntityData>> predicatesForTransactionEntity = idpPredicateFactory.createPredicatesForTransactionEntity(transactionEntityId);
        return idpsFilteredBy(predicatesForTransactionEntity);
    }

    private Set<IdentityProviderConfigEntityData> getIdentityProviderConfigEntityData(String transactionEntityId,
                                                                                      LevelOfAssurance levelOfAssurance) {
        Set<Predicate<IdentityProviderConfigEntityData>> predicatesForTransactionEntity =
                idpPredicateFactory.createPredicatesForTransactionEntityAndLoa(transactionEntityId, levelOfAssurance);
        return idpsFilteredBy(predicatesForTransactionEntity);
    }

    private Set<IdentityProviderConfigEntityData> getIdentityProviderConfigEntityDataForSignIn(String transactionEntityId) {
        Set<Predicate<IdentityProviderConfigEntityData>> predicatesForTransactionEntity =
                idpPredicateFactory.createPredicatesForSignIn(transactionEntityId);
        return idpsFilteredBy(predicatesForTransactionEntity);
    }

    private Set<IdentityProviderConfigEntityData> getIdentityProviderConfigEntityDataForSingleIdp(String transactionEntityId) {
        Set<Predicate<IdentityProviderConfigEntityData>> predicatesForTransactionEntity =
                idpPredicateFactory.createPredicatesForSingleIdp(transactionEntityId);
        return idpsFilteredBy(predicatesForTransactionEntity);
    }

    private Set<IdentityProviderConfigEntityData> idpsFilteredBy(Set<Predicate<IdentityProviderConfigEntityData>> predicatesForTransactionEntity) {
        return Sets.filter(identityProviderConfigEntityDataRepository.getAllData(), Predicates.and(predicatesForTransactionEntity));
    }
}
