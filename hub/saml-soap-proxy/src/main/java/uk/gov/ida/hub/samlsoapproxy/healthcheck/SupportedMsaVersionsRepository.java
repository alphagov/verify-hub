package uk.gov.ida.hub.samlsoapproxy.healthcheck;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class SupportedMsaVersionsRepository {

    @Inject
    public SupportedMsaVersionsRepository() {
    }

    private List<String> supportedMsaVersions = new ArrayList<>();

    public void add(final List<String> supportedMsaVersions) {
        this.supportedMsaVersions.addAll(supportedMsaVersions);
    }

    public List<String> getSupportedVersions() {
        return supportedMsaVersions;
    }
}
