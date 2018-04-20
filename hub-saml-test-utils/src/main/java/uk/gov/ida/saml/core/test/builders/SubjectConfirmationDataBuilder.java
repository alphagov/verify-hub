package uk.gov.ida.saml.core.test.builders;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.EncryptedAssertion;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;

import com.google.common.base.Optional;

import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.test.TestEntityIds;

public class SubjectConfirmationDataBuilder {

    public static final int NOT_ON_OR_AFTER_DEFAULT_PERIOD = 15;

    private OpenSamlXmlObjectFactory openSamlXmlObjectFactory = new OpenSamlXmlObjectFactory();
    private Optional<String> recipient = Optional.fromNullable(TestEntityIds.HUB_ENTITY_ID);
    private Optional<DateTime> notOnOrAfter = Optional.fromNullable(DateTime.now().plusMinutes(NOT_ON_OR_AFTER_DEFAULT_PERIOD));
    private Optional<DateTime> notBefore = Optional.absent();
    private Optional<String> address = Optional.absent();
    private Optional<String> inResponseTo = Optional.fromNullable(ResponseBuilder.DEFAULT_REQUEST_ID);
    private List<Assertion> assertions = new ArrayList<>();
    private List<EncryptedAssertion> encryptedAssertions = new ArrayList<>();

    public static SubjectConfirmationDataBuilder aSubjectConfirmationData() {
        return new SubjectConfirmationDataBuilder();
    }

    public SubjectConfirmationData build() {
        SubjectConfirmationData subjectConfirmationData = openSamlXmlObjectFactory.createSubjectConfirmationData();

        if (recipient.isPresent()) {
            subjectConfirmationData.setRecipient(recipient.get());
        }
        if (notOnOrAfter.isPresent()) {
            subjectConfirmationData.setNotOnOrAfter(notOnOrAfter.get());
        }
        if (notBefore.isPresent()) {
            subjectConfirmationData.setNotBefore(notBefore.get());
        }
        if (inResponseTo.isPresent()) {
            subjectConfirmationData.setInResponseTo(inResponseTo.get());
        }
        if (address.isPresent()) {
            subjectConfirmationData.setAddress(address.get());
        }
        subjectConfirmationData.getUnknownXMLObjects().addAll(assertions);
        subjectConfirmationData.getUnknownXMLObjects().addAll(encryptedAssertions);

        return subjectConfirmationData;
    }

    public SubjectConfirmationDataBuilder withRecipient(String recipient) {
        this.recipient = Optional.fromNullable(recipient);
        return this;
    }

    public SubjectConfirmationDataBuilder withNotOnOrAfter(DateTime notOnOrAfter) {
        this.notOnOrAfter = Optional.fromNullable(notOnOrAfter);
        return this;
    }

    public SubjectConfirmationDataBuilder withNotBefore(DateTime notBefore) {
        this.notBefore = Optional.fromNullable(notBefore);
        return this;
    }

    public SubjectConfirmationDataBuilder withAddress(String address) {
        this.address = Optional.fromNullable(address);
        return this;
    }

    public SubjectConfirmationDataBuilder withInResponseTo(String inResponseTo) {
        this.inResponseTo = Optional.fromNullable(inResponseTo);
        return this;
    }

    public SubjectConfirmationDataBuilder addAssertion(Assertion assertion) {
        this.assertions.add(assertion);
        return this;
    }

    public SubjectConfirmationDataBuilder addAssertions(List<Assertion> assertions) {
        this.assertions.addAll(assertions);
        return this;
    }

    public SubjectConfirmationDataBuilder addAssertion(final EncryptedAssertion assertion) {
        this.encryptedAssertions.add(assertion);
        return this;
    }
}
