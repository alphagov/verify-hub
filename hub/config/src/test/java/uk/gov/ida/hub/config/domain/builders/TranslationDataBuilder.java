package uk.gov.ida.hub.config.domain.builders;

import uk.gov.ida.hub.config.domain.TranslationData;

public class TranslationDataBuilder {
    private String simpleId = "default-transaction-simple-id";

    public static TranslationDataBuilder aTranslationData() {
        return new TranslationDataBuilder();
    }

    public TranslationData build() {
        return new TestTranslationData(simpleId);
    }

    public TranslationDataBuilder withSimpleId(String simpleId) {
        this.simpleId = simpleId;
        return this;
    }

    private static class TestTranslationData extends TranslationData {
        private TestTranslationData(String simpleId) {
            this.simpleId = simpleId;
        }
    }
}
