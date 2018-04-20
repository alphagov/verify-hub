package uk.gov.ida.saml.core.validators.assertion;

import com.google.common.base.Strings;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import uk.gov.ida.saml.core.IdaConstants;
import uk.gov.ida.saml.core.extensions.IPAddress;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;
public class IPAddressValidator {
    public void validate(Assertion assertion) {
        for (AttributeStatement attributeStatement : assertion.getAttributeStatements()) {
            for (Attribute attribute : attributeStatement.getAttributes()) {
                if (attribute.getName().equals(IdaConstants.Attributes_1_1.IPAddress.NAME)) {
                    IPAddress ipAddressAttributeValue = (IPAddress) attribute.getAttributeValues().get(0);
                    String addressValue = ipAddressAttributeValue.getValue();
                    if (!Strings.isNullOrEmpty(addressValue)) {
                        return;
                    }

                    SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.emptyIPAddress(assertion.getID());
                    throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
                }
            }
        }
        SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.missingIPAddress(assertion.getID());
        throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
    }
}
