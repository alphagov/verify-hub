package uk.gov.ida.hub.policy.domain;

import org.joda.time.DateTime;

import java.util.List;

public class EidasAuthnRequestFromHub {

    private String id;
    private List<LevelOfAssurance> levelsOfAssurance;
    private String recipientEntityId;
    private DateTime sessionExpiryTimestamp;

    @SuppressWarnings("unused")//Needed by JAXB
    private EidasAuthnRequestFromHub() {
    }

    public EidasAuthnRequestFromHub(
            String id,
            List<LevelOfAssurance> levelsOfAssurance,
            String recipientEntityId,
            DateTime sessionExpiryTimestamp) {

        this.id = id;
        this.levelsOfAssurance = levelsOfAssurance;
        this.recipientEntityId = recipientEntityId;
        this.sessionExpiryTimestamp = sessionExpiryTimestamp;
    }

    public String getId() {
        return id;
    }

    public String getRecipientEntityId() {
        return recipientEntityId;
    }

    public DateTime getSessionExpiryTimestamp() {
        return sessionExpiryTimestamp;
    }

    public List<LevelOfAssurance> getLevelsOfAssurance() {
        return levelsOfAssurance;
    }
}
