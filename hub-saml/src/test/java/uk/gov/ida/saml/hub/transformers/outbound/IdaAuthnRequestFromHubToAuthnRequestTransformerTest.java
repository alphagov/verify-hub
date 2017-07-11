package uk.gov.ida.saml.hub.transformers.outbound;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.io.MarshallingException;
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
import org.opensaml.security.SecurityException;
import org.opensaml.xmlsec.signature.support.SignatureException;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.domain.AuthnContext;
import uk.gov.ida.saml.core.extensions.IdaAuthnContext;
import uk.gov.ida.saml.core.extensions.RequestedAttribute;
import uk.gov.ida.saml.core.extensions.RequestedAttributes;
import uk.gov.ida.saml.core.extensions.SPType;
import uk.gov.ida.saml.core.extensions.impl.RequestedAttributeImpl;
import uk.gov.ida.saml.core.extensions.impl.SPTypeImpl;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.saml.hub.domain.EidasAuthnRequestFromHub;
import uk.gov.ida.saml.hub.domain.IdaAuthnRequestFromHub;
import uk.gov.ida.saml.hub.domain.LevelOfAssurance;

import javax.xml.namespace.QName;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.jodatime.api.Assertions.assertThat;
import static uk.gov.ida.saml.hub.test.builders.EidasAuthnRequestBuilder.anEidasAuthnRequest;
import static uk.gov.ida.saml.hub.test.builders.IdaAuthnRequestBuilder.anIdaAuthnRequest;

@RunWith(OpenSAMLMockitoRunner.class)
public class IdaAuthnRequestFromHubToAuthnRequestTransformerTest {

    public static final String A_PROVIDER = "A_PROVIDER";
    public static final String HTTP_ISSUER_ENTITY_ID_COM = "http://issuer-entity-id.com";
    private IdaAuthnRequestFromHubToAuthnRequestTransformer transformer;
    private EidasAuthnRequestFromHubToAuthnRequestTransformer eidasTransformer;

    private static final String EIDAS_SSO_LOCATION = "http://eidas/ssoLocation";
    private static final String AUTHN_REQUEST_ID = "aTestId";

    @Before
    public void setup() {
        transformer = new IdaAuthnRequestFromHubToAuthnRequestTransformer(new OpenSamlXmlObjectFactory());
        eidasTransformer = new EidasAuthnRequestFromHubToAuthnRequestTransformer(new OpenSamlXmlObjectFactory());
    }

    @Test
    public void shouldUseTheOriginalRequestIdForTheTransformedRequest() throws Exception {
        String originalRequestId = UUID.randomUUID().toString();
        IdaAuthnRequestFromHub originalRequestFromHub = anIdaAuthnRequest().withId(originalRequestId).buildFromHub();

        AuthnRequest transformedRequest = transformer.apply(originalRequestFromHub);

        assertThat(transformedRequest.getID()).isEqualTo(originalRequestId);
    }

    @Test
    public void shouldUseTheOriginalExpiryTimestampToSetTheNotOnOrAfter() throws Exception {
        DateTime sessionExpiry = DateTime.now().plusHours(2);
        IdaAuthnRequestFromHub originalRequestFromHub = anIdaAuthnRequest().withSessionExpiryTimestamp(sessionExpiry).buildFromHub();

        AuthnRequest transformedRequest = transformer.apply(originalRequestFromHub);
        assertThat(transformedRequest.getConditions().getNotOnOrAfter()).isEqualTo(sessionExpiry);
    }

    @Test
    public void shouldUseTheOriginalRequestIssuerIdForTheTransformedRequest() throws Exception {
        String originalIssuerId = UUID.randomUUID().toString();
        IdaAuthnRequestFromHub originalRequestFromHub = anIdaAuthnRequest().withIssuer(originalIssuerId).buildFromHub();

        AuthnRequest transformedRequest = transformer.apply(originalRequestFromHub);

        assertThat(transformedRequest.getIssuer().getValue()).isEqualTo(originalIssuerId);
    }

