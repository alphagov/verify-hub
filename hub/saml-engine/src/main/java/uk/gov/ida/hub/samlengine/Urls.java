package uk.gov.ida.hub.samlengine;

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

        // Start of new saml-engine is a real microservice resources

        String GENERATE_RP_AUTHN_RESPONSE_RESOURCE = SAML_ENGINE_ROOT + "/generate-rp-authn-response";
        String GENERATE_RP_ERROR_RESPONSE_RESOURCE = SAML_ENGINE_ROOT + "/generate-rp-error-response";
        String TRANSLATE_RP_AUTHN_REQUEST_RESOURCE = SAML_ENGINE_ROOT + "/translate-rp-authn-request";

        String GENERATE_IDP_AUTHN_REQUEST_RESOURCE =   SAML_ENGINE_ROOT + "/generate-idp-authn-request";
        String TRANSLATE_IDP_AUTHN_RESPONSE_RESOURCE = SAML_ENGINE_ROOT + "/translate-idp-authn-response";

        String GENERATE_COUNTRY_AUTHN_REQUEST_RESOURCE =   SAML_ENGINE_ROOT + "/generate-country-authn-request";
        String TRANSLATE_COUNTRY_AUTHN_RESPONSE_RESOURCE = SAML_ENGINE_ROOT + "/translate-country-authn-response";

        String GENERATE_ATTRIBUTE_QUERY_RESOURCE = SAML_ENGINE_ROOT + "/generate-attribute-query";
        String GENERATE_COUNTRY_ATTRIBUTE_QUERY_RESOURCE = SAML_ENGINE_ROOT + "/generate-country-attribute-query";
        String TRANSLATE_MATCHING_SERVICE_RESPONSE_RESOURCE = SAML_ENGINE_ROOT + "/translate-attribute-query";

        String GENERATE_MSA_HEALTHCHECK_ATTRIBUTE_QUERY_RESOURCE = SAML_ENGINE_ROOT + "/generate-msa-healthcheck-attribute-query";
        String TRANSLATE_MSA_HEALTHCHECK_ATTRIBUTE_QUERY_RESPONSE_RESOURCE = SAML_ENGINE_ROOT + "/translate-msa-healthcheck-attribute-query";

        // End of new saml-engine is a real microservice resources

    }

    interface SharedUrls {
        String ENTITY_ID_PARAM = "entityId";
    }

    interface FrontendUrls {
        String SAML2_SSO_ROOT = "/SAML2/SSO";
        String SAML2_SSO_RESPONSE_ENDPOINT = SAML2_SSO_ROOT + "/Response/POST";
        String SAML2_SSO_EIDAS_RESPONSE_ENDPOINT = SAML2_SSO_ROOT + "/EidasResponse/POST";
    }

    interface ConfigUrls {
        String CONFIG_ROOT = "/config";
        String ENTITY_ID_PATH_PARAM = "/{" + SharedUrls.ENTITY_ID_PARAM + "}";

        String CERTIFICATES_ROOT = CONFIG_ROOT + "/certificates";
        String SIGNATURE_VERIFICATION_CERTIFICATE_PATH = ENTITY_ID_PATH_PARAM + "/certs/signing";
        String ENCRYPTION_CERTIFICATE_PATH = ENTITY_ID_PATH_PARAM + "/certs/encryption";
        String ENCRYPTION_CERTIFICATES_RESOURCE = CERTIFICATES_ROOT + ENCRYPTION_CERTIFICATE_PATH;
        String SIGNATURE_VERIFICATION_CERTIFICATES_RESOURCE = CERTIFICATES_ROOT + SIGNATURE_VERIFICATION_CERTIFICATE_PATH;

        String TRANSACTIONS_ROOT = CONFIG_ROOT + "/transactions";

        String SHOULD_HUB_SIGN_RESPONSE_MESSAGES_PATH = ENTITY_ID_PATH_PARAM + "/should-hub-sign-response-messages";
        String SHOULD_HUB_USE_LEGACY_SAML_STANDARD_PATH = ENTITY_ID_PATH_PARAM + "/should-hub-use-legacy-saml-standard";
        String SHOULD_SIGN_WITH_SHA1_PATH = ENTITY_ID_PATH_PARAM + "/should-sign-with-sha1";

        String MATCHING_SERVICE_ROOT = CONFIG_ROOT + "/matching-services";
        String MATCHING_SERVICE_PATH = ENTITY_ID_PATH_PARAM;
        String MATCHING_SERVICE_RESOURCE = MATCHING_SERVICE_ROOT + MATCHING_SERVICE_PATH;

        String SHOULD_HUB_SIGN_RESPONSE_MESSAGES_RESOURCE = TRANSACTIONS_ROOT + SHOULD_HUB_SIGN_RESPONSE_MESSAGES_PATH;
        String SHOULD_HUB_USE_LEGACY_SAML_STANDARD_RESOURCE = TRANSACTIONS_ROOT + SHOULD_HUB_USE_LEGACY_SAML_STANDARD_PATH;
        String SHOULD_SIGN_WITH_SHA1_RESOURCE = TRANSACTIONS_ROOT + SHOULD_SIGN_WITH_SHA1_PATH;
    }
}
