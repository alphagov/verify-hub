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
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppRule;

import static uk.gov.ida.saml.core.test.TestEntityIds.TEST_RP_MS;

@Ignore
@RunWith(PactRunner.class)
@Provider("saml-engine")
@PactFolder("target/pacts")
public class ContractTest {

    @ClassRule
    public static ConfigStubRule configStub = new ConfigStubRule();

    @ClassRule
    public static SamlEngineAppRule samlEngineAppRule = new SamlEngineAppRule(
        ConfigOverride.config("configUri", configStub.baseUri().build().toASCIIString())
    );

    @BeforeClass
    public static void setUp() throws Exception {
        configStub.setupCertificatesForEntity(TEST_RP_MS);
    }

    @TestTarget
    public final Target target = new HttpTarget(samlEngineAppRule.getLocalPort());
}
