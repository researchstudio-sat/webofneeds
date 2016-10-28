package won.bot.framework.bot.context;

import java.io.Serializable;
import java.net.URI;
import java.util.*;

/**
 * Used by a bot to remember which needs and nodes and it knows. Additionally generic Java objects can be saved and
 * retrieved.
 *
 */
public interface BotContext
{
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
   *  removeGeneric a need uri from a named need uri list
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

  /**
   * getGeneric all the need from a named need uri list
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
   * removeGeneric a node uri
   *
   * @param uri
   */
  void removeNodeUri(final URI uri);

  /**
   * Put an arbitrary object in the context.
   *
   * @param collectionName
   * @param key
   * @param value
   */
  void putGeneric(String collectionName, String key, final Serializable value);

  /**
   * Retrieve an object object from a collection previously added using putGeneric().
   *
   * @param collectionName
   * @param key
   * @return the requested object or null if it was not found
   */
  Object getGeneric(String collectionName, String key);

  /**
   * Retrieve all objects from one collection
   *
   * @param collectionName
   * @return all objects from one collection or an empty collection if no objects are in there
   */
  Collection<Object> genericValues(String collectionName);

  /**
   * Remove an arbitrary object from the context by its key
   *
   * @param collectionName
   * @param key
   * @return the removed object or null if it was not found
   */
  void removeGeneric(String collectionName, String key);

}
