package uk.gov.ida.saml.core.transformers.outbound.decorators;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AttributeQuery;
import org.opensaml.saml.saml2.core.EncryptedAssertion;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;
import org.opensaml.saml.saml2.encryption.Encrypter;
import org.opensaml.security.credential.Credential;
import org.opensaml.xmlsec.encryption.support.EncryptionException;
import uk.gov.ida.saml.security.EncrypterFactory;
import uk.gov.ida.saml.security.EncryptionCredentialFactory;
import uk.gov.ida.saml.security.EntityToEncryptForLocator;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.ida.saml.core.test.builders.AssertionBuilder.anAssertion;
import static uk.gov.ida.saml.core.test.builders.AttributeQueryBuilder.anAttributeQuery;
import static uk.gov.ida.saml.core.test.builders.SubjectBuilder.aSubject;
import static uk.gov.ida.saml.core.test.builders.SubjectConfirmationBuilder.aSubjectConfirmation;
import static uk.gov.ida.saml.core.test.builders.SubjectConfirmationDataBuilder.aSubjectConfirmationData;

@RunWith(OpenSAMLMockitoRunner.class)
public class SamlAttributeQueryAssertionEncrypterTest {

    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    public Credential credential;
    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    public EncryptionCredentialFactory credentialFactory;
    public final EntityToEncryptForLocator entityToEncryptForLocator = mock(EntityToEncryptForLocator.class);
    public final EncrypterFactory encrypterFactory = mock(EncrypterFactory.class);
    public final Encrypter encrypter = mock(Encrypter.class);

    public AttributeQuery attributeQuery;
    public EncryptedAssertion encryptedAssertion;
    public SamlAttributeQueryAssertionEncrypter samlAttributeQueryAssertionEncrypter;
    public Assertion assertion;

    @Before
    public void setUp() throws Exception {
        assertion = anAssertion().buildUnencrypted();
        attributeQuery = anAttributeQueryWithAssertion(assertion);
        encryptedAssertion = anAssertion().build();
        when(entityToEncryptForLocator.fromRequestId(anyString())).thenReturn("some id");
        when(credentialFactory.getEncryptingCredential("some id")).thenReturn(credential);
        when(encrypterFactory.createEncrypter(credential)).thenReturn(encrypter);
        when(encrypter.encrypt(assertion)).thenReturn(encryptedAssertion);
        samlAttributeQueryAssertionEncrypter = new SamlAttributeQueryAssertionEncrypter(
                credentialFactory,
                encrypterFactory,
                entityToEncryptForLocator
        );
    }

    @Test
    public void shouldConvertAssertionIntoEncryptedAssertion() throws EncryptionException {
        final AttributeQuery decoratedAttributeQuery = samlAttributeQueryAssertionEncrypter.encryptAssertions(attributeQuery);

        final SubjectConfirmationData subjectConfirmationData = decoratedAttributeQuery.getSubject()
                .getSubjectConfirmations()
                .get(0)
                .getSubjectConfirmationData();
        final List<XMLObject> encryptedAssertions = subjectConfirmationData
                .getUnknownXMLObjects(EncryptedAssertion.DEFAULT_ELEMENT_NAME);
        assertThat(encryptedAssertions.size()).isEqualTo(1);
        assertThat((EncryptedAssertion) encryptedAssertions.get(0)).isEqualTo(encryptedAssertion);

        final List<XMLObject> unencryptedAssertions = subjectConfirmationData
                .getUnknownXMLObjects(Assertion.DEFAULT_ELEMENT_NAME);
        assertThat(unencryptedAssertions.size()).isEqualTo(0);
    }

    @Test
    public void decorate_shouldWrapEncryptionAssertionInSamlExceptionWhenEncryptionFails() throws EncryptionException {
        EncryptionException encryptionException = new EncryptionException("BLAM!");
        when(encrypter.encrypt(assertion)).thenThrow(encryptionException);

        SamlAttributeQueryAssertionEncrypter assertionEncrypter =
                new SamlAttributeQueryAssertionEncrypter(
                        credentialFactory,
                        encrypterFactory,
                        entityToEncryptForLocator
                );

        try {
            assertionEncrypter.encryptAssertions(attributeQuery);
        } catch (Exception e) {
            assertThat(e.getCause()).isEqualTo(encryptionException);
            return;
        }
        fail("Should never get here");
    }

    private AttributeQuery anAttributeQueryWithAssertion(final Assertion assertion) {
        return anAttributeQuery()
                    .withSubject(
                            aSubject()
                                    .withSubjectConfirmation(
                                            aSubjectConfirmation()
                                                    .withSubjectConfirmationData(
                                                            aSubjectConfirmationData()
                                                                    .addAssertion(assertion)
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();
    }
}
