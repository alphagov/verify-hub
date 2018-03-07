package uk.gov.ida.saml.hub.transformers.outbound;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeQuery;
import org.opensaml.saml.saml2.core.EncryptedAssertion;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.impl.EncryptedAssertionBuilder;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.domain.HubAssertion;
import uk.gov.ida.saml.core.domain.PersistentId;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.saml.core.transformers.outbound.OutboundAssertionToSubjectTransformer;
import uk.gov.ida.saml.deserializers.StringToOpenSamlObjectTransformer;
import uk.gov.ida.saml.hub.domain.HubEidasAttributeQueryRequest;
import uk.gov.ida.saml.hub.factories.AttributeFactory_1_1;
import uk.gov.ida.saml.hub.factories.AttributeQueryAttributeFactory;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static uk.gov.ida.saml.core.test.builders.AssertionBuilder.anAssertion;
import static uk.gov.ida.saml.core.test.builders.HubAssertionBuilder.aHubAssertion;
import static uk.gov.ida.saml.hub.domain.UserAccountCreationAttribute.CURRENT_ADDRESS;
import static uk.gov.ida.saml.hub.domain.UserAccountCreationAttribute.DATE_OF_BIRTH;
import static uk.gov.ida.saml.hub.builders.HubEidasAttributeQueryRequestBuilder.aHubEidasAttributeQueryRequest;

@RunWith(OpenSAMLMockitoRunner.class)
public class HubEidasAttributeQueryRequestToSamlAttributeQueryTransformerTest {

    public static final String ENCRYPTED_IDENTITY_ASSERTION = "encrypted-identity-assertion!";

    private HubEidasAttributeQueryRequestToSamlAttributeQueryTransformer transformer;

    private OpenSamlXmlObjectFactory openSamlXmlObjectFactory;

    @Mock
    private AttributeFactory_1_1 attributeFactory;

    @Mock
    private StringToOpenSamlObjectTransformer<Assertion> stringAssertionTransformer;

    @Mock
    private OutboundAssertionToSubjectTransformer outboundAssertionToSubjectTransformer;

    @Mock
    private AttributeQueryAttributeFactory attributeQueryAttributeFactory;

    @Mock
    private EncryptedAssertionUnmarshaller encryptedAssertionUnmarshaller;

    @Before
    public void setup() throws Exception {
        openSamlXmlObjectFactory = new OpenSamlXmlObjectFactory();
        HubAssertionMarshaller assertionTransformer = new HubAssertionMarshaller(openSamlXmlObjectFactory, attributeFactory, outboundAssertionToSubjectTransformer);
        AssertionFromIdpToAssertionTransformer assertionFromIdpAssertionTransformer = new AssertionFromIdpToAssertionTransformer(stringAssertionTransformer);

        when(stringAssertionTransformer.apply(anyString())).thenReturn(anAssertion().buildUnencrypted());

        transformer = new HubEidasAttributeQueryRequestToSamlAttributeQueryTransformer(
            openSamlXmlObjectFactory,
            assertionTransformer,
            assertionFromIdpAssertionTransformer,
            attributeQueryAttributeFactory,
            encryptedAssertionUnmarshaller);
    }

    @Test
    public void shouldTransformProperly() throws Exception {
        PersistentId persistentId = new PersistentId("default-name-id");
        HubEidasAttributeQueryRequest hubEidasAttributeQueryRequest = aHubEidasAttributeQueryRequest()
            .withId("originalId")
            .withPersistentId(persistentId)
            .build();

        AttributeQuery transformedQuery = transformer.apply(hubEidasAttributeQueryRequest);

        assertThat(transformedQuery.getID()).isEqualTo(hubEidasAttributeQueryRequest.getId());
        assertThat(transformedQuery.getSubject().getNameID().getValue()).isEqualTo(persistentId.getNameId());
        assertThat(transformedQuery.getIssuer().getValue()).isEqualTo(hubEidasAttributeQueryRequest.getIssuer());
        assertThat(transformedQuery.getVersion()).isEqualTo(SAMLVersion.VERSION_20);
    }

    @Test
    public void shouldIncludeCycle3Assertion() throws Exception {
        final HubAssertion cycle3DataAssertion = aHubAssertion().build();

        HubEidasAttributeQueryRequest hubEidasAttributeQueryRequest = aHubEidasAttributeQueryRequest()
            .withCycle3DataAssertion(cycle3DataAssertion)
            .build();

        AttributeQuery transformedQuery = transformer.apply(hubEidasAttributeQueryRequest);

        List<XMLObject> unknownXMLObjects = transformedQuery.getSubject().getSubjectConfirmations().get(0).getSubjectConfirmationData().getUnknownXMLObjects(Assertion.DEFAULT_ELEMENT_NAME);
        assertThat(unknownXMLObjects.size()).isEqualTo(1);
        assertThat(((Assertion) unknownXMLObjects.get(0)).getID()).isEqualTo(cycle3DataAssertion.getId());
    }

