package uk.gov.ida.hub.policy.domain.exception;

import org.slf4j.event.Level;
import uk.gov.ida.hub.policy.domain.IdpIdaStatus;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.util.List;
import java.util.Optional;

import static java.text.MessageFormat.format;

public class StateProcessingValidationException extends RuntimeException {
    private Level level;

    public StateProcessingValidationException(String message, Level level) {
        super(message);
        this.level = level;
    }

    public static StateProcessingValidationException wrongResponseIssuer(String requestId, String responseIssuer, String expectedIssuer) {
        return new StateProcessingValidationException(format("Response to request ID [{0}] came from [{1}] and was expected to come from [{2}]", requestId, responseIssuer, expectedIssuer), Level.WARN);
    }

    public static StateProcessingValidationException unavailableIdp(String idpEntityId, SessionId sessionId) {
        return new StateProcessingValidationException(format("Available Identity Provider for session ID [{0}] not found for entity ID [{1}].", sessionId.getSessionId(), idpEntityId), Level.ERROR);
    }

    public static StateProcessingValidationException wrongLevelOfAssurance(Optional<LevelOfAssurance> loa, List<LevelOfAssurance> expectedLevels) {
        return new StateProcessingValidationException(format("Level of assurance in the response does not match level of assurance in the request. Was [{0}] but expected [{1}].", loa.map(LevelOfAssurance::name).orElse("null"), expectedLevels), Level.ERROR);
    }

    public static StateProcessingValidationException noLevelOfAssurance() {
        return new StateProcessingValidationException("No level of assurance in the response.", Level.ERROR);
    }

    public static StateProcessingValidationException wrongInResponseTo(String expectedId, String inResponseTo) {
        return new StateProcessingValidationException(format("Response to request ID [{0}] was in response to [{1}]", expectedId, inResponseTo), Level.ERROR);
    }

    public static StateProcessingValidationException transactionLevelsOfAssuranceUnsupportedByIDP(String requestIssuerEntityId, List<LevelOfAssurance> levelsOfAssuranceForTransaction, String idpEntityId, List<LevelOfAssurance> idpLevelsOfAssurance) {
        return new StateProcessingValidationException(format("Transaction LevelsOfAssurance unsupported by IDP. Transaction: {0}, LOAs: {1}, IDP: {2}, IDP LOAs: {3}",
                requestIssuerEntityId, levelsOfAssuranceForTransaction, idpEntityId, idpLevelsOfAssurance), Level.ERROR);
    }

    public static StateProcessingValidationException requestedLevelOfAssuranceUnsupportedByTransactionEntity(String requestIssuerEntityId, List<LevelOfAssurance> levelsOfAssuranceForTransaction, LevelOfAssurance requestedLoa) {
        return new StateProcessingValidationException(format("Requested Level Of Assurance unsupported by Transaction Entity. Transaction Entity: {0}, transaction LOAs {1}, requested LOA: {2}",
                requestIssuerEntityId, levelsOfAssuranceForTransaction, requestedLoa), Level.ERROR);
    }

    public static StateProcessingValidationException idpReturnedUnsupportedLevelOfAssurance(LevelOfAssurance levelOfAssurance, String requestId, String issuer) {
        return new StateProcessingValidationException(format("Response to request ID [{0}] contains LOA {1} we do not accept from IDP [{2}]", requestId, levelOfAssurance, issuer), Level.ERROR);
    }

    public static StateProcessingValidationException eidasCountryNotEnabled(String countryEntityId) {
        return new StateProcessingValidationException(format("Country with entity id {0} is not enabled for eidas", countryEntityId), Level.ERROR);
    }

    public static StateProcessingValidationException authnResponseTranslationFailed(String requestId, IdpIdaStatus.Status status) {
        return new StateProcessingValidationException(format("Authn translation for request {0} failed with status {1}", requestId, status), Level.ERROR);
    }

    public static StateProcessingValidationException missingMandatoryAttribute(String requestId, String attribute) {
        return new StateProcessingValidationException(format("Authn translation for request {0} failed with missing mandatory attribute {1}", requestId, attribute), Level.ERROR);
    }

    public Level getLevel() {
        return level;
    }
}
