package won.bot.framework.bot.context;

import java.io.Serializable;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * In memory context implementation using nested maps. This is the default
 * implementation of the bot context.
 */
public class MemoryBotContext implements BotContext {
    private Map<String, Map<String, Object>> contextObjectMap = new HashMap<>();
    private Map<String, Map<String, List<Object>>> contextListMap = new HashMap<>();
    private Set<URI> nodeUris = new HashSet<>();
    private Map<String, List<URI>> namedAtomUriLists = new HashMap<>();

    @Override
    public Set<URI> retrieveAllAtomUris() {
        Set<URI> ret = new HashSet<>();
        ret.addAll(namedAtomUriLists.values().stream().flatMap(List::stream).collect(Collectors.toSet()));
        return ret;
    }

    @Override
    public synchronized boolean isAtomKnown(final URI atomURI) {
        return retrieveAllAtomUris().contains(atomURI);
    }

    @Override
    public synchronized void removeAtomUriFromNamedAtomUriList(URI uri, String name) {
        List<URI> uris = namedAtomUriLists.get(name);
        uris.remove(uri);
    }

    @Override
    public synchronized void appendToNamedAtomUriList(final URI uri, final String name) {
        List<URI> uris = this.namedAtomUriLists.get(name);
        if (uris == null) {
            uris = new ArrayList<>();
        }
        uris.add(uri);
        this.namedAtomUriLists.put(name, uris);
    }

    @Override
    public synchronized boolean isInNamedAtomUriList(URI uri, String name) {
        List<URI> uris = getNamedAtomUriList(name);
        for (URI tmpUri : uris) {
            if (tmpUri.equals(uri)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized List<URI> getNamedAtomUriList(final String name) {
        List<URI> ret = new LinkedList<>();
        List<URI> namedList = this.namedAtomUriLists.get(name);
        if (namedList != null) {
            ret.addAll(namedList);
        }
        return ret;
    }

    @Override
    public synchronized boolean isNodeKnown(final URI wonNodeURI) {
        return nodeUris.contains(wonNodeURI);
    }

    @Override
    public synchronized void rememberNodeUri(final URI uri) {
        nodeUris.add(uri);
    }

    @Override
    public synchronized void removeNodeUri(final URI uri) {
        nodeUris.remove(uri);
    }

    private Map<String, Object> getObjectMap(String collectionName) {
        Map<String, Object> collection = contextObjectMap.computeIfAbsent(collectionName, k -> new HashMap<>());
        return collection;
    }

    @Override
    public void dropCollection(String collectionName) {
        contextObjectMap.remove(collectionName);
    }

    @Override
    public synchronized void saveToObjectMap(String collectionName, String key, final Serializable value) {
        getObjectMap(collectionName).put(key, value);
    }

    @Override
    public synchronized final Object loadFromObjectMap(String collectionName, String key) {
        return getObjectMap(collectionName).get(key);
    }

    @Override
    public Map<String, Object> loadObjectMap(final String collectionName) {
        return new HashMap<>(getObjectMap(collectionName));
    }

    @Override
    public synchronized final void removeFromObjectMap(String collectionName, String key) {
        getObjectMap(collectionName).remove(key);
    }

    private Map<String, List<Object>> getListMap(String collectionName) {
        Map<String, List<Object>> collection = contextListMap.computeIfAbsent(collectionName, k -> new HashMap<>());
        return collection;
    }

    private List<Object> getList(String collectionName, String key) {
        List<Object> objectList = getListMap(collectionName).computeIfAbsent(key, k -> new LinkedList<>());
        return objectList;
    }

    @Override
    public void addToListMap(final String collectionName, final String key, final Serializable... value) {
        getList(collectionName, key).addAll(Arrays.asList(value));
    }

    @Override
    public void removeFromListMap(final String collectionName, final String key, final Serializable... values) {
        getList(collectionName, key).removeAll(Arrays.asList(values));
    }

    @Override
    public void removeLeavesFromListMap(String collectionName, final Serializable... values) {
        for (String key : getListMap(collectionName).keySet()) {
            removeFromListMap(collectionName, key, values);
        }
    }

    @Override
    public List<Object> loadFromListMap(final String collectionName, final String key) {
        return new LinkedList<>(getList(collectionName, key));
    }

    @Override
    public Map<String, List<Object>> loadListMap(final String collectionName) {
        return new HashMap<>(getListMap(collectionName));
    }

    @Override
    public void removeFromListMap(final String collectionName, final String key) {
        getListMap(collectionName).remove(key);
    }
}
