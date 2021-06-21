package uk.gov.ida.hub.samlengine.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeValue;
import org.opensaml.saml.saml2.core.impl.AttributeBuilder;
import uk.gov.ida.hub.samlengine.logging.data.AttributeStatementLogData;
import uk.gov.ida.hub.samlengine.logging.data.VerifiedAttributeLogData;
import uk.gov.ida.saml.core.IdaConstants;
import uk.gov.ida.saml.core.IdaSamlBootstrap;
import uk.gov.ida.saml.core.test.builders.AddressAttributeValueBuilder_1_1;
import uk.gov.ida.saml.core.test.builders.DateAttributeValueBuilder;
import uk.gov.ida.saml.core.test.builders.PersonNameAttributeValueBuilder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.samlengine.domain.LevelOfAssurance.LEVEL_2;
import static uk.gov.ida.hub.samlengine.logging.VerifiedAttributesLogger.formatAttributes;
import static uk.gov.ida.saml.core.test.builders.MatchingDatasetAttributeStatementBuilder_1_1.aMatchingDatasetAttributeStatement_1_1;

public class VerifiedAttributesLoggerTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setUp() {
        IdaSamlBootstrap.bootstrap();
    }

    @Test
    public void shouldLogIssuerAndLevelOfAssurance() throws Exception {
        List<Attribute> anyAttributesList = aMatchingDatasetAttributeStatement_1_1()
            .build()
            .getAttributes();

        AttributeStatementLogData actual = mapper.readValue(
            formatAttributes("some-issuer", LEVEL_2, anyAttributesList),
            AttributeStatementLogData.class
        );

        assertThat(actual.getIssuer()).isEqualTo("some-issuer");
        assertThat(actual.getLevelOfAssurance()).isEqualTo(LEVEL_2);
    }

    @Test
    public void shouldLogDateOfBirthHistory() throws Exception {
        Attribute dateOfBirthAttribute = new AttributeBuilder().buildObject();
        dateOfBirthAttribute.setName(IdaConstants.Attributes_1_1.DateOfBirth.NAME);

        AttributeValue oldDateOfBirthAttributeValue = new DateAttributeValueBuilder()
            .withTo(DateTime.now().minusDays(1))
            .withVerified(true)
            .build();
        AttributeValue currentDateOfBirthAttributeValue = new DateAttributeValueBuilder()
            .withTo(null)
            .build();

        dateOfBirthAttribute.getAttributeValues().add(oldDateOfBirthAttributeValue);
        dateOfBirthAttribute.getAttributeValues().add(currentDateOfBirthAttributeValue);

        List<Attribute> attributes = aMatchingDatasetAttributeStatement_1_1()
            .withDateOfBirth(dateOfBirthAttribute)
            .build()
            .getAttributes();

        AttributeStatementLogData actual = mapper.readValue(
            formatAttributes("any-issuer", LEVEL_2, attributes),
            AttributeStatementLogData.class
        );

        Map<String, List<VerifiedAttributeLogData>> attributesMap = actual.getAttributes();

        assertThat(attributesMap.get(IdaConstants.Attributes_1_1.DateOfBirth.NAME))
            .isEqualTo(List.of(
                new VerifiedAttributeLogData(true, "less than 180 days"),
                new VerifiedAttributeLogData(false, null)
            ));
    }

    @Test
    public void shouldLogFirstNameHistory() throws Exception {
        AttributeValue oldFirstNameAttributeValue = new PersonNameAttributeValueBuilder()
            .withTo(DateTime.now().minusDays(181))
            .withVerified(true)
            .build();
        AttributeValue currentFirstNameAttributeValue = new PersonNameAttributeValueBuilder()
            .withFrom(DateTime.now())
            .withTo(null)
            .build();

        Attribute firstNameAttribute = new AttributeBuilder().buildObject();
        firstNameAttribute.setName(IdaConstants.Attributes_1_1.Firstname.NAME);
        firstNameAttribute.getAttributeValues().add(oldFirstNameAttributeValue);
        firstNameAttribute.getAttributeValues().add(currentFirstNameAttributeValue);

        List<Attribute> attributes = aMatchingDatasetAttributeStatement_1_1()
            .withFirstname(firstNameAttribute)
            .build()
            .getAttributes();

        AttributeStatementLogData actual = mapper.readValue(
            formatAttributes("any-issuer", LEVEL_2, attributes),
            AttributeStatementLogData.class
        );

        Map<String, List<VerifiedAttributeLogData>> attributesMap = actual.getAttributes();

        assertThat(attributesMap.get(IdaConstants.Attributes_1_1.Firstname.NAME))
            .isEqualTo(List.of(
                new VerifiedAttributeLogData(true, "more than 180 days"),
                new VerifiedAttributeLogData(false, null)
            ));
    }

    @Test
    public void shouldLogMiddleNamesNameHistory() throws Exception {
        AttributeValue oldMiddleNamesAttributeValue = new PersonNameAttributeValueBuilder()
            .withFrom(DateTime.parse("2000-12-31"))
            .withTo(DateTime.now().minusDays(406))
            .withVerified(true)
            .build();
        AttributeValue currentMiddleNamesAttributeValue = new PersonNameAttributeValueBuilder()
            .withFrom(DateTime.now().minusDays(405))
            .withVerified(true)
            .build();

        Attribute middleNamesAttribute = new AttributeBuilder().buildObject();
        middleNamesAttribute.setName(IdaConstants.Attributes_1_1.Middlename.NAME);
        middleNamesAttribute.getAttributeValues().add(oldMiddleNamesAttributeValue);
        middleNamesAttribute.getAttributeValues().add(currentMiddleNamesAttributeValue);

        List<Attribute> attributes = aMatchingDatasetAttributeStatement_1_1()
            .withMiddleNames(middleNamesAttribute)
            .build()
            .getAttributes();

        AttributeStatementLogData actual = mapper.readValue(
            formatAttributes("any-issuer", LEVEL_2, attributes),
            AttributeStatementLogData.class
        );

        Map<String, List<VerifiedAttributeLogData>> attributesMap = actual.getAttributes();

        assertThat(attributesMap.get(IdaConstants.Attributes_1_1.Middlename.NAME))
            .isEqualTo(List.of(
                new VerifiedAttributeLogData(true, "more than 405 days"),
                new VerifiedAttributeLogData(true, null)
            ));
    }

    @Test
    public void shouldLogSurnameNameHistory() throws Exception {
        AttributeValue surnameAttributeValue = new PersonNameAttributeValueBuilder()
            .withFrom(DateTime.parse("2000-12-31"))
            .withVerified(true)
            .build();

        Attribute surnameAttribute = new AttributeBuilder().buildObject();
        surnameAttribute.setName(IdaConstants.Attributes_1_1.Surname.NAME);
        surnameAttribute.getAttributeValues().add(surnameAttributeValue);

        List<Attribute> attributes = aMatchingDatasetAttributeStatement_1_1()
            .withSurname(surnameAttribute)
            .build()
            .getAttributes();

        AttributeStatementLogData actual = mapper.readValue(
            formatAttributes("any-issuer", LEVEL_2, attributes),
            AttributeStatementLogData.class
        );

        Map<String, List<VerifiedAttributeLogData>> attributesMap = actual.getAttributes();

        assertThat(attributesMap.get(IdaConstants.Attributes_1_1.Surname.NAME))
            .isEqualTo(List.of(
                new VerifiedAttributeLogData(true, null)
            ));
    }

    @Test
    public void shouldLogCurrentAddressHistory() throws Exception {
        AttributeValue currentAddressAttributeValue = new AddressAttributeValueBuilder_1_1()
            .withFrom(DateTime.now().minusYears(1))
            .withVerified(true)
            .build();

        Attribute currentAddressAttribute = new AttributeBuilder().buildObject();
        currentAddressAttribute.setName(IdaConstants.Attributes_1_1.CurrentAddress.NAME);
        currentAddressAttribute.getAttributeValues().add(currentAddressAttributeValue);

        List<Attribute> attributes = aMatchingDatasetAttributeStatement_1_1()
            .withCurrentAddress(currentAddressAttribute)
            .build()
            .getAttributes();

        AttributeStatementLogData actual = mapper.readValue(
            formatAttributes("any-issuer", LEVEL_2, attributes),
            AttributeStatementLogData.class
        );

        Map<String, List<VerifiedAttributeLogData>> attributesMap = actual.getAttributes();

        assertThat(attributesMap.get(IdaConstants.Attributes_1_1.CurrentAddress.NAME))
            .isEqualTo(List.of(
                new VerifiedAttributeLogData(true, null)
            ));
    }

    @Test
    public void shouldLogPreviousAddressHistory() throws Exception {
        AttributeValue previousAddressAttributeValue = new AddressAttributeValueBuilder_1_1()
            .withFrom(DateTime.now().minusYears(10))
            .withTo(DateTime.now().minusYears(1))
            .withVerified(false)
            .build();

        Attribute previousAddressAttribute = new AttributeBuilder().buildObject();
        previousAddressAttribute.setName(IdaConstants.Attributes_1_1.PreviousAddress.NAME);
        previousAddressAttribute.getAttributeValues().add(previousAddressAttributeValue);

        List<Attribute> attributes = aMatchingDatasetAttributeStatement_1_1()
            .addPreviousAddress(previousAddressAttribute)
            .build()
            .getAttributes();

        AttributeStatementLogData actual = mapper.readValue(
            formatAttributes("any-issuer", LEVEL_2, attributes),
            AttributeStatementLogData.class
        );

        Map<String, List<VerifiedAttributeLogData>> attributesMap = actual.getAttributes();

        assertThat(attributesMap.get(IdaConstants.Attributes_1_1.PreviousAddress.NAME))
            .isEqualTo(List.of(
                new VerifiedAttributeLogData(false, "more than 180 days")
            ));
    }
}