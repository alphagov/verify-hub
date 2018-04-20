package uk.gov.ida.saml.hub.validators.response.common;

import com.google.common.base.Strings;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.core.Response;
import uk.gov.ida.saml.hub.exception.SamlValidationException;

import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.emptyIssuer;
import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.illegalIssuerFormat;
import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.missingIssuer;

public class IssuerValidator {
    public static void validate(Response response) {
        Issuer issuer = response.getIssuer();
        if (issuer == null) throw new SamlValidationException(missingIssuer());

        String issuerId = issuer.getValue();
        if (Strings.isNullOrEmpty(issuerId)) throw new SamlValidationException(emptyIssuer());

        String issuerFormat = issuer.getFormat();
        if (issuerFormat != null && !NameIDType.ENTITY.equals(issuerFormat))
            throw new SamlValidationException(illegalIssuerFormat(issuerFormat, NameIDType.ENTITY));
    }
}
