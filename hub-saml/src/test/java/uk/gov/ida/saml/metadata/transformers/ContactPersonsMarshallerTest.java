package uk.gov.ida.saml.metadata.transformers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensaml.saml.saml2.metadata.ContactPerson;
import org.opensaml.saml.saml2.metadata.TelephoneNumber;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.saml.core.test.builders.metadata.EmailAddressBuilder;
import uk.gov.ida.saml.metadata.domain.ContactPersonDto;

import java.net.URI;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.saml.core.test.builders.metadata.ContactPersonBuilder.aContactPerson;
import static uk.gov.ida.saml.core.test.builders.metadata.TelephoneNumberBuilder.aTelephoneNumber;

@RunWith(OpenSAMLMockitoRunner.class)
public class ContactPersonsMarshallerTest {

    private ContactPersonsMarshaller marshaller;

    @Before
    public void setup() {
        marshaller = new ContactPersonsMarshaller();
    }

    @Test
    public void transformContactPerson_shouldTransformContactPersonNames() throws Exception {
        final ContactPerson contactPerson = aContactPerson().build();
        final ContactPersonDto result = marshaller.toDto(asList(contactPerson)).get(0);

        assertThat(result.getCompanyName()).isEqualTo(contactPerson.getCompany().getName());
        assertThat(result.getGivenName()).isEqualTo(contactPerson.getGivenName().getName());
        assertThat(result.getSurName()).isEqualTo(contactPerson.getSurName().getName());
    }

    @Test
    public void transformContactPerson_shouldTransformContactPersonEmailAddresses() throws Exception {
        final URI addressOne = URI.create("mail:foo@bar.com");
        final URI addressTwo = URI.create("mail:baz@bar.com");
        final ContactPerson contactPerson = aContactPerson()
                .addEmailAddress(EmailAddressBuilder.anEmailAddress().withAddress(addressOne).build())
                .addEmailAddress(EmailAddressBuilder.anEmailAddress().withAddress(addressTwo).build())
                .build();
        final ContactPersonDto result = marshaller.toDto(asList(contactPerson)).get(0);

        assertThat(result.getEmailAddresses().size()).isEqualTo(2);
        assertThat(result.getEmailAddresses().get(0)).isEqualTo(addressOne);
        assertThat(result.getEmailAddresses().get(1)).isEqualTo(addressTwo);
    }

    @Test
    public void transformContactPerson_shouldTransformContactPersonPhoneNumbers() throws Exception {
        final TelephoneNumber numberOne = aTelephoneNumber().withNumber("01632 960000").build();
        final TelephoneNumber numberTwo = aTelephoneNumber().withNumber("01632 960999").build();
        final ContactPerson contactPerson = aContactPerson()
                .addTelephoneNumber(numberOne)
                .addTelephoneNumber(numberTwo)
                .build();
        final ContactPersonDto result = marshaller.toDto(asList(contactPerson)).get(0);

        assertThat(result.getTelephoneNumbers().size()).isEqualTo(2);
        assertThat(result.getTelephoneNumbers().get(0)).isEqualTo(numberOne.getNumber());
        assertThat(result.getTelephoneNumbers().get(1)).isEqualTo(numberTwo.getNumber());
    }
}
