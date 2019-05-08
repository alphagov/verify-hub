package uk.gov.ida.hub.config.data;

import com.google.common.collect.ImmutableSet;
import uk.gov.ida.hub.config.domain.IdentityProviderConfig;
import uk.gov.ida.hub.config.domain.LevelOfAssurance;
import uk.gov.ida.hub.config.domain.TransactionConfig;
import uk.gov.ida.hub.config.exceptions.ConfigValidationException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.function.Function.identity;
import static uk.gov.ida.hub.config.domain.LevelOfAssurance.LEVEL_1;
import static uk.gov.ida.hub.config.domain.LevelOfAssurance.LEVEL_2;

public class LevelsOfAssuranceConfigValidator {

    public void validateLevelsOfAssurance(final Set<IdentityProviderConfig> identityProviderConfig,
                                           final Set<TransactionConfig> transactionConfig) {
        validateAllIDPsSupportLOA1orLOA2(identityProviderConfig);
        validateAllTransactionsAreLOA1OrLOA2(transactionConfig);
        validateAllTransactionsAreSupportedByIDPs(identityProviderConfig, transactionConfig);
    }

    protected void validateAllTransactionsAreSupportedByIDPs(Set<IdentityProviderConfig> identityProviderConfig, Set<TransactionConfig> transactionConfig) {
        Map<TransactionConfig, List<IdentityProviderConfig>> unsupportedTransaction = transactionConfig.stream()
                .collect(Collectors.toMap(
                        identity(),
                        config -> unsupportedIdpsForATransaction(identityProviderConfig, config)
                ));

        if (unsupportedTransaction.values().stream().anyMatch(x -> !x.isEmpty())) {
            throw ConfigValidationException.createIncompatiblePairsOfTransactionsAndIDPs(unsupportedTransaction);
        }
    }

    private List<IdentityProviderConfig> unsupportedIdpsForATransaction(Set<IdentityProviderConfig> identityProviderConfig, TransactionConfig transactionConfig) {
        Set<LevelOfAssurance> transactionLOAs = ImmutableSet.copyOf(transactionConfig.getLevelsOfAssurance());
        return identityProviderConfig.stream()
                .filter(idp -> isIdpForTransaction(transactionConfig, idp))
                .filter(idp -> idpCannotFulfillLoaRequirements(transactionLOAs, idp))
                .collect(Collectors.toList());
    }

    private boolean isIdpForTransaction(TransactionConfig transactionConfig, IdentityProviderConfig idp) {
        return idp.getOnboardingTransactionEntityIds().contains(transactionConfig.getEntityId()) || idp.getOnboardingTransactionEntityIds().isEmpty();
    }

    private boolean idpCannotFulfillLoaRequirements(Set<LevelOfAssurance> transactionLOAs, IdentityProviderConfig idp) {
        Set<LevelOfAssurance> idpLOAs = new HashSet<>(idp.getSupportedLevelsOfAssurance());
        idpLOAs.retainAll(transactionLOAs);
        return idpLOAs.isEmpty();
    }

    protected void validateAllTransactionsAreLOA1OrLOA2(Set<TransactionConfig> transactionConfig) {
        List<TransactionConfig> badTransactionConfigs = transactionConfig.stream()
                .filter(x -> {
                    List<LevelOfAssurance> levelsOfAssurance = x.getLevelsOfAssurance();
                    boolean isLoa1 = levelsOfAssurance.equals(asList(LEVEL_1, LEVEL_2));
                    boolean isLoa2 = levelsOfAssurance.equals(asList(LEVEL_2));
                    return !(isLoa1 || isLoa2);
                })
                .collect(Collectors.toList());
        if(!badTransactionConfigs.isEmpty()) {
            throw ConfigValidationException.createTransactionsRequireUnsupportedLevelOfAssurance(badTransactionConfigs);
        }
    }

    protected void validateAllIDPsSupportLOA1orLOA2(Set<IdentityProviderConfig> identityProviderConfig) {
        List<IdentityProviderConfig> badIDPConfigs = identityProviderConfig.stream()
                .filter(x -> x.getSupportedLevelsOfAssurance().isEmpty() || containsUnsupportedLOAs(x))
                .collect(Collectors.toList());

        if(!badIDPConfigs.isEmpty()) {
            throw ConfigValidationException.createIDPLevelsOfAssuranceUnsupportedException(badIDPConfigs);
        }
    }

    private boolean containsUnsupportedLOAs(IdentityProviderConfig identityProviderConfig) {
        return !identityProviderConfig.getSupportedLevelsOfAssurance().stream()
                .filter(loa -> loa != LEVEL_1 && loa != LEVEL_2)
                .collect(Collectors.toList())
                .isEmpty();
    }
}
