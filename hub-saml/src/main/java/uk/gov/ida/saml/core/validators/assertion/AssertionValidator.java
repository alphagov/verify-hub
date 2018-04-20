package uk.gov.ida.saml.core.validators.assertion;

import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.xmlsec.signature.Signature;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;import uk.gov.ida.saml.core.validators.subject.AssertionSubjectValidator;
import uk.gov.ida.saml.core.validators.subjectconfirmation.BasicAssertionSubjectConfirmationValidator;
import uk.gov.ida.saml.security.validators.issuer.IssuerValidator;
import uk.gov.ida.saml.security.validators.signature.SamlSignatureUtil;

public class AssertionValidator {

    private final IssuerValidator issuerValidator;
    private final AssertionSubjectValidator subjectValidator;
    protected final AssertionAttributeStatementValidator assertionAttributeStatementValidator;
    private final BasicAssertionSubjectConfirmationValidator basicAssertionSubjectConfirmationValidator;

    public AssertionValidator(
            IssuerValidator issuerValidator,
            AssertionSubjectValidator subjectValidator,
            AssertionAttributeStatementValidator assertionAttributeStatementValidator,
            BasicAssertionSubjectConfirmationValidator basicAssertionSubjectConfirmationValidator) {

        this.issuerValidator = issuerValidator;
        this.subjectValidator = subjectValidator;
        this.assertionAttributeStatementValidator = assertionAttributeStatementValidator;
        this.basicAssertionSubjectConfirmationValidator = basicAssertionSubjectConfirmationValidator;
    }

    public void validate(
            Assertion assertion,
            String requestId,
            String expectedRecipientId) {

        Signature signature = assertion.getSignature();
        if (assertion.getID() == null) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.missingId();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }
        if (signature == null) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.assertionSignatureMissing(assertion.getID());
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }
        if (!SamlSignatureUtil.isSignaturePresent(signature)) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.assertionNotSigned(assertion.getID());
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }
        if (assertion.getIssueInstant() == null) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.missingIssueInstant(assertion.getID());
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }
        if (assertion.getVersion() == null) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.missingVersion(assertion.getID());
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }
        if (!assertion.getVersion().equals(SAMLVersion.VERSION_20)) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.illegalVersion(assertion.getID());
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }

        issuerValidator.validate(assertion.getIssuer());
        assertionAttributeStatementValidator.validate(assertion);

        validateSubject(assertion, requestId, expectedRecipientId);
        basicAssertionSubjectConfirmationValidator.validate(assertion.getSubject().getSubjectConfirmations().get(0));
    }

    protected void validateSubject(
            Assertion assertion,
            String requestId,
            String expectedRecipientId) {

        subjectValidator.validate(assertion.getSubject(), assertion.getID());
    }
}
