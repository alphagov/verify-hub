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
import uk.gov.ida.saml.hub.domain.HubAttributeQueryRequest;
import uk.gov.ida.saml.hub.factories.AttributeFactory_1_1;
import uk.gov.ida.saml.hub.factories.AttributeQueryAttributeFactory;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.ida.saml.core.test.builders.HubAssertionBuilder.aHubAssertion;
import static uk.gov.ida.saml.core.test.builders.PassthroughAssertionBuilder.aPassthroughAssertion;
import static uk.gov.ida.saml.hub.domain.UserAccountCreationAttribute.CURRENT_ADDRESS;
import static uk.gov.ida.saml.hub.domain.UserAccountCreationAttribute.DATE_OF_BIRTH;
import static uk.gov.ida.saml.hub.test.builders.HubAttributeQueryRequestBuilder.aHubAttributeQueryRequest;

@RunWith(OpenSAMLMockitoRunner.class)
public class HubAttributeQueryRequestToSamlAttributeQueryTransformerTest {

    public static final String ENCRYPTED_MDS_ASSERTION = "encrypted-mds-assertion!";
    public static final String ENCRYPTED_AUTHN_ASSERTION = "encrypted-authn-statement-assertion!";
    public static final String AUTHN_STATEMENT_ID = "AUTHEN_STATEMENT_ID";

    private HubAttributeQueryRequestToSamlAttributeQueryTransformer transformer;

    private OpenSamlXmlObjectFactory openSamlXmlObjectFactory;

    @Mock
    private AttributeFactory_1_1 attributeFactory;
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

        transformer = new HubAttributeQueryRequestToSamlAttributeQueryTransformer(
                openSamlXmlObjectFactory,
                assertionTransformer,
                attributeQueryAttributeFactory,
                encryptedAssertionUnmarshaller);
    }

    @Test
    public void transform_shouldProperlyTransform() throws Exception {
        PersistentId persistentId = new PersistentId("default-name-id");
        HubAttributeQueryRequest originalQuery = aHubAttributeQueryRequest()
                .withId("originalId")
                .withPersistentId(persistentId)
                .build();

        AttributeQuery transformedQuery = transformer.apply(originalQuery);

        assertThat(transformedQuery.getID()).isEqualTo(originalQuery.getId());
        assertThat(transformedQuery.getSubject().getNameID().getValue()).isEqualTo(persistentId.getNameId());
        assertThat(transformedQuery.getIssuer().getValue()).isEqualTo(originalQuery.getIssuer());
        assertThat(transformedQuery.getVersion()).isEqualTo(SAMLVersion.VERSION_20);
    }

    @Test
    public void transform_shouldOnlyIncludeCycle3Unencrypted() throws Exception {
        final HubAssertion cycle3DataAssertion = aHubAssertion().build();

        HubAttributeQueryRequest originalQuery = aHubAttributeQueryRequest()
                .withCycle3DataAssertion(cycle3DataAssertion)
                .build();

        AttributeQuery transformedQuery = transformer.apply(originalQuery);

        List<XMLObject> unknownXMLObjects = transformedQuery.getSubject().getSubjectConfirmations().get(0).getSubjectConfirmationData().getUnknownXMLObjects(Assertion.DEFAULT_ELEMENT_NAME);
        assertThat(unknownXMLObjects.size()).isEqualTo(1);
        assertThat(((Assertion)unknownXMLObjects.get(0)).getID()).isEqualTo(cycle3DataAssertion.getId());
    }

    @Test
    public void transform_shouldContainBothMdsAndAuthnAssertionsEncrypted() throws Exception {
        HubAttributeQueryRequest originalQuery = aHubAttributeQueryRequest()
                .withEncryptedMatchingDatasetAssertion(ENCRYPTED_MDS_ASSERTION)
                .withEncryptedAuthnAssertion(ENCRYPTED_AUTHN_ASSERTION)
                .build();

        final EncryptedAssertion value1 = new EncryptedAssertionBuilder().buildObject();
        final EncryptedAssertion value2 = new EncryptedAssertionBuilder().buildObject();
        when(encryptedAssertionUnmarshaller.transform(ENCRYPTED_MDS_ASSERTION)).thenReturn(value1);
        when(encryptedAssertionUnmarshaller.transform(ENCRYPTED_AUTHN_ASSERTION)).thenReturn(value2);
        AttributeQuery transformedQuery = transformer.apply(originalQuery);

        List<XMLObject> encryptedAssertions = transformedQuery.getSubject()
                .getSubjectConfirmations().get(0).getSubjectConfirmationData().getUnknownXMLObjects(EncryptedAssertion.DEFAULT_ELEMENT_NAME);
        assertThat(encryptedAssertions.size()).isEqualTo(2);
        assertThat(encryptedAssertions).contains(value1, value2);
    }

    @Test
    public void transform_shouldSetTheSPNameQualifierAndNameQualifierToValuesThatShouldntBeThereButCurrentlyHaveNoWhereBetterToBe() throws Exception {
        final String authnStatementAssertion = aPassthroughAssertion().withUnderlyingAssertion(ENCRYPTED_AUTHN_ASSERTION).buildAuthnStatementAssertionAsString();
        final HubAssertion cycle3DataAssertion = aHubAssertion().build();

        HubAttributeQueryRequest originalQuery = aHubAttributeQueryRequest()
                .withEncryptedAuthnAssertion(authnStatementAssertion)
                .withCycle3DataAssertion(cycle3DataAssertion)
                .withAssertionConsumerServiceUrl(URI.create("/foo"))
                .withAuthnRequestIssuerEntityId("authn-request-issuer")
                .build();

        AttributeQuery transformedQuery = transformer.apply(originalQuery);

        NameID nameID = transformedQuery.getSubject().getNameID();

        assertThat(nameID.getSPNameQualifier()).isEqualTo("authn-request-issuer");
        assertThat(nameID.getNameQualifier()).isEqualTo("/foo");
    }

    @Test
    public void transform_shouldSetAttributesToUserAccountCreationAttributes(){
        Attribute attribute1 = openSamlXmlObjectFactory.createAttribute();
        Attribute attribute2 = openSamlXmlObjectFactory.createAttribute();
        when(attributeQueryAttributeFactory.createAttribute(CURRENT_ADDRESS)).thenReturn(attribute1);
        when(attributeQueryAttributeFactory.createAttribute(DATE_OF_BIRTH)).thenReturn(attribute2);

        HubAttributeQueryRequest hubAttributeQueryRequest = aHubAttributeQueryRequest()
                .addUserAccountCreationAttribute(CURRENT_ADDRESS)
                .addUserAccountCreationAttribute(DATE_OF_BIRTH)
                .build();

        AttributeQuery transformedQuery = transformer.apply(hubAttributeQueryRequest);

        List<Attribute> transformedQueryAttributes = transformedQuery.getAttributes();
        assertThat(transformedQueryAttributes.size()).isEqualTo(2);
        assertThat(transformedQueryAttributes).contains(attribute1);
        assertThat(transformedQueryAttributes).contains(attribute2);
    }

    @Test
    public void transform_shouldNotExplodeWhenUserAccountCreationAttributesAreAbsent(){
        HubAttributeQueryRequest hubAttributeQueryRequest = aHubAttributeQueryRequest()
                .withoutUserAccountCreationAttributes()
                .build();

        AttributeQuery transformedQuery = transformer.apply(hubAttributeQueryRequest);

        List<Attribute> transformedQueryAttributes = transformedQuery.getAttributes();
        assertThat(transformedQueryAttributes.size()).isEqualTo(0);
    }
}
