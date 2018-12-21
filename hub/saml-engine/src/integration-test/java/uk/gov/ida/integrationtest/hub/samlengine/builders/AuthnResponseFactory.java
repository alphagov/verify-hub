package uk.gov.ida.integrationtest.hub.samlengine.builders;

import java.util.Optional;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.Conditions;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.security.credential.BasicCredential;
import org.opensaml.security.credential.Credential;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.extensions.IdaAuthnContext;
import uk.gov.ida.saml.core.test.TestCertificateStrings;
import uk.gov.ida.saml.core.test.TestCredentialFactory;
import uk.gov.ida.saml.core.test.TestEntityIds;
import uk.gov.ida.saml.core.test.builders.AssertionBuilder;
import uk.gov.ida.saml.core.test.builders.AuthnContextBuilder;
import uk.gov.ida.saml.core.test.builders.AuthnContextClassRefBuilder;
import uk.gov.ida.saml.core.test.builders.AuthnStatementBuilder;
import uk.gov.ida.saml.core.test.builders.ConditionsBuilder;
import uk.gov.ida.saml.core.test.builders.Gpg45StatusAttributeBuilder;
import uk.gov.ida.saml.core.test.builders.IdpFraudEventIdAttributeBuilder;
import uk.gov.ida.saml.core.test.builders.IssuerBuilder;
import uk.gov.ida.saml.core.test.builders.MatchingDatasetAttributeStatementBuilder_1_1;
import uk.gov.ida.saml.core.test.builders.ResponseBuilder;
import uk.gov.ida.saml.core.test.builders.SignatureBuilder;
import uk.gov.ida.saml.core.test.builders.SubjectBuilder;
import uk.gov.ida.saml.core.test.builders.SubjectConfirmationBuilder;
import uk.gov.ida.saml.core.test.builders.SubjectConfirmationDataBuilder;
import uk.gov.ida.saml.serializers.XmlObjectToBase64EncodedStringTransformer;

import java.util.Map;
import java.util.UUID;

import static uk.gov.ida.saml.core.test.builders.AttributeStatementBuilder.anAttributeStatement;
import static uk.gov.ida.saml.core.test.builders.IPAddressAttributeBuilder.anIPAddress;

public class AuthnResponseFactory {
    private final XmlObjectToBase64EncodedStringTransformer<Response> toBase64EncodedStringTransformer = new XmlObjectToBase64EncodedStringTransformer<>();


    TestCredentialFactory hubEncryptionCredentialFactory =
            new TestCredentialFactory(TestCertificateStrings.HUB_TEST_PUBLIC_ENCRYPTION_CERT, TestCertificateStrings.HUB_TEST_PRIVATE_ENCRYPTION_KEY);
    Map<String, String> publicSigningCerts = ImmutableMap.<String, String>builder()
            .put(TestEntityIds.STUB_IDP_ONE, TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_CERT)
            .put(TestEntityIds.TEST_RP, TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_CERT)
            .put(TestEntityIds.STUB_IDP_TWO, TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_CERT)
            .put(TestEntityIds.STUB_IDP_THREE, TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_CERT)
            .build();
    Map<String, String> privateSigningKeys = ImmutableMap.<String, String>builder()
            .put(TestEntityIds.STUB_IDP_ONE, TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_PRIVATE_KEY)
            .put(TestEntityIds.TEST_RP, TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_PRIVATE_KEY)
            .put(TestEntityIds.STUB_IDP_TWO, TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_PRIVATE_KEY)
            .put(TestEntityIds.STUB_IDP_THREE, TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_PRIVATE_KEY)
            .build();

    public AuthnResponseFactory() {
    }

    public ResponseBuilder aResponseFromIdpBuilder(String idpEntityId) throws Exception {
        return aResponseFromIdpBuilder(idpEntityId, "ipAddressSeenByIdp");
    }

    public ResponseBuilder aResponseFromIdpBuilder(String idpEntityId, String ipAddressSeenByIdp) throws Exception {
        return aResponseFromIdpBuilder(idpEntityId, ipAddressSeenByIdp, DateTime.now(), UUID.randomUUID().toString(), UUID.randomUUID().toString(), Optional.empty());
    }

    public ResponseBuilder aResponseFromIdpBuilder(String idpEntityId, BasicCredential basicCredential) throws Exception {
        return aResponseFromIdpBuilder(idpEntityId, "ipAddressSeenByIdp", DateTime.now(), UUID.randomUUID().toString(), UUID.randomUUID().toString(), Optional.of(basicCredential));
    }

