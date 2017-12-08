package uk.gov.ida.hub.samlsoapproxy;

public interface Urls {

    /** NOTE: the general form for this class should be
    * *_ROOT - used to annotate the Resource Class (root path for the resource)
    * *_PATH - used to annotate the Methods within the Resource Class
    * *_PARAM - used to annotate the parameters for the methods
    * *_RESOURCE - used by the Proxy classes in order to reference the resource. Internal to the hub (may be external to the micro service)
    * *_ENDPOINT - used for external (to the hub) endpoints
    *
    * If the parameter you are referencing/adding doesn't fit this style, perhaps it needs a different place to live.
    */
    interface SamlEngineUrls {
        String SAML_ENGINE_ROOT = "/saml-engine";

        String GENERATE_MSA_HEALTHCHECK_ATTRIBUTE_QUERY_RESOURCE = SAML_ENGINE_ROOT + "/generate-msa-healthcheck-attribute-query";
        String TRANSLATE_MSA_HEALTHCHECK_ATTRIBUTE_QUERY_RESPONSE_RESOURCE = SAML_ENGINE_ROOT + "/translate-msa-healthcheck-attribute-query";

    }

    interface SharedUrls {
        String ENTITY_ID_PARAM = "entityId";
        String SESSION_ID_PARAM = "sessionId";
        String SESSION_ID_PARAM_PATH = "/{"+SESSION_ID_PARAM+"}";
    }

    interface HubSupportUrls {
        String EVENT_SINK_ROOT = "/event-sink";
        String HUB_SUPPORT_EVENT_SINK_RESOURCE = EVENT_SINK_ROOT + "/hub-support-hub-events";
    }

    interface PolicyUrls {
        String POLICY_ROOT = "/policy";

        String SESSION_RESOURCE_ROOT = POLICY_ROOT + "/session";

        String NEW_SESSION_RESOURCE = SESSION_RESOURCE_ROOT;

        String IDP_AUTHN_REQUEST_PATH = SharedUrls.SESSION_ID_PARAM_PATH + "/idp-authn-request-from-hub";
        String IDP_AUTHN_REQUEST_RESOURCE = SESSION_RESOURCE_ROOT + IDP_AUTHN_REQUEST_PATH;

        String IDP_AUTHN_RESPONSE_PATH = SharedUrls.SESSION_ID_PARAM_PATH + "/idp-authn-response";
        String IDP_AUTHN_RESPONSE_RESOURCE = SESSION_RESOURCE_ROOT + IDP_AUTHN_RESPONSE_PATH;

        String ATTRIBUTE_QUERY_RESPONSE_PATH = SharedUrls.SESSION_ID_PARAM_PATH + "/attribute-query-response";
        String ATTRIBUTE_QUERY_RESPONSE_RESOURCE = SESSION_RESOURCE_ROOT + ATTRIBUTE_QUERY_RESPONSE_PATH;

        String MATCHING_SERVICE_REQUEST_FAILURE_PATH = SharedUrls.SESSION_ID_PARAM_PATH + "/matching-service-request-failure";
        String MATCHING_SERVICE_REQUEST_FAILURE_RESOURCE = SESSION_RESOURCE_ROOT + MATCHING_SERVICE_REQUEST_FAILURE_PATH;

    }

    interface ConfigUrls {
        String CONFIG_ROOT = "/config";
        String ENTITY_ID_PATH_PARAM = "/{" + SharedUrls.ENTITY_ID_PARAM + "}";

        String CERTIFICATES_ROOT = CONFIG_ROOT + "/certificates";
        String SIGNATURE_VERIFICATION_CERTIFICATE_PATH = ENTITY_ID_PATH_PARAM + "/certs/signing";
        String ENCRYPTION_CERTIFICATE_PATH = ENTITY_ID_PATH_PARAM + "/certs/encryption";
        String ENCRYPTION_CERTIFICATES_RESOURCE = CERTIFICATES_ROOT + ENCRYPTION_CERTIFICATE_PATH;
        String SIGNATURE_VERIFICATION_CERTIFICATES_RESOURCE = CERTIFICATES_ROOT + SIGNATURE_VERIFICATION_CERTIFICATE_PATH;

        String MATCHING_SERVICE_ROOT = CONFIG_ROOT + "/matching-services";
        String MATCHING_SERVICE_PATH = ENTITY_ID_PATH_PARAM;
        String MATCHING_SERVICE_RESOURCE = MATCHING_SERVICE_ROOT + MATCHING_SERVICE_PATH;
        String ENABLED_MATCHING_SERVICES_RESOURCE = MATCHING_SERVICE_ROOT;
    }

    interface SamlSoapProxyUrls {
        String SAML_SOAP_PROXY_ROOT = "/saml-soap-proxy";
        String MATCHING_SERVICE_REQUEST_SENDER_RESOURCE = "/matching-service-request-sender";
        String MATCHING_SERVICE_HEALTH_CHECK_RESOURCE = SAML_SOAP_PROXY_ROOT + "/matching-service-health-check";
        String MATCHING_SERVICE_VERSION_CHECK_RESOURCE = SAML_SOAP_PROXY_ROOT + "/matching-service-version-check";
    }
}
