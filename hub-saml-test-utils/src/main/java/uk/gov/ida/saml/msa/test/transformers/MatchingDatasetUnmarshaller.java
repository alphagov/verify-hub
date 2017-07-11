package uk.gov.ida.saml.msa.test.transformers;

import com.google.common.base.Optional;
import org.joda.time.LocalDate;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.saml.core.IdaConstants;
import uk.gov.ida.saml.core.extensions.PersonName;
import uk.gov.ida.saml.core.extensions.StringBasedMdsAttributeValue;
import uk.gov.ida.saml.core.domain.Address;
import uk.gov.ida.saml.core.domain.AddressFactory;
import uk.gov.ida.saml.core.domain.Gender;
import uk.gov.ida.saml.core.domain.MatchingDataset;
import uk.gov.ida.saml.core.domain.SimpleMdsValue;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Optional.*;
import static com.google.common.collect.Lists.*;
import static java.text.MessageFormat.*;

public class MatchingDatasetUnmarshaller {

    private static final Logger LOG = LoggerFactory.getLogger(MatchingDatasetUnmarshaller.class);
    private final AddressFactory addressFactory;

    public MatchingDatasetUnmarshaller(AddressFactory addressFactory) {
        this.addressFactory = addressFactory;
    }

    public MatchingDataset fromAssertion(Assertion assertion) {
        List<AttributeStatement> attributeStatements = assertion.getAttributeStatements();
        if (attributeStatements.isEmpty()) {
            // this returns null, and the consumer would wrap it with fromNullable.  Not awesome but it works.
            return null;
        }

        List<Attribute> attributes = attributeStatements.get(0).getAttributes();
        MatchingDatasetBuilder datasetBuilder = new MatchingDatasetBuilder();
        for (Attribute attribute : attributes) {
            transformAttribute(attribute, datasetBuilder);
        }

        return datasetBuilder.build();
    }

    private void transformAttribute(Attribute attribute, MatchingDatasetBuilder datasetBuilder) {
        switch (attribute.getName()) {
            case IdaConstants.Attributes_1_1.Firstname.NAME:
                datasetBuilder.firstname(transformPersonNameAttribute(attribute));
                break;

            case IdaConstants.Attributes_1_1.Middlename.NAME:
                datasetBuilder.middlenames(transformPersonNameAttribute(attribute));
                break;

            case IdaConstants.Attributes_1_1.Surname.NAME:
                datasetBuilder.addSurnames(transformPersonNameAttribute(attribute));
                break;

            case IdaConstants.Attributes_1_1.Gender.NAME:
                uk.gov.ida.saml.core.extensions.Gender gender = (uk.gov.ida.saml.core.extensions.Gender) attribute.getAttributeValues().get(0);
                datasetBuilder.gender(new SimpleMdsValue<>(Gender.fromString(gender.getValue()), gender.getFrom(), gender.getTo(), gender.getVerified()));
                break;

            case IdaConstants.Attributes_1_1.DateOfBirth.NAME:
                datasetBuilder.dateOfBirth(getBirthdates(attribute));
                break;

            case IdaConstants.Attributes_1_1.CurrentAddress.NAME:
                List<Address> transformedCurrentAddresses = addressFactory.create(attribute);
                datasetBuilder.addCurrentAddresses(transformedCurrentAddresses);
                break;

            case IdaConstants.Attributes_1_1.PreviousAddress.NAME:
                List<Address> transformedPreviousAddresses = addressFactory.create(attribute);
                datasetBuilder.addPreviousAddresses(transformedPreviousAddresses);
                break;

            default:
                String errorMessage = format("Attribute {0} is not a supported Matching Dataset attribute.", attribute.getName());
                LOG.warn(errorMessage);
                throw new IllegalArgumentException(errorMessage);
        }
    }

    private List<SimpleMdsValue<LocalDate>> getBirthdates(Attribute attribute) {
        List<SimpleMdsValue<LocalDate>> birthDates = new ArrayList<>();

        for (XMLObject xmlObject : attribute.getAttributeValues()) {
            StringBasedMdsAttributeValue stringBasedMdsAttributeValue = (StringBasedMdsAttributeValue) xmlObject;
            String dateOfBirthString = stringBasedMdsAttributeValue.getValue();
            birthDates.add(new SimpleMdsValue<>(
                    LocalDate.parse(dateOfBirthString),
                    stringBasedMdsAttributeValue.getFrom(),
                    stringBasedMdsAttributeValue.getTo(),
                    stringBasedMdsAttributeValue.getVerified()));
        }

        return birthDates;
    }

    private List<SimpleMdsValue<String>> transformPersonNameAttribute(Attribute attribute) {
        List<SimpleMdsValue<String>> personNames = new ArrayList<>();

        for (XMLObject xmlObject : attribute.getAttributeValues()) {
            PersonName personName = (PersonName) xmlObject;
            personNames.add(new SimpleMdsValue<>(personName.getValue(), personName.getFrom(), personName.getTo(), personName.getVerified()));
        }

        return personNames;
    }

    private static class MatchingDatasetBuilder {
        private List<SimpleMdsValue<String>> firstnames = new ArrayList<>();
        private List<SimpleMdsValue<String>> middlenames = new ArrayList<>();
        private List<SimpleMdsValue<String>> surnames = new ArrayList<>();
        private Optional<SimpleMdsValue<Gender>> gender = Optional.absent();
        private List<SimpleMdsValue<LocalDate>> dateOfBirths = new ArrayList<>();
        private List<Address> currentAddresses = newArrayList();
        private List<Address> previousAddresses = newArrayList();

        public MatchingDatasetBuilder firstname(List<SimpleMdsValue<String>> firstnames) {
            this.firstnames.addAll(firstnames);
            return this;
        }

        public MatchingDatasetBuilder middlenames(List<SimpleMdsValue<String>> middlenames) {
            this.middlenames.addAll(middlenames);
            return this;
        }

        public MatchingDatasetBuilder addSurnames(List<SimpleMdsValue<String>> surnames) {
            this.surnames.addAll(surnames);
            return this;
        }

        public MatchingDatasetBuilder gender(SimpleMdsValue<Gender> gender) {
            this.gender = fromNullable(gender);
            return this;
        }

        public MatchingDatasetBuilder dateOfBirth(List<SimpleMdsValue<LocalDate>> dateOfBirths) {
            this.dateOfBirths.addAll(dateOfBirths);
            return this;
        }

        public MatchingDatasetBuilder addCurrentAddresses(List<Address> currentAddresses) {
            this.currentAddresses.addAll(currentAddresses);
            return this;
        }

        public MatchingDatasetBuilder addPreviousAddresses(List<Address> previousAddresses) {
            this.previousAddresses.addAll(previousAddresses);
            return this;
        }

        public MatchingDataset build() {
            return new MatchingDataset(firstnames, middlenames, surnames, gender, dateOfBirths, currentAddresses, previousAddresses);
        }
    }
}
