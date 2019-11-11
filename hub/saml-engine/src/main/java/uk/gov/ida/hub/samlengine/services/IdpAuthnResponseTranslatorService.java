package uk.gov.ida.hub.samlengine.services;

import com.google.common.base.Strings;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.eidas.logging.EidasAttributesLogger;
import uk.gov.ida.eidas.logging.EidasResponseAttributesHashLogger;
import uk.gov.ida.hub.samlengine.contracts.SamlAuthnResponseTranslatorDto;
import uk.gov.ida.hub.samlengine.domain.InboundResponseFromIdpDto;
import uk.gov.ida.hub.samlengine.domain.LevelOfAssurance;
import uk.gov.ida.hub.samlengine.exceptions.SamlContextException;
import uk.gov.ida.hub.samlengine.logging.IdpAssertionMetricsCollector;
import uk.gov.ida.hub.samlengine.logging.MdcHelper;
import uk.gov.ida.hub.samlengine.logging.NotOnOrAfterLogger;
import uk.gov.ida.hub.samlengine.logging.UnknownMethodAlgorithmLogger;
import uk.gov.ida.hub.samlengine.logging.VerifiedAttributesLogger;
import uk.gov.ida.hub.samlengine.proxy.TransactionsConfigProxy;
import uk.gov.ida.saml.core.domain.InboundResponseFromIdpData;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.deserializers.StringToOpenSamlObjectTransformer;
import uk.gov.ida.saml.hub.domain.IdpIdaStatus;
import uk.gov.ida.saml.hub.domain.InboundResponseFromIdp;
import uk.gov.ida.saml.hub.factories.UserIdHashFactory;
import uk.gov.ida.saml.hub.transformers.inbound.InboundResponseFromIdpDataGenerator;
import uk.gov.ida.saml.hub.transformers.inbound.providers.DecoratedSamlResponseToIdaResponseIssuedByIdpTransformer;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Optional;


public class IdpAuthnResponseTranslatorService {

    private static final Logger LOG = LoggerFactory.getLogger(IdpAuthnResponseTranslatorService.class);

    private static final String AUTHN_STATEMENT = "AuthnStatement";
    private static final String MATCHING_DATASET = "MatchingDataset";
    private final StringToOpenSamlObjectTransformer<Response> stringToOpenSamlResponseTransformer;
    private final StringToOpenSamlObjectTransformer<Assertion> stringToAssertionTransformer;
    private final DecoratedSamlResponseToIdaResponseIssuedByIdpTransformer samlResponseToIdaResponseIssuedByIdpTransformer;
    private InboundResponseFromIdpDataGenerator inboundResponseFromIdpDataGenerator;
    private final IdpAssertionMetricsCollector idpAssertionMetricsCollector;
    private final TransactionsConfigProxy transactionsConfigProxy;

    @Inject
    public IdpAuthnResponseTranslatorService(StringToOpenSamlObjectTransformer<Response> stringToOpenSamlResponseTransformer,
                                             StringToOpenSamlObjectTransformer<Assertion> stringToAssertionTransformer,
                                                 @Named("IdpSamlResponseTransformer") DecoratedSamlResponseToIdaResponseIssuedByIdpTransformer samlResponseToIdaResponseIssuedByIdpTransformer,
                                             InboundResponseFromIdpDataGenerator inboundResponseFromIdpDataGenerator,
                                             IdpAssertionMetricsCollector idpAssertionMetricsCollector,
                                             TransactionsConfigProxy transactionsConfigProxy) {
        this.stringToOpenSamlResponseTransformer = stringToOpenSamlResponseTransformer;
        this.stringToAssertionTransformer = stringToAssertionTransformer;
        this.samlResponseToIdaResponseIssuedByIdpTransformer = samlResponseToIdaResponseIssuedByIdpTransformer;
        this.inboundResponseFromIdpDataGenerator = inboundResponseFromIdpDataGenerator;
        this.idpAssertionMetricsCollector = idpAssertionMetricsCollector;
        this.transactionsConfigProxy = transactionsConfigProxy;
    }

