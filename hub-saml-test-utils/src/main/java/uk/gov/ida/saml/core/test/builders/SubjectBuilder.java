package uk.gov.ida.saml.core.test.builders;

import static uk.gov.ida.saml.core.test.builders.NameIdBuilder.aNameId;

import java.util.ArrayList;
import java.util.List;

import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.core.SubjectConfirmation;

import com.google.common.base.Optional;

import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;

public class SubjectBuilder {

    private static OpenSamlXmlObjectFactory openSamlXmlObjectFactory = new OpenSamlXmlObjectFactory();
    private Optional<NameID> nameIdValue = Optional.fromNullable(aNameId().build());
    private List<SubjectConfirmation> subjectConfirmations = new ArrayList<>();
    private boolean shouldAddDefaultSubjectConfirmation = true;

    public static SubjectBuilder aSubject() {
        return new SubjectBuilder();
    }

    public Subject build() {
        Subject subject = openSamlXmlObjectFactory.createSubject();

        if (nameIdValue.isPresent()) {
            subject.setNameID(nameIdValue.get());
        }

        if (shouldAddDefaultSubjectConfirmation) {
            subjectConfirmations.add(SubjectConfirmationBuilder.aSubjectConfirmation().build());
        }
        subject.getSubjectConfirmations().addAll(subjectConfirmations);

        return subject;
    }

    public SubjectBuilder withNameId(NameID nameId) {
        this.nameIdValue = Optional.fromNullable(nameId);
        return this;
    }

    public SubjectBuilder withSubjectConfirmation(SubjectConfirmation subjectConfirmation) {
        this.subjectConfirmations.add(subjectConfirmation);
        this.shouldAddDefaultSubjectConfirmation = false;
        return this;
    }

    public SubjectBuilder withPersistentId(String persistentId) {
        return withNameId(aNameId().withValue(persistentId).build());
    }
}
