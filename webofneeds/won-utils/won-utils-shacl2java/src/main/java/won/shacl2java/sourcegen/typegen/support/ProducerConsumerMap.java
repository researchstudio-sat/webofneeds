package won.shacl2java.sourcegen.typegen.support;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class ProducerConsumerMap<K, V> {
    protected Map<K, V> internalMap = new HashMap<>();

    public ProducerConsumerMap() {
    }

    public Producer producer() {
        return new Producer();
    }

    public Consumer consumer() {
        return new Consumer();
    }

    public class Consumer {
        public Consumer() {
        }

        public Optional<V> get(K key) {
            return Optional.ofNullable(internalMap.get(key));
        }

        public Set<K> keySet() {
            return Collections.unmodifiableSet(internalMap.keySet());
        }

        public Collection<V> values() {
            return Collections.unmodifiableCollection(internalMap.values());
        }

        public Set<Map.Entry<K, V>> entrySet() {
            return Collections.unmodifiableSet(internalMap.entrySet());
        }

        public V getOrDefault(Object key, V defaultValue) {
            return internalMap.getOrDefault(key, defaultValue);
        }

        public boolean containsKey(Object key) {
            return internalMap.containsKey(key);
        }

        public boolean containsValue(Object value) {
            return internalMap.containsValue(value);
        }

        public Map asMap() {
            return Collections.unmodifiableMap(internalMap);
        }
    }

    public class Producer {
        public Producer() {
        }

        public void put(K key, V value) {
            internalMap.put(key, value);
        }

        public V computeIfAbsent(K key, Function<K, V> mappingFunction) {
            return internalMap.computeIfAbsent(key, mappingFunction);
        }
    }
}
