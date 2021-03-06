package uk.gov.ida.hub.samlsoapproxy.healthcheck;

import io.dropwizard.lifecycle.Managed;
import uk.gov.ida.hub.samlsoapproxy.exceptions.SupportedMsaVersionsFileAccessException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.URL;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;

@Singleton
public class SupportedMsaVersionsBootstrap implements Managed {
    private static final String SUPPORTED_MSA_VERSIONS_FILE_NAME = "/supported-msa-versions.yml";
    private static final Logger LOG = Logger.getLogger(SupportedMsaVersionsBootstrap.class.getName());

    private final SupportedMsaVersionsRepository supportedMsaVersionsRepository;
    private final SupportedMsaVersionsLoader supportedMsaVersionsLoader;

    @Inject
    public SupportedMsaVersionsBootstrap(
            final SupportedMsaVersionsRepository supportedMsaVersionsRepository,
            final SupportedMsaVersionsLoader supportedMsaVersionsLoader) {

        this.supportedMsaVersionsRepository = supportedMsaVersionsRepository;
        this.supportedMsaVersionsLoader = supportedMsaVersionsLoader;
    }

    @Override
    public void start() {
        LOG.info("Populating supported MSA version respository");
        SupportedMsaVersions supportedMsaVersions = loadSupportedMsaVersions();
        supportedMsaVersionsRepository.add(supportedMsaVersions.getVersions());
    }

    @Override
    public void stop() {
        // this method intentionally left blank
    }

    private SupportedMsaVersions loadSupportedMsaVersions() {
        final URL resourceUrl = SupportedMsaVersions.class.getResource(SUPPORTED_MSA_VERSIONS_FILE_NAME);
        if (resourceUrl == null) {
            throw new SupportedMsaVersionsFileAccessException(format("Unable to load expected resource: {0}", SUPPORTED_MSA_VERSIONS_FILE_NAME));
        }
        return supportedMsaVersionsLoader.loadSupportedMsaVersions(resourceUrl);
    }
}
