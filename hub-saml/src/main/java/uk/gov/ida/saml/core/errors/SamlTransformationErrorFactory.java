package uk.gov.ida.saml.core.errors;

import org.joda.time.DateTime;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationWarning;
import uk.gov.ida.saml.core.validation.errors.*;

import javax.xml.namespace.QName;
import java.net.URI;

import static java.text.MessageFormat.format;
import static uk.gov.ida.saml.core.validation.errors.ResponseProcessingValidationSpecification.ATTRIBUTE_STATEMENT_EMPTY;

public final class SamlTransformationErrorFactory {

    private SamlTransformationErrorFactory() {
    }

    public static SamlValidationSpecificationFailure duplicateRequestId(String requestId, String issuerId) {
        return new DuplicateRequestIdValidationSpecificationFailure(DuplicateRequestIdValidationSpecificationFailure.DUPLICATE_REQUEST_ID, requestId, issuerId);
    }

    public static SamlValidationSpecificationFailure authnContextMissingError() {
        return new AuthnContextMissingError(AuthnContextMissingError.MISSING_AUTHN_CONTEXT, false);
    }

    public static SamlValidationSpecificationFailure scopingNotAllowed() {
        return new AuthnRequestFromTransactionValidationSpecification(AuthnRequestFromTransactionValidationSpecification.SCOPE_NOT_ALLOWED);
    }

    public static SamlValidationSpecificationFailure illegalProtocolBindingError(final String protocolBinding, final String expectedProtocolBinding) {
        return new AuthnRequestFromTransactionValidationSpecification(AuthnRequestFromTransactionValidationSpecification.PROTOCOL_BINDING, protocolBinding, expectedProtocolBinding);
    }

    public static SamlValidationSpecificationFailure unrecognisedBinding(final String binding) {
        return new AuthnRequestFromTransactionValidationSpecification(AuthnRequestFromTransactionValidationSpecification.UNRECOGNISED_BINDING, binding);
    }

    public static SamlValidationSpecificationFailure isPassiveNotAllowed() {
        return new AuthnRequestFromTransactionValidationSpecification(AuthnRequestFromTransactionValidationSpecification.PASSIVE_NOT_ALLOWED);
    }

    public static SamlValidationSpecificationFailure missingNameIDPolicy() {
        return new AuthnRequestFromTransactionValidationSpecification(AuthnRequestFromTransactionValidationSpecification.MISSING_NAME_ID_POLICY);
    }

    public static SamlValidationSpecificationFailure illegalNameIDPolicy(final String policy) {
        return new AuthnRequestFromTransactionValidationSpecification(AuthnRequestFromTransactionValidationSpecification.ILLEGAL_NAME_ID_POLICY, policy);
    }

    public static SamlValidationSpecificationFailure attributeStatementEmpty(final String assertionId) {
        return new ResponseProcessingValidationSpecification(ATTRIBUTE_STATEMENT_EMPTY, assertionId);
    }

    public static SamlValidationSpecificationFailure missingInResponseTo() {
        return new ResponseProcessingValidationSpecification(ResponseProcessingValidationSpecification.MISSING_INRESPONSE_TO);
    }

    public static SamlValidationSpecificationFailure emptyInResponseTo() {
        return new ResponseProcessingValidationSpecification(ResponseProcessingValidationSpecification.EMPTY_INRESPONSE_TO);
    }

    public static SamlValidationSpecificationFailure authnContextClassRefMissing() {
        return new ResponseProcessingValidationSpecification(ResponseProcessingValidationSpecification.MISSING_AUTHN_CONTEXT_CLASS_REF, false);
    }

    public static SamlValidationSpecificationFailure authnContextClassRefValueMissing() {
        return new ResponseProcessingValidationSpecification(ResponseProcessingValidationSpecification.MISSING_AUTHN_CONTEXT_CLASS_REF_VALUE, false);
    }

    public static SamlValidationSpecificationFailure authnInstantMissing() {
        return new ResponseProcessingValidationSpecification("AuthnStatement is missing 'AuthnInstant'"); //TODO: need to update saml-extensions to constantise this string
    }

    public static SamlValidationSpecificationFailure missingAuthnStatement() {
        return new ResponseProcessingValidationSpecification(ResponseProcessingValidationSpecification.MISSING_AUTHN_STATEMENT, false);
    }

