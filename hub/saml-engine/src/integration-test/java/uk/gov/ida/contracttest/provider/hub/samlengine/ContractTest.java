package uk.gov.ida.contracttest.provider.hub.samlengine;

import au.com.dius.pact.provider.junit.PactRunner;
import au.com.dius.pact.provider.junit.Provider;
import au.com.dius.pact.provider.junit.loader.PactFolder;
import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junit.target.Target;
import au.com.dius.pact.provider.junit.target.TestTarget;
import io.dropwizard.testing.ConfigOverride;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.ConfigStubRule;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.CountryMetadataRule;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.MetadataRule;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppRule;

import static uk.gov.ida.saml.core.test.TestEntityIds.TEST_RP_MS;

@Ignore
@RunWith(PactRunner.class)
@Provider("saml-engine")
@PactFolder("target/pacts")
public class ContractTest {
    private static final String COUNTRY_ENTITY_ID = "/metadata/country";
    private static final String EIDAS_HUB_ENTITY_ID = "http://localhost/eidasHubMetadata";

    @ClassRule
    public static ConfigStubRule configStub = new ConfigStubRule();

    // The port number becomes part of the pact file so this must be kept consistent with the corresponding consumer test
    @ClassRule
    public static final CountryMetadataRule COUNTRY_METADATA = new CountryMetadataRule(COUNTRY_ENTITY_ID, 40_000);

    @ClassRule
    public static SamlEngineAppRule samlEngineAppRule = new SamlEngineAppRule(
        ConfigOverride.config("configUri", configStub.baseUri().build().toASCIIString()),
        ConfigOverride.config("country.saml.entityId", EIDAS_HUB_ENTITY_ID),
        ConfigOverride.config("country.metadata.uri", COUNTRY_METADATA.getCountryMetadataUri())
    );

    @BeforeClass
    public static void setUp() throws Exception {
        configStub.setupStubForCertificates(TEST_RP_MS);
    }

    @TestTarget
    public final Target target = new HttpTarget(samlEngineAppRule.getLocalPort());
}
