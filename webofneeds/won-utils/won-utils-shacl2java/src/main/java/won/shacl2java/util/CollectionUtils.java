package won.shacl2java.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CollectionUtils {
    public static <K, V> void addToMultivalueMap(Map<K, Set<V>> map, K key, V value) {
        Set<V> shapes = map.get(key);
        if (shapes == null) {
            shapes = new HashSet<>();
        }
        shapes.add(value);
        map.put(key, shapes);
    }

    public static <K, V> void addToMultivalueMap(Map<K, Set<V>> map, K key,
                    Collection<V> values) {
        Set<V> shapes = map.get(key);
        if (shapes == null) {
            shapes = new HashSet<>();
        }
        shapes.addAll(values);
        map.put(key, shapes);
    }
}
