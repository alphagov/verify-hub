package uk.gov.ida.hub.config.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.validation.ValidationMethod;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionConfig implements EntityIdentifiable, CertificateConfigurable {

    @Valid
    @NotNull
    @JsonProperty
    protected List<AssertionConsumerService> assertionConsumerServices;

    @Valid
    @NotNull
    @JsonProperty
    protected URI serviceHomepage;

    @Valid
    @NotNull
    @JsonProperty
    protected Boolean enabled;

    @Valid
    @JsonProperty
    protected Boolean enabledForSingleIdp = false;

    @Valid
    @NotNull
    @JsonProperty
    protected X509CertificateConfiguration encryptionCertificate;

    @Valid
    @NotNull
    @JsonProperty
    protected String entityId;

    @Valid
    @JsonProperty
    protected String simpleId;

    @Valid
    @JsonProperty
    protected MatchingProcess matchingProcess;

    @Valid
    @JsonProperty
    protected String matchingServiceEntityId;

    @Valid
    @NotNull
    @JsonProperty
    protected List<LevelOfAssurance> levelsOfAssurance;

    @Valid
    @JsonProperty
    protected boolean eidasEnabled;

    @Valid
    @JsonProperty
    private List<String> eidasCountries;

    @Valid
    @NotNull
    @JsonProperty
    protected Boolean shouldHubSignResponseMessages;

    @Valid
    @JsonProperty
    protected Boolean shouldHubUseLegacySamlStandard = false;

    @Valid
    @NotNull
    @JsonProperty
    protected List<X509CertificateConfiguration> signatureVerificationCertificates;

    @Valid
    @NotNull
    @JsonProperty
    protected Boolean shouldSignWithSHA1 = true;

    @Valid
    @JsonProperty
    protected List<UserAccountCreationAttribute> userAccountCreationAttributes;

    @Valid
    @JsonProperty
    protected URI headlessStartpage;

    @Valid
    @JsonProperty
    protected URI singleIdpStartpage;

    @Valid
    @JsonProperty
    protected boolean usingMatching = true;

    @Valid
    @JsonProperty
    protected boolean eidasProxyNode;

    @Valid
    @JsonProperty
    protected boolean selfService = false;


    @SuppressWarnings("unused") // needed to prevent guice injection
    protected TransactionConfig() {
    }

    @ValidationMethod(message = "Assertion Consumer Service indices must be unique.")
    @SuppressWarnings("unused") // used by the deserializer
    private boolean isAssertionConsumerServiceIndicesUnique() {
        Set<Integer> indices = new HashSet<>();
        for (AssertionConsumerService assertionConsumerService : assertionConsumerServices) {
            final Optional<Integer> index = assertionConsumerService.getIndex();
            if (index.isPresent() && !indices.add(index.get())) {
                return false;
            }
        }
        return true;
    }

    @ValidationMethod(message = "Exactly one Assertion Consumer Service must be marked as default.")
    @SuppressWarnings("unused") // used by the deserializer
    private boolean isOnlyOneDefaultAssertionConsumerServiceIndex() {
        boolean hasDefault = false;
        for (AssertionConsumerService assertionConsumerService : assertionConsumerServices) {
            if (assertionConsumerService.getDefault()) {
                if (hasDefault) {
                    return false;
                }
                hasDefault = true;
            }
        }
        return hasDefault;
    }

    public Optional<URI> getAssertionConsumerServiceUri(Optional<Integer> index) {
        for (AssertionConsumerService service : assertionConsumerServices) {
            boolean isDesiredEndpoint;
            if (index.isPresent() && service.getIndex().isPresent()) {
                isDesiredEndpoint = (service.getIndex().get().equals(index.get()));
            } else {
                isDesiredEndpoint = service.getDefault();
            }

            if (isDesiredEndpoint) {
                return ofNullable(service.getUri());
            }
        }
        return Optional.empty();
    }

    public URI getServiceHomepage() {
        return serviceHomepage;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public Boolean isEnabledForSingleIdp() {
        return enabledForSingleIdp;
    }

    public String getEntityId() {
        return entityId;
    }

    public EncryptionCertificate getEncryptionCertificate() {
        return new EncryptionCertificate(encryptionCertificate);
    }

    public Optional<MatchingProcess> getMatchingProcess() {
        return ofNullable(matchingProcess);
    }

    public String getMatchingServiceEntityId() {
        return matchingServiceEntityId;
    }

    public List<LevelOfAssurance> getLevelsOfAssurance() {
        return levelsOfAssurance;
    }

    public Boolean getShouldHubSignResponseMessages() {
        return shouldHubSignResponseMessages;
    }

    public Boolean getShouldSignWithSHA1() {
        return shouldSignWithSHA1;
    }

    public Boolean getShouldHubUseLegacySamlStandard() {
        return shouldHubUseLegacySamlStandard;
    }

    public Collection<SignatureVerificationCertificate> getSignatureVerificationCertificates() {
        return signatureVerificationCertificates
                .stream()
                .map(SignatureVerificationCertificate::new)
                .collect(Collectors.toList());
    }

    public Optional<List<UserAccountCreationAttribute>> getUserAccountCreationAttributes() {
        return ofNullable(userAccountCreationAttributes);
    }

    public Optional<String> getSimpleId() {
        return ofNullable(simpleId);
    }

    public boolean isEidasEnabled() {
        return eidasEnabled;
    }

    public Optional<List<String>> getEidasCountries() {
        return ofNullable(eidasCountries);
    }

    public URI getHeadlessStartpage() {
        return headlessStartpage;
    }

    public Optional<URI> getSingleIdpStartPage() {
        return ofNullable(singleIdpStartpage);
    }


    public boolean isUsingMatching() {
        return usingMatching;
    }

    public boolean isEidasProxyNode() {
        return eidasProxyNode;
    }

    public boolean isSelfService() {
        return selfService;
    }

}
