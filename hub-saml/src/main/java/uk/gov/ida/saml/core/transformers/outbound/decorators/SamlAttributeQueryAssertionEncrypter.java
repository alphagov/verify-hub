package uk.gov.ida.saml.core.transformers.outbound.decorators;

import com.google.inject.Inject;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AttributeQuery;
import org.opensaml.saml.saml2.core.EncryptedAssertion;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;
import uk.gov.ida.saml.security.EncrypterFactory;
import uk.gov.ida.saml.security.EncryptionCredentialFactory;
import uk.gov.ida.saml.security.EntityToEncryptForLocator;

import java.util.List;

public class SamlAttributeQueryAssertionEncrypter extends AbstractAssertionEncrypter<AttributeQuery> {

    @Inject
    public SamlAttributeQueryAssertionEncrypter(
            final EncryptionCredentialFactory credentialFactory,
            final EncrypterFactory encrypterFactory,
            final EntityToEncryptForLocator entityToEncryptForLocator) {

        super(encrypterFactory, entityToEncryptForLocator, credentialFactory);
    }

    @Override
    protected String getRequestId(final AttributeQuery attributeQuery) {
        return attributeQuery.getID();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected List<EncryptedAssertion> getEncryptedAssertions(final AttributeQuery attributeQuery) {
        final SubjectConfirmationData subjectConfirmationData = attributeQuery.getSubject()
                .getSubjectConfirmations()
                .get(0)
                .getSubjectConfirmationData();

        return (List<EncryptedAssertion>) (List<?>)
                subjectConfirmationData.getUnknownXMLObjects(Assertion.DEFAULT_ELEMENT_NAME);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected List<Assertion> getAssertions(final AttributeQuery attributeQuery) {
        final SubjectConfirmationData subjectConfirmationData = attributeQuery.getSubject()
                .getSubjectConfirmations()
                .get(0)
                .getSubjectConfirmationData();
        return (List<Assertion>) (List<?>)
                subjectConfirmationData.getUnknownXMLObjects(Assertion.DEFAULT_ELEMENT_NAME);
    }
}
