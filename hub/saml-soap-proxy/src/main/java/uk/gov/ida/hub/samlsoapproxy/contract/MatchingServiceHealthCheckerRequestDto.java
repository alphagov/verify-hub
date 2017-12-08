package uk.gov.ida.hub.samlsoapproxy.contract;

public class MatchingServiceHealthCheckerRequestDto {

    private String transactionEntityId;
    private String matchingServiceEntityId;

    private MatchingServiceHealthCheckerRequestDto() {}

    public MatchingServiceHealthCheckerRequestDto(String transactionEntityId, String matchingServiceEntityId) {
        this.transactionEntityId = transactionEntityId;
        this.matchingServiceEntityId = matchingServiceEntityId;
    }

    public String getMatchingServiceEntityId() {
        return matchingServiceEntityId;
    }

    public String getTransactionEntityId() {
        return transactionEntityId;
    }
}
