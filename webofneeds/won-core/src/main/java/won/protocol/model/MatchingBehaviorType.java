package won.protocol.model;

import won.protocol.vocabulary.WON;

import java.net.URI;

/**
 * Created by hfriedrich on 20.03.2017.
 */
public enum MatchingBehaviorType
{
  MUTUAL("Mutual"), //It's ok to send me a hint and it's ok to send the partner a hint
  DO_NOT_MATCH("DoNotMatch"), //Do not send any hints
  LAZY("Lazy"), //Send hint to partners, not to me
  STEALTHY("Stealthy"), //Send hints to me, not to partner
  GREEDY("Greedy"); //like MUTUAL, but I really want the hint (even if the partner is STEALTHY)

  private String name;

  MatchingBehaviorType(String name) {
    this.name = name;
  }

  /**
   * Returns true if a hint should be send to this need given the partner's matchingBehavior.
   * @param partnerType
   * @return
     */
  public boolean shouldSendHintGivenPartnerMatchingBehavior(MatchingBehaviorType partnerType){
    if (this == LAZY) return false;
    if (this == DO_NOT_MATCH) return false;
    switch (this){
      case MUTUAL:
        return partnerType != STEALTHY;
      case STEALTHY:
        return true;
      case  GREEDY:
        return true;
    }
    throw new IllegalArgumentException("could not determine if a hint should be sent to need of type " + this + " and partner need of type " + partnerType);
  }

  public URI getURI()
  {
    return URI.create(WON.BASE_URI + name);
  }

  /**
   * Tries to match the given URI against all enum values.
   *
   * @param uri URI to match
   * @return matched enum, null otherwise
   */
  public static MatchingBehaviorType fromURI(final URI uri)
  {
    for (MatchingBehaviorType type : values())
      if (type.getURI().equals(uri))
        return type;
    return null;
  }

  /**
   * Tries to match the given URI against all enum values.
   *
   * @param uri URI to match
   * @return matched enum, null otherwise
   */
  public static MatchingBehaviorType fromURI(final String uri)
  {
    for (MatchingBehaviorType type : values())
      if (type.getURI().toString().equals(uri))
        return type;
    return null;
  }
}
