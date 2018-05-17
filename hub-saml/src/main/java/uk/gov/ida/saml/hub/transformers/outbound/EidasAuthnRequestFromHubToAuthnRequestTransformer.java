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
import uk.gov.ida.saml.core.transformers.AuthnContextFactory;
import uk.gov.ida.saml.hub.domain.EidasAuthnRequestFromHub;
import javax.annotation.Nonnull;
import java.util.function.Function;


public class EidasAuthnRequestFromHubToAuthnRequestTransformer implements Function<EidasAuthnRequestFromHub,AuthnRequest> {
    public static final AuthnContextComparisonTypeEnumeration MINIMUM_AUTHNCONTEXT = AuthnContextComparisonTypeEnumeration.MINIMUM;

    public static final String NATURAL_PERSON_NAME_PREFIX = "http://eidas.europa.eu/attributes/naturalperson/";

    private final OpenSamlXmlObjectFactory samlObjectFactory;
    private final XMLObjectBuilderFactory xmlObjectBuilderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();
	private final AuthnContextFactory authnContextFactory;

    @Inject
    public EidasAuthnRequestFromHubToAuthnRequestTransformer(OpenSamlXmlObjectFactory samlObjectFactory, AuthnContextFactory authnContextFactory) {
        this.samlObjectFactory = samlObjectFactory;
        this.authnContextFactory = authnContextFactory;
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

    private RequestedAuthnContext getRequestedAuthnContext(EidasAuthnRequestFromHub originalRequestToCountry) throws IllegalStateException {
        RequestedAuthnContext requestedAuthnContext = samlObjectFactory.createRequestedAuthnContext(MINIMUM_AUTHNCONTEXT);

        String levelOfAssuranceRequested = null;
        try {
            AuthnContext lowestAuthnContext = originalRequestToCountry.getLevelsOfAssurance().stream()
                .min(AuthnContext::compareTo)
                .orElseThrow(() -> new SecurityException("Expected to find at least 1 Level of Assurance in requested authn context"));

            levelOfAssuranceRequested = authnContextFactory.mapFromLoAToEidas(lowestAuthnContext);
        } catch (SecurityException e) {
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
            createRequestedAttribute("FirstName", "CurrentGivenName", true),
            createRequestedAttribute("FamilyName", "CurrentFamilyName", true),
            createRequestedAttribute("DateOfBirth", "DateOfBirth", true),
            createRequestedAttribute("PersonIdentifier", "PersonIdentifier", true)
        );
        return requestedAttributesObject;
    }

    protected OpenSamlXmlObjectFactory getSamlObjectFactory() {
        return samlObjectFactory;
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
