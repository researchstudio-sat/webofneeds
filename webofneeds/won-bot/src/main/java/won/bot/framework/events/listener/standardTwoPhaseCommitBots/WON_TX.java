package won.bot.framework.events.listener.standardTwoPhaseCommitBots;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * User: Danijel
 * Date: 21.5.14.
 */
public class WON_TX
{
  public static final String BASE_URI = "http://purl.org/webofneeds/tx/model#";
  public static final String DEFAULT_PREFIX= "won-ba";

  private static Model m = ModelFactory.createDefaultModel();

  public static final Property COORDINATION_MESSAGE = m.createProperty(BASE_URI + "coordinationMessage");
  public static final Property COORDINATOR_VOTE_REQUEST = m.createProperty(BASE_URI + "coordinatorVoteRequest");
  public static final Resource COORDINATOR = m.createResource(BASE_URI + "Coordinator");
  public static final Resource PARTICIPANT = m.createResource(BASE_URI + "Participant");
  public static final Resource COORDINATION_MESSAGE_ABORT = m.createResource(BASE_URI + "Abort");
  public static final Resource COORDINATION_MESSAGE_COMMIT = m.createResource(BASE_URI + "Commit");
  public static final Resource COORDINATION_MESSAGE_ABORT_AND_COMPENSATE = m.createResource(BASE_URI + "AbortAndCompensate");


  /** returns the URI for this schema
   * @return the URI for this schema
   */
  public static String getURI() {
    return BASE_URI;
  }
}