    @Test
    public void shouldIncludeEncryptedIdentityAssertionOnly() throws Exception {
        HubEidasAttributeQueryRequest hubEidasAttributeQueryRequest = aHubEidasAttributeQueryRequest()
            .withEncryptedIdentityAssertion(ENCRYPTED_IDENTITY_ASSERTION)
            .build();

        final EncryptedAssertion value = new EncryptedAssertionBuilder().buildObject();
        when(encryptedAssertionUnmarshaller.transform(ENCRYPTED_IDENTITY_ASSERTION)).thenReturn(value);
        AttributeQuery transformedQuery = transformer.apply(hubEidasAttributeQueryRequest);

        List<XMLObject> encryptedAssertions = transformedQuery.getSubject()
            .getSubjectConfirmations().get(0).getSubjectConfirmationData().getUnknownXMLObjects(EncryptedAssertion.DEFAULT_ELEMENT_NAME);
        assertThat(encryptedAssertions.size()).isEqualTo(1);
        assertThat(((EncryptedAssertion) encryptedAssertions.get(0))).isEqualTo(value);

        List<XMLObject> assertions = transformedQuery.getSubject()
            .getSubjectConfirmations().get(0).getSubjectConfirmationData().getUnknownXMLObjects(Assertion.DEFAULT_ELEMENT_NAME);
        assertThat(assertions.size()).isEqualTo(0);
    }

    @Test
    public void shouldIncludeEncryptedIdentityAssertionAndCycle3Assertion() throws Exception {
        final HubAssertion cycle3DataAssertion = aHubAssertion().build();

        HubEidasAttributeQueryRequest hubEidasAttributeQueryRequest = aHubEidasAttributeQueryRequest()
            .withEncryptedIdentityAssertion(ENCRYPTED_IDENTITY_ASSERTION)
            .withCycle3DataAssertion(cycle3DataAssertion)
            .build();

        final EncryptedAssertion value = new EncryptedAssertionBuilder().buildObject();
        when(encryptedAssertionUnmarshaller.transform(ENCRYPTED_IDENTITY_ASSERTION)).thenReturn(value);
        AttributeQuery transformedQuery = transformer.apply(hubEidasAttributeQueryRequest);

        List<XMLObject> encryptedAssertions = transformedQuery.getSubject()
            .getSubjectConfirmations().get(0).getSubjectConfirmationData().getUnknownXMLObjects(EncryptedAssertion.DEFAULT_ELEMENT_NAME);
        assertThat(encryptedAssertions.size()).isEqualTo(1);
        assertThat(((EncryptedAssertion) encryptedAssertions.get(0))).isEqualTo(value);

        List<XMLObject> assertions = transformedQuery.getSubject()
            .getSubjectConfirmations().get(0).getSubjectConfirmationData().getUnknownXMLObjects(Assertion.DEFAULT_ELEMENT_NAME);
        assertThat(assertions.size()).isEqualTo(1);
        assertThat(((Assertion) assertions.get(0)).getID()).isEqualTo(cycle3DataAssertion.getId());
    }

    @Test
    public void shouldSetTheSPNameQualifierAndNameQualifierToValuesThatShouldntBeThereButCurrentlyHaveNoWhereBetterToBe() throws Exception {
        final HubAssertion cycle3DataAssertion = aHubAssertion().build();

        HubEidasAttributeQueryRequest hubEidasAttributeQueryRequest = aHubEidasAttributeQueryRequest()
            .withCycle3DataAssertion(cycle3DataAssertion)
            .withAssertionConsumerServiceUrl(URI.create("/foo"))
            .withAuthnRequestIssuerEntityId("authn-request-issuer")
            .build();

        AttributeQuery transformedQuery = transformer.apply(hubEidasAttributeQueryRequest);

        NameID nameID = transformedQuery.getSubject().getNameID();

        assertThat(nameID.getSPNameQualifier()).isEqualTo("authn-request-issuer");
        assertThat(nameID.getNameQualifier()).isEqualTo("/foo");
    }

    @Test
    public void shouldSetAttributesToUserAccountCreationAttributes() {
        Attribute attribute1 = openSamlXmlObjectFactory.createAttribute();
        Attribute attribute2 = openSamlXmlObjectFactory.createAttribute();
        when(attributeQueryAttributeFactory.createAttribute(CURRENT_ADDRESS)).thenReturn(attribute1);
        when(attributeQueryAttributeFactory.createAttribute(DATE_OF_BIRTH)).thenReturn(attribute2);

        HubEidasAttributeQueryRequest hubEidasAttributeQueryRequest = aHubEidasAttributeQueryRequest()
            .addUserAccountCreationAttribute(CURRENT_ADDRESS)
            .addUserAccountCreationAttribute(DATE_OF_BIRTH)
            .build();

        AttributeQuery transformedQuery = transformer.apply(hubEidasAttributeQueryRequest);

        List<Attribute> transformedQueryAttributes = transformedQuery.getAttributes();
        assertThat(transformedQueryAttributes.size()).isEqualTo(2);
        assertThat(transformedQueryAttributes).contains(attribute1);
        assertThat(transformedQueryAttributes).contains(attribute2);
    }

    @Test
    public void shouldNotExplodeWhenUserAccountCreationAttributesAreAbsent() {
        HubEidasAttributeQueryRequest hubEidasAttributeQueryRequest = aHubEidasAttributeQueryRequest()
            .withoutUserAccountCreationAttributes()
            .build();

        AttributeQuery transformedQuery = transformer.apply(hubEidasAttributeQueryRequest);

        List<Attribute> transformedQueryAttributes = transformedQuery.getAttributes();
        assertThat(transformedQueryAttributes.size()).isEqualTo(0);
    }
}
