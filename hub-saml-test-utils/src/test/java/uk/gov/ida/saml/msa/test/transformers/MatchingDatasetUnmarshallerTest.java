package uk.gov.ida.saml.msa.test.transformers;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import uk.gov.ida.saml.core.extensions.Address;
import uk.gov.ida.saml.core.extensions.Gender;
import uk.gov.ida.saml.core.extensions.PersonName;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.saml.core.domain.AddressFactory;
import uk.gov.ida.saml.core.domain.MatchingDataset;

import static java.util.Arrays.*;
import static org.assertj.core.api.Assertions.*;
import static uk.gov.ida.saml.core.test.builders.AddressAttributeBuilder_1_1.*;
import static uk.gov.ida.saml.core.test.builders.AddressAttributeValueBuilder_1_1.*;
import static uk.gov.ida.saml.core.test.builders.AssertionBuilder.*;
import static uk.gov.ida.saml.core.test.builders.AttributeStatementBuilder.*;
import static uk.gov.ida.saml.core.test.builders.DateAttributeBuilder_1_1.*;
import static uk.gov.ida.saml.core.test.builders.DateAttributeValueBuilder.*;
import static uk.gov.ida.saml.core.test.builders.GenderAttributeBuilder_1_1.*;
import static uk.gov.ida.saml.core.test.builders.PersonNameAttributeBuilder_1_1.*;
import static uk.gov.ida.saml.core.test.builders.PersonNameAttributeValueBuilder.*;

@RunWith(OpenSAMLMockitoRunner.class)
public class MatchingDatasetUnmarshallerTest {

    private MatchingDatasetUnmarshaller unmarshaller;

    @Before
    public void setUp() {
        this.unmarshaller = new MatchingDatasetUnmarshaller(new AddressFactory());
    }

