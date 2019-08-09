package uk.gov.ida.hub.samlengine.validation.country;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.slf4j.event.Level;
import uk.gov.ida.saml.core.IdaConstants.Eidas_Attributes;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;
import uk.gov.ida.saml.core.extensions.eidas.CurrentAddress;
import uk.gov.ida.saml.core.extensions.eidas.CurrentFamilyName;
import uk.gov.ida.saml.core.extensions.eidas.CurrentGivenName;
import uk.gov.ida.saml.core.extensions.eidas.DateOfBirth;
import uk.gov.ida.saml.core.extensions.eidas.Gender;
import uk.gov.ida.saml.core.extensions.eidas.PersonIdentifier;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorManager;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.attributeStatementEmpty;
import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.invalidAttributeNameFormat;

public class EidasAttributeStatementAssertionValidator {

    public EidasAttributeStatementAssertionValidator() {
    }

    private static final Set<String> VALID_EIDAS_ATTRIBUTE_NAMES = ImmutableSet.of(
            Eidas_Attributes.FirstName.NAME,
            Eidas_Attributes.FamilyName.NAME,
            Eidas_Attributes.DateOfBirth.NAME,
            Eidas_Attributes.PersonIdentifier.NAME,
            Eidas_Attributes.CurrentAddress.NAME,
            Eidas_Attributes.Gender.NAME
    );

    private static final Set<String> VALID_ATTRIBUTE_NAME_FORMATS = ImmutableSet.of(
            Attribute.URI_REFERENCE
    );

    private static final Map<String, QName> VALID_TYPE_FOR_ATTRIBUTE = ImmutableMap.<String, QName>builder()
            .put(Eidas_Attributes.FirstName.NAME, CurrentGivenName.TYPE_NAME)
            .put(Eidas_Attributes.FamilyName.NAME, CurrentFamilyName.TYPE_NAME)
            .put(Eidas_Attributes.DateOfBirth.NAME, DateOfBirth.TYPE_NAME)
            .put(Eidas_Attributes.PersonIdentifier.NAME, PersonIdentifier.TYPE_NAME)
            .put(Eidas_Attributes.CurrentAddress.NAME, CurrentAddress.TYPE_NAME)
            .put(Eidas_Attributes.Gender.NAME, Gender.TYPE_NAME)
            .build();

    private static final Map<String, String> MANDATORY_ATTRIBUTES = ImmutableMap.of(
            Eidas_Attributes.FirstName.NAME, Eidas_Attributes.FirstName.FRIENDLY_NAME,
            Eidas_Attributes.FamilyName.NAME, Eidas_Attributes.FamilyName.FRIENDLY_NAME,
            Eidas_Attributes.DateOfBirth.NAME, Eidas_Attributes.DateOfBirth.FRIENDLY_NAME,
            Eidas_Attributes.PersonIdentifier.NAME, Eidas_Attributes.PersonIdentifier.FRIENDLY_NAME);

    public void validate(Assertion assertion) {
        validateAttributes(assertion);
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

        Set<String> attributeNames = attributes.stream().map(Attribute::getName).collect(Collectors.toSet());
        if (!attributeNames.containsAll(MANDATORY_ATTRIBUTES.keySet())) {
            throw new SamlTransformationErrorException(String.format("Mandatory attributes not provided. Expected %s but got %s",
                    String.join(",", MANDATORY_ATTRIBUTES.values()),
                    attributes.stream().map(Attribute::getFriendlyName).collect(Collectors.joining(","))), Level.ERROR);
        }

        for (Attribute attribute : attributes) {
            final String attributeName = attribute.getName();
            if (!VALID_EIDAS_ATTRIBUTE_NAMES.contains(attributeName)) {
                SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.mdsAttributeNotRecognised(attributeName);
                throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
            }
            if (attribute.getAttributeValues().isEmpty()) {
                SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.emptyAttribute(attributeName);
                throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
            }
            if (!VALID_TYPE_FOR_ATTRIBUTE.get(attributeName).equals(attribute.getAttributeValues().get(0).getSchemaType())) {
                final QName schemaType = attribute.getAttributeValues().get(0).getSchemaType();
                SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.attributeWithIncorrectType(attributeName, VALID_TYPE_FOR_ATTRIBUTE.get(attributeName), schemaType);
                throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
            }
            if (!VALID_ATTRIBUTE_NAME_FORMATS.contains(attribute.getNameFormat())) {
                SamlTransformationErrorManager.warn(
                        invalidAttributeNameFormat(attribute.getNameFormat()));
            }
        }
    }
}
