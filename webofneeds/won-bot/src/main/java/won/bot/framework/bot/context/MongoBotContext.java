package won.bot.framework.bot.context;

import won.bot.framework.bot.BotContext;

import java.net.URI;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by hfriedrich on 24.10.2016.
 */
public class MongoBotContext implements BotContext
{
  @Override
  public List<URI> listNeedUris() {
    return new LinkedList<>();
  }

  @Override
  public boolean isNeedKnown(final URI needURI) {
    return false;
  }

  @Override
  public boolean isNodeKnown(final URI wonNodeURI) {
    return false;
  }

  @Override
  public void rememberNodeUri(final URI uri) {

  }

  @Override
  public void appendToNamedNeedUriList(final URI uri, final String name) {

  }

  @Override
  public List<URI> getNamedNeedUriList(final String name) {
    return null;
  }

  @Override
  public void removeNeedUriFromNamedNeedUriList(final URI uri, final String name) {

  }

  @Override
  public void put(String collectionName, String key, final Object value) {

  }

  @Override
  public Object get(String collectionName, String key) {
    return null;
  }

  @Override
  public Collection<Object> values(final String collectionName) {
    return null;
  }

  @Override
  public Object remove(final String collectionName, String key) {
    return null;
  }

}