    public static SamlValidationSpecificationFailure multipleAuthnStatements() {
        return new ResponseProcessingValidationSpecification(ResponseProcessingValidationSpecification.MULTIPLE_AUTHN_STATEMENTS, false);
    }

    public static SamlValidationSpecificationFailure authnStatementAlreadyReceived(final String id) {
        return new ResponseProcessingValidationSpecification(ResponseProcessingValidationSpecification.AUTHN_STATEMENT_ALREADY_RECEIVED, id);
    }

    public static SamlValidationSpecificationFailure missingAssertionSubject(final String id) {
        return new ResponseProcessingValidationSpecification(ResponseProcessingValidationSpecification.MISSING_SUBJECT, id);
    }

    public static SamlValidationSpecificationFailure assertionSubjectHasNoNameID(final String id) {
        return new ResponseProcessingValidationSpecification(ResponseProcessingValidationSpecification.SUBJECT_HAS_NO_NAME_ID, id);
    }

    public static SamlValidationSpecificationFailure missingAssertionSubjectNameIDFormat(final String id) {
        return new ResponseProcessingValidationSpecification(ResponseProcessingValidationSpecification.MISSING_SUBJECT_NAME_ID_FORMAT, id);
    }

    public static SamlValidationSpecificationFailure illegalAssertionSubjectNameIDFormat(final String id, final String format) {
        return new ResponseProcessingValidationSpecification(ResponseProcessingValidationSpecification.ILLEGAL_SUBJECT_NAME_ID_FORMAT, id, format);
    }

    public static SamlValidationSpecificationFailure unencryptedAssertion() {
        return new ResponseProcessingValidationSpecification(ResponseProcessingValidationSpecification.UNENCRYPTED_ASSERTIONS);
    }

    public static SamlValidationSpecificationFailure missingSuccessUnEncryptedAssertions() {
        return new ResponseProcessingValidationSpecification(ResponseProcessingValidationSpecification.MISSING_SUCCESS_UNENCRYPTED);
    }

    public static SamlValidationSpecificationFailure nonSuccessHasUnEncryptedAssertions() {
        return new ResponseProcessingValidationSpecification(ResponseProcessingValidationSpecification.NON_SUCCESS_HAS_UNENCRYPTED);
    }

    public static SamlValidationSpecificationFailure missingMatchingMds() {
        return new GenericHubProfileValidationSpecification(GenericHubProfileValidationSpecification.MISSING_MDS);
    }

    public static SamlValidationSpecificationFailure emptyIssuer() {
        return new GenericHubProfileValidationSpecification(GenericHubProfileValidationSpecification.EMPTY_ISSUER);
    }

    public static SamlValidationSpecificationFailure illegalIssuerFormat(final String providedFormat, final String expectedFormat) {
        return new GenericHubProfileValidationSpecification(GenericHubProfileValidationSpecification.ILLEGAL_ISSUER_FORMAT, providedFormat, expectedFormat);
    }

    public static SamlValidationSpecificationFailure destinationMissing(final URI expectedUri) {
        return new GenericHubProfileValidationSpecification(GenericHubProfileValidationSpecification.MISSING_DESTINATION, expectedUri);
    }

    public static SamlValidationSpecificationFailure destinationEmpty(final URI expectedUri, final String destination) {
        return new GenericHubProfileValidationSpecification(GenericHubProfileValidationSpecification.EMPTY_DESTINATION, expectedUri, destination);
    }

    public static SamlValidationSpecificationFailure destinationInvalid(URI uri, String endpoint) {
        return new GenericHubProfileValidationSpecification("Destination is incorrect. Received: {0}{1}{2}", uri.getScheme(), uri.getPath(), endpoint);
    }

    public static SamlValidationSpecificationFailure assertionNotSigned(final String id) {
        return new GenericHubProfileValidationSpecification(GenericHubProfileValidationSpecification.ASSERTION_SIGNATURE_NOT_SIGNED, id);
    }

    public static SamlValidationSpecificationFailure assertionSignatureMissing(final String id) {
        return new GenericHubProfileValidationSpecification(GenericHubProfileValidationSpecification.MISSING_ASSERTION_SIGNATURE, id);
    }

