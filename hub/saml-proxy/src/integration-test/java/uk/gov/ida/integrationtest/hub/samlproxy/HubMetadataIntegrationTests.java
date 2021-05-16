package uk.gov.ida.integrationtest.hub.samlproxy;

import io.dropwizard.testing.ResourceHelpers;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import ru.vyarus.dropwizard.guice.test.ClientSupport;
import ru.vyarus.dropwizard.guice.test.jupiter.ext.TestDropwizardAppExtension;
import uk.gov.ida.hub.samlproxy.SamlProxyApplication;
import uk.gov.ida.hub.samlproxy.domain.SamlDto;
import uk.gov.ida.integrationtest.hub.samlproxy.apprule.support.SamlProxyAppExtension;
import uk.gov.ida.saml.core.test.TestCertificateStrings;
import uk.gov.ida.saml.deserializers.OpenSamlXMLObjectUnmarshaller;
import uk.gov.ida.saml.deserializers.parser.SamlObjectParser;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.saml.core.test.TestEntityIds.HUB_ENTITY_ID;
import static uk.gov.ida.saml.core.test.TestEntityIds.HUB_SECONDARY_ENTITY_ID;
import static uk.gov.ida.saml.core.test.TestEntityIds.STUB_IDP_FOUR;
import static uk.gov.ida.saml.core.test.TestEntityIds.STUB_IDP_ONE;
import static uk.gov.ida.saml.core.test.TestEntityIds.STUB_IDP_THREE;
import static uk.gov.ida.saml.core.test.TestEntityIds.STUB_IDP_TWO;

public class HubMetadataIntegrationTests {

    public static ClientSupport client;

    @RegisterExtension
    public static TestDropwizardAppExtension samlProxyApp = SamlProxyAppExtension.forApp(SamlProxyApplication.class)
            .withDefaultConfigOverridesAnd()
            .config(ResourceHelpers.resourceFilePath("saml-proxy.yml"))
            .randomPorts()
            .create();

    private final SamlObjectParser samlObjectParser = new SamlObjectParser();

    @BeforeAll
    public static void beforeClass(ClientSupport clientSupport) {
        client = clientSupport;
        DateTimeFreezer.freezeTime();
    }

    @AfterAll
    public static void tearDown() {
        SamlProxyAppExtension.tearDown();
        DateTimeFreezer.unfreezeTime();
    }

    @Test
    public void getIdpMetadataFromApi_shouldWork() {
        SamlDto samlDto = client.targetMain("/API/metadata/idp").request().get(SamlDto.class);

        EntityDescriptor entityDescriptor = getEntityDescriptor(samlDto);
        assertThat(entityDescriptor.getEntityID()).isEqualTo(HUB_ENTITY_ID);
        assertThat(entityDescriptor.getSPSSODescriptor(SAMLConstants.SAML20P_NS)).isNull();
        assertThat(entityDescriptor.getIDPSSODescriptor(SAMLConstants.SAML20P_NS)).isNotNull();
        List<KeyDescriptor> keyDescriptors = entityDescriptor.getIDPSSODescriptor(SAMLConstants.SAML20P_NS).getKeyDescriptors();

        //this is a bit fragile and dependent on the ordering of IDPs and in federation metadata
        //this endpoint should be removed soon though...
        assertThat(keyDescriptors).hasSize(7);

        //signing certificates
        validateKeyDescriptor(keyDescriptors, 0, HUB_ENTITY_ID);
        validateKeyDescriptor(keyDescriptors, 1, HUB_ENTITY_ID, TestCertificateStrings.PUBLIC_SIGNING_CERTS.get(HUB_SECONDARY_ENTITY_ID));
        validateKeyDescriptor(keyDescriptors, 2, STUB_IDP_ONE);
        validateKeyDescriptor(keyDescriptors, 3, STUB_IDP_TWO);
        validateKeyDescriptor(keyDescriptors, 4, STUB_IDP_THREE);
        validateKeyDescriptor(keyDescriptors, 5, STUB_IDP_FOUR);


        //encryption certificate
        assertThat(getKeyName(keyDescriptors, 6)).isEqualTo(HUB_ENTITY_ID);
        assertThat(getCertificateData(keyDescriptors, 6)).isEqualTo(TestCertificateStrings.getPrimaryPublicEncryptionCert(HUB_ENTITY_ID));

        assertThat(entityDescriptor.getValidUntil()).isEqualTo(DateTime.now(DateTimeZone.UTC).plusHours(1));
    }

    private void validateKeyDescriptor(List<KeyDescriptor> keyDescriptors, int index, String issuer) {
        validateKeyDescriptor(keyDescriptors, index, issuer, TestCertificateStrings.PUBLIC_SIGNING_CERTS.get(issuer));
    }

    private void validateKeyDescriptor(List<KeyDescriptor> keyDescriptors, int index, String issuer, String certificate) {
        assertThat(getKeyName(keyDescriptors, index)).isEqualTo(issuer);
        assertThat(getCertificateData(keyDescriptors, index)).isEqualTo(certificate);
    }

    private String getKeyName(List<KeyDescriptor> keyDescriptors, int index) {
        return keyDescriptors.get(index).getKeyInfo().getKeyNames().get(0).getValue();
    }

    private String getCertificateData(List<KeyDescriptor> keyDescriptors, int index) {
        return keyDescriptors.get(index).getKeyInfo().getX509Datas().get(0).getX509Certificates().get(0).getValue();
    }

    private EntityDescriptor getEntityDescriptor(SamlDto samlDto) {
        return new OpenSamlXMLObjectUnmarshaller<EntityDescriptor>(samlObjectParser).fromString(samlDto.getSaml());
    }

    @Test
    public void getSpMetadataFromApi_shouldReturnTheHubFromNewMetadataAsAnSp() throws Exception {
        SamlDto samlDto = client.targetMain("/API/metadata/sp").request().get(SamlDto.class);
        EntityDescriptor entityDescriptor = getEntityDescriptor(samlDto);

        assertThat(entityDescriptor.getEntityID()).isEqualTo(HUB_ENTITY_ID);
        assertThat(entityDescriptor.getIDPSSODescriptor(SAMLConstants.SAML20P_NS)).isNull();
        assertThat(entityDescriptor.getSPSSODescriptor(SAMLConstants.SAML20P_NS)).isNotNull();
        assertThat(entityDescriptor.getSPSSODescriptor(SAMLConstants.SAML20P_NS).getAssertionConsumerServices().get(0).getLocation()).isEqualTo("http://foo.com/bar");
        assertThat(entityDescriptor.getValidUntil()).isEqualTo(DateTime.now(DateTimeZone.UTC).plusHours(1));
    }

}
