package uk.gov.ida.saml.hub.transformers.outbound;

import com.google.inject.Inject;
import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Extensions;
import org.opensaml.saml.saml2.core.NameIDPolicy;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.core.RequestedAuthnContext;
import org.opensaml.saml.saml2.core.StatusResponseType;
import org.opensaml.saml.saml2.core.impl.ExtensionsBuilder;
import org.opensaml.security.SecurityException;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.domain.AuthnContext;
import uk.gov.ida.saml.core.extensions.RequestedAttribute;
import uk.gov.ida.saml.core.extensions.RequestedAttributes;
import uk.gov.ida.saml.core.extensions.SPType;
import uk.gov.ida.saml.core.extensions.impl.RequestedAttributeImpl;
import uk.gov.ida.saml.core.extensions.impl.RequestedAttributesImpl;
import uk.gov.ida.saml.core.extensions.impl.SPTypeImpl;
import uk.gov.ida.saml.hub.domain.EidasAuthnRequestFromHub;
import uk.gov.ida.saml.hub.domain.LevelOfAssurance;

import javax.annotation.Nonnull;
import java.util.function.Function;


public abstract class EidasAuthnRequestToAuthnRequestTransformer implements Function<EidasAuthnRequestFromHub,AuthnRequest> {
    public static final AuthnContextComparisonTypeEnumeration MINIMUM_AUTHNCONTEXT = AuthnContextComparisonTypeEnumeration.MINIMUM;

    public static final String NATURAL_PERSON_NAME_PREFIX = "http://eidas.europa.eu/attributes/naturalperson/";

    private OpenSamlXmlObjectFactory samlObjectFactory;
    private final XMLObjectBuilderFactory xmlObjectBuilderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();

    @Inject
    protected EidasAuthnRequestToAuthnRequestTransformer(OpenSamlXmlObjectFactory samlObjectFactory) {
        this.samlObjectFactory = samlObjectFactory;
    }

    @Override
    public AuthnRequest apply(EidasAuthnRequestFromHub originalRequestToCountry) {
        AuthnRequest authnRequest = samlObjectFactory.createAuthnRequest();
        authnRequest.setID(originalRequestToCountry.getId());
        authnRequest.setIssueInstant(originalRequestToCountry.getIssueInstant());
        authnRequest.setDestination(originalRequestToCountry.getDestination().toASCIIString());
        authnRequest.setProtocolBinding(SAMLConstants.SAML2_POST_BINDING_URI);
        authnRequest.setConsent(StatusResponseType.UNSPECIFIED_CONSENT);
        authnRequest.setForceAuthn(true);
        authnRequest.setProviderName(originalRequestToCountry.getProviderName());
        authnRequest.setNameIDPolicy(getNameIDPolicy());
        authnRequest.setIssuer(samlObjectFactory.createIssuer(originalRequestToCountry.getIssuer()));
        authnRequest.setRequestedAuthnContext(getRequestedAuthnContext(originalRequestToCountry));
        authnRequest.setExtensions(getExtensions());
        return authnRequest;
    }

    private NameIDPolicy getNameIDPolicy() {
        NameIDPolicy nameIdPolicy = samlObjectFactory.createNameIdPolicy();
        nameIdPolicy.setFormat(NameIDType.PERSISTENT);
        nameIdPolicy.setAllowCreate(true);
        return nameIdPolicy;
    }

