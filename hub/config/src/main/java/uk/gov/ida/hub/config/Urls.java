package uk.gov.ida.hub.config;

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
    interface SharedUrls {
        String LEVEL_OF_ASSURANCE_PARAM = "levelOfAssurance";
        String TRANSACTION_ENTITY_ID_PARAM = "transactionEntityId";
        String ENTITY_ID_PARAM = "entityId";
        String SIMPLE_ID_PARAM = "simpleId";
        String LOCALE_PARAM = "locale";
    }

    interface ConfigUrls {
        String CONFIG_ROOT = "/config";
        String ENTITY_ID_PATH_PARAM = "/{" + SharedUrls.ENTITY_ID_PARAM + "}";
        String LEVEL_OF_ASSURANCE_PATH_PARAM = "/{" + SharedUrls.LEVEL_OF_ASSURANCE_PARAM + "}";
        String TRANSACTION_ENTITY_ID_PARAM_PATH = "/{" + SharedUrls.TRANSACTION_ENTITY_ID_PARAM + "}";
        String TRANSLATION_SIMPLE_ID_PATH_PARAM = "/{" + SharedUrls.SIMPLE_ID_PARAM + "}";
        String TRANSLATION_LOCALE_PATH_PARAM = "/{" + SharedUrls.LOCALE_PARAM + "}";

        String CERTIFICATES_ROOT = CONFIG_ROOT + "/certificates";
        String SIGNATURE_VERIFICATION_CERTIFICATE_PATH = ENTITY_ID_PATH_PARAM + "/certs/signing";
        String ENCRYPTION_CERTIFICATE_PATH = ENTITY_ID_PATH_PARAM + "/certs/encryption";
        String ENCRYPTION_CERTIFICATES_RESOURCE = CERTIFICATES_ROOT + ENCRYPTION_CERTIFICATE_PATH;
        String SIGNATURE_VERIFICATION_CERTIFICATES_RESOURCE = CERTIFICATES_ROOT + SIGNATURE_VERIFICATION_CERTIFICATE_PATH;
        String CERTIFICATES_HEALTH_CHECK_PATH = "/health-check";
        String CERTIFICATES_HEALTH_CHECK_RESOURCE = CERTIFICATES_ROOT + CERTIFICATES_HEALTH_CHECK_PATH;
        String INVALID_CERTIFICATES_CHECK_PATH = "/invalid";
        String INVALID_CERTIFICATES_CHECK_RESOURCE = CERTIFICATES_ROOT + INVALID_CERTIFICATES_CHECK_PATH;
        String IDENTITY_PROVIDER_ROOT = CONFIG_ROOT + "/idps";
        String ENABLED_IDENTITY_PROVIDERS_PATH = "/enabled";
        String ENABLED_IDENTITY_PROVIDERS_RESOURCE = IDENTITY_PROVIDER_ROOT + ENABLED_IDENTITY_PROVIDERS_PATH;
        @Deprecated String ENABLED_IDENTITY_PROVIDERS_PARAM_PATH = ENTITY_ID_PATH_PARAM + "/enabled";
        @Deprecated String ENABLED_IDENTITY_PROVIDERS_PARAM_PATH_RESOURCE = IDENTITY_PROVIDER_ROOT + ENABLED_IDENTITY_PROVIDERS_PARAM_PATH;
        String ENABLED_ID_PROVIDERS_FOR_REGISTRATION_AUTHN_REQUEST_PATH = ENTITY_ID_PATH_PARAM + LEVEL_OF_ASSURANCE_PATH_PARAM + "/registration-authn-request" + ENABLED_IDENTITY_PROVIDERS_PATH;
        String ENABLED_ID_PROVIDERS_FOR_REGISTRATION_AUTHN_REQUEST_RESOURCE = IDENTITY_PROVIDER_ROOT + ENABLED_ID_PROVIDERS_FOR_REGISTRATION_AUTHN_REQUEST_PATH;
        String ENABLED_ID_PROVIDERS_FOR_REGISTRATION_AUTHN_RESPONSE_PATH = ENTITY_ID_PATH_PARAM + LEVEL_OF_ASSURANCE_PATH_PARAM + "/registration-authn-response" + ENABLED_IDENTITY_PROVIDERS_PATH;
        String ENABLED_ID_PROVIDERS_FOR_REGISTRATION_AUTHN_RESPONSE_RESOURCE = IDENTITY_PROVIDER_ROOT + ENABLED_ID_PROVIDERS_FOR_REGISTRATION_AUTHN_RESPONSE_PATH;
        String ENABLED_ID_PROVIDERS_FOR_SIGN_IN_PATH = ENTITY_ID_PATH_PARAM + "/enabled-for-sign-in";
        String ENABLED_ID_PROVIDERS_FOR_SIGN_IN_RESOURCE = IDENTITY_PROVIDER_ROOT + ENABLED_ID_PROVIDERS_FOR_SIGN_IN_PATH;

        String IDP_CONFIG_DATA = ENTITY_ID_PATH_PARAM + "/display-data";
        String IDP_CONFIG_DATA_RESOURCE = IDENTITY_PROVIDER_ROOT + IDP_CONFIG_DATA;

        // Registration
        String IDP_LIST_FOR_REGISTRATION = "/idp-list-for-registration";
        String IDP_LIST_FOR_REGISTRATION_PATH = IDP_LIST_FOR_REGISTRATION + TRANSACTION_ENTITY_ID_PARAM_PATH + LEVEL_OF_ASSURANCE_PATH_PARAM;
        String IDP_LIST_FOR_REGISTRATION_RESOURCE = IDENTITY_PROVIDER_ROOT + IDP_LIST_FOR_REGISTRATION_PATH;
        String DISCONNECTED_IDP_LIST_FOR_REGISTRATION_PATH = IDP_LIST_FOR_REGISTRATION + TRANSACTION_ENTITY_ID_PARAM_PATH + LEVEL_OF_ASSURANCE_PATH_PARAM + "/disconnected";
        String DISCONNECTED_IDP_LIST_FOR_REGISTRATION_PATH_RESOURCE = IDENTITY_PROVIDER_ROOT + DISCONNECTED_IDP_LIST_FOR_REGISTRATION_PATH;

        // Sign-in
        String IDP_LIST_FOR_SIGN_IN_PATH = "/idp-list-for-sign-in" + TRANSACTION_ENTITY_ID_PARAM_PATH;
        String IDP_LIST_FOR_SIGN_IN_RESOURCE = IDENTITY_PROVIDER_ROOT + IDP_LIST_FOR_SIGN_IN_PATH;

        // Single IDP
        String IDP_LIST_FOR_SINGLE_IDP_PATH = "/idp-list-for-single-idp" + TRANSACTION_ENTITY_ID_PARAM_PATH;
        String IDP_LIST_FOR_SINGLE_IDP_RESOURCE = IDENTITY_PROVIDER_ROOT + IDP_LIST_FOR_SINGLE_IDP_PATH;

        String TRANSACTIONS_ROOT = CONFIG_ROOT + "/transactions";
        String ASSERTION_CONSUMER_SERVICE_URI_PATH = ENTITY_ID_PATH_PARAM + "/assertion-consumer-service-uri";
        String ASSERTION_CONSUMER_SERVICE_INDEX_PARAM = "index";
        String TRANSACTIONS_ASSERTION_CONSUMER_SERVICE_URI_RESOURCE = TRANSACTIONS_ROOT + ASSERTION_CONSUMER_SERVICE_URI_PATH;
        String TRANSACTION_DISPLAY_DATA_PATH = ENTITY_ID_PATH_PARAM + "/display-data";
        String TRANSACTION_DISPLAY_DATA_RESOURCE = TRANSACTIONS_ROOT + TRANSACTION_DISPLAY_DATA_PATH;
        String ENABLED_TRANSACTIONS_PATH = "/enabled";
        String SINGLE_IDP_ENABLED_LIST_PATH = "/single-idp-enabled-list";
        String TRANSLATIONS_PATH = TRANSLATION_SIMPLE_ID_PATH_PARAM + "/translations";
        String TRANSLATIONS_LOCALE_PATH = TRANSLATIONS_PATH + TRANSLATION_LOCALE_PATH_PARAM;
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
        String SHOULD_HUB_SIGN_RESPONSE_MESSAGES_PATH = ENTITY_ID_PATH_PARAM + "/should-hub-sign-response-messages";
        String SHOULD_HUB_USE_LEGACY_SAML_STANDARD_PATH = ENTITY_ID_PATH_PARAM + "/should-hub-use-legacy-saml-standard";

        String EIDAS_ENABLED_FOR_TRANSACTION_RESOURCE = TRANSACTIONS_ROOT + EIDAS_ENABLED_FOR_TRANSACTION_PATH;
        String SHOULD_HUB_SIGN_RESPONSE_MESSAGES_RESOURCE = TRANSACTIONS_ROOT + SHOULD_HUB_SIGN_RESPONSE_MESSAGES_PATH;
        String SHOULD_HUB_USE_LEGACY_SAML_STANDARD_RESOURCE = TRANSACTIONS_ROOT + SHOULD_HUB_USE_LEGACY_SAML_STANDARD_PATH;
        String MATCHING_SERVICE_ROOT = CONFIG_ROOT + "/matching-services";
        String MATCHING_SERVICE_PATH = ENTITY_ID_PATH_PARAM;
        String MATCHING_SERVICE_RESOURCE = MATCHING_SERVICE_ROOT + MATCHING_SERVICE_PATH;

        String COUNTRIES_ROOT = CONFIG_ROOT + "/countries";
        String MATCHING_ENABLED_FOR_TRANSACTION_PATH = ENTITY_ID_PATH_PARAM + "/matching-enabled";
        String MATCHING_ENABLED_FOR_TRANSACTION_RESOURCE = TRANSACTIONS_ROOT + MATCHING_ENABLED_FOR_TRANSACTION_PATH;
        String IS_AN_EIDAS_PROXY_NODE_FOR_TRANSACTION_PATH = ENTITY_ID_PATH_PARAM + "/is-eidas-proxy-node";
        String IS_AN_EIDAS_PROXY_NODE_FOR_TRANSACTION_RESOURCE = TRANSACTIONS_ROOT + IS_AN_EIDAS_PROXY_NODE_FOR_TRANSACTION_PATH;
    }
}
