package uk.gov.ida.saml.core.validators.assertion;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import uk.gov.ida.saml.core.IdaConstants;
import uk.gov.ida.saml.core.extensions.Address;
import uk.gov.ida.saml.core.extensions.Date;
import uk.gov.ida.saml.core.extensions.Gender;
import uk.gov.ida.saml.core.extensions.PersonName;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorManager;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;
import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.attributeStatementEmpty;
import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.invalidAttributeNameFormat;

public class MatchingDatasetAssertionValidator {

    private final DuplicateAssertionValidator duplicateAssertionValidator;

    public MatchingDatasetAssertionValidator(DuplicateAssertionValidator duplicateAssertionValidator) {
        this.duplicateAssertionValidator = duplicateAssertionValidator;
    }

    private static final Set<String> VALID_ATTRIBUTE_NAMES_1_1 = ImmutableSet.of(
            IdaConstants.Attributes_1_1.Firstname.NAME,
            IdaConstants.Attributes_1_1.Middlename.NAME,
            IdaConstants.Attributes_1_1.Surname.NAME,
            IdaConstants.Attributes_1_1.Gender.NAME,
            IdaConstants.Attributes_1_1.DateOfBirth.NAME,
            IdaConstants.Attributes_1_1.CurrentAddress.NAME,
            IdaConstants.Attributes_1_1.PreviousAddress.NAME
    );

    private static final Set<String> VALID_ATTRIBUTE_NAME_FORMATS = ImmutableSet.of(
            Attribute.UNSPECIFIED
    );

    private static final Map<String, QName> VALID_TYPE_FOR_ATTRIBUTE = ImmutableMap.<String, QName>builder()
            .put(IdaConstants.Attributes_1_1.Firstname.NAME, PersonName.TYPE_NAME)
            .put(IdaConstants.Attributes_1_1.Middlename.NAME, PersonName.TYPE_NAME)
            .put(IdaConstants.Attributes_1_1.Surname.NAME, PersonName.TYPE_NAME)
            .put(IdaConstants.Attributes_1_1.Gender.NAME, Gender.TYPE_NAME)
            .put(IdaConstants.Attributes_1_1.DateOfBirth.NAME, Date.TYPE_NAME)
            .put(IdaConstants.Attributes_1_1.CurrentAddress.NAME, Address.TYPE_NAME)
            .put(IdaConstants.Attributes_1_1.PreviousAddress.NAME, Address.TYPE_NAME)
            .build();

    public void validate(Assertion assertion, String responseIssuerId) {
        validateAssertion(assertion, responseIssuerId);
        validateAttributes(assertion);
    }

    private void validateAssertion(Assertion assertion, String responseIssuerId) {
        if (!duplicateAssertionValidator.valid(assertion)) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.duplicateMatchingDataset(assertion.getID(), responseIssuerId);
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }
    }

    private void validateAttributes(Assertion assertion) {
        final List<AttributeStatement> attributeStatements = assertion.getAttributeStatements();
        if (attributeStatements.isEmpty()) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.mdsStatementMissing();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }
        if (attributeStatements.size() > 1) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.mdsMultipleStatements();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }

        final List<Attribute> attributes = attributeStatements.get(0).getAttributes();
        if (attributes.isEmpty()) {
            SamlValidationSpecificationFailure failure = attributeStatementEmpty(assertion.getID());
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }

        for (Attribute attribute : attributes) {
            final String attributeName = attribute.getName();
            if (!VALID_ATTRIBUTE_NAMES_1_1.contains(attributeName)) {
                SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.mdsAttributeNotRecognised(attributeName);
                throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
            }
            if(attribute.getAttributeValues().isEmpty()) {
                SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.emptyAttribute(attributeName);
                throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
            }
            if(!VALID_TYPE_FOR_ATTRIBUTE.get(attributeName).equals(attribute.getAttributeValues().get(0).getSchemaType())) {
                final QName schemaType = attribute.getAttributeValues().get(0).getSchemaType();
                SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.attributeWithIncorrectType(attributeName, VALID_TYPE_FOR_ATTRIBUTE.get(attributeName), schemaType);
                throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
            }
            if(!VALID_ATTRIBUTE_NAME_FORMATS.contains(attribute.getNameFormat())) {
                SamlTransformationErrorManager.warn(
                        invalidAttributeNameFormat(attribute.getNameFormat()));
            }
        }
    }
}
