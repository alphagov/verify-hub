package uk.gov.ida.hub.config.data;

import com.google.common.collect.ImmutableSet;
import uk.gov.ida.hub.config.domain.IdentityProviderConfigEntityData;
import uk.gov.ida.hub.config.domain.LevelOfAssurance;
import uk.gov.ida.hub.config.domain.TransactionConfigEntityData;
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

    public void validateLevelsOfAssurance(final Set<IdentityProviderConfigEntityData> identityProviderConfigEntityData,
                                           final Set<TransactionConfigEntityData> transactionConfigEntityData) {
        validateAllIDPsSupportLOA1orLOA2(identityProviderConfigEntityData);
        validateAllTransactionsAreLOA1OrLOA2(transactionConfigEntityData);
        validateAllTransactionsAreSupportedByIDPs(identityProviderConfigEntityData, transactionConfigEntityData);
    }

    protected void validateAllTransactionsAreSupportedByIDPs(Set<IdentityProviderConfigEntityData> identityProviderConfigEntityData, Set<TransactionConfigEntityData> transactionConfigEntityData) {
        Map<TransactionConfigEntityData, List<IdentityProviderConfigEntityData>> unsupportedTransaction = transactionConfigEntityData.stream()
                .collect(Collectors.toMap(
                        identity(),
                        config -> unsupportedIdpsForATransaction(identityProviderConfigEntityData, config)
                ));

        if (unsupportedTransaction.values().stream().anyMatch(x -> !x.isEmpty())) {
            throw ConfigValidationException.createIncompatiblePairsOfTransactionsAndIDPs(unsupportedTransaction);
        }
    }

    private List<IdentityProviderConfigEntityData> unsupportedIdpsForATransaction(Set<IdentityProviderConfigEntityData> identityProviderConfigEntityData, TransactionConfigEntityData transactionConfigEntityData) {
        Set<LevelOfAssurance> transactionLOAs = ImmutableSet.copyOf(transactionConfigEntityData.getLevelsOfAssurance());
        return identityProviderConfigEntityData.stream()
                .filter(idp -> isIdpForTransaction(transactionConfigEntityData, idp))
                .filter(idp -> idpCannotFulfillLoaRequirements(transactionLOAs, idp))
                .collect(Collectors.toList());
    }

    private boolean isIdpForTransaction(TransactionConfigEntityData transactionConfigEntityData, IdentityProviderConfigEntityData idp) {
        return idp.getOnboardingTransactionEntityIds().contains(transactionConfigEntityData.getEntityId()) || idp.getOnboardingTransactionEntityIds().isEmpty();
    }

    private boolean idpCannotFulfillLoaRequirements(Set<LevelOfAssurance> transactionLOAs, IdentityProviderConfigEntityData idp) {
        Set<LevelOfAssurance> idpLOAs = new HashSet<>(idp.getSupportedLevelsOfAssurance());
        idpLOAs.retainAll(transactionLOAs);
        return idpLOAs.isEmpty();
    }

    protected void validateAllTransactionsAreLOA1OrLOA2(Set<TransactionConfigEntityData> transactionConfigEntityData) {
        List<TransactionConfigEntityData> badTransactionConfigs = transactionConfigEntityData.stream()
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

    protected void validateAllIDPsSupportLOA1orLOA2(Set<IdentityProviderConfigEntityData> identityProviderConfigEntityData) {
        List<IdentityProviderConfigEntityData> badIDPConfigs = identityProviderConfigEntityData.stream()
                .filter(x -> x.getSupportedLevelsOfAssurance().isEmpty() || containsUnsupportedLOAs(x))
                .collect(Collectors.toList());

        if(!badIDPConfigs.isEmpty()) {
            throw ConfigValidationException.createIDPLevelsOfAssuranceUnsupportedException(badIDPConfigs);
        }
    }

    private boolean containsUnsupportedLOAs(IdentityProviderConfigEntityData identityProviderConfigEntityData) {
        return !identityProviderConfigEntityData.getSupportedLevelsOfAssurance().stream()
                .filter(loa -> loa != LEVEL_1 && loa != LEVEL_2)
                .collect(Collectors.toList())
                .isEmpty();
    }
}
