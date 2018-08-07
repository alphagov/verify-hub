package uk.gov.ida.saml.core.validators.subject;

import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.core.Subject;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;

import java.util.stream.Stream;

public class AssertionSubjectValidator {

    public void validate(
            Subject subject,
            String assertionId) {

        if (subject == null) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.missingAssertionSubject(assertionId);
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }

        if (subject.getNameID() == null) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.assertionSubjectHasNoNameID(assertionId);
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }

        if (subject.getNameID().getFormat() == null) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.missingAssertionSubjectNameIDFormat(assertionId);
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }

        boolean correctNameIdType = Stream
                .of(NameIDType.PERSISTENT, NameIDType.TRANSIENT)
                .anyMatch(type -> type.equals(subject.getNameID().getFormat()));

        if (!correctNameIdType) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.illegalAssertionSubjectNameIDFormat(assertionId, subject.getNameID().getFormat());
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }
    }
}