    private RequestedAuthnContext getRequestedAuthnContext(EidasAuthnRequestFromHub originalRequestToCountry) {
        RequestedAuthnContext requestedAuthnContext = samlObjectFactory.createRequestedAuthnContext(MINIMUM_AUTHNCONTEXT);

        String levelOfAssuranceRequested = null;
        try {
            AuthnContext lowestAuthnContext = originalRequestToCountry.getLevelsOfAssurance().stream()
                .min(AuthnContext::compareTo)
                .orElseThrow(() -> new SecurityException("Expected to find at least 1 Level of Assurance in requested authn context"));

            switch (lowestAuthnContext) {
                case LEVEL_1    :   levelOfAssuranceRequested = LevelOfAssurance.LOW.toString();            break;
                case LEVEL_2    :   levelOfAssuranceRequested = LevelOfAssurance.SUBSTANTIAL.toString();    break;
                case LEVEL_3    :   levelOfAssuranceRequested = LevelOfAssurance.SUBSTANTIAL.toString();    break;
                case LEVEL_4    :   levelOfAssuranceRequested = LevelOfAssurance.HIGH.toString();           break;
                default         :
                    throw new SecurityException("Unknown level of assurance from requested AuthnContext : " + lowestAuthnContext);
            }
        } catch(SecurityException e) {
            throw new IllegalStateException("Expected to find, or map to, a level of assurance ", e);
        }
        requestedAuthnContext.getAuthnContextClassRefs().add(samlObjectFactory.createAuthnContextClassReference(levelOfAssuranceRequested));
        return requestedAuthnContext;
    }

    private Extensions getExtensions() {
        Extensions extensions = new ExtensionsBuilder().buildObject();

        XMLObjectBuilder<?> spTypeBuilder = xmlObjectBuilderFactory.getBuilder(SPType.DEFAULT_ELEMENT_NAME);
        SPTypeImpl spTypeObject = (SPTypeImpl) spTypeBuilder.buildObject(SPType.DEFAULT_ELEMENT_NAME);
        spTypeObject.setValue("public");
        extensions.getUnknownXMLObjects().add(spTypeObject);

        RequestedAttributesImpl requestedAttributesObject = getRequestedAttributes();

        extensions.getUnknownXMLObjects().add(requestedAttributesObject);
        return extensions;
    }

    private RequestedAttributesImpl getRequestedAttributes() {
        XMLObjectBuilder<?> requestedAttributesBuilder = xmlObjectBuilderFactory.getBuilder(RequestedAttributes.DEFAULT_ELEMENT_NAME);
        RequestedAttributesImpl requestedAttributesObject = (RequestedAttributesImpl) requestedAttributesBuilder.buildObject(RequestedAttributes.DEFAULT_ELEMENT_NAME);
        requestedAttributesObject.setRequestedAttributes(
            createMandatoryRequestedAttribute("FirstName", "CurrentGivenName"),
            createMandatoryRequestedAttribute("FamilyName", "CurrentFamilyName"),
            createMandatoryRequestedAttribute("DateOfBirth", "DateOfBirth"),
            createMandatoryRequestedAttribute("PersonIdentifier", "PersonIdentifier"),
            createOptionalRequestedAttribute("CurrentAddress", "CurrentAddress"),
            createOptionalRequestedAttribute("Gender", "Gender")
        );
        return requestedAttributesObject;
    }

    protected OpenSamlXmlObjectFactory getSamlObjectFactory() {
        return samlObjectFactory;
    }

    @Nonnull
    private RequestedAttributeImpl createOptionalRequestedAttribute(String friendlyName, String nameSuffix) {
        return createRequestedAttribute(friendlyName, nameSuffix, false);
    }

    @Nonnull
    private RequestedAttributeImpl createMandatoryRequestedAttribute(String friendlyName, String nameSuffix) {
        return createRequestedAttribute(friendlyName, nameSuffix, true);
    }

    @Nonnull
    private RequestedAttributeImpl createRequestedAttribute(String friendlyName, String nameSuffix, boolean required) {
        XMLObjectBuilder<?> requestedAttributeBuilder = xmlObjectBuilderFactory.getBuilder(RequestedAttribute.DEFAULT_ELEMENT_NAME);
        RequestedAttributeImpl requestedAttribute = (RequestedAttributeImpl) requestedAttributeBuilder.buildObject(RequestedAttribute.DEFAULT_ELEMENT_NAME);
        requestedAttribute.setName(NATURAL_PERSON_NAME_PREFIX + nameSuffix);
        requestedAttribute.setFriendlyName(friendlyName);
        requestedAttribute.setNameFormat(Attribute.URI_REFERENCE);
        requestedAttribute.setIsRequired(required);
        return requestedAttribute;
    }
}
