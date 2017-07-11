package uk.gov.ida.saml.core.validators.subjectconfirmation;

import org.joda.time.DateTime;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;
public class BasicAssertionSubjectConfirmationValidator {

    public void validate(SubjectConfirmation subjectConfirmation) {

        final SubjectConfirmationData subjectConfirmationData = subjectConfirmation.getSubjectConfirmationData();

        if (subjectConfirmationData == null) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.missingSubjectConfirmationData();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }
        if (subjectConfirmationData.getInResponseTo() == null) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.missingBearerInResponseTo();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }

        if (subjectConfirmationData.getRecipient() == null) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.missingBearerRecipient();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }

        final DateTime notOnOrAfter = subjectConfirmationData.getNotOnOrAfter();
        if (notOnOrAfter == null) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.missingNotOnOrAfter();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }

        final DateTime now = DateTime.now();
        if (notOnOrAfter.isEqual(now) || notOnOrAfter.isBefore(now)) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.exceededNotOnOrAfter(notOnOrAfter);
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }
        if (subjectConfirmationData.getNotBefore() != null) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.notBeforeExists();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }
    }
}
