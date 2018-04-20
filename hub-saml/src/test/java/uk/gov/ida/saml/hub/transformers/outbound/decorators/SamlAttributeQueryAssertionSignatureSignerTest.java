package uk.gov.ida.saml.hub.transformers.outbound.decorators;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AttributeQuery;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.security.credential.Credential;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.saml.core.test.PrivateKeyStoreFactory;
import uk.gov.ida.saml.core.test.TestCertificateStrings;
import uk.gov.ida.saml.core.test.TestCredentialFactory;
import uk.gov.ida.saml.core.test.TestEntityIds;
import uk.gov.ida.saml.security.IdaKeyStoreCredentialRetriever;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static uk.gov.ida.saml.core.test.builders.AssertionBuilder.anAssertion;
import static uk.gov.ida.saml.core.test.builders.AttributeQueryBuilder.anAttributeQuery;
import static uk.gov.ida.saml.core.test.builders.AttributeStatementBuilder.anAttributeStatement;
import static uk.gov.ida.saml.core.test.builders.IssuerBuilder.anIssuer;
import static uk.gov.ida.saml.core.test.builders.SimpleStringAttributeBuilder.aSimpleStringAttribute;
import static uk.gov.ida.saml.core.test.builders.SubjectBuilder.aSubject;
import static uk.gov.ida.saml.core.test.builders.SubjectConfirmationBuilder.aSubjectConfirmation;
import static uk.gov.ida.saml.core.test.builders.SubjectConfirmationDataBuilder.aSubjectConfirmationData;
import static uk.gov.ida.saml.hub.transformers.outbound.decorators.StringEncoding.toBase64Encoded;

@RunWith(OpenSAMLMockitoRunner.class)
public class SamlAttributeQueryAssertionSignatureSignerTest {

    private OpenSamlXmlObjectFactory samlObjectFactory = new OpenSamlXmlObjectFactory();

    @Mock
    private IdaKeyStoreCredentialRetriever keyStoreCredentialRetriever;
    public SamlAttributeQueryAssertionSignatureSigner assertionSignatureSigner;

    @Before
    public void setUp() throws Exception {
        assertionSignatureSigner = new SamlAttributeQueryAssertionSignatureSigner(
                keyStoreCredentialRetriever,
                samlObjectFactory,
                TestEntityIds.HUB_ENTITY_ID);
    }

    @Test
    public void decorate_shouldSetSignatureOnAssertionIssuedByTheHub() {
        Credential hubSigningCredential = createHubSigningCredential();
        when(keyStoreCredentialRetriever.getSigningCredential()).thenReturn(hubSigningCredential);
        AttributeQuery inputAttributeQuery = anAttributeQueryWithHubSignature();

        AttributeQuery attributeQuery = assertionSignatureSigner.signAssertions(inputAttributeQuery);

        final Credential assertionSigningCredential = getAssertionSigningCredential(attributeQuery);
        assertThat(assertionSigningCredential).isSameAs(hubSigningCredential);
        verify(keyStoreCredentialRetriever, times(1)).getSigningCredential();
        verifyNoMoreInteractions(keyStoreCredentialRetriever);
    }

    private Credential createHubSigningCredential() {
        return new TestCredentialFactory(TestCertificateStrings.HUB_TEST_PUBLIC_SIGNING_CERT, toBase64Encoded(
                new PrivateKeyStoreFactory().create(TestEntityIds.HUB_ENTITY_ID).getSigningPrivateKey()
                        .getEncoded()
        )).getSigningCredential();
    }

    private Credential getAssertionSigningCredential(final AttributeQuery attributeQuery) {
        Assertion cycle3DatasetAssertion = (Assertion) attributeQuery.getSubject().getSubjectConfirmations().get(0).getSubjectConfirmationData().getUnknownXMLObjects(Assertion.TYPE_NAME).get(0);
        return cycle3DatasetAssertion.getSignature().getSigningCredential();
    }

    private AttributeQuery anAttributeQueryWithHubSignature() {
        return anAttributeQuery()
                .withSubject(
                        aSubject()
                                .withSubjectConfirmation(
                                        aSubjectConfirmation()
                                                .withSubjectConfirmationData(
                                                        aSubjectConfirmationData()
                                                                .addAssertion(
                                                                        unencyptedCycle3DatasetAssertion()
                                                                ).build()
                                                ).build()
                                ).build()
                ).build();
    }

    private Assertion unencyptedCycle3DatasetAssertion() {
        return anAssertion()
                .withIssuer(
                        hubIssuer()
                )
                .addAttributeStatement(
                        anAttributeStatement()
                                .addAttribute(
                                        aSimpleStringAttribute()
                                                .withName(
                                                        "NINO"
                                                )
                                                .withSimpleStringValue(
                                                        "MyNINO"
                                                )
                                                .build()
                                )
                                .build()
                )
                .buildUnencrypted();
    }

    private Issuer hubIssuer() {
        return anIssuer().withIssuerId(TestEntityIds.HUB_ENTITY_ID).build();
    }
}
