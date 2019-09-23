package uk.gov.ida.hub.policy;

public interface Urls {

    String SESSION_REPO_TIMED_GROUP = "state-response-times";

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

        // Start of new saml-engine is a real microservice resources

        String GENERATE_RP_AUTHN_RESPONSE_RESOURCE = SAML_ENGINE_ROOT + "/generate-rp-authn-response";
        String GENERATE_RP_AUTHN_RESPONSE_WRAPPING_COUNTRY_RESPONSE_RESOURCE = SAML_ENGINE_ROOT + "/generate-rp-authn-response-wrapping-country-response";
        String GENERATE_RP_ERROR_RESPONSE_RESOURCE = SAML_ENGINE_ROOT + "/generate-rp-error-response";
        String TRANSLATE_RP_AUTHN_REQUEST_RESOURCE = SAML_ENGINE_ROOT + "/translate-rp-authn-request";

        String GENERATE_IDP_AUTHN_REQUEST_RESOURCE =   SAML_ENGINE_ROOT + "/generate-idp-authn-request";
        String TRANSLATE_IDP_AUTHN_RESPONSE_RESOURCE = SAML_ENGINE_ROOT + "/translate-idp-authn-response";

        String GENERATE_COUNTRY_AUTHN_REQUEST_RESOURCE =   SAML_ENGINE_ROOT + "/generate-country-authn-request";
        String TRANSLATE_COUNTRY_AUTHN_RESPONSE_RESOURCE = SAML_ENGINE_ROOT + "/translate-country-authn-response";

