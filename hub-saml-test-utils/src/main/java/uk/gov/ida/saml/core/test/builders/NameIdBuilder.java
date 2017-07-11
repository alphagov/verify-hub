package uk.gov.ida.saml.core.test.builders;

import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.NameIDType;

import com.google.common.base.Optional;

import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;

public class NameIdBuilder {

    private OpenSamlXmlObjectFactory openSamlXmlObjectFactory = new OpenSamlXmlObjectFactory();
    private String value;
    private Optional<String> format = Optional.fromNullable(NameIDType.PERSISTENT);
    private Optional<String> nameQualifier = Optional.absent();
    private Optional<String> spNameQualifier = Optional.absent();

    public static NameIdBuilder aNameId() {
        return new NameIdBuilder();
    }

    public NameID build() {
        NameID nameId = openSamlXmlObjectFactory.createNameId(value);
        nameId.setFormat(null);

        if (format.isPresent()) {
            nameId.setFormat(format.get());
        }

        if (nameQualifier.isPresent()) {
            nameId.setNameQualifier(nameQualifier.get());
        }

        if (spNameQualifier.isPresent()) {
            nameId.setSPNameQualifier(spNameQualifier.get());
        }

        return nameId;
    }

    public NameIdBuilder withValue(String value) {
        this.value = value;
        return this;
    }

    public NameIdBuilder withFormat(String format) {
        this.format = Optional.fromNullable(format);
        return this;
    }

    public NameIdBuilder withNameQualifier(String nameQualifier) {
        this.nameQualifier = Optional.fromNullable(nameQualifier);
        return this;
    }

    public NameIdBuilder withSpNameQualifier(String spNameQualifier) {
        this.spNameQualifier = Optional.fromNullable(spNameQualifier);
        return this;
    }
}