    @Test
    public void transform_shouldTransformAnAssertionIntoAMatchingDataset_1_1() throws Exception {
        Attribute firstname = aPersonName_1_1().addValue(aPersonNameValue().withValue("Bob").withFrom(DateTime.parse("2000-03-5")).withTo(DateTime.parse("2001-02-6")).withVerified(true).build()).buildAsFirstname();
        Attribute middlenames = aPersonName_1_1().addValue(aPersonNameValue().withValue("foo").withFrom(DateTime.parse("2000-03-5")).withTo(DateTime.parse("2001-02-6")).withVerified(true).build()).buildAsMiddlename();
        Attribute surname = aPersonName_1_1().addValue(aPersonNameValue().withValue("Bobbins").withFrom(DateTime.parse("2000-03-5")).withTo(DateTime.parse("2001-02-6")).withVerified(true).build()).buildAsSurname();
        Attribute gender = aGender_1_1().withValue("Male").withFrom(DateTime.parse("2000-03-5")).withTo(DateTime.parse("2001-02-6")).withVerified(true).build();
        Attribute dateOfBirth = aDate_1_1().addValue(aDateValue().withValue("1986-12-05").withFrom(DateTime.parse("2001-09-08")).withTo(DateTime.parse("2002-03-05")).withVerified(false).build()).buildAsDateOfBirth();
        Address address = anAddressAttributeValue().addLines(asList("address-line-1")).withFrom(DateTime.parse("2012-08-08")).withTo(DateTime.parse("2012-09-09")).build();
        Attribute currentAddress = anAddressAttribute().addAddress(address).buildCurrentAddress();

        Address previousAddress1 = anAddressAttributeValue().addLines(asList("address-line-2")).withFrom(DateTime.parse("2011-08-08")).withTo(DateTime.parse("2012-08-07")).build();
        Address previousAddress2 = anAddressAttributeValue().addLines(asList("address-line-3")).withFrom(DateTime.parse("2010-08-08")).withTo(DateTime.parse("2011-08-07")).build();
        Attribute previousAddresses = anAddressAttribute().addAddress(previousAddress1).addAddress(previousAddress2).buildPreviousAddress();

        Assertion originalAssertion = aMatchingDatasetAssertion(firstname, middlenames, surname, gender, dateOfBirth, currentAddress, previousAddresses);

        MatchingDataset matchingDataset = unmarshaller.fromAssertion(originalAssertion);

        final PersonName firstNameAttributeValue = (PersonName) firstname.getAttributeValues().get(0);
        final PersonName middleNameAttributeValue = (PersonName) middlenames.getAttributeValues().get(0);
        final PersonName surnameAttributeValue = (PersonName) surname.getAttributeValues().get(0);
        final Gender genderAttributeValue = (Gender) gender.getAttributeValues().get(0);
        final uk.gov.ida.saml.core.extensions.Date dateOfBirthAttributeValue = (uk.gov.ida.saml.core.extensions.Date) dateOfBirth.getAttributeValues().get(0);
        final Address currentAddressAttributeValue = (Address) currentAddress.getAttributeValues().get(0);
        assertThat(matchingDataset.getFirstNames().get(0).getValue()).isEqualTo(firstNameAttributeValue.getValue());
        assertThat(matchingDataset.getFirstNames().get(0).getFrom()).isEqualTo(firstNameAttributeValue.getFrom());
        assertThat(matchingDataset.getFirstNames().get(0).getTo()).isEqualTo(firstNameAttributeValue.getTo());
        assertThat(matchingDataset.getMiddleNames().get(0).getValue()).isEqualTo(middleNameAttributeValue.getValue());
        assertThat(matchingDataset.getMiddleNames().get(0).getFrom()).isEqualTo(middleNameAttributeValue.getFrom());
        assertThat(matchingDataset.getMiddleNames().get(0).getTo()).isEqualTo(middleNameAttributeValue.getTo());
        assertThat(matchingDataset.getSurnames().get(0).getValue()).isEqualTo(surnameAttributeValue.getValue());
        assertThat(matchingDataset.getSurnames().get(0).getFrom()).isEqualTo(surnameAttributeValue.getFrom());
        assertThat(matchingDataset.getSurnames().get(0).getTo()).isEqualTo(surnameAttributeValue.getTo());

        assertThat(matchingDataset.getGender().get().getValue().getValue()).isEqualTo(genderAttributeValue.getValue());
        assertThat(matchingDataset.getGender().get().getFrom()).isEqualTo(genderAttributeValue.getFrom());
        assertThat(matchingDataset.getGender().get().getTo()).isEqualTo(genderAttributeValue.getTo());

        assertThat(matchingDataset.getDateOfBirths().get(0).getValue()).isEqualTo(LocalDate.parse(dateOfBirthAttributeValue.getValue()));
        assertThat(matchingDataset.getDateOfBirths().get(0).getFrom()).isEqualTo(dateOfBirthAttributeValue.getFrom());
        assertThat(matchingDataset.getDateOfBirths().get(0).getTo()).isEqualTo(dateOfBirthAttributeValue.getTo());

        assertThat(matchingDataset.getAddresses().size()).isEqualTo(3);

        uk.gov.ida.saml.core.domain.Address transformedCurrentAddress = matchingDataset.getAddresses().get(0);
        assertThat(transformedCurrentAddress.getLines().get(0)).isEqualTo(currentAddressAttributeValue.getLines().get(0).getValue());
        assertThat(transformedCurrentAddress.getPostCode().get()).isEqualTo(currentAddressAttributeValue.getPostCode().getValue());
        assertThat(transformedCurrentAddress.getInternationalPostCode().get()).isEqualTo(currentAddressAttributeValue.getInternationalPostCode().getValue());
        assertThat(transformedCurrentAddress.getUPRN().get()).isEqualTo(currentAddressAttributeValue.getUPRN().getValue());
        assertThat(transformedCurrentAddress.getFrom()).isEqualTo(currentAddressAttributeValue.getFrom());
        assertThat(transformedCurrentAddress.getTo().get()).isEqualTo(currentAddressAttributeValue.getTo());

        uk.gov.ida.saml.core.domain.Address transformedPreviousAddress1 = matchingDataset.getAddresses().get(1);
        assertThat(transformedPreviousAddress1.getLines().get(0)).isEqualTo(previousAddress1.getLines().get(0).getValue());
        uk.gov.ida.saml.core.domain.Address transformedPreviousAddress2 = matchingDataset.getAddresses().get(2);
        assertThat(transformedPreviousAddress2.getLines().get(0)).isEqualTo(previousAddress2.getLines().get(0).getValue());

    }

