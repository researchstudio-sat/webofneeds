package won.bot.framework.bot.context;

import won.bot.framework.bot.BotContext;

import java.net.URI;
import java.util.List;

/**
 * Created by hfriedrich on 24.10.2016.
 */
public class MongoDbBotContext implements BotContext
{

  @Override
  public List<URI> listNeedUris() {
    return null;
  }

  @Override
  public List<URI> listNodeUris() {
    return null;
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
  public void rememberNeedUriWithName(final URI uri, final String name) {

  }

  @Override
  public void rememberNeedUri(final URI uri) {

  }

  @Override
  public void rememberNodeUri(final URI uri) {

  }

  @Override
  public void rememberNamedNeedUriList(final List<URI> uris, final String name) {

  }

  @Override
  public void appendToNamedNeedUriList(final URI uri, final String name) {

  }

  @Override
  public List<URI> getNamedNeedUriList(final String name) {
    return null;
  }

  @Override
  public void removeNeedUri(final URI uri) {

  }

  @Override
  public void removeNamedNeedUri(final String name) {

  }

  @Override
  public void removeNeedUriFromNamedNeedUriList(final URI uri, final String name) {

  }

  @Override
  public URI getNeedByName(final String name) {
    return null;
  }

  @Override
  public List<String> listNeedUriNames() {
    return null;
  }

  @Override
  public void put(final Object key, final Object value) {

  }

  @Override
  public Object get(final Object key) {
    return null;
  }
}
