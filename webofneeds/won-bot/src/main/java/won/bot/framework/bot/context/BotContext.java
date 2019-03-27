package won.bot.framework.bot.context;

import java.io.Serializable;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Used by a bot to remember which needs and nodes and it knows. Additionally
 * generic Java objects can be saved and retrieved.
 */
public interface BotContext {
    String DEFAULT_NEED_LIST_NAME = "need_uris";

    // ===============================
    // application specific methods
    // ===============================
    /**
     * Return a set of all known need uris stored in all named need uri lists
     *
     * @return
     */
    Set<URI> retrieveAllNeedUris();

    /**
     * Check if need uri is known among all stored need uris
     *
     * @param needURI
     * @return
     */
    boolean isNeedKnown(final URI needURI);

    /**
     * removeFromObjectMap a need uri from a named need uri list
     *
     * @param uri
     * @param name
     */
    void removeNeedUriFromNamedNeedUriList(URI uri, String name);

    /**
     * add a need uri to a named need uri list
     *
     * @param uri
     * @param name
     */
    void appendToNamedNeedUriList(URI uri, String name);

    boolean isInNamedNeedUriList(URI uri, String name);

    /**
     * loadFromObjectMap all the need from a named need uri list
     *
     * @param name
     * @return
     */
    List<URI> getNamedNeedUriList(String name);

    /**
     * check if a node uri is known among all stored nodes
     *
     * @param wonNodeURI
     * @return
     */
    boolean isNodeKnown(final URI wonNodeURI);

    /**
     * store a node uri
     *
     * @param uri
     */
    void rememberNodeUri(final URI uri);

    /**
     * removeFromObjectMap a node uri
     *
     * @param uri
     */
    void removeNodeUri(final URI uri);

    // ===============================
    // generic methods
    // ==============================
    /**
     * Remove the whole collection from the bot context
     *
     * @param collectionName
     */
    void dropCollection(String collectionName);

    /**
     * Put an arbitrary single object in the context. If there exists already an
     * object at the specified key then replace that object by the new one.
     *
     * @param collectionName
     * @param key
     * @param value
     */
    void saveToObjectMap(String collectionName, String key, final Serializable value);

    /**
     * Retrieve an object from a collection previously added using
     * saveToObjectMap().
     *
     * @param collectionName
     * @param key
     * @return the requested object or null if it was not found
     */
    Object loadFromObjectMap(String collectionName, String key);

    /**
     * Retrieve a copy of the whole object map of the collection
     *
     * @return
     */
    Map<String, Object> loadObjectMap(String collectionName);

    /**
     * Remove an object saved at a specific map key in the collection
     *
     * @param collectionName
     * @param key
     */
    void removeFromObjectMap(String collectionName, String key);

    /**
     * Add one or more arbitrary objects to a list at a specific key in the
     * collection.
     *
     * @param collectionName
     * @param key
     * @param values
     */
    void addToListMap(String collectionName, String key, final Serializable... values);

    /**
     * Remove one or more arbitrary objects from the list at a specific key in the
     * collection.
     *
     * @param collectionName
     * @param key
     * @param values
     */
    void removeFromListMap(String collectionName, String key, final Serializable... values);

    /**
     * Remove one or more arbitrary objects from the list for all keys in the
     * collection.
     *
     * @param collectionName
     * @param values
     */
    void removeLeavesFromListMap(String collectionName, final Serializable... values);

    /**
     * Retrieve all objects from one collection at one key previously added using
     * addToListMap().
     *
     * @param collectionName
     * @return all objects from one collection or an empty collection if no objects
     * are in there
     */
    List<Object> loadFromListMap(String collectionName, String key);

    /**
     * Retrieve a copy of the whole object list map in a collection
     *
     * @param collectionName
     * @return
     */
    Map<String, List<Object>> loadListMap(String collectionName);

    /**
     * Remove all object in the list at a specific map key in the collection
     *
     * @param collectionName
     * @param key
     */
    void removeFromListMap(String collectionName, String key);
}
