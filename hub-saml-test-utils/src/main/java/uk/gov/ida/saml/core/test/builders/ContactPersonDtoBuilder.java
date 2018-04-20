package uk.gov.ida.saml.core.test.builders;


import uk.gov.ida.saml.metadata.domain.ContactPersonDto;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class ContactPersonDtoBuilder {

    private static final String DEFAULT_TELEPHONE_NUMBER = "0113 496 0000";
    private static final URI DEFAULT_EMAIL_ADDRESS = URI.create("mailto:default@example.com");

    private String companyName = "default-company-name";
    private String givenName = "default-given-name";
    private String surName = "default-sur-name";
    private List<String> telephoneNumbers = new ArrayList<>();
    private List<URI> emailAddresses = new ArrayList<>();

    private boolean addDefaultTelephoneNumber = true;
    private boolean addDefaultEmailAddress = true;

    public static ContactPersonDtoBuilder aContactPersonDto() {
        return new ContactPersonDtoBuilder();
    }

    public ContactPersonDto build() {
        if (addDefaultTelephoneNumber) {
            telephoneNumbers.add(DEFAULT_TELEPHONE_NUMBER);
        }
        if (addDefaultEmailAddress) {
            emailAddresses.add(DEFAULT_EMAIL_ADDRESS);
        }
        return new ContactPersonDto(
                companyName,
                givenName,
                surName,
                telephoneNumbers,
                emailAddresses);
    }

    public ContactPersonDtoBuilder withCompanyName(String companyName) {
        this.companyName = companyName;
        return this;
    }

    public ContactPersonDtoBuilder withGivenName(String givenName) {
        this.givenName = givenName;
        return this;
    }

    public ContactPersonDtoBuilder withSurName(String surName) {
        this.surName = surName;
        return this;
    }

    public ContactPersonDtoBuilder addTelephoneNumber(String number) {
        telephoneNumbers.add(number);
        addDefaultTelephoneNumber = false;
        return this;
    }

    public ContactPersonDtoBuilder withoutDefaultTelephoneNumber() {
        addDefaultTelephoneNumber = false;
        return this;
    }

    public ContactPersonDtoBuilder addEmailAddress(URI emailAddress) {
        emailAddresses.add(emailAddress);
        addDefaultEmailAddress = false;
        return this;
    }

    public ContactPersonDtoBuilder withoutDefaultEmailAddress() {
        addDefaultEmailAddress = false;
        return this;
    }
}
