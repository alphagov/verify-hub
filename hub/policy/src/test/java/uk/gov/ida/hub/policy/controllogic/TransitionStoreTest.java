package uk.gov.ida.hub.policy.controllogic;

import org.junit.Test;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.assertj.core.api.Assertions.assertThat;

public class TransitionStoreTest {

    @Test
    public void sizeShouldReturnSizeOfExisting() {
        ConcurrentMap existing = new ConcurrentHashMap() {{
            put(new SessionId("key1"), "value1");
            put(new SessionId("key2"), "value2");
        }};
        ConcurrentMap transition = new ConcurrentHashMap() {{
            put(new SessionId("key1"), "value1");
        }};

        TransitionStore transitionStore = new TransitionStore(existing, Optional.of(transition));
        assertThat(transitionStore.size()).isEqualTo(existing.size());
    }

    @Test
    public void isEmptyReturnsIfExistingIsEmpty() {
        ConcurrentMap existing = new ConcurrentHashMap();
        ConcurrentMap transition = new ConcurrentHashMap() {{
            put(new SessionId("key1"), "value1");
        }};

        TransitionStore transitionStore = new TransitionStore(existing, Optional.of(transition));
        assertThat(transitionStore.isEmpty()).isEqualTo(existing.isEmpty());
    }

    @Test
    public void containsKeyChecksIfExistingContainsKey() {
        final SessionId key1 = new SessionId("key1");
        final SessionId key2 = new SessionId("key2");
        ConcurrentMap existing = new ConcurrentHashMap() {{
            put(key1, "value1");
        }};
        ConcurrentMap transition = new ConcurrentHashMap() {{
            put(key1, "value1");
            put(key2, "value2");
        }};

        TransitionStore transitionStore = new TransitionStore(existing, Optional.of(transition));
        assertThat(transitionStore.containsKey(key1)).isTrue();
        assertThat(transitionStore.containsKey(key2)).isFalse();
    }

    @Test
    public void containsValueChecksIfExistingContainsValue() {
        ConcurrentMap existing = new ConcurrentHashMap() {{
            put(new SessionId("key1"), "value1");
        }};
        ConcurrentMap transition = new ConcurrentHashMap() {{
            put(new SessionId("key2"), "value2");
        }};

        TransitionStore transitionStore = new TransitionStore(existing, Optional.of(transition));
        assertThat(transitionStore.containsValue("value1")).isTrue();
        assertThat(transitionStore.containsValue("value2")).isFalse();
    }

    @Test
    public void getReturnsValueFromExisting() {
        final SessionId key1 = new SessionId("key1");
        ConcurrentMap existing = new ConcurrentHashMap() {{
            put(key1, "value1");
        }};
        ConcurrentMap transition = new ConcurrentHashMap() {{
            put(key1, "value2");
        }};

        TransitionStore transitionStore = new TransitionStore(existing, Optional.of(transition));
        assertThat(transitionStore.get(key1)).isEqualTo("value1");
    }

    @Test
    public void putPutsValueInBothMapsAndReturnsThePreviousValueFromExisting() {
        final SessionId key1 = new SessionId("key1");
        ConcurrentMap existing = new ConcurrentHashMap() {{
            put(key1, "value1");
        }};
        ConcurrentMap transition = new ConcurrentHashMap() {{
            put(key1, "value2");
        }};

        TransitionStore transitionStore = new TransitionStore(existing, Optional.of(transition));
        assertThat(transitionStore.put(key1, "value3")).isEqualTo("value1");
        assertThat(existing.get(key1)).isEqualTo("value3");
        assertThat(transition.get(key1)).isEqualTo("value3");
    }

    @Test
    public void removeRemovesFromBothQueuesAndReturnsValueFromExisting() {
        final SessionId key1 = new SessionId("key1");
        ConcurrentMap existing = new ConcurrentHashMap() {{
            put(key1, "value1");
        }};
        ConcurrentMap transition = new ConcurrentHashMap() {{
            put(key1, "value2");
        }};

        TransitionStore transitionStore = new TransitionStore(existing, Optional.of(transition));
        assertThat(transitionStore.remove(key1)).isEqualTo("value1");
        assertThat(existing.containsKey(key1)).isFalse();
        assertThat(transition.containsKey(key1)).isFalse();
    }

