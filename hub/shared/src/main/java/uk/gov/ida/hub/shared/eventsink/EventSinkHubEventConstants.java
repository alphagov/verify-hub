package uk.gov.ida.hub.shared.eventsink;

public interface EventSinkHubEventConstants {
    interface EventTypes {
        String SESSION_EVENT = "session_event";
        String ERROR_EVENT = "error_event";
        String HUB_EVENT = "hub_event";
        String EXTERNAL_COMMUNICATION_EVENT = "external_communication_event";
    }

    interface SessionEvents {
        String SESSION_STARTED = "session_started";
        String IDP_SELECTED = "idp_selected";
        String IDP_SELECTION_CANCELLED = "idp_selection_cancelled";
        String IDP_AUTHN_SUCCEEDED = "idp_authn_succeeded";
        String NO_AUTHN_CONTEXT = "no_authn_context";
        String IDP_AUTHN_FAILED = "idp_authn_failed";
        String IDP_AUTHN_PENDING = "idp_authn_pending";
        String CYCLE01_MATCH = "cycle01_match";
        String CYCLE01_NO_MATCH = "cycle01_no_match";
        String WAITING_FOR_CYCLE3_ATTRIBUTES = "waiting_for_cycle3_attributes";
        String CYCLE3_DATA_OBTAINED = "cycle3_data_obtained";
        String CYCLE3_MATCH = "cycle3_match";
        String CYCLE3_NO_MATCH = "cycle3_no_match";
        String FRAUD_DETECTED = "fraud_detected";
        String REQUESTER_ERROR = "requester_error";
        String CYCLE3_CANCEL = "cycle3_input_cancelled";
        String SESSION_TIMEOUT = "session_timeout";
        String USER_ACCOUNT_CREATION_REQUEST_SENT = "user_account_creation_request_sent";
        String USER_ACCOUNT_CREATED = "user_account_created";
        String USER_ACCOUNT_CREATION_FAILED = "user_account_creation_failed";
    }

    interface ExternalCommunicationsTypes {
        String AUTHN_REQUEST = "authn_request";
        String MATCHING_SERVICE_REQUEST = "matching_service_request";
        String RESPONSE_FROM_HUB = "response_from_hub";
    }

    interface HubEvents {
        String RECEIVED_AUTHN_REQUEST_FROM_HUB = "received_authn_request_from_hub";
    }
}
