package uk.gov.ida.hub.policy.services;

import com.google.inject.Inject;
import uk.gov.ida.hub.policy.contracts.AbstractAttributeQueryRequestDto;
import uk.gov.ida.hub.policy.domain.Cycle3AttributeRequestData;
import uk.gov.ida.hub.policy.domain.Cycle3Dataset;
import uk.gov.ida.hub.policy.domain.Cycle3UserInput;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.SessionRepository;
import uk.gov.ida.hub.policy.domain.controller.AbstractAwaitingCycle3DataStateController;
import uk.gov.ida.hub.policy.domain.state.AbstractAwaitingCycle3DataState;

public class Cycle3Service {
    private final SessionRepository sessionRepository;
    private final AttributeQueryService attributeQueryService;

    @Inject
    public Cycle3Service(SessionRepository sessionRepository, AttributeQueryService attributeQueryService) {
        this.sessionRepository = sessionRepository;
        this.attributeQueryService = attributeQueryService;
    }

    public void sendCycle3MatchingRequest(SessionId sessionId, Cycle3UserInput cycle3UserInput) {
        AbstractAwaitingCycle3DataStateController controller = (AbstractAwaitingCycle3DataStateController) sessionRepository.getStateController(sessionId, AbstractAwaitingCycle3DataState.class);
        String attributeName = controller.getCycle3AttributeRequestData().getAttributeName();
        Cycle3Dataset cycle3Dataset = Cycle3Dataset.createFromData(attributeName, cycle3UserInput.getCycle3Input());
        AbstractAttributeQueryRequestDto attributeQuery = controller.createAttributeQuery(cycle3Dataset);
        attributeQueryService.sendAttributeQueryRequest(sessionId, attributeQuery);
        controller.handleCycle3DataSubmitted(cycle3UserInput.getPrincipalIpAddress());
    }

    public void cancelCycle3DataInput(SessionId sessionId) {
        AbstractAwaitingCycle3DataStateController controller = (AbstractAwaitingCycle3DataStateController) sessionRepository.getStateController(sessionId, AbstractAwaitingCycle3DataState.class);
        controller.handleCancellation();
    }

    public Cycle3AttributeRequestData getCycle3AttributeRequestData(SessionId sessionId) {
        AbstractAwaitingCycle3DataStateController controller = (AbstractAwaitingCycle3DataStateController) sessionRepository.getStateController(sessionId, AbstractAwaitingCycle3DataState.class);
        return controller.getCycle3AttributeRequestData();
    }
}