    @Test
    public void putAllPutsAllInBothQueues() {
        ConcurrentMap existing = new ConcurrentHashMap();
        ConcurrentMap transition = new ConcurrentHashMap();
        SessionId key3 = new SessionId("key3");
        SessionId key2 = new SessionId("key2");
        Map putAll = new HashMap() {{
            put(key2, "value2");
            put(key3, "value3");
        }};

        TransitionStore transitionStore = new TransitionStore(existing, Optional.of(transition));
        transitionStore.putAll(putAll);
        assertThat(existing.containsKey(key2) && existing.containsKey(key3)).isTrue();
        assertThat(existing.containsKey(key2) && existing.containsKey(key3)).isTrue();
    }

    @Test
    public void clearClearsBothQueues() {
        ConcurrentMap existing = new ConcurrentHashMap() {{
            put(new SessionId("key1"), "value1");
        }};
        ConcurrentMap transition = new ConcurrentHashMap() {{
            put(new SessionId("key2"), "value2");
        }};

        TransitionStore transitionStore = new TransitionStore(existing, Optional.of(transition));
        transitionStore.clear();
        assertThat(existing.isEmpty()).isTrue();
        assertThat(transition.isEmpty()).isTrue();
    }

    @Test
    public void keySetGetsKeySetFromExisting() {
        final SessionId key1 = new SessionId("key1");
        final SessionId key2 = new SessionId("key2");
        ConcurrentMap existing = new ConcurrentHashMap() {{
            put(key1, "value1");
        }};
        ConcurrentMap transition = new ConcurrentHashMap() {{
            put(key2, "value2");
        }};

        TransitionStore transitionStore = new TransitionStore(existing, Optional.of(transition));
        assertThat(transitionStore.keySet().contains(key1)).isTrue();
        assertThat(transitionStore.keySet().contains(key2)).isFalse();

    }

    @Test
    public void valuesGetsValuesFromExisting() {
        ConcurrentMap existing = new ConcurrentHashMap() {{
            put(new SessionId("key1"), "value1");
        }};
        ConcurrentMap transition = new ConcurrentHashMap() {{
            put(new SessionId("key2"), "value2");
        }};

        TransitionStore transitionStore = new TransitionStore(existing, Optional.of(transition));
        assertThat(transitionStore.values().contains("value1")).isTrue();
        assertThat(transitionStore.values().contains("value2")).isFalse();
    }

    @Test
    public void entrySetReturnsEntrySetFromExisting() {
        ConcurrentMap existing = new ConcurrentHashMap() {{
            put(new SessionId("key1"), "value1");
        }};
        ConcurrentMap transition = new ConcurrentHashMap() {{
            put(new SessionId("key2"), "value2");
        }};

        TransitionStore transitionStore = new TransitionStore(existing, Optional.of(transition));
        assertThat(transitionStore.entrySet()).isEqualTo(existing.entrySet());
        assertThat(transitionStore.entrySet()).isNotEqualTo(transition.entrySet());
    }

    @Test
    public void putIfAbsentUpdatesBothMapsIfAbsentInExisting() {
        ConcurrentMap existing = new ConcurrentHashMap() {{
            put(new SessionId("key1"), "value1");
        }};
        ConcurrentMap transition = new ConcurrentHashMap() {{
            put(new SessionId("key2"), "value2");
        }};

        SessionId key3 = new SessionId("key3");
        TransitionStore transitionStore = new TransitionStore(existing, Optional.of(transition));
        assertThat(transitionStore.putIfAbsent(key3, "value3")).isEqualTo(null);
        assertThat(existing.containsKey(key3)).isTrue();
        assertThat(transition.containsKey(key3)).isTrue();
    }

    @Test
    public void putIfAbsentUpdatesNeitherMapIfAlreadyInExisting() {
        SessionId key1 = new SessionId("key1");
        ConcurrentMap existing = new ConcurrentHashMap() {{
            put(key1, "value1");
        }};
        ConcurrentMap transition = new ConcurrentHashMap() {{
            put(new SessionId("key2"), "value2");
        }};

        TransitionStore transitionStore = new TransitionStore(existing, Optional.of(transition));
        assertThat(transitionStore.putIfAbsent(key1, "value3")).isEqualTo("value1");
        assertThat(existing.containsKey(key1)).isTrue();
        assertThat(existing.get(key1)).isEqualTo("value1");
        assertThat(transition.containsKey(key1)).isFalse();
    }

