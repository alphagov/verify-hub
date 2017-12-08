package uk.gov.ida.hub.samlproxy;

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
    interface SamlProxyUrls {
        String METADATA_API_ROOT = "/API/metadata";
        String SP_METADATA_PATH = "/sp";
        String IDP_METADATA_PATH = "/idp";

        String SAML2_SSO_SENDER_API_ROOT = "/SAML2/SSO/API/SENDER";
        String SAML2_SSO_RECEIVER_API_ROOT = "/SAML2/SSO/API/RECEIVER";
        String RESPONSE_POST_PATH = "/Response/POST";
        String SAML2_SSO_RECEIVER_API_RESOURCE = SAML2_SSO_RECEIVER_API_ROOT + RESPONSE_POST_PATH;
        String EIDAS_RESPONSE_POST_PATH = "/EidasResponse/POST";
        String EIDAS_SAML2_SSO_RECEIVER_API_RESOURCE = SAML2_SSO_RECEIVER_API_ROOT + EIDAS_RESPONSE_POST_PATH;
        String SEND_AUTHN_REQUEST_PATH = "/AUTHN_REQ";
        String SEND_AUTHN_REQUEST_API_RESOURCE = SAML2_SSO_SENDER_API_ROOT + SEND_AUTHN_REQUEST_PATH;
        String SEND_RESPONSE_FROM_HUB_PATH = "/RESPONSE";
        String SEND_RESPONSE_FROM_HUB_API_RESOURCE = SAML2_SSO_SENDER_API_ROOT + SEND_RESPONSE_FROM_HUB_PATH;
        String SEND_ERROR_RESPONSE_FROM_HUB_PATH = "/ERROR_RESPONSE";
        String SEND_ERROR_RESPONSE_FROM_HUB_API_RESOURCE = SAML2_SSO_SENDER_API_ROOT + SEND_ERROR_RESPONSE_FROM_HUB_PATH;
    }

    interface SharedUrls {
        String RELAY_STATE_PARAM = "RelayState";
        String SAML_REQUEST_PARAM = "SAMLRequest";
        String ENTITY_ID_PARAM = "entityId";
        String SESSION_ID_PARAM = "sessionId";
        String SESSION_ID_PARAM_PATH = "/{"+SESSION_ID_PARAM+"}";
        String SERVICE_NAME_ROOT = "/service-name";
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

        String RP_AUTHN_RESPONSE_PATH = SharedUrls.SESSION_ID_PARAM_PATH + "/rp-response";
        String RP_AUTHN_RESPONSE_RESOURCE = SESSION_RESOURCE_ROOT + RP_AUTHN_RESPONSE_PATH;

        String RP_ERROR_RESPONSE_PATH = SharedUrls.SESSION_ID_PARAM_PATH + "/error-response";
        String RP_ERROR_RESPONSE_RESOURCE = SESSION_RESOURCE_ROOT + RP_ERROR_RESPONSE_PATH;

        String COUNTRY_AUTHN_RESPONSE_PATH = SharedUrls.SESSION_ID_PARAM_PATH + "/country-authn-response";
        String COUNTRY_AUTHN_RESPONSE_RESOURCE = SESSION_RESOURCE_ROOT + COUNTRY_AUTHN_RESPONSE_PATH;
    }

    interface FrontendUrls {
        String SAML2_SSO_ROOT = "/SAML2/SSO";
        String SAML2_SSO_REQUEST_ENDPOINT = SAML2_SSO_ROOT;
    }

    interface ConfigUrls {
        String CONFIG_ROOT = "/config";
        String ENTITY_ID_PATH_PARAM = "/{" + SharedUrls.ENTITY_ID_PARAM + "}";

        String CERTIFICATES_ROOT = CONFIG_ROOT + "/certificates";
        String SIGNATURE_VERIFICATION_CERTIFICATE_PATH = ENTITY_ID_PATH_PARAM + "/certs/signing";
        String ENCRYPTION_CERTIFICATE_PATH = ENTITY_ID_PATH_PARAM + "/certs/encryption";
        String ENCRYPTION_CERTIFICATES_RESOURCE = CERTIFICATES_ROOT + ENCRYPTION_CERTIFICATE_PATH;
        String SIGNATURE_VERIFICATION_CERTIFICATES_RESOURCE = CERTIFICATES_ROOT + SIGNATURE_VERIFICATION_CERTIFICATE_PATH;
    }
}
