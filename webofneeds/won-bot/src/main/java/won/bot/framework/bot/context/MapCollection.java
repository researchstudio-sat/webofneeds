package won.bot.framework.bot.context;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Created by hfriedrich on 24.10.2016.
 */
public class MapCollection implements Map<Object, Object>
{
  private String collectionName;

  public MapCollection(String name) {
    collectionName = name;
  }

  @Override
  public int size() {
    return 0;
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public boolean containsKey(final Object key) {

    return false;
  }

  @Override
  public boolean containsValue(final Object value) {
    return false;
  }

  @Override
  public Object get(final Object key) {
    return null;
  }

  @Override
  public Object put(final Object key, final Object value) {
    return null;
  }

  @Override
  public Object remove(final Object key) {
    return null;
  }

  @Override
  public void putAll(final Map<?, ?> m) {

  }

  @Override
  public void clear() {

  }

  @Override
  public Set<Object> keySet() {
    return null;
  }

  @Override
  public Collection<Object> values() {
    return null;
  }

  @Override
  public Set<Entry<Object, Object>> entrySet() {
    return null;
  }
}
