package uk.gov.ida.saml.core.test;

import org.joda.time.DateTime;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.security.credential.Credential;
import org.opensaml.xmlsec.signature.support.SignatureException;
import uk.gov.ida.saml.configuration.SamlConfiguration;
import uk.gov.ida.saml.security.EncryptionCredentialFactory;
import uk.gov.ida.saml.core.test.builders.ResponseBuilder;
import uk.gov.ida.saml.core.test.builders.SignatureBuilder;
import uk.gov.ida.saml.core.test.builders.SubjectConfirmationBuilder;
import uk.gov.ida.shared.utils.xml.XmlUtils;

import static uk.gov.ida.saml.core.test.builders.AssertionBuilder.anAssertion;
import static uk.gov.ida.saml.core.test.builders.IssuerBuilder.anIssuer;
import static uk.gov.ida.saml.core.test.builders.SubjectBuilder.aSubject;
import static uk.gov.ida.saml.core.test.builders.SubjectConfirmationDataBuilder.aSubjectConfirmationData;
import static uk.gov.ida.saml.core.test.builders.AuthnStatementBuilder.anAuthnStatement;

public class MatchingServiceResponseFactory {

    private final EncryptionCredentialFactory encryptionCredentialFactory;

    /*
     * @Deprecated Use the other constructor. samlConfiguration is ignored.
     */
    @Deprecated
    public MatchingServiceResponseFactory(SamlConfiguration samlConfiguration, EncryptionCredentialFactory encryptionCredentialFactory) {
        this.encryptionCredentialFactory = encryptionCredentialFactory;
    }

    public MatchingServiceResponseFactory(EncryptionCredentialFactory encryptionCredentialFactory) {
        this.encryptionCredentialFactory = encryptionCredentialFactory;
    }

    public String aSamlResponseFromMatchingService(
            String requestId,
            SamlConfiguration configuration,
            Status status,
            String issuer,
            String publicCert,
            String privateKey
    ) throws MarshallingException, SignatureException {
        return XmlUtils.writeToString(aValidResponseFromMatchingService(requestId, configuration, status, issuer, publicCert, privateKey).getDOM());
    }

    public Response aValidResponseFromMatchingService(
            String requestId,
            SamlConfiguration configuration,
            Status status,
            String issuer,
            String publicCert,
            String privateKey) throws MarshallingException, SignatureException {
        final Credential signingCredential = new TestCredentialFactory(publicCert, privateKey).getSigningCredential();
        return ResponseBuilder.aResponse()
                .withStatus(status)
                .withIssuer(anIssuer().withIssuerId(issuer).build())
                .withSigningCredential(signingCredential)
                .addEncryptedAssertion(
                        anAssertion()
                                .withSubject(
                                        aSubject()
                                                .withSubjectConfirmation(
                                                        SubjectConfirmationBuilder.aSubjectConfirmation()
                                                                .withSubjectConfirmationData(
                                                                        aSubjectConfirmationData()
                                                                                .withInResponseTo(requestId)
                                                                                .withNotOnOrAfter(
                                                                                        DateTime.now()
                                                                                                .plusDays(5)
                                                                                )
                                                                                .build()
                                                                )
                                                                .build()
                                                )
                                                .build()
                                )
                                .withIssuer(anIssuer().withIssuerId(issuer).build())
                                .withSignature(
                                        SignatureBuilder.aSignature().withSigningCredential(signingCredential
                                        ).build()
                                )
                                .addAuthnStatement(anAuthnStatement().build())
                                .buildWithEncrypterCredential(encryptionCredentialFactory.getEncryptingCredential(configuration.getEntityId()))
                ).build();
    }
}
