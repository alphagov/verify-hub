package uk.gov.ida.hub.config.dto;

import uk.gov.ida.hub.config.domain.LevelOfAssurance;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public class TransactionDisplayData {
    private String simpleId;
    private URI serviceHomepage;
    private List<LevelOfAssurance> loaList;

    // Needed by jaxb for integration test :(
    protected TransactionDisplayData() {
    }

    public TransactionDisplayData(String simpleId,
                                  URI serviceHomepage,
                                  List<LevelOfAssurance> loaList) {

        this.simpleId = simpleId;
        this.serviceHomepage = serviceHomepage;
        this.loaList = loaList;
    }

    public URI getServiceHomepage() { return serviceHomepage; }

    public Optional<String> getSimpleId() {
        return Optional.ofNullable(simpleId);
    }

    public List<LevelOfAssurance> getLoaList() {
        return loaList;
    }
}
