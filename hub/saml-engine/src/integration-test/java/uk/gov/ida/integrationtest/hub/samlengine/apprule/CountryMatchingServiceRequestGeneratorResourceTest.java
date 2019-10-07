package uk.gov.ida.integrationtest.hub.samlengine.apprule;

import helpers.JerseyClientConfigurationBuilder;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.util.Duration;
import net.shibboleth.utilities.java.support.xml.XMLParserException;
import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeQuery;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.EncryptedAssertion;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;
import org.opensaml.saml.saml2.encryption.Decrypter;
import org.opensaml.security.credential.BasicCredential;
import uk.gov.ida.common.shared.security.PrivateKeyFactory;
import uk.gov.ida.common.shared.security.PublicKeyFactory;
import uk.gov.ida.common.shared.security.X509CertificateFactory;
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.hub.samlengine.builders.EidasAttributeQueryRequestDtoBuilder;
import uk.gov.ida.hub.samlengine.contracts.AttributeQueryContainerDto;
import uk.gov.ida.hub.samlengine.domain.EidasAttributeQueryRequestDto;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.ConfigStubRule;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppRule;
import uk.gov.ida.saml.core.IdaConstants;
import uk.gov.ida.saml.deserializers.parser.SamlObjectParser;
import uk.gov.ida.saml.security.AssertionDecrypter;
import uk.gov.ida.saml.security.DecrypterFactory;
import uk.gov.ida.saml.security.validators.encryptedelementtype.EncryptionAlgorithmValidator;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;

import static io.dropwizard.testing.ConfigOverride.config;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_MS_PRIVATE_ENCRYPTION_KEY;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_MS_PUBLIC_ENCRYPTION_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_MS_PUBLIC_SIGNING_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_PUBLIC_ENCRYPTION_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_PUBLIC_SIGNING_CERT;
import static uk.gov.ida.saml.core.test.TestEntityIds.HUB_ENTITY_ID;
import static uk.gov.ida.saml.core.test.TestEntityIds.TEST_RP;
import static uk.gov.ida.saml.core.test.TestEntityIds.TEST_RP_MS;

public class CountryMatchingServiceRequestGeneratorResourceTest {

    private static Client client;

    @ClassRule
    public static ConfigStubRule configStub = new ConfigStubRule();

    @ClassRule
    public static SamlEngineAppRule samlEngineAppRule = new SamlEngineAppRule(
        config("configUri", configStub.baseUri().build().toASCIIString())
    );

    @BeforeClass
    public static void setupClass() throws Exception {
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder
            .aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        client = new JerseyClientBuilder(samlEngineAppRule.getEnvironment()).using(jerseyClientConfiguration)
            .build(CountryMatchingServiceRequestGeneratorResourceTest.class.getSimpleName());
    }

    @Before
    public void beforeEach() throws Exception {
        configStub.setupCertificatesForEntity(TEST_RP_MS, TEST_RP_MS_PUBLIC_SIGNING_CERT, TEST_RP_MS_PUBLIC_ENCRYPTION_CERT);
        configStub.setupCertificatesForEntity(TEST_RP, TEST_RP_PUBLIC_SIGNING_CERT, TEST_RP_PUBLIC_ENCRYPTION_CERT);
    }

    @Test
    public void shouldCreateAttributeQueryRequest() throws Exception {

        EidasAttributeQueryRequestDto eidasAttributeQueryRequestDto = new EidasAttributeQueryRequestDtoBuilder().withAnEncryptedIdentityAssertion().build();
        Response response = generateEidasAttributeQueryRequest(eidasAttributeQueryRequestDto);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        AttributeQueryContainerDto attributeQueryContainerDto = response.readEntity(AttributeQueryContainerDto.class);
        assertThat(attributeQueryContainerDto.getId()).isEqualTo(eidasAttributeQueryRequestDto.getRequestId());
        assertThat(attributeQueryContainerDto.getIssuer()).isEqualTo(HUB_ENTITY_ID);
        assertThat(attributeQueryContainerDto.getMatchingServiceUri()).isEqualTo(eidasAttributeQueryRequestDto.getAttributeQueryUri());
        assertThat(attributeQueryContainerDto.getAttributeQueryClientTimeOut()).isEqualTo(eidasAttributeQueryRequestDto.getMatchingServiceRequestTimeOut());
        assertThat(attributeQueryContainerDto.isOnboarding()).isEqualTo(eidasAttributeQueryRequestDto.isOnboarding());
        assertThat(attributeQueryContainerDto.getSamlRequest()).contains("saml2p:AttributeQuery");
    }

