package uk.gov.ida.integrationtest.hub.policy.apprule.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import httpstub.HttpStubRule;
import httpstub.RecordedRequest;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.contracts.AttributeQueryContainerDto;
import uk.gov.ida.hub.policy.contracts.AuthnResponseFromHubContainerDto;
import uk.gov.ida.hub.policy.contracts.InboundResponseFromMatchingServiceDto;
import uk.gov.ida.hub.policy.contracts.SamlAuthnResponseTranslatorDto;
import uk.gov.ida.hub.policy.contracts.SamlMessageDto;
import uk.gov.ida.hub.policy.contracts.SamlRequestDto;
import uk.gov.ida.hub.policy.contracts.SamlResponseWithAuthnRequestInformationDto;
import uk.gov.ida.hub.policy.domain.InboundResponseFromIdpDto;

import javax.ws.rs.core.Response;
import java.util.UUID;

public class SamlEngineStubRule extends HttpStubRule {
    public void setUpStubForAuthnResponseGenerate(AuthnResponseFromHubContainerDto translatedMessage) throws JsonProcessingException {
        register(Urls.SamlEngineUrls.GENERATE_RP_AUTHN_RESPONSE_RESOURCE, Response.Status.OK.getStatusCode(), translatedMessage);
    }

    public void setupStubForAuthnRequestTranslate(SamlResponseWithAuthnRequestInformationDto translatedMessage) throws JsonProcessingException {
        register(Urls.SamlEngineUrls.TRANSLATE_RP_AUTHN_REQUEST_RESOURCE, Response.Status.OK.getStatusCode(), translatedMessage);
    }

    public void setupStubToReturnInvalidSamlException() throws JsonProcessingException {
        register(Urls.SamlEngineUrls.TRANSLATE_RP_AUTHN_REQUEST_RESOURCE, Response.Status.BAD_REQUEST.getStatusCode(), ErrorStatusDto.createAuditedErrorStatus(UUID.randomUUID(), ExceptionType.INVALID_SAML));
    }

    public void setupStubForIdpAuthnRequestGenerate(SamlRequestDto generatedAuthnRequest) throws JsonProcessingException {
        register(Urls.SamlEngineUrls.GENERATE_IDP_AUTHN_REQUEST_RESOURCE, Response.Status.OK.getStatusCode(), generatedAuthnRequest);
    }

    public void setupStubForIdpAuthnResponseTranslate(InboundResponseFromIdpDto inboundResponseFromIdpDto) throws JsonProcessingException {
        register(Urls.SamlEngineUrls.TRANSLATE_IDP_AUTHN_RESPONSE_RESOURCE, Response.Status.OK.getStatusCode(), inboundResponseFromIdpDto);
    }

    public void setupStubForAttributeQueryRequest(AttributeQueryContainerDto msaRequest) throws JsonProcessingException {
        register(Urls.SamlEngineUrls.GENERATE_ATTRIBUTE_QUERY_RESOURCE, Response.Status.OK.getStatusCode(), msaRequest);
    }

    public void setupStubForIdpAuthnResponseTranslateReturningError(ErrorStatusDto errorStatusDto) throws JsonProcessingException {
        register(Urls.SamlEngineUrls.TRANSLATE_IDP_AUTHN_RESPONSE_RESOURCE, Response.Status.BAD_REQUEST.getStatusCode(), errorStatusDto);
    }

    public void setupStubForAttributeResponseTranslate(InboundResponseFromMatchingServiceDto inboundResponseFromMatchingServiceDto) throws JsonProcessingException {
        register(Urls.SamlEngineUrls.TRANSLATE_MATCHING_SERVICE_RESPONSE_RESOURCE, Response.Status.OK.getStatusCode(), inboundResponseFromMatchingServiceDto);
    }

    public void setupStubForAttributeResponseTranslateReturningError(ErrorStatusDto errorStatusDto) throws JsonProcessingException {
        register(Urls.SamlEngineUrls.TRANSLATE_MATCHING_SERVICE_RESPONSE_RESOURCE, Response.Status.BAD_REQUEST.getStatusCode(), errorStatusDto);
    }

    public void setUpStubForErrorResponseGenerate(SamlMessageDto samlMessageDto) throws JsonProcessingException {
        register(Urls.SamlEngineUrls.GENERATE_RP_ERROR_RESPONSE_RESOURCE, Response.Status.OK.getStatusCode(), samlMessageDto);
    }

    public void setUpStubForErrorResponseGenerateErrorOccurring() throws JsonProcessingException {
        register(Urls.SamlEngineUrls.GENERATE_RP_ERROR_RESPONSE_RESOURCE, Response.Status.BAD_REQUEST.getStatusCode(), ErrorStatusDto.createAuditedErrorStatus(UUID.randomUUID(), ExceptionType.INVALID_INPUT));
    }

    private RecordedRequest getRecordedTranslateRequest() {
        return getRecordedRequest()
                .stream()
                .filter(request -> request.getPath().equals(Urls.SamlEngineUrls.TRANSLATE_IDP_AUTHN_RESPONSE_RESOURCE))
                .findFirst()
                .orElseThrow();
    }

    public SamlAuthnResponseTranslatorDto getSamlAuthnResponseTranslatorDto(ObjectMapper objectMapper) throws java.io.IOException {
        return objectMapper.readValue(new String(getRecordedTranslateRequest().getEntityBytes()), SamlAuthnResponseTranslatorDto.class);
    }
}
