package uk.gov.ida.integrationtest.hub.samlproxy.support;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import uk.gov.ida.hub.samlproxy.Urls;
import uk.gov.ida.shared.utils.string.StringEncoding;

import java.util.UUID;

import static java.text.MessageFormat.format;

/*
* Generates SamlRequest Query parameter in the form
* SAMLRequest=<base64 + url encoded request>
*
* Note: Refactor when this class becomes a bit of a mess
*/
public class TestSamlRequestFactory {

    public static final String VALID_RELAYSTATE = "valid-relaystate";

    public static String createUnsignedRequest(String id, String issuer) {
        return format("{0}={1}&{2}={3}", Urls.SharedUrls.SAML_REQUEST_PARAM, encodeRequest(getSimpleAuthnRequestTemplate(id, DateTime.now(), issuer)), Urls.SharedUrls.RELAY_STATE_PARAM, VALID_RELAYSTATE);
    }

    public static String createNonBase64Request() {
        return format("{0}={1}&{2}={3}", Urls.SharedUrls.SAML_REQUEST_PARAM, StringEncoding.urlEncode(getSimpleAuthnRequestBody()), Urls.SharedUrls.RELAY_STATE_PARAM, VALID_RELAYSTATE);
    }

    private static String getSimpleAuthnRequestTemplate(String id, DateTime date, String issuer) {
        DateTimeFormatter formatter = ISODateTimeFormat.dateHourMinuteSecond();
        return format(simpleAuthnRequestTemplate, id, "2.0", formatter.print(date), issuer);
    }

    private static String encodeRequest(String request) {
        return StringEncoding.urlEncode(StringEncoding.toBase64Encoded(request));
    }

    public static String getSimpleAuthnRequestBody() {
        return getSimpleAuthnRequestTemplate(UUID.randomUUID().toString(), DateTime.now(), "https://sp.example.com/SAML2");
    }

    private static String simpleAuthnRequestTemplate = "<samlp:AuthnRequest\n" +
            "        xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\"\n" +
            "        xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"\n" +
            "        ID=\"{0}\"\n" +
            "        Version=\"{1}\"\n" +
            "        IssueInstant=\"{2}\"\n" +
            "        AssertionConsumerServiceIndex=\"0\"\n" +
            "        AttributeConsumingServiceIndex=\"0\">\n" +
            "  <saml:Issuer>{3}</saml:Issuer>\n" +
            "</samlp:AuthnRequest>";
}