    public ResponseBuilder aResponseFromIdpBuilder(String idpEntityId, String authnStatementAssertionId, String mdsStatementAssertionId) throws Exception {
        return aResponseFromIdpBuilder(idpEntityId, "ipAddressSeenByIdp", DateTime.now(), authnStatementAssertionId, mdsStatementAssertionId, Optional.empty());
    }

    public ResponseBuilder aResponseFromIdpBuilder(String idpEntityId, String ipAddressSeenByIdp,  String authnAssertionSubjectPid, String mdsAssertionSubjectPid) throws Exception {
        return aResponseFromIdpBuilder(idpEntityId, ipAddressSeenByIdp, DateTime.now(), UUID.randomUUID().toString(), authnAssertionSubjectPid, UUID.randomUUID().toString(), mdsAssertionSubjectPid, Optional.empty());
    }

    public ResponseBuilder aResponseFromIdpBuilderWithIssuers(String idpEntityId, String authnAssertionIssuer, String mdsAssertionIssuer) throws Exception {
        String subjectPersistentIdentifier = generateId();
        return aResponseFromIdpBuilder(idpEntityId, "ipAddressSeenByIdp", DateTime.now(), UUID.randomUUID().toString(), subjectPersistentIdentifier, authnAssertionIssuer, UUID.randomUUID().toString(), subjectPersistentIdentifier, mdsAssertionIssuer, Optional.empty());
    }

    public ResponseBuilder aResponseFromIdpBuilderWithInResponseToValues(String idpEntityId, String requestId, String authnAssertionInResponseTo, String mdsAssertionInResponseTo) throws Exception {
        String subjectPersistentIdentifier = generateId();
        return aResponseFromIdpBuilder(idpEntityId, "ipAddressSeenByIdp", requestId, DateTime.now(), UUID.randomUUID().toString(), subjectPersistentIdentifier, idpEntityId, authnAssertionInResponseTo, UUID.randomUUID().toString(), subjectPersistentIdentifier, idpEntityId, mdsAssertionInResponseTo, Optional.empty());
    }

    public ResponseBuilder aResponseFromIdpBuilder(String idpEntityId,
                                                   String ipAddressSeenByIdp,
                                                   DateTime issueInstant,
                                                   String authnStatementAssertionId,
                                                   String mdsStatementAssertionId,
                                                   Optional<BasicCredential> basicCredential) throws Exception {
        String subjectPersistentIdentifier = generateId();
        return aResponseFromIdpBuilder(idpEntityId, ipAddressSeenByIdp, issueInstant, authnStatementAssertionId, subjectPersistentIdentifier, mdsStatementAssertionId, subjectPersistentIdentifier, basicCredential);
    }

    public ResponseBuilder aResponseFromIdpBuilder(String idpEntityId,
                                                   String ipAddressSeenByIdp,
                                                   DateTime issueInstant,
                                                   String authnStatementAssertionId,
                                                   String authnAssertionSubjectPid,
                                                   String mdsStatementAssertionId,
                                                   String mdsAssertionSubjectPid,
                                                   Optional<BasicCredential> basicCredential) throws Exception {
        return aResponseFromIdpBuilder(idpEntityId, ipAddressSeenByIdp, issueInstant, authnStatementAssertionId, authnAssertionSubjectPid, idpEntityId, mdsStatementAssertionId, mdsAssertionSubjectPid, idpEntityId, basicCredential);
    }

    public ResponseBuilder aResponseFromIdpBuilder(String idpEntityId,
                                                   String ipAddressSeenByIdp,
                                                   DateTime issueInstant,
                                                   String authnStatementAssertionId,
                                                   String authnAssertionSubjectPid,
                                                   String authnAssertionIssuer,
                                                   String mdsStatementAssertionId,
                                                   String mdsAssertionSubjectPid,
                                                   String mdsAssertionIssuer,
                                                   Optional<BasicCredential> basicCredential) throws Exception {
        String requestId = generateId();
        return aResponseFromIdpBuilder(idpEntityId, ipAddressSeenByIdp, requestId, issueInstant, authnStatementAssertionId, authnAssertionSubjectPid, authnAssertionIssuer, requestId, mdsStatementAssertionId, mdsAssertionSubjectPid, mdsAssertionIssuer, requestId, basicCredential);
    }

