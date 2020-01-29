package uk.gov.ida.saml.hub.transformers.outbound;

import org.joda.time.DateTime;
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
        final EidasAuthnRequestFromHub request = new EidasAuthnRequestFromHub(
                "theId",
                "theIssuer",
                DateTime.now(),
                Collections.singletonList(AuthnContext.LEVEL_2),
                URI.create("theUri"),
                "theProviderName"
        );

        final AuthnRequest authnRequest = transformer.apply(request);

        assertThat(authnRequest.getNameIDPolicy()).isNotNull();
        assertThat(authnRequest.getNameIDPolicy().getFormat()).isEqualTo(NameIDType.PERSISTENT);
        assertThat(authnRequest.getNameIDPolicy().getAllowCreate()).isTrue();
    }

    @Test
    public void shouldCreateAnEidasAuthnRequest() {
        final List<AuthnContext> authnContexts = Collections.singletonList(AuthnContext.LEVEL_2);
        final EidasAuthnRequestFromHub originalRequestFromTransaction = anEidasAuthnRequestFromHub(A_PROVIDER, HTTP_ISSUER_ENTITY_ID_COM, authnContexts);
        final AuthnRequest transformedRequest = transformer.apply(originalRequestFromTransaction);

        assertThat(transformedRequest.getProtocolBinding()).isEqualTo(SAMLConstants.SAML2_POST_BINDING_URI);
        assertThat(transformedRequest).isNotNull();
        assertThat(transformedRequest.getIssueInstant()).isNotNull();
        assertThat(transformedRequest.getDestination()).isEqualTo(EIDAS_SSO_LOCATION);
        assertThat(transformedRequest.getID()).isEqualTo(AUTHN_REQUEST_ID);

        assertThat(transformedRequest.getConsent()).isEqualTo(StatusResponseType.UNSPECIFIED_CONSENT);
        assertThat(transformedRequest.isForceAuthn()).isTrue();
        assertThat(transformedRequest.isPassive()).isFalse();
        assertThat(transformedRequest.getVersion()).isEqualTo(SAMLVersion.VERSION_20);

        assertThat(transformedRequest.getProviderName()).isEqualTo(A_PROVIDER);
        assertThat(transformedRequest.getIssuer().getValue()).isEqualTo(HTTP_ISSUER_ENTITY_ID_COM);

        final NameIDPolicy nameIDPolicy = transformedRequest.getNameIDPolicy();
        assertThat(nameIDPolicy.getAllowCreate()).isTrue();
        assertThat(nameIDPolicy.getFormat()).isEqualTo(NameIDType.PERSISTENT);

        final RequestedAuthnContext requestedAuthnContext = transformedRequest.getRequestedAuthnContext();
        assertThat(requestedAuthnContext.getComparison()).isEqualTo(AuthnContextComparisonTypeEnumeration.MINIMUM);

        final AuthnContextClassRef authnContextClassRef = requestedAuthnContext.getAuthnContextClassRefs().get(0);
        assertThat(authnContextClassRef.getAuthnContextClassRef()).isEqualTo(LevelOfAssurance.SUBSTANTIAL.toString());
    }

    @Test
    public void shouldGenerateAnEidasAuthnRequestExtensions() {
        final List<AuthnContext> authnContexts = Collections.singletonList(AuthnContext.LEVEL_2);
        final EidasAuthnRequestFromHub originalRequestFromTransaction = anEidasAuthnRequestFromHub(A_PROVIDER, HTTP_ISSUER_ENTITY_ID_COM, authnContexts);
        final AuthnRequest transformedRequest = transformer.apply(originalRequestFromTransaction);
        final Extensions extensions = transformedRequest.getExtensions();

        assertThat(extensions).isNotNull();
        final Optional<XMLObject> spType = extensions
                .getUnknownXMLObjects(SPType.DEFAULT_ELEMENT_NAME)
                .stream().findFirst();
        assertThat(spType.isPresent()).as("There should be at least one eidas:SPType element").isTrue();

        final XMLObject xmlObject = spType.get();
        assertThat(xmlObject.getClass()).as("Should be an instance of SPType").isEqualTo(SPTypeImpl.class);
        assertThat(((SPTypeImpl) xmlObject).getValue()).isEqualTo("public");

        final Optional<XMLObject> requestedAttributes = extensions
                .getUnknownXMLObjects(RequestedAttributes.DEFAULT_ELEMENT_NAME)
                .stream().findFirst();

        assertThat(requestedAttributes.isPresent()).as("There should be at least one eidas:RequestedAttributes").isTrue();

        final List<XMLObject> requestedAttributeList = requestedAttributes.get().getOrderedChildren();
        assertThat(requestedAttributeList).isNotNull();
        assertThat(requestedAttributeList.isEmpty()).as("There should be at least one eidas:RequestedAttribute").isFalse();

        final Map<String, RequestedAttributeImpl> reqAttrMap = getRequestedAttributesByFriendlyName(requestedAttributeList);

        final RequestedAttributeImpl firstNameRequestedAttribute = reqAttrMap.get("FirstName");
        final QName elementQName = firstNameRequestedAttribute.getElementQName();
        assertThat(elementQName.getLocalPart()).isEqualTo(RequestedAttribute.DEFAULT_ELEMENT_LOCAL_NAME);
        assertThat(elementQName.getNamespaceURI()).isEqualTo("http://eidas.europa.eu/saml-extensions");
        assertThat(elementQName.getPrefix()).isEqualTo("eidas");

        assertThat(firstNameRequestedAttribute).isNotNull();
        assertThat(firstNameRequestedAttribute.getName()).isEqualTo(EidasAuthnRequestFromHubToAuthnRequestTransformer.NATURAL_PERSON_NAME_PREFIX + "CurrentGivenName");
        assertThat(firstNameRequestedAttribute.getNameFormat()).isEqualTo(Attribute.URI_REFERENCE);
        assertThat(firstNameRequestedAttribute.isRequired()).isTrue();

        assertThat(reqAttrMap.get("FirstName")).isNotNull();
        assertThat(reqAttrMap.get("FamilyName")).isNotNull();
        assertThat(reqAttrMap.get("DateOfBirth")).isNotNull();
        assertThat(reqAttrMap.get("PersonIdentifier")).isNotNull();
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
                .map(x -> (RequestedAttributeImpl) x)
                .collect(Collectors.toMap(AttributeImpl::getFriendlyName, x -> x));
    }
}
