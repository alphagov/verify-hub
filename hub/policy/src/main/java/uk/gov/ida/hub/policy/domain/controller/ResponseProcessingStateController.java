package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.domain.ResponseProcessingDetails;
import uk.gov.ida.hub.policy.domain.StateController;

public interface ResponseProcessingStateController extends StateController {
    ResponseProcessingDetails getResponseProcessingDetails();
}
