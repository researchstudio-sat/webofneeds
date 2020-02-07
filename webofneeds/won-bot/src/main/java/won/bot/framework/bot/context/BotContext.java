package won.bot.framework.bot.context;

import java.io.Serializable;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Used by a bot to remember which atoms and nodes and it knows. Additionally
 * generic Java objects can be saved and retrieved.
 */
public interface BotContext {
    /**
     * @deprecated will be removed in favor of BotContextWrapper/Bot specific naming
     * use {@link BotContextWrapper#getAtomCreateListName()} instead
     */
    @Deprecated
    String DEFAULT_ATOM_LIST_NAME = "atom_uris";

    // ===============================
    // application specific methods
    // ===============================
    /**
     * removeFromObjectMap an atom uri from a named atom uri list
     *
     * @param uri
     * @param name
     * @deprecated will be removed, use
     * {@link BotContext#removeFromUriList(URI, String)} instead
     */
    @Deprecated
    void removeAtomUriFromNamedAtomUriList(URI uri, String name);

    /**
     * removes Uri from given list
     *
     * @param uri
     * @param name
     */
    void removeFromUriList(URI uri, String name);

    /**
     * add an atom uri to a named atom uri list
     *
     * @param uri
     * @param name
     * @deprecated will be removed, use
     * {@link BotContext#appendToUriList(URI, String)} instead
     */
    @Deprecated
    void appendToNamedAtomUriList(URI uri, String name);

    /**
     * add an atom uri to a named atom uri list
     *
     * @param uri
     * @param name
     */
    void appendToUriList(URI uri, String name);

    /**
     * @deprecated will be removed, use {@link BotContext#isInUriList(URI, String)}
     * instead
     */
    @Deprecated
    boolean isInNamedAtomUriList(URI uri, String name);

    /**
     * Checks if uri is stored in the list with the given name
     * 
     * @param uri to check
     * @param name list name
     * @return true if uri is in list
     */
    boolean isInUriList(URI uri, String name);

    /**
     * loadFromObjectMap all the atom from a named atom uri list
     *
     * @param name
     * @deprecated will be removed, use {@link BotContext#getUriList(String)}
     * instead
     * @return
     */
    @Deprecated
    List<URI> getNamedAtomUriList(String name);

    List<URI> getUriList(String name);

    Object getSingleValue(String name);

    void setSingleValue(String name, final Serializable value);

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