    @Test
    public void shouldCreateAttributeQueryRequestUnsigned() throws XMLParserException, UnmarshallingException {
        EidasAttributeQueryRequestDto eidasAttributeQueryRequestDto = new EidasAttributeQueryRequestDtoBuilder()
                .withAnEncryptedIdentityAssertion()
                .withACountrySignedResponseWithIssuer("a country entity id")
                .build();
        Response response = generateEidasAttributeQueryRequest(eidasAttributeQueryRequestDto);
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        AttributeQueryContainerDto attributeQueryContainerDto = response.readEntity(AttributeQueryContainerDto.class);
        List<Assertion> assertions = getDecryptedAssertions(attributeQueryContainerDto);
        assertThat(assertions.size()).isEqualTo(1);
        Assertion assertion = assertions.iterator().next();
        List<AuthnStatement> authnStatements = assertion.getAuthnStatements();
        assertThat(authnStatements.size()).isEqualTo(1);
        List<AttributeStatement> attributeStatements = assertion.getAttributeStatements();
        assertThat(attributeStatements.size()).isEqualTo(1);
        AttributeStatement attributeStatement = attributeStatements.iterator().next();
        List<Attribute> attributes = attributeStatement.getAttributes();
        assertThat(attributes.size()).isEqualTo(2);
        assertThat(attributes.get(0).getName()).isEqualTo(IdaConstants.Eidas_Attributes.UnsignedAssertions.EidasSamlResponse.NAME);
        assertThat(attributes.get(1).getName()).isEqualTo(IdaConstants.Eidas_Attributes.UnsignedAssertions.EncryptedSecretKeys.NAME);

    }

    private List<Assertion> getDecryptedAssertions(AttributeQueryContainerDto attributeQueryContainerDto) throws UnmarshallingException, XMLParserException {
        AttributeQuery attributeQuery = new SamlObjectParser().getSamlObject(attributeQueryContainerDto.getSamlRequest());
        List<SubjectConfirmation> subjectConfirmations = attributeQuery.getSubject().getSubjectConfirmations();
        assertThat(subjectConfirmations.size()).isEqualTo(1);
        SubjectConfirmationData subjectConfirmationData = subjectConfirmations.get(0).getSubjectConfirmationData();
        EncryptedAssertion encryptedAssertion = (EncryptedAssertion) subjectConfirmationData.getUnknownXMLObjects().get(0);
        PublicKey publicKey = new PublicKeyFactory(new X509CertificateFactory()).createPublicKey(TEST_RP_MS_PUBLIC_ENCRYPTION_CERT);
        PrivateKey privateKey = new PrivateKeyFactory().createPrivateKey(Base64.decodeBase64(TEST_RP_MS_PRIVATE_ENCRYPTION_KEY));
        BasicCredential basicCredential = new BasicCredential(publicKey, privateKey);
        Decrypter decrypter = new DecrypterFactory().createDecrypter(List.of(basicCredential));
        AssertionDecrypter assertionDecrypter = new AssertionDecrypter(new EncryptionAlgorithmValidator(), decrypter);
        return assertionDecrypter.decryptAssertions(() -> List.of(encryptedAssertion));
    }

    private Response generateEidasAttributeQueryRequest(EidasAttributeQueryRequestDto dto) {
        final URI uri = samlEngineAppRule.getUri(Urls.SamlEngineUrls.GENERATE_COUNTRY_ATTRIBUTE_QUERY_RESOURCE);
        return client.target(uri)
            .request()
            .post(Entity.json(dto), Response.class);
    }
}