    public static SamlValidationSpecificationFailure missingIssueInstant(final String id) {
        return new GenericHubProfileValidationSpecification(GenericHubProfileValidationSpecification.MISSING_ISSUE_INSTANT, id);
    }

    public static SamlValidationSpecificationFailure missingVersion(final String id) {
        return new GenericHubProfileValidationSpecification(GenericHubProfileValidationSpecification.MISSING_VERSION, id);
    }

    public static SamlValidationSpecificationFailure illegalVersion(final String id) {
        return new GenericHubProfileValidationSpecification(GenericHubProfileValidationSpecification.ILLEGAL_VERSION, id);
    }

    public static SamlValidationSpecificationFailure noSubjectConfirmationWithBearerMethod(final String id) {
        return new GenericHubProfileValidationSpecification(GenericHubProfileValidationSpecification.NO_SUBJECT_CONF_WITH_BEARER_METHOD, id);
    }

    public static SamlValidationSpecificationFailure emptyIPAddress(final String id) {
        return new GenericHubProfileValidationSpecification(GenericHubProfileValidationSpecification.EMPTY_IP_ADDRESS, id);
    }

    public static SamlValidationSpecificationFailure missingIPAddress(final String id) {
        return new GenericHubProfileValidationSpecification(GenericHubProfileValidationSpecification.MISSING_IP_ADDRESS, id);
    }

    public static SamlValidationSpecificationFailure duplicateMatchingDataset(final String id, final String responseIssuerId) {
        return new GenericHubProfileValidationSpecification(GenericHubProfileValidationSpecification.DUPLICATE_MATCHING_DATASET, id, responseIssuerId);
    }

    public static SamlValidationSpecificationFailure mdsStatementMissing() {
        return new GenericHubProfileValidationSpecification(GenericHubProfileValidationSpecification.MDS_STATEMENT_MISSING);
    }

    public static SamlValidationSpecificationFailure mdsMultipleStatements() {
        return new GenericHubProfileValidationSpecification(GenericHubProfileValidationSpecification.MDS_MULTIPLE_STATEMENTS);
    }

    public static SamlValidationSpecificationFailure mdsAttributeNotRecognised(final String attributeName) {
        return new GenericHubProfileValidationSpecification(GenericHubProfileValidationSpecification.MDS_ATTRIBUTE_NOT_RECOGNISED, attributeName);
    }

    public static SamlValidationSpecificationFailure emptyAttribute(final String attributeName) {
        return new GenericHubProfileValidationSpecification(GenericHubProfileValidationSpecification.EMPTY_ATTRIBUTE, attributeName);
    }

    public static SamlValidationSpecificationFailure attributeWithIncorrectType(final String attributeName, final QName expected, final QName received) {
        return new GenericHubProfileValidationSpecification(GenericHubProfileValidationSpecification.ATTRIBUTE_HAS_INCORRECT_TYPE, attributeName, expected, received);
    }

    public static SamlValidationSpecificationFailure illegalRequestVersionNumber() {
        return new AuthnRequestFromTransactionValidationSpecification(AuthnRequestFromTransactionValidationSpecification.INCORRECT_VERSION);
    }

    public static SamlValidationSpecificationFailure requestTooOld(String requestId, DateTime issueInstant, DateTime currentTime) {
        return new RequestFreshnessValidationSpecification(RequestFreshnessValidationSpecification.REQUEST_TOO_OLD, requestId, issueInstant, currentTime);
    }

    public static SamlValidationSpecificationFailure missingRequestId() {
        return new AuthnRequestFromTransactionValidationSpecification(AuthnRequestFromTransactionValidationSpecification.MISSING_ID);
    }

    public static SamlValidationSpecificationFailure missingRequestIssueInstant(final String requestId) {
        return new AuthnRequestFromTransactionValidationSpecification(AuthnRequestFromTransactionValidationSpecification.MISSING_ISSUE_INSTANT, requestId);
    }

    public static SamlValidationSpecificationFailure missingRequestVersion(final String requestId) {
        return new AuthnRequestFromTransactionValidationSpecification(AuthnRequestFromTransactionValidationSpecification.MISSING_VERSION, requestId);
    }

    public static SamlValidationSpecificationFailure nestedSubStatusCodesBreached(int subStatusCodeLimit) {
        return new GenericHubProfileValidationSpecification(GenericHubProfileValidationSpecification.SUB_STATUS_CODE_LIMIT_EXCEEDED, subStatusCodeLimit);
    }

