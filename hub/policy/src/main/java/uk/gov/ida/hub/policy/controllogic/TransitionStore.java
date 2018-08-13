package uk.gov.ida.hub.policy.controllogic;

import uk.gov.ida.hub.policy.domain.SessionId;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/* This class is designed to transition all values from the existing
 * to the transition store */
public class TransitionStore<V> implements ConcurrentMap<SessionId, V> {

    private ConcurrentMap<SessionId, V> existing;
    private Optional<ConcurrentMap<SessionId, V>> transition;

    public TransitionStore(ConcurrentMap<SessionId, V> existing, Optional<ConcurrentMap<SessionId, V>> transition) {
        this.existing = existing;
        this.transition = transition;
    }

    @Override
    public int size() {
        return existing.size();
    }

    @Override
    public boolean isEmpty() {
        return existing.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return existing.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return existing.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return existing.get(key);
    }

    @Override
    public V put(SessionId key, V value) {
        transition.ifPresent(t -> t.put(key, value));
        return existing.put(key, value);
    }

    @Override
    public V remove(Object key) {
        transition.ifPresent(t -> t.remove(key));
        return existing.remove(key);
    }

    @Override
    public void putAll(Map<? extends SessionId, ? extends V> m) {
        transition.ifPresent(t -> t.putAll(m));
        existing.putAll(m);
    }

    @Override
    public void clear() {
        transition.ifPresent(Map::clear);
        existing.clear();
    }

    @Override
    public Set<SessionId> keySet() {
        return existing.keySet();
    }

    @Override
    public Collection<V> values() {
        return existing.values();
    }

    @Override
    public Set<Entry<SessionId, V>> entrySet() {
        return existing.entrySet();
    }

    @Override
    public V putIfAbsent(SessionId key, V value) {
        V val = existing.putIfAbsent(key, value);
        // If `value` was added to existing, add it to transition
        if (val == null) {
            transition.ifPresent(t -> t.put(key, value));
        }
        return val;
    }

    @Override
    public boolean remove(Object key, Object value) {
        boolean remove = existing.remove(key, value);
        // If entry was removed from existing, remove from transition
        if (remove) {
            transition.ifPresent(t -> t.remove(key));
        }
        return remove;
    }

    @Override
    public boolean replace(SessionId key, V oldValue, V newValue) {
        boolean replace = existing.replace(key, oldValue, newValue);
        // If `oldValue` was replaced with `newValue` in existing, add `newValue` to transition
        if (replace) {
            transition.ifPresent(t -> t.put(key, newValue));
        }
        return replace;
    }

    @Override
    public V replace(SessionId key, V value) {
        V val = existing.replace(key, value);
        // If `value` was added to existing, add it to transition
        if (val != null) {
            transition.ifPresent(t -> t.put(key, value));
        }
        return val;
    }
}
