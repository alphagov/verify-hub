package uk.gov.ida.saml.core.test.builders;

import com.google.common.base.Optional;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeQuery;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.opensaml.xmlsec.signature.support.Signer;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Throwables.propagate;

public class AttributeQueryBuilder {

    private Optional<Signature> signature = Optional.fromNullable(SignatureBuilder.aSignature().build());
    private boolean shouldSign = true;
    private Optional<Subject> subject = Optional.fromNullable(SubjectBuilder.aSubject().build());
    private Optional<Issuer> issuer = Optional.fromNullable(IssuerBuilder.anIssuer().build());
    private Optional<String> id = Optional.fromNullable("anId");
    private List<Attribute> attributes = new ArrayList<>();

    public static AttributeQueryBuilder anAttributeQuery() {
        return new AttributeQueryBuilder();
    }

    public AttributeQuery build() {
        AttributeQuery attributeQuery = new OpenSamlXmlObjectFactory().createAttributeQuery();

        if (subject.isPresent()) {
            attributeQuery.setSubject(subject.get());
        }

        if (issuer.isPresent()) {
            attributeQuery.setIssuer(issuer.get());
        }

        if (id.isPresent()) {
            attributeQuery.setID(id.get());
        }

        if (signature.isPresent()) {
            attributeQuery.setSignature(signature.get());
            try {
                XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(attributeQuery).marshall(attributeQuery);
                if (shouldSign) {
                    Signer.signObject(attributeQuery.getSignature());
                }
            } catch (MarshallingException | SignatureException e) {
                throw propagate(e);
            }
        }

        attributeQuery.getAttributes().addAll(attributes);

        return attributeQuery;
    }

    public AttributeQueryBuilder withSubject(Subject subject) {
        this.subject = Optional.fromNullable(subject);
        return this;
    }

    public AttributeQueryBuilder withoutSigning() {
        shouldSign = false;
        return this;
    }

    public AttributeQueryBuilder withId(String id) {
        this.id = Optional.fromNullable(id);
        return this;
    }

    public AttributeQueryBuilder withIssuer(Issuer issuer) {
        this.issuer = Optional.fromNullable(issuer);
        return this;
    }

    public AttributeQueryBuilder withAttributes(List<Attribute> attributes){
        this.attributes = attributes;
        return this;
    }

    public AttributeQueryBuilder withSignature(Signature signature) {
        this.signature = Optional.fromNullable(signature);
        return this;
    }
}