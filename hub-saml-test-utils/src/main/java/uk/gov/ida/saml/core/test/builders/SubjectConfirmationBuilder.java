package uk.gov.ida.saml.core.test.builders;

import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;

import com.google.common.base.Optional;

import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;

public class SubjectConfirmationBuilder {

    private OpenSamlXmlObjectFactory openSamlXmlObjectFactory = new OpenSamlXmlObjectFactory();
    private Optional<String> method = Optional.fromNullable(SubjectConfirmation.METHOD_BEARER);
    private Optional<SubjectConfirmationData> subjectConfirmationData = Optional.fromNullable(SubjectConfirmationDataBuilder.aSubjectConfirmationData().build());

    public static SubjectConfirmationBuilder aSubjectConfirmation() {
        return new SubjectConfirmationBuilder();
    }

    public SubjectConfirmation build() {
        SubjectConfirmation subjectConfirmation = openSamlXmlObjectFactory.createSubjectConfirmation();

        if (method.isPresent()) {
            subjectConfirmation.setMethod(method.get());
        }

        if (subjectConfirmationData.isPresent()) {
            subjectConfirmation.setSubjectConfirmationData(subjectConfirmationData.get());
        }

        return subjectConfirmation;
    }

    public SubjectConfirmationBuilder withMethod(String method) {
        this.method = Optional.fromNullable(method);
        return this;
    }

    public SubjectConfirmationBuilder withSubjectConfirmationData(SubjectConfirmationData subjectConfirmationData) {
        this.subjectConfirmationData = Optional.fromNullable(subjectConfirmationData);
        return this;
    }
}
