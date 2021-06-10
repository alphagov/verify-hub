package uk.gov.ida.integrationtest.hub.samlproxy.apprule;

import httpstub.HttpStubExtension;
import io.dropwizard.testing.ResourceHelpers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import ru.vyarus.dropwizard.guice.test.ClientSupport;
import ru.vyarus.dropwizard.guice.test.jupiter.ext.TestDropwizardAppExtension;
import uk.gov.ida.hub.samlproxy.SamlProxyApplication;
import uk.gov.ida.hub.samlproxy.Urls;
import uk.gov.ida.hub.samlproxy.contracts.SamlRequestDto;
import uk.gov.ida.integrationtest.hub.samlproxy.apprule.support.SamlProxyAppExtension;
import uk.gov.ida.shared.utils.string.StringEncoding;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class DenialOfServiceAttacksIntegrationTests {

    private static ClientSupport client;

    @Order(0)
    @RegisterExtension
    public static HttpStubExtension eventSinkStub = new HttpStubExtension();

    @Order(1)
    @RegisterExtension
    public static TestDropwizardAppExtension samlProxyApp = SamlProxyAppExtension.forApp(SamlProxyApplication.class)
            .withDefaultConfigOverridesAnd()
            .configOverride("eventSinkUri", () -> eventSinkStub.baseUri().build().toASCIIString())
            .config(ResourceHelpers.resourceFilePath("saml-proxy.yml"))
            .randomPorts()
            .create();

    @BeforeAll
    public static void beforeClass(ClientSupport clientSupport) {
        client = clientSupport;
        eventSinkStub.register(Urls.HubSupportUrls.HUB_SUPPORT_EVENT_SINK_RESOURCE, Response.Status.OK.getStatusCode());
    }

    @AfterAll
    public static void tearDown() {
        SamlProxyAppExtension.tearDown();
    }

    @Test
    public void requestPost_shouldRedirectToGenericErrorWhenEntityExpansionAttackOccurs() throws Exception {
        String xmlString = "<?xml version=\"1.0\"?>\n" +
                "<!DOCTYPE lolz [\n" +
                " <!ENTITY lol \"lol\">\n" +
                " <!ELEMENT lolz (#PCDATA)>\n" +
                " <!ENTITY lol1 \"&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;\">\n" +
                " <!ENTITY lol2 \"&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;\">\n" +
                " <!ENTITY lol3 \"&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;\">\n" +
                " <!ENTITY lol4 \"&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;\">\n" +
                " <!ENTITY lol5 \"&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;\">\n" +
                " <!ENTITY lol6 \"&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;\">\n" +
                " <!ENTITY lol7 \"&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;\">\n" +
                " <!ENTITY lol8 \"&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;\">\n" +
                " <!ENTITY lol9 \"&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;\">\n" +
                "]>\n" +
                "<lolz>&lol9;</lolz>" +
                "          ".repeat(80);
        String samlAuthnRequest = StringEncoding.toBase64Encoded(xmlString);
        String relayState = "aRelayState";
        String analyticsSessionId = UUID.randomUUID().toString();
        String journeyType = "some-journey-type";

        Response response = client.targetMain(Urls.SamlProxyUrls.SAML2_SSO_RECEIVER_API_ROOT)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(new SamlRequestDto(samlAuthnRequest, relayState, "12.23.34.45", analyticsSessionId, journeyType)));

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

}
