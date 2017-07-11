package uk.gov.ida.saml.metadata.transformers;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.opensaml.saml.saml2.metadata.ContactPerson;
import org.opensaml.saml.saml2.metadata.EmailAddress;
import org.opensaml.saml.saml2.metadata.TelephoneNumber;
import uk.gov.ida.saml.metadata.domain.ContactPersonDto;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class ContactPersonsMarshaller {

    private ContactPersonDto toDto(ContactPerson contactPerson) {
        List<URI> emailAddresses = new ArrayList<>();
        for (EmailAddress address : contactPerson.getEmailAddresses()) {
            emailAddresses.add(URI.create(address.getAddress()));
        }
        List<String> telephoneNumbers = new ArrayList<>();
        for (TelephoneNumber number : contactPerson.getTelephoneNumbers()) {
            telephoneNumbers.add(number.getNumber());
        }
        return new ContactPersonDto(
                contactPerson.getCompany().getName(),
                contactPerson.getGivenName().getName(),
                contactPerson.getSurName().getName(),
                telephoneNumbers,
                emailAddresses
        );
    }

    public List<ContactPersonDto> toDto(List<ContactPerson> contactPersons) {
        return Lists.transform(contactPersons, new Function<ContactPerson, ContactPersonDto>() {
            @Override
            public ContactPersonDto apply(ContactPerson input) {
                return toDto(input);
            }
        });
    }
}