    @Test
    public void putIfAbsentDoesSameWhenUsingSingleMapAndAbsent() {
        ConcurrentMap existing = new ConcurrentHashMap() {{
            put(new SessionId("key1"), "value1");
        }};

        SessionId key2 = new SessionId("key2");
        TransitionStore transitionStore = new TransitionStore(existing, Optional.empty());
        assertThat(transitionStore.putIfAbsent(key2, "value2")).isEqualTo(null);
        assertThat(existing.containsKey(key2)).isTrue();
    }

    @Test
    public void putIfAbsentDoesSameWhenUsingSingleMapAndPresent() {
        SessionId key1 = new SessionId("key1");
        ConcurrentMap existing = new ConcurrentHashMap() {{
            put(key1, "value1");
        }};

        TransitionStore transitionStore = new TransitionStore(existing, Optional.empty());
        assertThat(transitionStore.putIfAbsent(key1, "value2")).isEqualTo("value1");
        assertThat(existing.get(key1)).isEqualTo("value1");
    }

    @Test
    public void removeRemovesFromBothMapsIfRemovedFromExisting() {
        SessionId key1 = new SessionId("key1");
        ConcurrentMap existing = new ConcurrentHashMap() {{
            put(key1, "value1");
        }};
        ConcurrentMap transition = new ConcurrentHashMap() {{
            put(key1, "value2");
        }};

        TransitionStore transitionStore = new TransitionStore(existing, Optional.of(transition));
        assertThat(transitionStore.remove(key1, "value1")).isTrue();
        assertThat(existing.containsKey(key1)).isFalse();
        assertThat(transition.containsKey(key1)).isFalse();
    }

    @Test
    public void removeRemovesFromNeitherMapIfNotRemovedFromExisting() {
        SessionId key1 = new SessionId("key1");
        ConcurrentMap existing = new ConcurrentHashMap() {{
            put(key1, "value1");
        }};
        ConcurrentMap transition = new ConcurrentHashMap() {{
            put(key1, "value2");
        }};

        TransitionStore transitionStore = new TransitionStore(existing, Optional.of(transition));
        assertThat(transitionStore.remove(key1, "value3")).isFalse();
        assertThat(existing.get(key1)).isEqualTo("value1");
        assertThat(transition.get(key1)).isEqualTo("value2");
    }

    @Test
    public void removeDoesSameAsRemoveWhenUsingSingleMapAndExists() {
        SessionId key1 = new SessionId("key1");
        ConcurrentMap existing = new ConcurrentHashMap() {{
            put(key1, "value1");
        }};

        TransitionStore transitionStore = new TransitionStore(existing, Optional.empty());
        assertThat(transitionStore.remove(key1, "value1")).isTrue();
        assertThat(existing.containsKey(key1)).isFalse();
    }

    @Test
    public void removeDoesSameAsRemoveWhenUsingSingleMapAndDoesNotExist() {
        SessionId key1 = new SessionId("key1");
        ConcurrentMap existing = new ConcurrentHashMap() {{
            put(key1, "value1");
        }};

        TransitionStore transitionStore = new TransitionStore(existing, Optional.empty());
        assertThat(transitionStore.remove(key1, "value2")).isFalse();
        assertThat(existing.get(key1)).isEqualTo("value1");
    }

    @Test
    public void replaceAddsValueToBothMapsIfReplacedInExisting() {
        SessionId key1 = new SessionId("key1");
        ConcurrentMap existing = new ConcurrentHashMap() {{
            put(key1, "value1");
        }};
        ConcurrentMap transition = new ConcurrentHashMap() {{
            put(new SessionId("key2"), "value2");
        }};

        TransitionStore transitionStore = new TransitionStore(existing, Optional.of(transition));
        assertThat(transitionStore.replace(key1, "value1", "value3")).isTrue();
        assertThat(existing.get(key1)).isEqualTo("value3");
        assertThat(transition.get(key1)).isEqualTo("value3");
    }

    @Test
    public void replaceAddsValueToNeitherMapIfNotReplacedInExisting() {
        SessionId key1 = new SessionId("key1");
        ConcurrentMap existing = new ConcurrentHashMap() {{
            put(key1, "value1");
        }};
        ConcurrentMap transition = new ConcurrentHashMap() {{
            put(new SessionId("key2"), "value2");
        }};

        TransitionStore transitionStore = new TransitionStore(existing, Optional.of(transition));
        assertThat(transitionStore.replace(key1, "value2", "value3")).isFalse();
        assertThat(existing.get(key1)).isEqualTo("value1");
        assertThat(transition.containsKey(key1)).isFalse();
    }

