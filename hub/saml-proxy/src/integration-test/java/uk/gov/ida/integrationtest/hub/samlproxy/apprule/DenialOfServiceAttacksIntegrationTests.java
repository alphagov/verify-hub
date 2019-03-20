package uk.gov.ida.integrationtest.hub.samlproxy.apprule;

import com.fasterxml.jackson.core.JsonProcessingException;
import helpers.JerseyClientConfigurationBuilder;
import httpstub.HttpStubRule;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.util.Duration;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.ida.hub.samlproxy.Urls;
import uk.gov.ida.hub.samlproxy.contracts.SamlRequestDto;
import uk.gov.ida.integrationtest.hub.samlproxy.apprule.support.SamlProxyAppRule;
import uk.gov.ida.shared.utils.string.StringEncoding;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

public class DenialOfServiceAttacksIntegrationTests {

    private static Client client;

    @ClassRule
    public static HttpStubRule eventSinkStubRule = new HttpStubRule();

    @ClassRule
    public static SamlProxyAppRule samlProxyAppRule = new SamlProxyAppRule(
            ConfigOverride.config("eventSinkUri", eventSinkStubRule.baseUri().build().toASCIIString())
            );

    @BeforeClass
    public static void setUpClass() throws Exception {
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder.aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        client = new JerseyClientBuilder(samlProxyAppRule.getEnvironment()).using(jerseyClientConfiguration).build(SamlMessageReceiverApiResourceTest.class.getSimpleName());
    }

    @Before
    public void setUp() throws JsonProcessingException {
        eventSinkStubRule.register(Urls.HubSupportUrls.HUB_SUPPORT_EVENT_SINK_RESOURCE, Response.Status.OK.getStatusCode());
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
            "<lolz>&lol9;</lolz>";
        for (int i = 0; i < 80; i++) {
            xmlString += "          ";
        }
        String samlAuthnRequest = StringEncoding.toBase64Encoded(xmlString);
        String relayState = "aRelayState";

        final URI ssoUri = samlProxyAppRule.getUri(Urls.SamlProxyUrls.SAML2_SSO_RECEIVER_API_ROOT);
        Response response = client.target(ssoUri)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(new SamlRequestDto(samlAuthnRequest, relayState, "12.23.34.45")));

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

}
