package uk.gov.ida.saml.metadata.domain;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class ContactPersonDto {
    private String givenName;
    private String surName;
    private List<String> telephoneNumbers = new ArrayList<>();
    private List<URI> emailAddresses = new ArrayList<>();
    private String companyName;

    @SuppressWarnings("unused")//Needed by JAXB
    private ContactPersonDto() {}

    public ContactPersonDto(
            String companyName,
            String givenName,
            String surName,
            List<String> telephoneNumbers,
            List<URI> emailAddresses) {
        this.companyName = companyName;
        this.givenName = givenName;
        this.surName = surName;
        this.telephoneNumbers = telephoneNumbers;
        this.emailAddresses = emailAddresses;
    }

    public String getGivenName() {
        return givenName;
    }

    public String getSurName() {
        return surName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public List<URI> getEmailAddresses() {
        return emailAddresses;
    }

    public List<String> getTelephoneNumbers() {
        return telephoneNumbers;
    }
}