    @Test
    public void shouldCreateAProxyElementWithAProxyCountOfZeroInTheTransformedRequest() throws Exception {
        AuthnRequest transformedRequest = transformer.apply(anIdaAuthnRequest().buildFromHub());

        assertThat(transformedRequest.getScoping().getProxyCount()).isEqualTo(0);
    }

    @Test
    public void shouldCreateANameIdPolicyElementWithAFormatOfPersistentInTheTransformedRequest() throws Exception {
        AuthnRequest transformedRequest = transformer.apply(anIdaAuthnRequest().buildFromHub());

        assertThat(transformedRequest.getNameIDPolicy().getFormat()).isEqualTo(NameIDType.PERSISTENT);
    }
    
    @Test
    public void shouldCorrectlyMapLevelsOfAssurance() {
        List<AuthnContext> levelsOfAssurance = Arrays.asList(AuthnContext.LEVEL_1, AuthnContext.LEVEL_2);
        List<String> expected = Arrays.asList(IdaAuthnContext.LEVEL_1_AUTHN_CTX, IdaAuthnContext.LEVEL_2_AUTHN_CTX);


        IdaAuthnRequestFromHub originalRequestFromHub = anIdaAuthnRequest()
                .withLevelsOfAssurance(levelsOfAssurance).buildFromHub();
        AuthnRequest transformedRequest = transformer.apply(originalRequestFromHub);
        RequestedAuthnContext requestedAuthnContext = transformedRequest.getRequestedAuthnContext();

        List<String> actual = requestedAuthnContext.getAuthnContextClassRefs().stream()
                .map(AuthnContextClassRef::getAuthnContextClassRef)
                .collect(Collectors.toList());

        assertThat(actual).containsAll(expected);
    }

    @Test
    public void shouldPropagateComparisonType() {
        IdaAuthnRequestFromHub originalRequestFromHub = anIdaAuthnRequest()
                .withComparisonType(AuthnContextComparisonTypeEnumeration.MINIMUM)
                .buildFromHub();
        AuthnRequest transformedRequest = transformer.apply(originalRequestFromHub);

        RequestedAuthnContext requestedAuthnContext = transformedRequest.getRequestedAuthnContext();

        assertThat(requestedAuthnContext.getComparison()).isEqualTo(AuthnContextComparisonTypeEnumeration.MINIMUM);
    }

    @Test
    public void shouldMaintainTheAuthnContextsInPreferenceOrder() throws Exception {
        IdaAuthnRequestFromHub originalRequestFromHub = anIdaAuthnRequest()
                .withLevelsOfAssurance(Arrays.asList(AuthnContext.LEVEL_1, AuthnContext.LEVEL_2))
                .buildFromHub();
        AuthnRequest transformedRequest = transformer.apply(originalRequestFromHub);

        RequestedAuthnContext requestedAuthnContext = transformedRequest.getRequestedAuthnContext();

        List<AuthnContextClassRef> authnContextClassRefs = requestedAuthnContext.getAuthnContextClassRefs();
        List<String> authnContexts = authnContextClassRefs.stream()
                .map(AuthnContextClassRef::getAuthnContextClassRef).collect(Collectors.toList());

        assertThat(authnContexts).containsSequence(IdaAuthnContext.LEVEL_1_AUTHN_CTX, IdaAuthnContext.LEVEL_2_AUTHN_CTX);
    }

    @Test
    public void shouldSetAllowCreateToTrue() {
        IdaAuthnRequestFromHub originalRequestFromHub = anIdaAuthnRequest().buildFromHub();
        AuthnRequest transformedRequest = transformer.apply(originalRequestFromHub);

        NameIDPolicy nameIDPolicy = transformedRequest.getNameIDPolicy();

        assertThat(nameIDPolicy.getAllowCreate()).isEqualTo(true);
    }

    @Test
    public void shouldSetForceAuthnToTrue() throws Exception {
        IdaAuthnRequestFromHub originalRequestFromTransaction = anIdaAuthnRequest()
                .withForceAuthentication(ofNullable(true))
                .buildFromHub();

        AuthnRequest transformedRequest = transformer.apply(originalRequestFromTransaction);

        assertThat(transformedRequest.isForceAuthn()).isEqualTo(true);

    }

