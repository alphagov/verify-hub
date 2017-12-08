package uk.gov.ida.hub.samlproxy.controllogic;

public enum SamlMessageType {
    SAML_REQUEST  ("SAMLRequest"),
    SAML_RESPONSE ("SAMLResponse") ;

    private SamlMessageType(String formName) {
        this.formName = formName;
    }

    private String formName;
    public String toString() {
        return formName;
    }
}