    public static SamlValidationSpecificationFailure invalidStatusCode(final String statusCode) {
        return new GenericHubProfileValidationSpecification(GenericHubProfileValidationSpecification.INVALID_STATUS_CODE, statusCode);
    }

    public static SamlValidationSpecificationFailure invalidSubStatusCode(final String subStatusCode, final String statusCode) {
        return new GenericHubProfileValidationSpecification(GenericHubProfileValidationSpecification.INVALID_SUB_STATUS_CODE, subStatusCode, statusCode);
    }

    public static SamlValidationSpecificationFailure subStatusMustBeOneOf(final String subStatus, final String code1, final String code2, final String code3) {
        return new ResponseProcessingValidationSpecification(ResponseProcessingValidationSpecification.STATUS_CODE_MUST_BE_ONE_OF, subStatus, code1, code2, code3);
    }

    public static SamlValidationSpecificationFailure missingSubStatus() {
        return new ResponseProcessingValidationSpecification(ResponseProcessingValidationSpecification.MISSING_SUB_STATUS);
    }

    public static SamlValidationSpecificationFailure unexpectedNumberOfAssertions(int expected, int actual) {
        return new ResponseProcessingValidationSpecification(ResponseProcessingValidationSpecification.UNEXPECTED_NUMBER_OF_ASSERTIONS, expected, actual);
    }

    public static SamlValidationSpecificationFailure notMatchInResponseTo(final String inResponseTo, final String requestId) {
        return new BearerSubjectConfirmationValidationSpecification(BearerSubjectConfirmationValidationSpecification.IN_RESPONSE_TO_DOES_NOT_MATCH, inResponseTo, requestId);
    }

    public static SamlValidationSpecificationFailure incorrectRecipientFormat(String recipient, String expectedRecipient) {
        return new BearerSubjectConfirmationValidationSpecification(BearerSubjectConfirmationValidationSpecification.INCORRECT_RECIPIENT_FORMAT, recipient, expectedRecipient);
    }

    public static SamlValidationSpecificationFailure missingSubjectConfirmationData() {
        return new BearerSubjectConfirmationValidationSpecification(BearerSubjectConfirmationValidationSpecification.MISSING_SUBJECT_CONFIRMATION_DATA);
    }

    public static SamlValidationSpecificationFailure missingBearerInResponseTo() {
        return new BearerSubjectConfirmationValidationSpecification(BearerSubjectConfirmationValidationSpecification.NO_INRESPONSETO_VALUE);
    }

    public static SamlValidationSpecificationFailure missingBearerRecipient() {
        return new BearerSubjectConfirmationValidationSpecification(BearerSubjectConfirmationValidationSpecification.NO_RECIPIENT);
    }

    public static SamlValidationSpecificationFailure missingNotOnOrAfter() {
        return new BearerSubjectConfirmationValidationSpecification(BearerSubjectConfirmationValidationSpecification.NO_NOT_ON_OR_AFTER);
    }

    public static SamlValidationSpecificationFailure exceededNotOnOrAfter(DateTime expiredTime) {
        return new BearerSubjectConfirmationValidationSpecification(format(BearerSubjectConfirmationValidationSpecification.EXCEEDED_NOT_ON_OR_AFTER, expiredTime));
    }

    public static SamlValidationSpecificationFailure notBeforeExists() {
        return new BearerSubjectConfirmationValidationSpecification(BearerSubjectConfirmationValidationSpecification.NOT_BEFORE_ATTRIBUTE_EXISTS);
    }

    public static SamlValidationSpecificationFailure missingOrEmptyEntityID() {
        return new GenericHubProfileValidationSpecification(GenericHubProfileValidationSpecification.MISSING_OR_EMPTY_ENTITY_ID);
    }

    public static SamlValidationSpecificationFailure missingCacheDurationAndValidUntil() {
        return new GenericHubProfileValidationSpecification(GenericHubProfileValidationSpecification.MISSING_CACHEDUR_VALIDUNTIL);
    }

    public static SamlValidationSpecificationFailure missingRoleDescriptor() {
        return new GenericHubProfileValidationSpecification(GenericHubProfileValidationSpecification.MISSING_ROLE_DESCRIPTOR);
    }

