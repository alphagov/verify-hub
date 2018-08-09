package uk.gov.ida.hub.config.dto;

import uk.gov.ida.hub.config.domain.LevelOfAssurance;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public class TransactionDetailedDisplayData {
    private String simpleId;
    private URI serviceHomepage;
    private List<LevelOfAssurance> loaList;
    private String entityId;

    // Needed by jaxb for integration test :(
    protected TransactionDetailedDisplayData() {
    }

    public TransactionDetailedDisplayData(String simpleId,
                                          URI serviceHomepage,
                                          List<LevelOfAssurance> loaList,
                                          String entityId) {

        this.simpleId = simpleId;
        this.serviceHomepage = serviceHomepage;
        this.loaList = loaList;
        this.entityId = entityId;
    }

    public URI getServiceHomepage() { return serviceHomepage; }

    public Optional<String> getSimpleId() {
        return Optional.ofNullable(simpleId);
    }

    public List<LevelOfAssurance> getLoaList() {
        return loaList;
    }

    public String getEntityId() { return entityId; }
}
