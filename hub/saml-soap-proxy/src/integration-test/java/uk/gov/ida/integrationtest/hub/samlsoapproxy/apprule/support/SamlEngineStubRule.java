package uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import httpstub.AbstractHttpStub;
import httpstub.HttpStub;
import httpstub.HttpStubRule;
import httpstub.RequestAndResponse;
import httpstub.StackedHttpStub;
import org.opensaml.saml.saml2.core.AttributeQuery;
import org.opensaml.security.credential.Credential;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.hub.samlsoapproxy.contract.MatchingServiceHealthCheckerRequestDto;
import uk.gov.ida.hub.samlsoapproxy.contract.MatchingServiceHealthCheckerResponseDto;
import uk.gov.ida.hub.samlsoapproxy.contract.SamlMessageDto;
import uk.gov.ida.saml.core.test.TestCredentialFactory;
import uk.gov.ida.shared.utils.xml.XmlUtils;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static httpstub.builders.ExpectedRequestBuilder.expectRequest;
import static javax.ws.rs.core.Response.Status.OK;
import static uk.gov.ida.hub.samlsoapproxy.Urls.SamlEngineUrls.GENERATE_MSA_HEALTHCHECK_ATTRIBUTE_QUERY_RESOURCE;
import static uk.gov.ida.hub.samlsoapproxy.Urls.SamlEngineUrls.TRANSLATE_MSA_HEALTHCHECK_ATTRIBUTE_QUERY_RESPONSE_RESOURCE;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.HUB_TEST_PRIVATE_SIGNING_KEY;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.HUB_TEST_PUBLIC_SIGNING_CERT;
import static uk.gov.ida.saml.core.test.TestEntityIds.HUB_ENTITY_ID;
import static uk.gov.ida.saml.core.test.builders.AttributeQueryBuilder.anAttributeQuery;
import static uk.gov.ida.saml.core.test.builders.IssuerBuilder.anIssuer;
import static uk.gov.ida.saml.core.test.builders.SignatureBuilder.aSignature;

public class SamlEngineStubRule extends HttpStubRule {

    private final Credential signingCredential;

    private SamlEngineStubRule(AbstractHttpStub abstractHttpStub) {
        super(abstractHttpStub);
        signingCredential = new TestCredentialFactory(HUB_TEST_PUBLIC_SIGNING_CERT, HUB_TEST_PRIVATE_SIGNING_KEY).getSigningCredential();
    }

    public static SamlEngineStubRule stackedSamlEngineStubRule() {
        return new SamlEngineStubRule(new StackedHttpStub());
    }

    public static SamlEngineStubRule samlEngineStubRule() {
        return new SamlEngineStubRule(new HttpStub());
    }

    public void setupStubForAttributeResponseTranslate(MatchingServiceHealthCheckerResponseDto msaHealthCheckerResponseDto) throws JsonProcessingException {
        register(TRANSLATE_MSA_HEALTHCHECK_ATTRIBUTE_QUERY_RESPONSE_RESOURCE, OK.getStatusCode(), msaHealthCheckerResponseDto);
    }

    public void setupStubForAttributeResponseTranslateReturningError(ErrorStatusDto errorStatusDto) throws JsonProcessingException {
        register(TRANSLATE_MSA_HEALTHCHECK_ATTRIBUTE_QUERY_RESPONSE_RESOURCE, Response.Status.BAD_REQUEST.getStatusCode(), errorStatusDto);
    }

    public void prepareForHealthCheckSamlGeneration() throws JsonProcessingException {
        AttributeQuery attributeQuery = anAttributeQuery().withSignature(aSignature().withSigningCredential(signingCredential).build())
                                                          .withIssuer(anIssuer().withIssuerId(HUB_ENTITY_ID).build())
                                                          .build();
        SamlMessageDto samlMessageDto = new SamlMessageDto(XmlUtils.writeToString(attributeQuery.getDOM()));

        register(GENERATE_MSA_HEALTHCHECK_ATTRIBUTE_QUERY_RESOURCE, OK.getStatusCode(), samlMessageDto);
    }

    public void prepareForHealthCheckSamlGeneration(final MatchingServiceHealthCheckerRequestDto msaHealthCheckerRequest) throws JsonProcessingException {

        final AttributeQuery attributeQuery = anAttributeQuery().withSignature(aSignature().withSigningCredential(signingCredential).build())
                                                                .withIssuer(anIssuer().withIssuerId(HUB_ENTITY_ID).build())
                                                                .build();
        final SamlMessageDto samlMessage = new SamlMessageDto(XmlUtils.writeToString(attributeQuery.getDOM()));
        final RequestAndResponse requestAndResponse = expectRequest().withPath(GENERATE_MSA_HEALTHCHECK_ATTRIBUTE_QUERY_RESOURCE)
                                                                     .withMethod("POST")
                                                                     .withBody(msaHealthCheckerRequest)
                                                                     .andWillRespondWith()
                                                                     .withStatus(OK.getStatusCode())
                                                                     .withContentType(MediaType.APPLICATION_JSON)
                                                                     .withBody(samlMessage)
                                                                     .build();
        register(requestAndResponse);
    }

    public void setupStubForAttributeResponseTranslate(final SamlMessageDto samlMessage,
                                                       final MatchingServiceHealthCheckerResponseDto msaHealthCheckerResponse) throws JsonProcessingException {

        final RequestAndResponse requestAndResponse = expectRequest().withPath(TRANSLATE_MSA_HEALTHCHECK_ATTRIBUTE_QUERY_RESPONSE_RESOURCE)
                                                                     .withMethod("POST")
                                                                     .withBody(samlMessage)
                                                                     .andWillRespondWith()
                                                                     .withStatus(OK.getStatusCode())
                                                                     .withContentType(MediaType.APPLICATION_JSON)
                                                                     .withBody(msaHealthCheckerResponse)
                                                                     .build();
        register(requestAndResponse);
    }
}