    public static SamlValidationSpecificationFailure missingKeyDescriptor() {
        return new GenericHubProfileValidationSpecification(GenericHubProfileValidationSpecification.MISSING_KEY_DESCRIPTOR);
    }

    public static SamlValidationSpecificationFailure missingKeyInfo() {
        return new GenericHubProfileValidationSpecification(GenericHubProfileValidationSpecification.MISSING_KEY_INFO);
    }

    public static SamlValidationSpecificationFailure missingX509Data() {
        return new GenericHubProfileValidationSpecification(GenericHubProfileValidationSpecification.MISSING_X509DATA);
    }

    public static SamlValidationSpecificationFailure missingX509Certificate() {
        return new GenericHubProfileValidationSpecification(GenericHubProfileValidationSpecification.MISSING_X509CERT);
    }

    public static SamlValidationSpecificationFailure emptyX509Certificiate() {
        return new GenericHubProfileValidationSpecification(GenericHubProfileValidationSpecification.EMPTY_X509CERT);
    }

    public static SamlValidationSpecificationFailure missingOrganization() {
        return new GenericHubProfileValidationSpecification(GenericHubProfileValidationSpecification.MISSING_ORGANIZATION);
    }

    public static SamlValidationSpecificationFailure missingDisplayName() {
        return new GenericHubProfileValidationSpecification(GenericHubProfileValidationSpecification.MISSING_DISPLAY_NAME);
    }

    public static SamlValidationSpecificationFailure missingSignature() {
        return new GenericHubProfileValidationSpecification(GenericHubProfileValidationSpecification.MISSING_SIGNATURE);
    }

    public static SamlValidationSpecificationFailure signatureNotSigned() {
        return new GenericHubProfileValidationSpecification(GenericHubProfileValidationSpecification.SIGNATURE_NOT_SIGNED);
    }

    public static SamlValidationSpecificationFailure missingIssuer() {
        return new GenericHubProfileValidationSpecification(GenericHubProfileValidationSpecification.MISSING_ISSUER);
    }

    public static SamlValidationSpecificationFailure missingId() {
        return new GenericHubProfileValidationSpecification(GenericHubProfileValidationSpecification.MISSING_ID);
    }

    public static SamlValidationSpecificationFailure missingKey(final String key, final String entity) {
        return new GenericHubProfileValidationSpecification(GenericHubProfileValidationSpecification.MISSING_KEY, key, entity);
    }

    public static SamlValidationSpecificationFailure unsupportedKey(final String key) {
        return new GenericHubProfileValidationSpecification(GenericHubProfileValidationSpecification.UNSUPPORTED_KEY, key);
    }

    public static SamlValidationSpecificationFailure invalidRequestID() {
        return new GenericHubProfileValidationSpecification(GenericHubProfileValidationSpecification.INVALID_REQUEST_ID);
    }

    public static SamlValidationSpecificationFailure invalidRelayState(String relayState) {
        return new RelayStateValidationSpecification(RelayStateValidationSpecification.INVALID_RELAY_STATE, relayState);
    }

    public static SamlValidationSpecificationFailure relayStateContainsInvalidCharacter(String invalidCharacter, String relayState) {
        return new RelayStateValidationSpecification(RelayStateValidationSpecification.INVALID_RELAY_STATE_CHARACTER, invalidCharacter, relayState);
    }

    public static SamlValidationSpecificationFailure invalidAttributeLanguageInAssertion(final String name, final String language) {
        return new InvalidAttributeLanguageInAssertion(name, language);
    }

    public static SamlValidationSpecificationFailure invalidFraudAttribute(String message) {
        return new SamlValidationSpecification(SamlValidationSpecification.DESERIALIZATION_ERROR, message);
    }

    public static SamlValidationSpecificationWarning invalidAttributeNameFormat(final String nameFormat) {
        return new InvalidAttributeNameFormat(nameFormat);
    }

    public static SamlValidationSpecificationFailure mismatchedPersistentIdentifiers() {
        return new GenericHubProfileValidationSpecification(GenericHubProfileValidationSpecification.MISMATCHED_PIDS);
    }

    public static SamlValidationSpecificationFailure mismatchedIssuers() {
        return new GenericHubProfileValidationSpecification(GenericHubProfileValidationSpecification.MISMATCHED_ISSUERS);
    }
}
