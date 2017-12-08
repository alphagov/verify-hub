package uk.gov.ida.integrationtest.hub.samlproxy;

import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.util.Duration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.w3c.dom.Element;
import uk.gov.ida.common.shared.security.Certificate;
import uk.gov.ida.hub.samlproxy.domain.SamlDto;
import uk.gov.ida.integrationtest.hub.samlproxy.apprule.support.SamlProxyAppRule;
import uk.gov.ida.saml.core.api.CoreTransformersFactory;
import uk.gov.ida.saml.deserializers.ElementToOpenSamlXMLObjectTransformer;
import uk.gov.ida.saml.hub.api.HubTransformersFactory;
import uk.gov.ida.saml.metadata.domain.HubIdentityProviderMetadataDto;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;
import uk.gov.ida.shared.utils.xml.XmlUtils;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.UriBuilder;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.saml.core.test.TestEntityIds.HUB_ENTITY_ID;
import static uk.gov.ida.saml.core.test.TestEntityIds.STUB_IDP_FOUR;
import static uk.gov.ida.saml.core.test.TestEntityIds.STUB_IDP_ONE;
import static uk.gov.ida.saml.core.test.TestEntityIds.STUB_IDP_THREE;
import static uk.gov.ida.saml.core.test.TestEntityIds.STUB_IDP_TWO;

public class HubMetadataIntegrationTests {

    @ClassRule
    public static final SamlProxyAppRule samlProxyAppRule = new SamlProxyAppRule();

    public static Client client;

    @BeforeClass
    public static void setUp() throws Exception {
        JerseyClientConfiguration jerseyClientConfiguration = new JerseyClientConfiguration();
        jerseyClientConfiguration.setConnectionTimeout(Duration.seconds(10));
        jerseyClientConfiguration.setTimeout(Duration.seconds(10));
        client = new JerseyClientBuilder(samlProxyAppRule.getEnvironment())
                .using(jerseyClientConfiguration)
                .build(HubMetadataIntegrationTests.class.getName());
        DateTimeFreezer.freezeTime();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        DateTimeFreezer.unfreezeTime();
    }

    @Test
    public void getIdpMetadataFromApi_shouldWork() throws Exception {
        final DateTime time = DateTime.now(DateTimeZone.UTC).plusHours(1);

        SamlDto samlDto = client.target(UriBuilder.fromUri(samlProxyAppRule.getUri("/API/metadata/idp"))).request().get(SamlDto.class);
        Function<Element, HubIdentityProviderMetadataDto> transformer = new HubTransformersFactory().getMetadataForSpTransformer(HUB_ENTITY_ID);
        final Element element = XmlUtils.convertToElement(samlDto.getSaml());
        HubIdentityProviderMetadataDto identityProviderMetadataDto = transformer.apply(element);
        thenAssertIdpMetadataDto(time, identityProviderMetadataDto);
    }

    @Test
    public void getSpMetadataFromApi_shouldReturnTheHubFromNewMetadataAsAnSp() throws Exception {
        SamlDto samlDto = client.target(UriBuilder.fromUri(samlProxyAppRule.getUri("/API/metadata/sp"))).request().get(SamlDto.class);
        final Element element = XmlUtils.convertToElement(samlDto.getSaml());
        ElementToOpenSamlXMLObjectTransformer<EntityDescriptor> elementToOpenSamlXmlObjectTransformer = new CoreTransformersFactory().<EntityDescriptor>getElementToOpenSamlXmlObjectTransformer();
        EntityDescriptor entityDescriptor = elementToOpenSamlXmlObjectTransformer.apply(element);
        assertThat(entityDescriptor.getEntityID()).isEqualTo(HUB_ENTITY_ID);
        assertThat(entityDescriptor.getSPSSODescriptor(SAMLConstants.SAML20P_NS)).isNotNull();
        assertThat(entityDescriptor.getSPSSODescriptor(SAMLConstants.SAML20P_NS).getAssertionConsumerServices().get(0).getLocation()).isEqualTo("http://foo.com/bar");
        assertThat(entityDescriptor.getValidUntil()).isEqualTo(DateTime.now(DateTimeZone.UTC).plusHours(1));
    }

    private void thenAssertIdpMetadataDto(
            DateTime validUntil,
            HubIdentityProviderMetadataDto metadata
    ) {
        assertThat(metadata.getEntityId()).isEqualTo(HUB_ENTITY_ID);

        assertThat(metadata.getSigningCertificates()).hasSize(2);
        assertThat(metadata.getSigningCertificates().get(0).getIssuerId()).isEqualTo(HUB_ENTITY_ID);
        assertThat(metadata.getSigningCertificates().get(1).getIssuerId()).isEqualTo(HUB_ENTITY_ID);

        List<String> idpIssuerIds = metadata.getIdpSigningCertificates().stream()
                .map(Certificate::getIssuerId)
                .collect(Collectors.toList());

        assertThat(idpIssuerIds).containsOnly(STUB_IDP_ONE, STUB_IDP_TWO, STUB_IDP_THREE, STUB_IDP_FOUR);

        assertThat(metadata.getValidUntil()).isEqualTo(validUntil);
    }

}
