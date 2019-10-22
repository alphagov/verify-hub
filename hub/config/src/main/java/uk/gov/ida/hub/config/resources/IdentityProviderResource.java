package uk.gov.ida.hub.config.resources;

import com.codahale.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.hub.config.Urls;
import uk.gov.ida.hub.config.data.LocalConfigRepository;
import uk.gov.ida.hub.config.domain.IdentityProviderConfig;
import uk.gov.ida.hub.config.domain.LevelOfAssurance;
import uk.gov.ida.hub.config.domain.filters.IdpPredicateFactory;
import uk.gov.ida.hub.config.dto.IdpConfigDto;
import uk.gov.ida.hub.config.dto.IdpDto;
import uk.gov.ida.hub.config.exceptions.ExceptionFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Path(Urls.ConfigUrls.IDENTITY_PROVIDER_ROOT)
@Produces(MediaType.APPLICATION_JSON)
public class IdentityProviderResource {

    private final LocalConfigRepository<IdentityProviderConfig> identityProviderConfigRepository;
    private IdpPredicateFactory idpPredicateFactory;
    private final ExceptionFactory exceptionFactory;

    private static final Logger LOG = LoggerFactory.getLogger(IdentityProviderResource.class);

    @Inject
    public IdentityProviderResource(
            LocalConfigRepository<IdentityProviderConfig> identityProviderConfigRepository,
            IdpPredicateFactory idpPredicateFactory,
            ExceptionFactory exceptionFactory) {

        this.identityProviderConfigRepository = identityProviderConfigRepository;
        this.idpPredicateFactory = idpPredicateFactory;
        this.exceptionFactory = exceptionFactory;
    }

    @GET
    @Path(Urls.ConfigUrls.IDP_LIST_FOR_TRANSACTION_AND_LOA_PATH)
    @Timed
    public List<IdpDto> getIdpList(@PathParam(Urls.SharedUrls.TRANSACTION_ENTITY_ID_PARAM) final String transactionEntityId,
                                   @PathParam(Urls.SharedUrls.LEVEL_OF_ASSURANCE_PARAM) final LevelOfAssurance levelOfAssurance) {
        Collection<IdentityProviderConfig> matchingIdps = getIdentityProviderConfig(transactionEntityId, levelOfAssurance);
        return matchingIdps.stream()
                .map(configData ->
                        new IdpDto(
                                configData.getSimpleId(),
                                configData.getEntityId(),
                                configData.getSupportedLevelsOfAssurance(),
                                configData.isAuthenticationEnabled(),
                                configData.isTemporarilyUnavailable()))
                .collect(Collectors.toList());
    }

    @GET
    @Path(Urls.ConfigUrls.IDP_LIST_FOR_SIGN_IN_PATH)
    @Timed
    public List<IdpDto> getIdpListForSignIn(@PathParam(Urls.SharedUrls.TRANSACTION_ENTITY_ID_PARAM) final String transactionEntityId) {
        Collection<IdentityProviderConfig> matchingIdps = getIdentityProviderConfigForSignIn(transactionEntityId);
        return matchingIdps.stream()
                .map(configData ->
                        new IdpDto(
                                configData.getSimpleId(),
                                configData.getEntityId(),
                                configData.getSupportedLevelsOfAssurance(),
                                configData.isAuthenticationEnabled(),
                                configData.isTemporarilyUnavailable()))
                .collect(Collectors.toList());
    }

    @GET
    @Path(Urls.ConfigUrls.IDP_LIST_FOR_SINGLE_IDP_PATH)
    @Timed
    public List<IdpDto> getIdpListForSingleIdp(@PathParam(Urls.SharedUrls.TRANSACTION_ENTITY_ID_PARAM) final String transactionEntityId) {
        Collection<IdentityProviderConfig> matchingIdps = getIdentityProviderConfigForSingleIdp(transactionEntityId);
        return matchingIdps.stream()
                .map(configData ->
                        new IdpDto(
                                configData.getSimpleId(),
                                configData.getEntityId(),
                                configData.getSupportedLevelsOfAssurance(),
                                configData.isAuthenticationEnabled(),
                                configData.isTemporarilyUnavailable()))
                .collect(Collectors.toList());
    }

    @GET
    @Path(Urls.ConfigUrls.IDP_CONFIG_DATA)
    @Timed
    public IdpConfigDto getIdpConfig(@PathParam(Urls.SharedUrls.ENTITY_ID_PARAM) String entityId) {

        IdentityProviderConfig idpData = getIdentityProviderConfigData(entityId);
        return new IdpConfigDto(
                idpData.getSimpleId(),
                idpData.isEnabled(),
                idpData.getSupportedLevelsOfAssurance(),
                idpData.getUseExactComparisonType()
        );
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

    private IdentityProviderConfig getIdentityProviderConfigData(String identityProviderEntityId) {
        final IdentityProviderConfig configData = identityProviderConfigRepository.getData(identityProviderEntityId)
                .orElseThrow(() -> exceptionFactory.createNoDataForEntityException(identityProviderEntityId));

        if (!configData.isEnabled()) {
            throw exceptionFactory.createDisabledIdentityProviderException(identityProviderEntityId);
        }

        return configData;
    }

    private Set<IdentityProviderConfig> getIdentityProviderConfig(String transactionEntityId,
                                                                  LevelOfAssurance levelOfAssurance) {
        Predicate<IdentityProviderConfig> predicateForTransactionEntity =
                idpPredicateFactory.createPredicateForTransactionEntityAndLoa(transactionEntityId, levelOfAssurance);
        return idpsFilteredBy(predicateForTransactionEntity);
    }

    private Set<IdentityProviderConfig> getIdentityProviderConfigForSignIn(String transactionEntityId) {
        Predicate<IdentityProviderConfig> predicateForTransactionEntity =
                idpPredicateFactory.createPredicateForSignIn(transactionEntityId);
        return idpsFilteredBy(predicateForTransactionEntity);
    }

    private Set<IdentityProviderConfig> getIdentityProviderConfigForSingleIdp(String transactionEntityId) {
        Predicate<IdentityProviderConfig> predicateForTransactionEntity =
                idpPredicateFactory.createPredicateForSingleIdp(transactionEntityId);
        return idpsFilteredBy(predicateForTransactionEntity);
    }

    private Set<IdentityProviderConfig> idpsFilteredBy(Predicate<IdentityProviderConfig> predicateForTransactionEntity) {
        return identityProviderConfigRepository.getAllData()
                .stream()
                .filter(predicateForTransactionEntity)
                .collect(Collectors.toSet());
    }
}