    @Test
    public void transform_shoulHandleWhenMatchingDatasetIsPresentAndToDateIsMissingFromCurrentAddress() throws Exception {
        Attribute currentAddress = anAddressAttribute().addAddress(anAddressAttributeValue().withTo(null).build()).buildCurrentAddress();
        Assertion assertion = aMatchingDatasetAssertion(
                aPersonName_1_1().buildAsFirstname(),
                aPersonName_1_1().buildAsMiddlename(),
                aPersonName_1_1().buildAsSurname(),
                aGender_1_1().build(),
                aDate_1_1().buildAsDateOfBirth(),
                currentAddress,
                anAddressAttribute().addAddress(anAddressAttributeValue().build()).buildPreviousAddress());

        MatchingDataset matchingDataset = unmarshaller.fromAssertion(assertion);

        assertThat(matchingDataset).isNotNull();
    }

    @Test
    public void transform_shoulHandleWhenMatchingDatasetIsPresentAndToDateIsMissingFromPreviousAddress() throws Exception {
        Assertion assertion = aMatchingDatasetAssertion(
                aPersonName_1_1().buildAsFirstname(),
                aPersonName_1_1().buildAsMiddlename(),
                aPersonName_1_1().buildAsSurname(),
                aGender_1_1().build(),
                aDate_1_1().buildAsDateOfBirth(),
                anAddressAttribute().addAddress(anAddressAttributeValue().build()).buildCurrentAddress(),
                anAddressAttribute().addAddress(anAddressAttributeValue().withTo(null).build()).buildPreviousAddress());

        MatchingDataset matchingDataset = unmarshaller.fromAssertion(assertion);

        assertThat(matchingDataset).isNotNull();
    }

    @Test
    public void transform_shoulHandleWhenMatchingDatasetIsPresentAndToDateIsMissingFromFirstName() throws Exception {
        Attribute firstName = aPersonName_1_1().addValue(aPersonNameValue().withTo(null).build()).buildAsFirstname();
        Assertion assertion = aMatchingDatasetAssertion(
                firstName,
                aPersonName_1_1().buildAsMiddlename(),
                aPersonName_1_1().buildAsSurname(),
                aGender_1_1().build(),
                aDate_1_1().buildAsDateOfBirth(),
                anAddressAttribute().addAddress(anAddressAttributeValue().build()).buildCurrentAddress(),
                anAddressAttribute().addAddress(anAddressAttributeValue().build()).buildPreviousAddress());

        MatchingDataset matchingDataset = unmarshaller.fromAssertion(assertion);

        assertThat(matchingDataset).isNotNull();
    }

    @Test
    public void transform_shoulHandleWhenMatchingDatasetIsPresentAndToDateIsPresentInFirstName() throws Exception {
        Attribute firstName = aPersonName_1_1().addValue(aPersonNameValue().withTo(DateTime.parse("1066-01-05")).build()).buildAsFirstname();
        Assertion assertion = aMatchingDatasetAssertion(
                firstName,
                aPersonName_1_1().buildAsMiddlename(),
                aPersonName_1_1().buildAsSurname(),
                aGender_1_1().build(),
                aDate_1_1().buildAsDateOfBirth(),
                anAddressAttribute().addAddress(anAddressAttributeValue().build()).buildCurrentAddress(),
                anAddressAttribute().addAddress(anAddressAttributeValue().build()).buildPreviousAddress());

        MatchingDataset matchingDataset = unmarshaller.fromAssertion(assertion);

        assertThat(matchingDataset).isNotNull();
    }

