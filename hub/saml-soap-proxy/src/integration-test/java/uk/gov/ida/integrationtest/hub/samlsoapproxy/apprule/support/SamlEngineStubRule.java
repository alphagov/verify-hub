package uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import httpstub.HttpStubRule;
import httpstub.StackedHttpStub;
import org.opensaml.saml.saml2.core.AttributeQuery;
import org.opensaml.security.credential.Credential;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.hub.samlsoapproxy.Urls;
import uk.gov.ida.hub.samlsoapproxy.contract.MatchingServiceHealthCheckerResponseDto;
import uk.gov.ida.hub.samlsoapproxy.contract.SamlMessageDto;
import uk.gov.ida.saml.core.test.TestCredentialFactory;
import uk.gov.ida.saml.core.test.builders.AttributeQueryBuilder;
import uk.gov.ida.saml.core.test.builders.IssuerBuilder;
import uk.gov.ida.saml.core.test.builders.SignatureBuilder;
import uk.gov.ida.shared.utils.xml.XmlUtils;

import javax.ws.rs.core.Response;

import static uk.gov.ida.saml.core.test.TestCertificateStrings.HUB_TEST_PRIVATE_SIGNING_KEY;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.HUB_TEST_PUBLIC_SIGNING_CERT;
import static uk.gov.ida.saml.core.test.TestEntityIds.HUB_ENTITY_ID;

public class SamlEngineStubRule extends HttpStubRule {

    private final Credential signingCredential;

    public SamlEngineStubRule() {
        super(new StackedHttpStub());
        signingCredential = new TestCredentialFactory(HUB_TEST_PUBLIC_SIGNING_CERT, HUB_TEST_PRIVATE_SIGNING_KEY).getSigningCredential();
    }

    public void setupStubForAttributeResponseTranslate(MatchingServiceHealthCheckerResponseDto matchingServiceHealthCheckerResponseDto) throws JsonProcessingException {
        register(Urls.SamlEngineUrls.TRANSLATE_MSA_HEALTHCHECK_ATTRIBUTE_QUERY_RESPONSE_RESOURCE, Response.Status.OK.getStatusCode(), matchingServiceHealthCheckerResponseDto);
    }

    public void setupStubForAttributeResponseTranslateReturningError(ErrorStatusDto errorStatusDto) throws JsonProcessingException {
        register(Urls.SamlEngineUrls.TRANSLATE_MSA_HEALTHCHECK_ATTRIBUTE_QUERY_RESPONSE_RESOURCE, Response.Status.BAD_REQUEST.getStatusCode(), errorStatusDto);
    }

    public void prepareForHealthCheckSamlGeneration() throws JsonProcessingException {
        AttributeQuery attributeQuery = AttributeQueryBuilder.anAttributeQuery().withSignature(SignatureBuilder.aSignature().withSigningCredential(signingCredential).build()).withIssuer(IssuerBuilder.anIssuer().withIssuerId(HUB_ENTITY_ID).build()).build();
        SamlMessageDto samlMessageDto = new SamlMessageDto(XmlUtils.writeToString(attributeQuery.getDOM()));

        register(Urls.SamlEngineUrls.GENERATE_MSA_HEALTHCHECK_ATTRIBUTE_QUERY_RESOURCE, Response.Status.OK.getStatusCode(), samlMessageDto);
    }
}
