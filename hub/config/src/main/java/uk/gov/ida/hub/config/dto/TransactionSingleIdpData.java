package uk.gov.ida.hub.config.dto;

import uk.gov.ida.hub.config.domain.LevelOfAssurance;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public class TransactionSingleIdpData {
    private String simpleId;
    private URI redirectUrl;
    private List<LevelOfAssurance> loaList;
    private String entityId;

    // Needed by jaxb for integration test :(
    protected TransactionSingleIdpData() {
    }

    public TransactionSingleIdpData(String simpleId,
                                    URI redirectUrl,
                                    List<LevelOfAssurance> loaList,
                                    String entityId) {

        this.simpleId = simpleId;
        this.redirectUrl = redirectUrl;
        this.loaList = loaList;
        this.entityId = entityId;
    }

    public URI getRedirectUrl() { return redirectUrl; }

    public Optional<String> getSimpleId() {
        return Optional.ofNullable(simpleId);
    }

    public List<LevelOfAssurance> getLoaList() {
        return loaList;
    }

    public String getEntityId() { return entityId; }
}
