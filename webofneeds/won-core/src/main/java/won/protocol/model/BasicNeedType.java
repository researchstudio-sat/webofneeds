package won.protocol.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.vocabulary.WON;

import java.net.URI;

/**
 * User: Alan Tus
 * Date: 17.04.13
 * Time: 13:38
 */
public enum BasicNeedType
{

  DEMAND("Demand"),
  SUPPLY("Supply"),
  DO_TOGETHER("DoTogether"),
  CRITIQUE("Critique");

  private static final Logger logger = LoggerFactory.getLogger(BasicNeedType.class);

  private String name;

  private BasicNeedType(String name)
  {
    this.name = name;
  }

  public URI getURI()
  {
    return URI.create(WON.BASE_URI + name);
  }

  /**
   * Tries to match the given string against all enum values.
   *
   * @param fragment string to match
   * @return matched enum, null otherwise
   */
  public static BasicNeedType parseString(final String fragment)
  {
    for (BasicNeedType state : values())
      if (state.name.equals(fragment))
        return state;
    return null;
  }

  /**
   * Tries to match the given URI against all enum values.
   *
   * @param uri URI to match
   * @return matched enum, null otherwise
   */
  public static BasicNeedType fromURI(final URI uri)
  {
    for (BasicNeedType state : values())
      if (state.getURI().equals(uri))
        return state;
    return null;
  }

  public BasicNeedType getMatchesWith() {
    switch (this) {
      case SUPPLY: return DEMAND;
      case DEMAND: return SUPPLY;
      case DO_TOGETHER: return DO_TOGETHER;
      case CRITIQUE: return CRITIQUE;
    }

    logger.warn("BasicNeedType could not be matched.");

    return null;
  }
}