        String GENERATE_ATTRIBUTE_QUERY_RESOURCE = SAML_ENGINE_ROOT + "/generate-attribute-query";
        String GENERATE_COUNTRY_ATTRIBUTE_QUERY_RESOURCE = SAML_ENGINE_ROOT + "/generate-country-attribute-query";
        String TRANSLATE_MATCHING_SERVICE_RESPONSE_RESOURCE = SAML_ENGINE_ROOT + "/translate-attribute-query";
    }

    interface SharedUrls {
        String RELAY_STATE_PARAM = "RelayState";
        String LEVEL_OF_ASSURANCE_PARAM = "levelOfAssurance";
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
        String AUTHN_SESSION_ID_PATH = "/{sessionId}";
        String AUTHN_REQUEST_FROM_TRANSACTION_ROOT = POLICY_ROOT + "/received-authn-request";
        String RESPONSE_FROM_IDP_ROOT = AUTHN_REQUEST_FROM_TRANSACTION_ROOT + AUTHN_SESSION_ID_PATH + "/response-from-idp";

        // Start of new saml-engine is a real microservice resources

        String SESSION_RESOURCE_ROOT = POLICY_ROOT + "/session";
        String SESSION_RESOURCE = SESSION_RESOURCE_ROOT + SharedUrls.SESSION_ID_PARAM_PATH;

        String NEW_SESSION_RESOURCE = SESSION_RESOURCE_ROOT;

        String IDP_AUTHN_REQUEST_PATH = SharedUrls.SESSION_ID_PARAM_PATH + "/idp-authn-request-from-hub";
        String IDP_AUTHN_REQUEST_RESOURCE = SESSION_RESOURCE_ROOT + IDP_AUTHN_REQUEST_PATH;

        String IDP_AUTHN_RESPONSE_PATH = SharedUrls.SESSION_ID_PARAM_PATH + "/idp-authn-response";
        String IDP_AUTHN_RESPONSE_RESOURCE = SESSION_RESOURCE_ROOT + IDP_AUTHN_RESPONSE_PATH;

        String RP_AUTHN_RESPONSE_PATH = SharedUrls.SESSION_ID_PARAM_PATH + "/rp-response";
        String RP_AUTHN_RESPONSE_RESOURCE = SESSION_RESOURCE_ROOT + RP_AUTHN_RESPONSE_PATH;

        String RP_ERROR_RESPONSE_PATH = SharedUrls.SESSION_ID_PARAM_PATH + "/error-response";
        String RP_ERROR_RESPONSE_RESOURCE = SESSION_RESOURCE_ROOT + RP_ERROR_RESPONSE_PATH;

        String LOA_FOR_SESSION_PATH = "/loa" + SharedUrls.SESSION_ID_PARAM_PATH;
        String LOA_FOR_SESSION_RESOURCE = SESSION_RESOURCE_ROOT + LOA_FOR_SESSION_PATH;

        String ATTRIBUTE_QUERY_RESPONSE_PATH = SharedUrls.SESSION_ID_PARAM_PATH + "/attribute-query-response";
        String ATTRIBUTE_QUERY_RESPONSE_RESOURCE = SESSION_RESOURCE_ROOT + ATTRIBUTE_QUERY_RESPONSE_PATH;

        String MATCHING_SERVICE_REQUEST_FAILURE_PATH = SharedUrls.SESSION_ID_PARAM_PATH + "/matching-service-request-failure";
        String MATCHING_SERVICE_REQUEST_FAILURE_RESOURCE = SESSION_RESOURCE_ROOT + MATCHING_SERVICE_REQUEST_FAILURE_PATH;

        // End of new saml-engine is a real microservice resources

        String AUTHN_REQUEST_RESTART_JOURNEY_PATH = AUTHN_SESSION_ID_PATH + "/restart-journey";
        String AUTHN_REQUEST_TRY_ANOTHER_IDP_PATH = AUTHN_SESSION_ID_PATH + "/try-another-idp";

        String AUTHN_REQUEST_SELECT_IDP_PATH    = AUTHN_SESSION_ID_PATH + "/select-identity-provider";
        String AUTHN_REQUEST_SIGN_IN_PROCESS_DETAILS_PATH = AUTHN_SESSION_ID_PATH + "/sign-in-process-details";
        String AUTHN_REQUEST_ISSUER_ID_PATH = AUTHN_SESSION_ID_PATH + "/registration-request-issuer-id";

        String RESPONSE_PROCESSING_DETAILS_PATH = "/response-processing-details";
        String FAILURE_DETAILS_PATH = "/failure-details";
        String CYCLE_3_REQUEST_PATH             = AUTHN_SESSION_ID_PATH + "/cycle-3-attribute";
        String AUTHN_REQUEST_SIGN_IN_PROCESS_RESOURCE = AUTHN_REQUEST_FROM_TRANSACTION_ROOT + AUTHN_REQUEST_SIGN_IN_PROCESS_DETAILS_PATH;

        String AUTHN_REQUEST_SELECT_IDP_RESOURCE    = AUTHN_REQUEST_FROM_TRANSACTION_ROOT + AUTHN_REQUEST_SELECT_IDP_PATH;

        String RESPONSE_PROCESSING_DETAILS_RESOURCE = RESPONSE_FROM_IDP_ROOT              + RESPONSE_PROCESSING_DETAILS_PATH;
        String CYCLE_3_REQUEST_ROOT                 = AUTHN_REQUEST_FROM_TRANSACTION_ROOT + CYCLE_3_REQUEST_PATH;
        String CYCLE_3_REQUEST_RESOURCE             = CYCLE_3_REQUEST_ROOT;
        String CYCLE_3_CANCEL_PATH                  = "/cancel";
        String CYCLE_3_SUBMIT_PATH                  = "/submit";
        String CYCLE_3_CANCEL_RESOURCE              = AUTHN_REQUEST_FROM_TRANSACTION_ROOT + CYCLE_3_REQUEST_PATH + CYCLE_3_CANCEL_PATH;
        String CYCLE_3_SUBMIT_RESOURCE              = AUTHN_REQUEST_FROM_TRANSACTION_ROOT + CYCLE_3_REQUEST_PATH + CYCLE_3_SUBMIT_PATH;

        String COUNTRIES_RESOURCE = POLICY_ROOT + "/countries";
        String COUNTRY_SET_PATH_PARAM = "countryCode";
        String COUNTRY_SET_PATH = SharedUrls.SESSION_ID_PARAM_PATH + "/{" + COUNTRY_SET_PATH_PARAM + "}";

        String COUNTRY_AUTHN_RESPONSE_PATH = SharedUrls.SESSION_ID_PARAM_PATH + "/country-authn-response";
        String COUNTRY_AUTHN_RESPONSE_RESOURCE = SESSION_RESOURCE_ROOT + COUNTRY_AUTHN_RESPONSE_PATH;
        String EIDAS_SESSION_RESOURCE_ROOT = SESSION_RESOURCE_ROOT;
    }

    interface ConfigUrls {
        String CONFIG_ROOT = "/config";
        String ENTITY_ID_PATH_PARAM = "/{" + SharedUrls.ENTITY_ID_PARAM + "}";
        String LEVEL_OF_ASSURANCE_PATH_PARAM = "/{" + SharedUrls.LEVEL_OF_ASSURANCE_PARAM + "}";

        String IDENTITY_PROVIDER_ROOT = CONFIG_ROOT + "/idps";
        String ENABLED_IDENTITY_PROVIDERS_PATH = "/enabled";
        String ENABLED_ID_PROVIDERS_FOR_LOA_PATH = ENTITY_ID_PATH_PARAM + LEVEL_OF_ASSURANCE_PATH_PARAM + ENABLED_IDENTITY_PROVIDERS_PATH;
        String ENABLED_ID_PROVIDERS_FOR_LOA_RESOURCE = IDENTITY_PROVIDER_ROOT + ENABLED_ID_PROVIDERS_FOR_LOA_PATH;
        String ENABLED_ID_PROVIDERS_FOR_SIGN_IN_PATH = ENTITY_ID_PATH_PARAM + "/enabled-for-sign-in";
        String ENABLED_ID_PROVIDERS_FOR_SIGN_IN_RESOURCE = IDENTITY_PROVIDER_ROOT + ENABLED_ID_PROVIDERS_FOR_SIGN_IN_PATH;
        String IDP_CONFIG_DATA = ENTITY_ID_PATH_PARAM + "/display-data";
        String IDENTITY_PROVIDER_CONFIG_DATA_RESOURCE = IDENTITY_PROVIDER_ROOT + IDP_CONFIG_DATA;

        String TRANSACTIONS_ROOT = CONFIG_ROOT + "/transactions";
        String ASSERTION_CONSUMER_SERVICE_URI_PATH = ENTITY_ID_PATH_PARAM + "/assertion-consumer-service-uri";
        String ASSERTION_CONSUMER_SERVICE_INDEX_PARAM = "index";
        String TRANSACTIONS_ASSERTION_CONSUMER_SERVICE_URI_RESOURCE = TRANSACTIONS_ROOT + ASSERTION_CONSUMER_SERVICE_URI_PATH;
        String EIDAS_ENABLED_FOR_TRANSACTION_PATH = ENTITY_ID_PATH_PARAM + "/eidas-enabled";
        String EIDAS_COUNTRIES_FOR_TRANSACTION_PATH = ENTITY_ID_PATH_PARAM + "/eidas-countries";
        String MATCHING_PROCESS_PATH = ENTITY_ID_PATH_PARAM + "/matching-process";
        String MATCHING_PROCESS_RESOURCE = TRANSACTIONS_ROOT + MATCHING_PROCESS_PATH;
        String LEVELS_OF_ASSURANCE_PATH = ENTITY_ID_PATH_PARAM + "/levels-of-assurance";
        String LEVELS_OF_ASSURANCE_RESOURCE = TRANSACTIONS_ROOT + LEVELS_OF_ASSURANCE_PATH;
        String MATCHING_SERVICE_ENTITY_ID_PATH = ENTITY_ID_PATH_PARAM + "/matching-service-entity-id";
        String USER_ACCOUNT_CREATION_ATTRIBUTES_PATH = ENTITY_ID_PATH_PARAM + "/user-account-creation-attributes";

        String MATCHING_SERVICE_ENTITY_ID_RESOURCE = TRANSACTIONS_ROOT + MATCHING_SERVICE_ENTITY_ID_PATH;
        String USER_ACCOUNT_CREATION_ATTRIBUTES_RESOURCE = TRANSACTIONS_ROOT + USER_ACCOUNT_CREATION_ATTRIBUTES_PATH;

        String EIDAS_ENABLED_FOR_TRANSACTION_RESOURCE = TRANSACTIONS_ROOT + EIDAS_ENABLED_FOR_TRANSACTION_PATH;
        String EIDAS_RP_COUNTRIES_FOR_TRANSACTION_RESOURCE = TRANSACTIONS_ROOT + EIDAS_COUNTRIES_FOR_TRANSACTION_PATH;
        String MATCHING_SERVICE_ROOT = CONFIG_ROOT + "/matching-services";
        String MATCHING_SERVICE_PATH = ENTITY_ID_PATH_PARAM;
        String MATCHING_SERVICE_RESOURCE = MATCHING_SERVICE_ROOT + MATCHING_SERVICE_PATH;
        String ENABLED_MATCHING_SERVICES_RESOURCE = MATCHING_SERVICE_ROOT;

        String COUNTRIES_ROOT = CONFIG_ROOT + "/countries";
        String MATCHING_ENABLED_FOR_TRANSACTION_PATH = ENTITY_ID_PATH_PARAM + "/matching-enabled";
        String MATCHING_ENABLED_FOR_TRANSACTION_RESOURCE = TRANSACTIONS_ROOT + MATCHING_ENABLED_FOR_TRANSACTION_PATH;
    }

    interface FrontendUrls {
        String SAML2_SSO_ROOT = "/SAML2/SSO";
        String SAML2_SSO_RESPONSE_ENDPOINT = SAML2_SSO_ROOT + "/Response/POST";
        String SAML2_SSO_EIDAS_RESPONSE_ENDPOINT = SAML2_SSO_ROOT + "/EidasResponse/POST";
    }

    interface SamlSoapProxyUrls {
        String MATCHING_SERVICE_REQUEST_SENDER_RESOURCE = "/matching-service-request-sender";
    }
}