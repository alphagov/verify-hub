package uk.gov.ida.saml.hub.transformers.outbound.decorators;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AttributeQuery;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.hub.HubConstants;
import uk.gov.ida.saml.security.IdaKeyStoreCredentialRetriever;

import static org.opensaml.xmlsec.signature.support.Signer.signObject;

public class SamlAttributeQueryAssertionSignatureSigner {

    private static final Logger LOG = LoggerFactory.getLogger(SamlAttributeQueryAssertionSignatureSigner.class);

    private final IdaKeyStoreCredentialRetriever keyStoreCredentialRetriever;
    private final OpenSamlXmlObjectFactory samlObjectFactory;
    private final String hubEntityId;

    public SamlAttributeQueryAssertionSignatureSigner(
            IdaKeyStoreCredentialRetriever keyStoreCredentialRetriever,
            OpenSamlXmlObjectFactory samlObjectFactory,
            String hubEntityId) {

        this.keyStoreCredentialRetriever = keyStoreCredentialRetriever;
        this.samlObjectFactory = samlObjectFactory;
        this.hubEntityId = hubEntityId;
    }

    public AttributeQuery signAssertions(AttributeQuery attributeQuery) {
        LOG.debug("Sign attribute query's C3 assertion's signatures");

        for (SubjectConfirmation subjectConfirmation : attributeQuery.getSubject().getSubjectConfirmations()) {
            for (XMLObject xmlObject : subjectConfirmation.getSubjectConfirmationData().getUnknownXMLObjects(Assertion.TYPE_NAME)) {
                Assertion assertion = (Assertion) xmlObject;
                if (assertion.getIssuer().getValue().equals(hubEntityId)) {
                    assertion.setSignature(samlObjectFactory.createSignature());
                    assertion.getSignature().setSigningCredential(keyStoreCredentialRetriever.getSigningCredential());
                    try {
                        XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(assertion).marshall(assertion);
                        signObject(assertion.getSignature());
                    } catch (SignatureException | MarshallingException e) {
                        throw new IllegalStateException("Unable to sign assertion.", e);
                    }
                }
            }
        }
        return attributeQuery;
    }
}
