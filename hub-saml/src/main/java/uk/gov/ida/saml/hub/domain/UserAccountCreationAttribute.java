package uk.gov.ida.saml.hub.domain;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import java.io.Serializable;

public enum UserAccountCreationAttribute implements Serializable {
    FIRST_NAME("firstname"),
    FIRST_NAME_VERIFIED("firstname_verified"),
    MIDDLE_NAME("middlename"),
    MIDDLE_NAME_VERIFIED("middlename_verified"),
    SURNAME("surname"),
    SURNAME_VERIFIED("surname_verified"),
    DATE_OF_BIRTH("dateofbirth"),
    DATE_OF_BIRTH_VERIFIED("dateofbirth_verified"),
    CURRENT_ADDRESS("currentaddress"),
    CURRENT_ADDRESS_VERIFIED("currentaddress_verified"),
    ADDRESS_HISTORY("addresshistory"),
    CYCLE_3("cycle_3");

    private String attributeName;

    private UserAccountCreationAttribute(final String attributeName) {
        this.attributeName = attributeName;
    }

    public String getAttributeName(){
        return attributeName;
    }

    public static UserAccountCreationAttribute getUserAccountCreationAttribute(final String name){
        return Iterables.find(ImmutableList.copyOf(values()), new Predicate<UserAccountCreationAttribute>() {
            @Override
            public boolean apply(final UserAccountCreationAttribute input) {
                return input.getAttributeName().equals(name);
            }
        });
    }
}

