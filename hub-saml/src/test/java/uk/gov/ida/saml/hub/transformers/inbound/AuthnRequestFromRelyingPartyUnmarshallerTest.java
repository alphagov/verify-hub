package uk.gov.ida.saml.hub.transformers.inbound;

import com.google.common.collect.ImmutableList;
import org.apache.commons.codec.binary.Base64;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Extensions;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.impl.AttributeBuilder;
import org.opensaml.saml.saml2.core.impl.AuthnRequestBuilder;
import org.opensaml.saml.saml2.core.impl.ExtensionsBuilder;
import org.opensaml.saml.saml2.core.impl.IssuerBuilder;
import org.opensaml.saml.saml2.encryption.Encrypter;
import org.opensaml.security.credential.BasicCredential;
import org.opensaml.xmlsec.signature.impl.SignatureBuilder;
import org.opensaml.xmlsec.signature.impl.SignatureImpl;
import uk.gov.ida.common.shared.security.PrivateKeyFactory;
import uk.gov.ida.common.shared.security.PublicKeyFactory;
import uk.gov.ida.common.shared.security.X509CertificateFactory;
import uk.gov.ida.saml.core.IdaSamlBootstrap;
import uk.gov.ida.saml.core.extensions.versioning.Version;
import uk.gov.ida.saml.core.extensions.versioning.VersionImpl;
import uk.gov.ida.saml.core.extensions.versioning.application.ApplicationVersion;
import uk.gov.ida.saml.core.extensions.versioning.application.ApplicationVersionImpl;
import uk.gov.ida.saml.hub.domain.AuthnRequestFromRelyingParty;
import uk.gov.ida.saml.security.DecrypterFactory;
import uk.gov.ida.saml.security.EncrypterFactory;

import java.net.URI;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.HUB_TEST_PRIVATE_ENCRYPTION_KEY;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.HUB_TEST_PUBLIC_ENCRYPTION_CERT;

public class AuthnRequestFromRelyingPartyUnmarshallerTest {

    private static Encrypter encrypter;

    private AuthnRequestFromRelyingPartyUnmarshaller unmarshaller;

    @Before
    public void setUp() {
        IdaSamlBootstrap.bootstrap();

        final BasicCredential basicCredential = createBasicCredential();
        encrypter = new EncrypterFactory().createEncrypter(basicCredential);

        unmarshaller = new AuthnRequestFromRelyingPartyUnmarshaller(new DecrypterFactory().createDecrypter(ImmutableList.of(basicCredential)));
    }

    @Test
    public void fromSamlMessage_shouldMapAuthnRequestToAuthnRequestFromRelyingParty() throws Exception {
        DateTime issueInstant = new DateTime();
        SignatureImpl signature = new SignatureBuilder().buildObject();

        AuthnRequest authnRequest = new AuthnRequestBuilder().buildObject();
        authnRequest.setID("some-id");
        Issuer issuer = new IssuerBuilder().buildObject();
        issuer.setValue("some-service-entity-id");
        authnRequest.setIssuer(issuer);
        authnRequest.setIssueInstant(issueInstant);
        authnRequest.setDestination("http://example.com");
        authnRequest.setForceAuthn(true);
        authnRequest.setAssertionConsumerServiceURL("some-url");
        authnRequest.setAssertionConsumerServiceIndex(Integer.valueOf(5));
        authnRequest.setSignature(signature);
        authnRequest.setExtensions(createApplicationVersionExtensions("some-version"));

        AuthnRequestFromRelyingParty authnRequestFromRelyingParty = unmarshaller.fromSamlMessage(authnRequest);
        AuthnRequestFromRelyingParty expected = new AuthnRequestFromRelyingParty(
            "some-id",
            "some-service-entity-id",
            issueInstant,
            URI.create("http://example.com"),
            Optional.ofNullable(true),
            Optional.of(URI.create("some-url")),
            Optional.of(Integer.valueOf(5)),
            Optional.of(signature),
            Optional.of("some-version")
        );

        assertThat(authnRequestFromRelyingParty).isEqualTo(expected);
    }

    @Test
    public void fromSamlMessage_shouldNotComplainWhenThereIsNoExtensionsElement() throws Exception {
        AuthnRequest authnRequest = new AuthnRequestBuilder().buildObject();
        authnRequest.setIssuer(new IssuerBuilder().buildObject());
        authnRequest.setDestination("http://example.com");

        AuthnRequestFromRelyingParty authnRequestFromRelyingParty = unmarshaller.fromSamlMessage(authnRequest);

        assertThat(authnRequestFromRelyingParty.getVerifyServiceProviderVersion()).isEqualTo(Optional.empty());
    }

    @Test
    public void fromSamlMessage_shouldNotComplainWhenExceptionDuringDecryption() throws Exception {
        AuthnRequest authnRequest = new AuthnRequestBuilder().buildObject();
        authnRequest.setIssuer(new IssuerBuilder().buildObject());
        authnRequest.setDestination("http://example.com");
        authnRequest.setExtensions(createApplicationVersionExtensions(null));

        AuthnRequestFromRelyingParty authnRequestFromRelyingParty = unmarshaller.fromSamlMessage(authnRequest);

        assertThat(authnRequestFromRelyingParty.getVerifyServiceProviderVersion()).isEqualTo(Optional.empty());
    }

    private Extensions createApplicationVersionExtensions(String version) throws Exception {
        Extensions extensions = new ExtensionsBuilder().buildObject();
        Attribute versionsAttribute = new AttributeBuilder().buildObject();
        versionsAttribute.setName("Versions");
        versionsAttribute.getAttributeValues().add(createApplicationVersion(version));
        extensions.getUnknownXMLObjects().add(encrypter.encrypt(versionsAttribute));
        return extensions;
    }

    private Version createApplicationVersion(String versionNumber) {
        ApplicationVersion applicationVersion = new ApplicationVersionImpl();
        applicationVersion.setValue(versionNumber);
        Version version = new VersionImpl() {{
            setApplicationVersion(applicationVersion);
        }};
        return version;
    }

    private BasicCredential createBasicCredential() {
        final PublicKey publicKey = new PublicKeyFactory(new X509CertificateFactory()).createPublicKey(HUB_TEST_PUBLIC_ENCRYPTION_CERT);
        PrivateKey privateKey = new PrivateKeyFactory().createPrivateKey(Base64.decodeBase64(HUB_TEST_PRIVATE_ENCRYPTION_KEY));
        return new BasicCredential(publicKey, privateKey);
    }
}