    @Test
    public void transform_shouldMapMultipleFirstNames() throws Exception {
        Attribute firstName = aPersonName_1_1()
                .addValue(aPersonNameValue().withValue("name1").build())
                .addValue(aPersonNameValue().withValue("name2").build())
                .buildAsFirstname();

        AttributeStatement attributeStatementBuilder = anAttributeStatement().addAttribute(firstName).build();
        Assertion matchingDatasetAssertion = anAssertion()
                .addAttributeStatement(attributeStatementBuilder)
                .buildUnencrypted();

        MatchingDataset matchingDataset = unmarshaller.fromAssertion(matchingDatasetAssertion);

        assertThat(matchingDataset).isNotNull();
        assertThat(matchingDataset.getFirstNames().size()).isEqualTo(2);
    }
    @Test
    public void transform_shouldMapMultipleSurnames() throws Exception {
        Attribute surName = aPersonName_1_1()
                .addValue(aPersonNameValue().withValue("name1").build())
                .addValue(aPersonNameValue().withValue("name2").build())
                .buildAsSurname();

        AttributeStatement attributeStatementBuilder = anAttributeStatement().addAttribute(surName).build();
        Assertion matchingDatasetAssertion = anAssertion()
                .addAttributeStatement(attributeStatementBuilder)
                .buildUnencrypted();

        MatchingDataset matchingDataset = unmarshaller.fromAssertion(matchingDatasetAssertion);

        assertThat(matchingDataset).isNotNull();
        assertThat(matchingDataset.getSurnames().size()).isEqualTo(2);
    }

    @Test
    public void transform_shouldMapMultipleMiddleNames() throws Exception {
        Attribute middleName = aPersonName_1_1()
                .addValue(aPersonNameValue().withValue("name1").build())
                .addValue(aPersonNameValue().withValue("name2").build())
                .buildAsMiddlename();

        AttributeStatement attributeStatementBuilder = anAttributeStatement().addAttribute(middleName).build();
        Assertion matchingDatasetAssertion = anAssertion()
                .addAttributeStatement(attributeStatementBuilder)
                .buildUnencrypted();

        MatchingDataset matchingDataset = unmarshaller.fromAssertion(matchingDatasetAssertion);

        assertThat(matchingDataset).isNotNull();
        assertThat(matchingDataset.getMiddleNames().size()).isEqualTo(2);
    }

    @Test
    public void transform_shouldMapMultipleBirthdates() throws Exception {
        Attribute attribute = aDate_1_1().addValue(aDateValue().withValue("2012-12-12").build()).addValue(aDateValue().withValue("2011-12-12").build()).buildAsDateOfBirth();

        AttributeStatement attributeStatementBuilder = anAttributeStatement().addAttribute(attribute).build();
        Assertion matchingDatasetAssertion = anAssertion()
                .addAttributeStatement(attributeStatementBuilder)
                .buildUnencrypted();

        MatchingDataset matchingDataset = unmarshaller.fromAssertion(matchingDatasetAssertion);

        assertThat(matchingDataset).isNotNull();
        assertThat(matchingDataset.getDateOfBirths().size()).isEqualTo(2);
    }

    @Test
    public void transform_shouldMapMultipleCurrentAddresses() throws Exception {
        Attribute attribute = anAddressAttribute().addAddress(anAddressAttributeValue().build()).addAddress(anAddressAttributeValue().build()).buildCurrentAddress();

        AttributeStatement attributeStatementBuilder = anAttributeStatement().addAttribute(attribute).build();
        Assertion matchingDatasetAssertion = anAssertion()
                .addAttributeStatement(attributeStatementBuilder)
                .buildUnencrypted();

        MatchingDataset matchingDataset = unmarshaller.fromAssertion(matchingDatasetAssertion);

        assertThat(matchingDataset).isNotNull();
        assertThat(matchingDataset.getAddresses().size()).isEqualTo(2);
    }

    @Test
    public void transform_shouldMapMultiplePreviousAddresses() throws Exception {
        Attribute attribute = anAddressAttribute().addAddress(anAddressAttributeValue().build()).addAddress(anAddressAttributeValue().build()).buildPreviousAddress();

        AttributeStatement attributeStatementBuilder = anAttributeStatement().addAttribute(attribute).build();
        Assertion matchingDatasetAssertion = anAssertion()
                .addAttributeStatement(attributeStatementBuilder)
                .buildUnencrypted();

        MatchingDataset matchingDataset = unmarshaller.fromAssertion(matchingDatasetAssertion);

        assertThat(matchingDataset).isNotNull();
        assertThat(matchingDataset.getAddresses().size()).isEqualTo(2);
    }


}