    public ResponseBuilder aResponseFromIdpBuilder(String idpEntityId,
                                                   String ipAddressSeenByIdp,
                                                   String requestId,
                                                   DateTime issueInstant,
                                                   String authnStatementAssertionId,
                                                   String authnAssertionSubjectPid,
                                                   String authnAssertionIssuer,
                                                   String authnAssertionInResponseTo,
                                                   String mdsStatementAssertionId,
                                                   String mdsAssertionSubjectPid,
                                                   String mdsAssertionIssuer,
                                                   String mdsAssertionInResponseTo,
                                                   Optional<BasicCredential> basicCredential) {

        TestCredentialFactory idpSigningCredentialFactory =
                new TestCredentialFactory(publicSigningCerts.get(idpEntityId), privateSigningKeys.get(idpEntityId));

        final Subject mdsAssertionSubject = SubjectBuilder.aSubject()
                .withPersistentId(mdsAssertionSubjectPid)
                .withSubjectConfirmation(SubjectConfirmationBuilder.aSubjectConfirmation()
                        .withSubjectConfirmationData(SubjectConfirmationDataBuilder.aSubjectConfirmationData()
                                .withInResponseTo(mdsAssertionInResponseTo)
                                .build())
                        .build())
                .build();
        final Subject authnAssertionSubject = SubjectBuilder.aSubject()
                .withNameId(buildNameID(authnAssertionSubjectPid))
                .withSubjectConfirmation(SubjectConfirmationBuilder.aSubjectConfirmation()
                        .withSubjectConfirmationData(SubjectConfirmationDataBuilder.aSubjectConfirmationData()
                                .withInResponseTo(authnAssertionInResponseTo)
                                .build())
                        .build())
                .build();
        final Conditions mdsAssertionConditions = ConditionsBuilder.aConditions().validFor(new Duration(1000*60*60)).build();
        final AttributeStatement matchingDatasetAttributeStatement = MatchingDatasetAttributeStatementBuilder_1_1.aMatchingDatasetAttributeStatement_1_1().build();
        final Credential encryptingCredential;
        if(basicCredential.isPresent()) {
            encryptingCredential = basicCredential.get();
        } else {
            encryptingCredential = hubEncryptionCredentialFactory.getEncryptingCredential();
        }
        final Credential signingCredential = idpSigningCredentialFactory.getSigningCredential();
        final AssertionBuilder mdsAssertion = AssertionBuilder.anAssertion().withId(generateId())
                .withIssuer(IssuerBuilder.anIssuer().withIssuerId(mdsAssertionIssuer).build())
                .withSubject(mdsAssertionSubject)
                .withConditions(mdsAssertionConditions)
                .withId(mdsStatementAssertionId)
                .addAttributeStatement(matchingDatasetAttributeStatement);
        final AssertionBuilder authnAssertion = AssertionBuilder.anAssertion().withId(generateId())
                .addAttributeStatement(anAttributeStatement().addAttribute(anIPAddress().withValue(ipAddressSeenByIdp).build()).build())
                .withIssuer(IssuerBuilder.anIssuer().withIssuerId(authnAssertionIssuer).build())
                .withSubject(authnAssertionSubject)
                .withId(authnStatementAssertionId)
                .withIssueInstant(issueInstant)
                .addAuthnStatement(AuthnStatementBuilder.anAuthnStatement().build());

        ResponseBuilder responseBuilder = ResponseBuilder.aResponse().withId(generateId())
                .withIssuer(IssuerBuilder.anIssuer().withIssuerId(idpEntityId).build())
                .withSigningCredential(signingCredential)
                .withInResponseTo(requestId)
                .addEncryptedAssertion(mdsAssertion
                        .withSignature(SignatureBuilder.aSignature().withSigningCredential(signingCredential).build())
                        .buildWithEncrypterCredential(encryptingCredential))
                .addEncryptedAssertion(authnAssertion
                        .withSignature(SignatureBuilder.aSignature().withSigningCredential(signingCredential).build())
                        .buildWithEncrypterCredential(encryptingCredential));
        return responseBuilder;
    }

    public ResponseBuilder aFraudResponseFromIdpBuilder(String idpEntityId) throws Exception {
        return aFraudResponseFromIdpBuilder(idpEntityId, generateId());
    }

