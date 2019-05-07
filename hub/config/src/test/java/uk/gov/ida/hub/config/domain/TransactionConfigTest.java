package uk.gov.ida.hub.config.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.hamcrest.core.StringContains;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.validation.ConstraintViolation;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.config.domain.builders.AssertionConsumerServiceBuilder.anAssertionConsumerService;
import static uk.gov.ida.hub.config.domain.builders.TransactionConfigBuilder.aTransactionConfigData;
import static uk.gov.ida.hub.shared.ValidationTestHelper.runValidations;

public class TransactionConfigTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void getAssertionConsumerServiceUri_shouldReturnDefaultUriWhenNoIndexIsSpecified() throws Exception {
        URI uri = URI.create("/some-uri");
        TransactionConfig systemUnderTests = aTransactionConfigData()
                .addAssertionConsumerService(anAssertionConsumerService().isDefault(false).build())
                .addAssertionConsumerService(anAssertionConsumerService().isDefault(true).withUri(uri).build())
                .build();

        final Optional<URI> returnedUri = systemUnderTests.getAssertionConsumerServiceUri(Optional.empty());

        assertThat(returnedUri.isPresent()).isEqualTo(true);
        assertThat(returnedUri.get()).isEqualTo(uri);
    }

    @Test
    public void getAssertionConsumerServiceUri_shouldReturnCorrectUriWhenIndexIsSpecified() throws Exception {
        URI uri = URI.create("/expected-uri");
        int index = 1;
        TransactionConfig systemUnderTests = aTransactionConfigData()
                .addAssertionConsumerService(anAssertionConsumerService().withIndex(0).build())
                .addAssertionConsumerService(anAssertionConsumerService().withIndex(index).withUri(uri).build())
                .build();

        final Optional<URI> returnedUri = systemUnderTests.getAssertionConsumerServiceUri(Optional.of(index));

        assertThat(returnedUri.isPresent()).isEqualTo(true);
        assertThat(returnedUri.get()).isEqualTo(uri);
    }

    @Test
    public void getAssertionConsumerServiceUri_shouldReturnAbsentWhenInvalidIndexIsSpecified() throws Exception {
        TransactionConfig systemUnderTests = aTransactionConfigData()
                .addAssertionConsumerService(anAssertionConsumerService().withIndex(0).build())
                .build();

        final Optional<URI> returnedUri = systemUnderTests.getAssertionConsumerServiceUri(Optional.of(1));

        assertThat(returnedUri.isPresent()).isEqualTo(false);
    }

    @Test
    public void isAssertionConsumerServiceIndicesUnique_shouldReturnViolationWhenIndicesAreDuplicated() throws Exception {
        TransactionConfig transactionConfigData = aTransactionConfigData()
                .addAssertionConsumerService(anAssertionConsumerService().isDefault(false).withIndex(1).build())
                .addAssertionConsumerService(anAssertionConsumerService().withIndex(1).build())
                .build();

        final Set<ConstraintViolation<TransactionConfig>> constraintViolations = runValidations(transactionConfigData);
        assertThat(constraintViolations.size()).isEqualTo(1);
        assertThat(constraintViolations.iterator().next().getMessage()).isEqualTo("Assertion Consumer Service indices must be unique.");
    }

    @Test
    public void isAssertionConsumerServiceIndicesUnique_shouldReturnNoViolationsWhenIndicesAreUnique() throws Exception {
        TransactionConfig transactionConfigData = aTransactionConfigData()
                .addAssertionConsumerService(anAssertionConsumerService().isDefault(false).withIndex(1).build())
                .addAssertionConsumerService(anAssertionConsumerService().withIndex(2).build())
                .build();

        final Set<ConstraintViolation<TransactionConfig>> constraintViolations = runValidations(transactionConfigData);
        assertThat(constraintViolations.size()).isEqualTo(0);
    }

    @Test
    public void isOnlyOneDefaultAssertionConsumerServiceIndex_shouldReturnNoViolationsWhenOneACSIsDefault() throws Exception {
        TransactionConfig transactionConfigData = aTransactionConfigData()
                .addAssertionConsumerService(anAssertionConsumerService().isDefault(false).build())
                .addAssertionConsumerService(anAssertionConsumerService().isDefault(true).build())
                .build();

        final Set<ConstraintViolation<TransactionConfig>> constraintViolations = runValidations(transactionConfigData);
        assertThat(constraintViolations.size()).isEqualTo(0);
    }

    @Test
    public void isOnlyOneDefaultAssertionConsumerServiceIndex_shouldReturnViolationWhenNoACSsAreDefault() throws Exception {
        TransactionConfig transactionConfigData = aTransactionConfigData()
                .addAssertionConsumerService(anAssertionConsumerService().isDefault(false).build())
                .addAssertionConsumerService(anAssertionConsumerService().isDefault(false).build())
                .build();

        final Set<ConstraintViolation<TransactionConfig>> constraintViolations = runValidations(transactionConfigData);
        assertThat(constraintViolations.size()).isEqualTo(1);
        assertThat(constraintViolations.iterator().next().getMessage()).isEqualTo("Exactly one Assertion Consumer Service must be marked as default.");
    }

    @Test
    public void isOnlyOneDefaultAssertionConsumerServiceIndex_shouldReturnViolationWhenMoreThanOneACSIsDefault() throws Exception {
        TransactionConfig transactionConfigData = aTransactionConfigData()
                .addAssertionConsumerService(anAssertionConsumerService().isDefault(true).build())
                .addAssertionConsumerService(anAssertionConsumerService().isDefault(true).build())
                .build();

        final Set<ConstraintViolation<TransactionConfig>> constraintViolations = runValidations(transactionConfigData);
        assertThat(constraintViolations.size()).isEqualTo(1);
        assertThat(constraintViolations.iterator().next().getMessage()).isEqualTo("Exactly one Assertion Consumer Service must be marked as default.");
    }

    @Test
    public void shouldThrowExceptionWhenLoadingConfigFileWithInvalidUserAccountAttributes() throws IOException {
        String badAttribute = "[ \"FIRST_NAME\", \"FIRST_NAME_VERIFIED\", \"INVALID_ATTRIBUTE\" ]";
        InputStream badAttributeStream = new ByteArrayInputStream(badAttribute.getBytes());

        ObjectMapper mapper = new ObjectMapper();

        exception.expect(InvalidFormatException.class);
        StringContains matcher = new StringContains("\"INVALID_ATTRIBUTE\": value not one of declared Enum instance names: [MIDDLE_NAME_VERIFIED, MIDDLE_NAME, DATE_OF_BIRTH, CURRENT_ADDRESS_VERIFIED, FIRST_NAME, SURNAME, SURNAME_VERIFIED, FIRST_NAME_VERIFIED, CURRENT_ADDRESS, DATE_OF_BIRTH_VERIFIED, ADDRESS_HISTORY, CYCLE_3]");
        exception.expectMessage(matcher);

        mapper.readValue(badAttributeStream, mapper.getTypeFactory().constructCollectionType(List.class, UserAccountCreationAttribute.class));
    }
}
