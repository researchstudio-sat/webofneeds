package won.protocol.model;

import won.protocol.vocabulary.WON;

import java.net.URI;

/**
 * User: Alan Tus
 * Date: 17.04.13
 * Time: 13:38
 */
public enum BasicNeedType
{

  TAKE("Take"),
  GIVE("Give"),
  DO("Do"),;

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

    System.err.println("No enum could be matched for: " + fragment);
    return null;
  }

}
