package uk.gov.ida.hub.samlproxy.domain;

import org.w3c.dom.Document;
import uk.gov.ida.shared.utils.xml.XmlUtils;

public class SamlDto {
    private String saml;

    @SuppressWarnings("unused") // used by jackson serializer
    public SamlDto() {}

    public SamlDto(String saml) {
        this.saml = saml;
    }

    public SamlDto(Document document) {
        this(XmlUtils.writeToString(document));
    }

    public String getSaml() {
        return saml;
    }
}
