package uk.gov.ida.hub.samlsoapproxy.healthcheck;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

public class SupportedMsaVersions {

    @SuppressWarnings("unused") // needed to prevent guice injection
    protected SupportedMsaVersions() {
    }

    @Valid
    @NotNull
    protected List<String> versions;

    public List<String> getVersions() {
        return versions;
    }
}
