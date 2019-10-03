package won.bot.framework.bot.context;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.io.Serializable;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Bot context implementation using persistent mongo db for storage.
 * <p>
 * Created by hfriedrich on 27.10.2016.
 */
public class MongoBotContext implements BotContext {
    protected static final String ATOM_URI_COLLECTION = "atom_uris";
    protected static final String NODE_URI_COLLECTION = "node_uris";
    @Autowired
    private MongoTemplate template;

    public void setTemplate(final MongoTemplate template) {
        this.template = template;
    }

    @Override
    public Set<URI> retrieveAllAtomUris() {
        Set<URI> uris = new HashSet<>();
        List<MongoContextObjectList> contextObjects = template.findAll(MongoContextObjectList.class,
                        ATOM_URI_COLLECTION);
        for (MongoContextObjectList mco : contextObjects) {
            List<Object> objectList = mco.getList();
            if (objectList != null) {
                Set<URI> tempSet = mco.getList().stream().map(x -> (URI) x).collect(Collectors.toSet());
                uris.addAll(tempSet);
            }
        }
        return uris;
    }

    @Override
    public Object getSingleValue(String collectionName) {
        checkValidCollectionName(collectionName);
        return get(collectionName, "singleVal");
    }

    @Override
    public void setSingleValue(String collectionName, Serializable value) {
        checkValidCollectionName(collectionName);
        save(collectionName, "singleVal", value);
    }

    @Override
    public boolean isAtomKnown(final URI atomURI) {
        // query the field "objectList" since this is the name of the member variable of
        // MongoContextObjectList
        Query query = new Query(Criteria.where("objectList.string").is(atomURI.toString()));
        return null != template.find(query, String.class, ATOM_URI_COLLECTION);
    }

    @Override
    public void removeAtomUriFromNamedAtomUriList(final URI uri, final String name) {
        MongoContextObject obj = new MongoContextObject(uri.toString(), uri);
        template.remove(obj, name);
    }

    @Override
    public void appendToNamedAtomUriList(final URI uri, final String name) {
        MongoContextObject obj = new MongoContextObject(uri.toString(), uri);
        template.insert(obj, name);
    }

    @Override
    public List<URI> getNamedAtomUriList(final String name) {
        return template.findAll(URI.class, name);
    }

    @Override
    public boolean isInNamedAtomUriList(URI uri, String name) {
        List<URI> uris = getNamedAtomUriList(name);
        for (URI tmpUri : uris) {
            if (tmpUri.equals(uri)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isNodeKnown(final URI wonNodeURI) {
        return null != get(NODE_URI_COLLECTION, wonNodeURI.toString());
    }

    @Override
    public void rememberNodeUri(final URI uri) {
        save(NODE_URI_COLLECTION, uri.toString(), uri);
    }

    @Override
    public void removeNodeUri(final URI uri) {
        remove(NODE_URI_COLLECTION, uri.toString());
    }

    /**
     * Use this method to make sure that certain collections (atom and node uris)
     * are only accessed with non-generic methods
     *
     * @param collectionName
     */
    private void checkValidCollectionName(String collectionName) {
        if (collectionName == null || collectionName.equals(ATOM_URI_COLLECTION)
                        || collectionName.equals(NODE_URI_COLLECTION) || collectionName.trim().isEmpty()) {
            throw new IllegalArgumentException("Generic collection name must be valid and not one of the following:"
                            + " " + NODE_URI_COLLECTION + ", " + ATOM_URI_COLLECTION);
        }
    }

    @Override
    public void dropCollection(String collectionName) {
        template.dropCollection(collectionName);
    }

    @Override
    public void saveToObjectMap(String collectionName, String key, final Serializable value) {
        checkValidCollectionName(collectionName);
        save(collectionName, key, value);
    }

    private void save(String collectionName, String key, final Object value) {
        MongoContextObject mco = new MongoContextObject(key, value);
        template.save(mco, collectionName);
    }

    @Override
    public final Object loadFromObjectMap(String collectionName, String key) {
        checkValidCollectionName(collectionName);
        return get(collectionName, key);
    }

    private Object get(String collectionName, String key) {
        MongoContextObject mco = template.findById(key, MongoContextObject.class, collectionName);
        if (mco != null) {
            return mco.getObject();
        }
        return null;
    }

    @Override
    public Map<String, Object> loadObjectMap(String collectionName) {
        checkValidCollectionName(collectionName);
        List<MongoContextObject> contextObjects = template.findAll(MongoContextObject.class, collectionName);
        HashMap<String, Object> objectMap = new HashMap<>();
        for (MongoContextObject mco : contextObjects) {
            objectMap.put(mco.getId(), mco.getObject());
        }
        return objectMap;
    }

    @Override
    public final void removeFromObjectMap(String collectionName, String key) {
        checkValidCollectionName(collectionName);
        remove(collectionName, key);
    }

    private void remove(String collectionName, String key) {
        MongoContextObject mco = new MongoContextObject(key, null);
        template.remove(mco, collectionName);
    }

    @Override
    public List<Object> loadFromListMap(final String collectionName, final String key) {
        checkValidCollectionName(collectionName);
        MongoContextObjectList mco = template.findById(key, MongoContextObjectList.class, collectionName);
        return mco == null ? new LinkedList<>() : mco.getList();
    }

    @Override
    public void addToListMap(final String collectionName, final String key, final Serializable... value) {
        checkValidCollectionName(collectionName);
        push(collectionName, key, value);
    }

    private void push(String collectionName, String key, final Serializable... value) {
        Update update = new Update();
        Query query = new Query(Criteria.where("_id").is(key));
        // push values to the field "objectList" since this is the name of the member
        // variable of MongoContextObjectList
        // from there we can easily access it again for loading
        template.upsert(query, update.pushAll("objectList", value), collectionName);
    }

    @Override
    public void removeFromListMap(final String collectionName, final String key, final Serializable... values) {
        checkValidCollectionName(collectionName);
        pull(collectionName, key, values);
    }

    public void removeLeavesFromListMap(String collectionName, final Serializable... values) {
        checkValidCollectionName(collectionName);
        for (String key : loadListMap(collectionName).keySet()) {
            removeFromListMap(collectionName, key, values);
        }
    }

    private void pull(String collectionName, String key, final Serializable... values) {
        Update update = new Update();
        Query query = new Query(Criteria.where("_id").is(key));
        // pull values of the field "objectList" since this is the name of the member
        // variable of MongoContextObjectList
        template.upsert(query, update.pullAll("objectList", values), collectionName);
    }

    @Override
    public Map<String, List<Object>> loadListMap(final String collectionName) {
        checkValidCollectionName(collectionName);
        List<MongoContextObjectList> contextObjects = template.findAll(MongoContextObjectList.class, collectionName);
        HashMap<String, List<Object>> objectMap = new HashMap<>();
        for (MongoContextObjectList mco : contextObjects) {
            objectMap.put(mco.getId(), mco.getList());
        }
        return objectMap;
    }

    @Override
    public void removeFromListMap(final String collectionName, final String key) {
        checkValidCollectionName(collectionName);
        MongoContextObjectList mco = new MongoContextObjectList(key);
        template.remove(mco, collectionName);
    }
}
