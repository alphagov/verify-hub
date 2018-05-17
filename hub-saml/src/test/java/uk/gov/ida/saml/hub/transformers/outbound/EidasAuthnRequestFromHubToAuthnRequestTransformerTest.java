package uk.gov.ida.saml.hub.transformers.outbound;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Extensions;
import org.opensaml.saml.saml2.core.NameIDPolicy;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.core.RequestedAuthnContext;
import org.opensaml.saml.saml2.core.StatusResponseType;
import org.opensaml.saml.saml2.core.impl.AttributeImpl;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.domain.AuthnContext;
import uk.gov.ida.saml.core.extensions.RequestedAttribute;
import uk.gov.ida.saml.core.extensions.RequestedAttributes;
import uk.gov.ida.saml.core.extensions.SPType;
import uk.gov.ida.saml.core.extensions.impl.RequestedAttributeImpl;
import uk.gov.ida.saml.core.extensions.impl.SPTypeImpl;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.saml.core.transformers.AuthnContextFactory;
import uk.gov.ida.saml.hub.domain.EidasAuthnRequestFromHub;
import uk.gov.ida.saml.hub.domain.LevelOfAssurance;

import javax.xml.namespace.QName;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.saml.hub.test.builders.EidasAuthnRequestBuilder.anEidasAuthnRequest;

@RunWith(OpenSAMLMockitoRunner.class)
public class EidasAuthnRequestFromHubToAuthnRequestTransformerTest {
    private static final String A_PROVIDER = "A_PROVIDER";
    private static final String HTTP_ISSUER_ENTITY_ID_COM = "http://issuer-entity-id.com";
    private static final String AUTHN_REQUEST_ID = "aTestId";
    private static final String EIDAS_SSO_LOCATION = "http://eidas/ssoLocation";

    private EidasAuthnRequestFromHubToAuthnRequestTransformer transformer;

    @Before
    public void setUp() throws Exception {
        transformer = new EidasAuthnRequestFromHubToAuthnRequestTransformer(new OpenSamlXmlObjectFactory(), new AuthnContextFactory());
    }

    @Test
    public void shouldApplyNameIdPolicy() {
        EidasAuthnRequestFromHub request = new EidasAuthnRequestFromHub(
            "theId",
            "theIssuer",
            DateTime.now(),
            Arrays.asList(AuthnContext.LEVEL_2),
            URI.create("theUri"),
            "theProviderName"
        );

        AuthnRequest authnRequest = transformer.apply(request);

        assertThat(authnRequest.getNameIDPolicy()).isNotNull();
        assertThat(authnRequest.getNameIDPolicy().getFormat()).isEqualTo(NameIDType.PERSISTENT);
        assertThat(authnRequest.getNameIDPolicy().getAllowCreate()).isTrue();
    }

    @Test
    public void shouldCreateAnEidasAuthnRequest() throws Exception {
        List<AuthnContext> authnContexts = Collections.singletonList(AuthnContext.LEVEL_2);
        EidasAuthnRequestFromHub originalRequestFromTransaction = anEidasAuthnRequestFromHub(A_PROVIDER, HTTP_ISSUER_ENTITY_ID_COM, authnContexts);

        AuthnRequest transformedRequest = transformer.apply(originalRequestFromTransaction);

        assertThat(transformedRequest.getProtocolBinding()).isEqualTo(SAMLConstants.SAML2_POST_BINDING_URI);
        Assert.assertNotNull(transformedRequest);
        Assert.assertNotNull(transformedRequest.getIssueInstant());
        Assert.assertEquals(EIDAS_SSO_LOCATION, transformedRequest.getDestination());
        Assert.assertEquals(AUTHN_REQUEST_ID, transformedRequest.getID());

        Assert.assertEquals(StatusResponseType.UNSPECIFIED_CONSENT, transformedRequest.getConsent());
        Assert.assertEquals(true, transformedRequest.isForceAuthn());
        Assert.assertEquals(false, transformedRequest.isPassive());
        Assert.assertEquals(SAMLVersion.VERSION_20, transformedRequest.getVersion());

        Assert.assertEquals(A_PROVIDER, transformedRequest.getProviderName());

        Assert.assertEquals(HTTP_ISSUER_ENTITY_ID_COM, transformedRequest.getIssuer().getValue());

        NameIDPolicy nameIDPolicy = transformedRequest.getNameIDPolicy();
        Assert.assertEquals(true, nameIDPolicy.getAllowCreate());
        Assert.assertEquals(NameIDType.PERSISTENT, nameIDPolicy.getFormat());

        RequestedAuthnContext requestedAuthnContext = transformedRequest.getRequestedAuthnContext();
        Assert.assertEquals(AuthnContextComparisonTypeEnumeration.MINIMUM, requestedAuthnContext.getComparison());
        AuthnContextClassRef authnContextClassRef = requestedAuthnContext.getAuthnContextClassRefs().get(0);
        Assert.assertEquals(LevelOfAssurance.SUBSTANTIAL.toString(), authnContextClassRef.getAuthnContextClassRef());
    }