    public InboundResponseFromIdpDto translate(SamlAuthnResponseTranslatorDto samlResponseDto) {
        Response response = stringToOpenSamlResponseTransformer.apply(samlResponseDto.getSamlResponse());
        MdcHelper.addContextToMdc(response);
        try {
            String matchingServiceEntityId = samlResponseDto.getMatchingServiceEntityId();
            if (transactionsConfigProxy.isProxyNodeEntityId(matchingServiceEntityId)) {
                samlResponseToIdaResponseIssuedByIdpTransformer.setEidasAttributesLogger(
                        new EidasAttributesLogger(EidasResponseAttributesHashLogger::instance, new UserIdHashFactory(matchingServiceEntityId))
                );
            }
            InboundResponseFromIdp idaResponseFromIdp = samlResponseToIdaResponseIssuedByIdpTransformer.apply(response);
            UnknownMethodAlgorithmLogger.probeResponseForMethodAlgorithm(idaResponseFromIdp);
            if (idaResponseFromIdp.getAuthnStatementAssertion().isPresent()) {
                Assertion authnStatementAssertion = stringToAssertionTransformer.apply(idaResponseFromIdp.getAuthnStatementAssertion().get().getUnderlyingAssertionBlob());
                logAnalytics(authnStatementAssertion, AUTHN_STATEMENT);
            }

            Assertion matchingDatasetAssertion = null;
            if (idaResponseFromIdp.getMatchingDatasetAssertion().isPresent()) {
                matchingDatasetAssertion = stringToAssertionTransformer.apply(idaResponseFromIdp.getMatchingDatasetAssertion().get().getUnderlyingAssertionBlob());
                logAnalytics(matchingDatasetAssertion, MATCHING_DATASET);
            }

            InboundResponseFromIdpData inboundResponseFromIdpData = inboundResponseFromIdpDataGenerator.generate(idaResponseFromIdp, samlResponseDto.getMatchingServiceEntityId());

            Optional<LevelOfAssurance> levelOfAssurance = Optional.empty();
            if (!Strings.isNullOrEmpty(inboundResponseFromIdpData.getLevelOfAssurance())) {
                levelOfAssurance = Optional.of(LevelOfAssurance.valueOf(inboundResponseFromIdpData.getLevelOfAssurance()));
            }

            logVerifiedAttributes(idaResponseFromIdp, matchingDatasetAssertion, levelOfAssurance);

            return new InboundResponseFromIdpDto(
                    inboundResponseFromIdpData.getStatus(),
                    inboundResponseFromIdpData.getStatusMessage(),
                    inboundResponseFromIdpData.getIssuer(),
                    inboundResponseFromIdpData.getEncryptedAuthnAssertion(),
                    inboundResponseFromIdpData.getEncryptedMatchingDatasetAssertion(),
                    inboundResponseFromIdpData.getPersistentId(),
                    inboundResponseFromIdpData.getPrincipalIpAddressAsSeenByIdp(),
                    levelOfAssurance,
                    inboundResponseFromIdpData.getIdpFraudEventId(),
                    inboundResponseFromIdpData.getFraudIndicator(),
                    inboundResponseFromIdpData.getNotOnOrAfter());
        } catch (SamlTransformationErrorException e) {
            throw new SamlContextException(response.getID(), response.getIssuer().getValue(), e);
        }
    }

    private void logVerifiedAttributes(InboundResponseFromIdp idaResponseFromIdp, Assertion matchingDatasetAssertion, Optional<LevelOfAssurance> levelOfAssurance) {
        if (IdpIdaStatus.success().equals(idaResponseFromIdp.getStatus()) && matchingDatasetAssertion != null) {
            try {
                VerifiedAttributesLogger.probeAssertionForVerifiedAttributes(matchingDatasetAssertion, levelOfAssurance.orElse(null));
            } catch (Exception e) {
                LOG.error("Failed to log verified attributes on assertion", e);
            }
        }
    }

    private void logAnalytics(Assertion authnStatementAssertion, String assertionType) {
        UnknownMethodAlgorithmLogger.probeAssertionForMethodAlgorithm(authnStatementAssertion, assertionType);
        NotOnOrAfterLogger.logAssertionNotOnOrAfter(authnStatementAssertion, assertionType);
        idpAssertionMetricsCollector.update(authnStatementAssertion);
    }
}

