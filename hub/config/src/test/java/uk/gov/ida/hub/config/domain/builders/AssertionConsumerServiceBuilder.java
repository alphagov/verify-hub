package uk.gov.ida.hub.config.domain.builders;

import com.fasterxml.jackson.annotation.JsonIgnore;
import uk.gov.ida.hub.config.domain.AssertionConsumerService;

import java.net.URI;

public class AssertionConsumerServiceBuilder {

    private boolean isDefault = true;
    private URI uri = URI.create("http://default-uri");
    private Integer index;

    public static AssertionConsumerServiceBuilder anAssertionConsumerService() {
        return new AssertionConsumerServiceBuilder();
    }

    public AssertionConsumerService build() {
        return new TestAssertionConsumerService(
                uri,
                index,
                isDefault);
    }

    public AssertionConsumerServiceBuilder isDefault(boolean isDefault) {
        this.isDefault = isDefault;
        return this;
    }

    public AssertionConsumerServiceBuilder withUri(URI uri) {
        this.uri = uri;
        return this;
    }

    public AssertionConsumerServiceBuilder withIndex(int index) {
        this.index = index;
        return this;
    }

    private static class TestAssertionConsumerService extends AssertionConsumerService {

        @Override
        @JsonIgnore
        public boolean isIndexAbsentOrUnsigned() {
            return super.isIndexAbsentOrUnsigned();
        }

        @Override
        @JsonIgnore
        public Boolean getDefault() {
            return super.getDefault();
        }

        @Override
        @JsonIgnore
        public boolean isUriValid() {
            return super.isUriValid();
        }

        private TestAssertionConsumerService(
                URI uri,
                Integer index,
                boolean isDefault) {

            this.uri = uri;
            this.index = index;
            this.isDefault = isDefault;
        }
    }
}
