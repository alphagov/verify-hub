package uk.gov.ida.hub.config.domain.builders;

import java.util.Collections;
import java.util.List;

import uk.gov.ida.hub.config.domain.TranslationData;

public class TranslationDataBuilder {
    private String simpleId = "default-transaction-simple-id";
    private List<TranslationData.Translation> translations = Collections.singletonList(new TestTranslationData.TestTranslation()
            .setLocale("en")
            .setName("Test RP Profile")
            .setRpName("Test RP")
            .setAnalyticsDescription("Test RP analytics description")
            .setOtherWaysDescription("Test RP other ways description")
            .setOtherWaysText("Test RP other ways text")
            .setIdpDisconnectedAlternativeHtml("Test RP alternative methods when IDP has been disconnected")
            .setTailoredText("Test RP tailored text")
            .setTaxonName("Test RP")
    );

    public static TranslationDataBuilder aTranslationData() {
        return new TranslationDataBuilder();
    }

    public TranslationData build() {
        return new TestTranslationData(simpleId, translations);
    }

    public TranslationDataBuilder withSimpleId(String simpleId) {
        this.simpleId = simpleId;
        return this;
    }

    public TranslationDataBuilder withTranslations(TranslationData.Translation translation) {
        this.translations.add(translation);
        return this;
    }

    private static class TestTranslationData extends TranslationData {
        private TestTranslationData(String simpleId, List<TranslationData.Translation> translations) {
            this.simpleId = simpleId;
            this.translations = translations;
        }

        private static class TestTranslation extends TestTranslationData.Translation {}
    }
}
