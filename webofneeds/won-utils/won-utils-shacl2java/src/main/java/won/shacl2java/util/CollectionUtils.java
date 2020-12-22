package won.shacl2java.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class CollectionUtils {
    /**
     * Returns true if a new element was added, false otherwise; assumes no keys are
     * ever removed.
     * 
     * @param map
     * @param value
     * @param <K>
     * @param <V>
     * @return
     */
    public static <K, V> boolean addToMultivalueConcurrentHashMap(ConcurrentHashMap<K, Set<V>> map, K key, V value) {
        AtomicBoolean changed = new AtomicBoolean(false);
        if (!map.containsKey(key)) {
            // outer check to avoid blocking the map too much in case we can append
            // concurrent threads will get to here but only one gets to be first in
            // computeIfAbsent
            map.computeIfAbsent(key,
                            v -> {
                                changed.set(true);
                                Set<V> ret = new HashSet<>();
                                ret.add(value);
                                return ret;
                            });
        }
        // we may have other elements added in between these two calls,
        // but the result is still correct
        if (!changed.get()) {
            map.computeIfPresent(key,
                            (k, v) -> {
                                if (v.contains(value)) {
                                    return v;
                                }
                                changed.set(true);
                                Set<V> ret = new HashSet<>(v);
                                ret.add(value);
                                return ret;
                            });
        }
        return changed.get();
    }

    /**
     * Returns true if a new element was added, false otherwise. Assumes no keys are
     * ever removed.
     *
     * @param map
     * @param value
     * @param <K>
     * @param <V>
     * @return
     */
    public static <K, V> boolean addToMultivalueConcurrentHashMap(ConcurrentHashMap<K, Set<V>> map, K key,
                    Set<V> value) {
        AtomicBoolean changed = new AtomicBoolean(false);
        if (!map.containsKey(key)) {
            // outer check to avoid blocking the map too much in case we can append
            // concurrent threads will get to here but only one gets to be first in
            // computeIfAbsent
            map.computeIfAbsent(key,
                            v -> {
                                changed.set(true);
                                Set<V> ret = new HashSet<>();
                                ret.addAll(value);
                                return ret;
                            });
        }
        // we may have other elements added in between these two calls,
        // but the result is still correct
        if (!changed.get()) {
            map.computeIfPresent(key,
                            (k, v) -> {
                                if (v.containsAll(value)) {
                                    return v;
                                }
                                changed.set(true);
                                Set<V> ret = new HashSet<>(v);
                                ret.addAll(value);
                                return ret;
                            });
        }
        return changed.get();
    }

    public static <K, V> void addToMultivalueMap(Map<K, Set<V>> map, K key, V value) {
        Set<V> existing = map.get(key);
        if (existing == null) {
            existing = new HashSet<>();
        }
        existing.add(value);
        map.put(key, existing);
    }

    public static <K, V> void addToMultivalueMap(Map<K, Set<V>> map, K key,
                    Collection<V> values) {
        Set<V> existing = map.get(key);
        if (existing == null) {
            existing = new HashSet<>();
        }
        existing.addAll(values);
        map.put(key, existing);
    }
}