    @Test
    public void shouldSetForceAuthnToFalse() throws Exception {
        IdaAuthnRequestFromHub originalRequestFromTransaction = anIdaAuthnRequest()
                .withForceAuthentication(ofNullable(false))
                .buildFromHub();

        AuthnRequest transformedRequest = transformer.apply(originalRequestFromTransaction);

        assertThat(transformedRequest.isForceAuthn()).isEqualTo(false);

        originalRequestFromTransaction = anIdaAuthnRequest()
                .withForceAuthentication(Optional.empty())
                .buildFromHub();

        transformedRequest = transformer.apply(originalRequestFromTransaction);

        assertThat(transformedRequest.isForceAuthn()).isEqualTo(false);

    }

    @Test
    public void shouldSetProtocolBindingToPost() throws Exception {
        IdaAuthnRequestFromHub originalRequestFromTransaction = anIdaAuthnRequest()
            .buildFromHub();

        AuthnRequest transformedRequest = transformer.apply(originalRequestFromTransaction);

        assertThat(transformedRequest.getProtocolBinding()).isEqualTo(SAMLConstants.SAML2_POST_BINDING_URI);
    }

    @Test
    public void shouldCreateAnEidasAuthnRequest() throws Exception {
        List<AuthnContext> authnContexts = Collections.singletonList(AuthnContext.LEVEL_3);
        EidasAuthnRequestFromHub originalRequestFromTransaction = anEidasAuthnRequestFromHub(A_PROVIDER, HTTP_ISSUER_ENTITY_ID_COM, authnContexts);

        AuthnRequest transformedRequest = eidasTransformer.apply(originalRequestFromTransaction);

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
        Assert.assertEquals(NameIDType.UNSPECIFIED, nameIDPolicy.getFormat());

        RequestedAuthnContext requestedAuthnContext = transformedRequest.getRequestedAuthnContext();
        Assert.assertEquals(AuthnContextComparisonTypeEnumeration.MINIMUM, requestedAuthnContext.getComparison());
        AuthnContextClassRef authnContextClassRef = requestedAuthnContext.getAuthnContextClassRefs().get(0);
        Assert.assertEquals(LevelOfAssurance.HIGH.toString(), authnContextClassRef.getAuthnContextClassRef());
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

    @Test
    public void shouldGenerateAnEidasAuthnRequestExtensions() throws MarshallingException, SignatureException, SecurityException {
        List<AuthnContext> authnContexts = Collections.singletonList(AuthnContext.LEVEL_3);
        EidasAuthnRequestFromHub originalRequestFromTransaction = anEidasAuthnRequestFromHub(A_PROVIDER, HTTP_ISSUER_ENTITY_ID_COM, authnContexts);

        AuthnRequest transformedRequest = eidasTransformer.apply(originalRequestFromTransaction);
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
        Assert.assertEquals(EidasAuthnRequestToAuthnRequestTransformer.NATURAL_PERSON_NAME_PREFIX + "CurrentGivenName", firstNameRequestedAttribute.getName());
        Assert.assertEquals(Attribute.URI_REFERENCE, firstNameRequestedAttribute.getNameFormat());
        Assert.assertEquals(true, firstNameRequestedAttribute.isRequired());

        Assert.assertNotNull(reqAttrMap.get("FamilyName"));
        Assert.assertNotNull(reqAttrMap.get("CurrentAddress"));
        Assert.assertNotNull(reqAttrMap.get("DateOfBirth"));
        Assert.assertNotNull(reqAttrMap.get("PersonIdentifier"));
    }

    private Map<String, RequestedAttributeImpl> getRequestedAttributesByFriendlyName(List<XMLObject> requestedAttributes) {
        return requestedAttributes.stream()
            .map(x -> (RequestedAttributeImpl)x)
            .collect(Collectors.toMap(AttributeImpl::getFriendlyName, x -> x));
    }
}
