package won.shacl2java.runtime;

import won.shacl2java.runtime.model.GraphEntity;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Provides frequently needed comparisons using
 * <code>GraphEntity.deepEquals()</code>.
 */
public class DeepEqualsUtils {
    /**
     * Returns true if the <code>collection</code> contains the <code>entity</code>,
     * using {@link GraphEntity#deepEquals(Object)} for comparing elements.
     *
     * @param collection the collection to check
     * @param entity the entity to find in the collection
     * @param <T>
     * @return true if <code>collection</code> deep-equals-contains
     * <code>entity</code>
     */
    public static <T extends GraphEntity> boolean contains(Collection<T> collection, T entity) {
        return collection.stream().anyMatch(e -> e.deepEquals(entity));
    }

    /**
     * Retrieves an element in the <code>collection</code> found to be equal to
     * <code>entity</code> using {@link GraphEntity#deepEquals(Object)}.
     *
     * @param collection
     * @param entity
     * @param <T>
     * @return an element of the collection that is deep-equal to the entity.
     */
    public static <T extends GraphEntity> Optional<T> find(Collection<T> collection, T entity) {
        return collection.stream().filter(e -> e.deepEquals(entity)).findAny();
    }

    /**
     * Returns true if the <code>collection</code> contains all elements of
     * <code>toFind</code>, using {@link GraphEntity#deepEquals(Object)} for
     * comparing elements.
     *
     * @param collection the collection to check
     * @param toFind the entities to find in the collection
     * @param <T>
     * @return true if <code>collection</code> deep-equals-contains all of
     * <code>toFind</code>
     */
    public static <T extends GraphEntity> boolean containsAll(Collection<T> collection, Collection<T> toFind) {
        return toFind.stream().allMatch(e -> contains(collection, e));
    }

    /**
     * Returns true if the <code>left</code> and <code>right</code> contain the same
     * elements, using {@link GraphEntity#deepEquals(Object)} </code> for comparing
     * elements, or if both are null or empty.
     *
     * @param left a collection {@link GraphEntity}
     * @param right a colleciton of {@link GraphEntity}
     * @param <T>
     * @return true if <code>left</code> and <code>right</code> are both null, both
     * empty, or contain the deep-equals-same elements.
     */
    public static <T extends GraphEntity> boolean sameContent(Collection<T> left, Collection<T> right) {
        if (left == null) {
            return right == null;
        }
        if (right == null) {
            return false;
        }
        if (left.isEmpty()) {
            return right.isEmpty();
        }
        if (right.isEmpty()) {
            return false;
        }
        left = deepEqualsDeduplicate(left);
        right = deepEqualsDeduplicate(right);
        return left.size() == right.size() && containsAll(left, right);
    }

    /**
     * Returns true if <code>left</code> and <code>right</code> are both null or
     * equal using {@link GraphEntity#deepEquals(Object)}.
     *
     * @param left
     * @param right
     * @param <T>
     * @return true if both are null or deep-equal.
     */
    public static <T extends GraphEntity> boolean same(T left, T right) {
        if (left == null) {
            return right == null;
        }
        if (right == null) {
            return false;
        }
        return left.deepEquals(right);
    }

    /**
     * Returns a new collection containing all distinct elements of the specified
     * <code>collection</code>, using {@link GraphEntity#deepEquals(Object)} for
     * comparison.
     *
     * @param collection the colleciton to deduplicate
     * @param <T>
     * @return a new collection without deep-equals duplicates
     */
    public static <T extends GraphEntity> Collection<T> deepEqualsDeduplicate(Collection<T> collection) {
        Set<T> result = new HashSet<>();
        for (T element : collection) {
            if (!contains(result, element)) {
                result.add(element);
            }
        }
        return result;
    }
}