    public ResponseBuilder aFraudResponseFromIdpBuilder(String idpEntityId, String persistentId) {

        TestCredentialFactory idpSigningCredentialFactory =
                new TestCredentialFactory(publicSigningCerts.get(idpEntityId), privateSigningKeys.get(idpEntityId));

        String requestId = generateId();

        final Subject mdsAssertionSubject = SubjectBuilder.aSubject()
                .withPersistentId(persistentId)
                .withSubjectConfirmation(SubjectConfirmationBuilder.aSubjectConfirmation()
                        .withSubjectConfirmationData(SubjectConfirmationDataBuilder.aSubjectConfirmationData()
                                .withInResponseTo(requestId)
                                .build())
                        .build())
                .build();
        final Subject authnAssertionSubject = SubjectBuilder.aSubject()
                .withNameId(buildNameID(persistentId))
                .withSubjectConfirmation(SubjectConfirmationBuilder.aSubjectConfirmation()
                        .withSubjectConfirmationData(SubjectConfirmationDataBuilder.aSubjectConfirmationData()
                                .withInResponseTo(requestId)
                                .build())
                        .build())
                .build();
        final AttributeStatement matchingDatasetAttributeStatement = MatchingDatasetAttributeStatementBuilder_1_1.aMatchingDatasetAttributeStatement_1_1().build();
        final Credential encryptingCredential = hubEncryptionCredentialFactory.getEncryptingCredential();
        final Credential signingCredential = idpSigningCredentialFactory.getSigningCredential();
        final AssertionBuilder mdsAssertion = AssertionBuilder.anAssertion().withId(generateId())
                .withIssuer(IssuerBuilder.anIssuer().withIssuerId(idpEntityId).build())
                .withSubject(mdsAssertionSubject)
                .addAttributeStatement(matchingDatasetAttributeStatement);
        final AssertionBuilder authnAssertion = AssertionBuilder.anAssertion().withId(generateId())
                .addAttributeStatement(anAttributeStatement()
                        .addAttribute(IdpFraudEventIdAttributeBuilder.anIdpFraudEventIdAttribute().withValue("a-fraud-event").build())
                        .addAttribute(Gpg45StatusAttributeBuilder.aGpg45StatusAttribute().withValue("IT01").build())
                        .addAttribute(anIPAddress().build())
                        .build())
                .withIssuer(IssuerBuilder.anIssuer().withIssuerId(idpEntityId).build())
                .withSubject(authnAssertionSubject)
                .addAuthnStatement(AuthnStatementBuilder.anAuthnStatement()
                        .withAuthnContext(AuthnContextBuilder.anAuthnContext()
                                .withAuthnContextClassRef(AuthnContextClassRefBuilder.anAuthnContextClassRef()
                                        .withAuthnContextClasRefValue(IdaAuthnContext.LEVEL_X_AUTHN_CTX)
                                        .build())
                                .build())
                        .build());
        ResponseBuilder responseBuilder = ResponseBuilder.aResponse().withId(generateId())
                .withIssuer(IssuerBuilder.anIssuer().withIssuerId(idpEntityId).build())
                .withInResponseTo(requestId)
                .addEncryptedAssertion(mdsAssertion
                        .withSignature(SignatureBuilder.aSignature().withSigningCredential(signingCredential).build())
                        .buildWithEncrypterCredential(encryptingCredential))
                .addEncryptedAssertion(authnAssertion
                        .withSignature(SignatureBuilder.aSignature().withSigningCredential(signingCredential).build())
                        .buildWithEncrypterCredential(encryptingCredential));
        return responseBuilder;
    }

    public ResponseBuilder anAuthnFailedResponseFromIdpBuilder(String idpEntityId) {

        String requestId = generateId();

        return ResponseBuilder.aResponse().withId(generateId())
                .withIssuer(IssuerBuilder.anIssuer().withIssuerId(idpEntityId).build())
                .withNoDefaultAssertion()
                .withInResponseTo(requestId);
    }

    public String transformResponseToSaml(Response response) {
        return toBase64EncodedStringTransformer.apply(response);
    }

    private NameID buildNameID(String id) {
        NameID nameId = new OpenSamlXmlObjectFactory().createNameId(id);
        nameId.setFormat(NameIDType.PERSISTENT);

        return nameId;
    }


    private String generateId() {
        return UUID.randomUUID().toString();
    }
}