    @Test
    public void shouldGenerateAnEidasAuthnRequestExtensions() {
        List<AuthnContext> authnContexts = Collections.singletonList(AuthnContext.LEVEL_2);
        EidasAuthnRequestFromHub originalRequestFromTransaction = anEidasAuthnRequestFromHub(A_PROVIDER, HTTP_ISSUER_ENTITY_ID_COM, authnContexts);

        AuthnRequest transformedRequest = transformer.apply(originalRequestFromTransaction);
        Extensions extensions = transformedRequest.getExtensions();

        Assert.assertNotNull(extensions);
        Optional<XMLObject> spType = extensions
                .getUnknownXMLObjects(SPType.DEFAULT_ELEMENT_NAME)
                .stream().findFirst();
        Assert.assertTrue("There should be at least one eidas:SPType element", spType.isPresent());
        XMLObject xmlObject = spType.get();
        Assert.assertTrue("Should be an instance of SPType", xmlObject.getClass().equals(SPTypeImpl.class));
        Assert.assertEquals("public", ((SPTypeImpl) xmlObject).getValue());

        Optional<XMLObject> requestedAttributes = extensions
                .getUnknownXMLObjects(RequestedAttributes.DEFAULT_ELEMENT_NAME)
                .stream().findFirst();

        Assert.assertTrue("There should be at least one eidas:RequestedAttributes", requestedAttributes.isPresent());

        List<XMLObject> requestedAttributeList = requestedAttributes.get().getOrderedChildren();
        Assert.assertTrue("There should be at least one eidas:RequestedAttribute", requestedAttributeList.size() > 0);

        Map<String, RequestedAttributeImpl> reqAttrMap = getRequestedAttributesByFriendlyName(requestedAttributeList);

        RequestedAttributeImpl firstNameRequestedAttribute = reqAttrMap.get("FirstName");
        QName elementQName = firstNameRequestedAttribute.getElementQName();
        Assert.assertEquals(RequestedAttribute.DEFAULT_ELEMENT_LOCAL_NAME, elementQName.getLocalPart());
        Assert.assertEquals("http://eidas.europa.eu/saml-extensions", elementQName.getNamespaceURI());
        Assert.assertEquals("eidas", elementQName.getPrefix());

        Assert.assertNotNull(firstNameRequestedAttribute);
        Assert.assertEquals(EidasAuthnRequestFromHubToAuthnRequestTransformer.NATURAL_PERSON_NAME_PREFIX + "CurrentGivenName", firstNameRequestedAttribute.getName());
        Assert.assertEquals(Attribute.URI_REFERENCE, firstNameRequestedAttribute.getNameFormat());
        Assert.assertEquals(true, firstNameRequestedAttribute.isRequired());

        Assert.assertNotNull(reqAttrMap.get("FirstName"));
        Assert.assertNotNull(reqAttrMap.get("FamilyName"));
        Assert.assertNotNull(reqAttrMap.get("DateOfBirth"));
        Assert.assertNotNull(reqAttrMap.get("PersonIdentifier"));
    }

    private EidasAuthnRequestFromHub anEidasAuthnRequestFromHub(String A_PROVIDER, String HTTP_ISSUER_ENTITY_ID_COM, List<AuthnContext> authnContexts) {
        return anEidasAuthnRequest()
                .withDestination(EIDAS_SSO_LOCATION)
                .withId(AUTHN_REQUEST_ID)
                .withProviderName(A_PROVIDER)
                .withIssuer(HTTP_ISSUER_ENTITY_ID_COM)
                .withLevelsOfAssurance(authnContexts)
                .buildFromHub();
    }

    private Map<String, RequestedAttributeImpl> getRequestedAttributesByFriendlyName(List<XMLObject> requestedAttributes) {
        return requestedAttributes.stream()
                .map(x -> (RequestedAttributeImpl)x)
                .collect(Collectors.toMap(AttributeImpl::getFriendlyName, x -> x));
    }
}