    @Test
    public void replaceDoesSameAsReplaceWhenUsingSingleMapAndExists() {
        SessionId key1 = new SessionId("key1");
        ConcurrentMap existing = new ConcurrentHashMap() {{
            put(key1, "value1");
        }};

        TransitionStore transitionStore = new TransitionStore(existing, Optional.empty());
        assertThat(transitionStore.replace(key1, "value1", "value3")).isTrue();
        assertThat(existing.get(key1)).isEqualTo("value3");
    }

    @Test
    public void replaceDoesSameAsReplaceWhenUsingSingleMapAndDoesNotExist() {
        SessionId key1 = new SessionId("key1");
        ConcurrentMap existing = new ConcurrentHashMap() {{
            put(key1, "value1");
        }};

        TransitionStore transitionStore = new TransitionStore(existing, Optional.empty());
        assertThat(transitionStore.replace(key1, "value2", "value3")).isFalse();
        assertThat(existing.get(key1)).isEqualTo("value1");
    }

    @Test
    public void replace2AddsValueToBothMapsIfReplacedInExisting() {
        SessionId key1 = new SessionId("key1");
        ConcurrentMap existing = new ConcurrentHashMap() {{
            put(key1, "value1");
        }};
        ConcurrentMap transition = new ConcurrentHashMap() {{
            put(new SessionId("key2"), "value2");
        }};

        TransitionStore transitionStore = new TransitionStore(existing, Optional.of(transition));
        assertThat(transitionStore.replace(key1, "value3")).isEqualTo("value1");
        assertThat(existing.get(key1)).isEqualTo("value3");
        assertThat(transition.get(key1)).isEqualTo("value3");
    }

    @Test
    public void replace2AddsValueToNeitherMapIfNotReplacedInExisting() {
        SessionId key1 = new SessionId("key1");
        ConcurrentMap existing = new ConcurrentHashMap() {{
            put(key1, "value1");
        }};
        ConcurrentMap transition = new ConcurrentHashMap() {{
            put(new SessionId("key2"), "value2");
        }};

        TransitionStore transitionStore = new TransitionStore(existing, Optional.of(transition));
        SessionId key3 = new SessionId("key3");
        assertThat(transitionStore.replace(key3, "value3")).isEqualTo(null);
        assertThat(existing.containsKey(key3)).isFalse();
        assertThat(transition.containsKey(key3)).isFalse();
    }

    @Test
    public void replace2DoesSameAsReplace2WhenUsingSingleMap() {
        SessionId key1 = new SessionId("key1");
        ConcurrentMap existing = new ConcurrentHashMap() {{
            put(key1, "value1");
        }};

        TransitionStore transitionStore = new TransitionStore(existing, Optional.empty());
        assertThat(transitionStore.replace(key1, "value3")).isEqualTo("value1");
        assertThat(existing.get(key1)).isEqualTo("value3");
    }

    @Test
    public void transitionSizeShouldReturnSizeOfTransitionMap() {
        ConcurrentMap existing = new ConcurrentHashMap() {{
            put(new SessionId("key1"), "value1");
        }};
        ConcurrentMap transition = new ConcurrentHashMap() {{
            put(new SessionId("key2"), "value2");
            put(new SessionId("key3"), "value3");
        }};

        TransitionStore transitionStore = new TransitionStore(existing, Optional.of(transition));
        assertThat(transitionStore.transitionSize()).isEqualTo(transition.size());
    }

    @Test
    public void getDifferingKeysShouldGiveSessionIdsOnlyInExistingMap() {
        SessionId key1 = new SessionId("key1");
        ConcurrentMap existing = new ConcurrentHashMap() {{
            put(key1, "value1");
            put(new SessionId("key2"), "value2");
        }};
        ConcurrentMap transition = new ConcurrentHashMap() {{
            put(new SessionId("key2"), "value2");
            put(new SessionId("key3"), "value3");
        }};

        TransitionStore transitionStore = new TransitionStore(existing, Optional.of(transition));
        assertThat(transitionStore.getDifferingSessionIds()).containsOnly(key1);
    }
}