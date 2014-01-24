package won.node.facet.impl;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 14.1.14.
 * Time: 12.06
 * To change this template use File | Settings | File Templates.
 */
public class WON_BA {
   // public static final String BASE_URI = "http://purl.org/webofneeds/tx/model#";
    public static final String BASE_URI = "http://purl.org/webofneeds/model#";

    public static final String DEFAULT_PREFIX= "won-tx";


    private static Model m = ModelFactory.createDefaultModel();

    public static final Property COORDINATION_MESSAGE = m.createProperty(BASE_URI + "coordinationMessage");
    public static final Property COORDINATOR_VOTE_REQUEST = m.createProperty(BASE_URI + "coordinatorVoteRequest");
    public static final Resource COORDINATOR = m.createResource(BASE_URI + "Coordinator");
    public static final Resource PARTICIPANT = m.createResource(BASE_URI + "Participant");
    public static final Resource COORDINATION_MESSAGE_ABORT = m.createResource(BASE_URI + "Abort");
    public static final Resource COORDINATION_MESSAGE_COMMIT = m.createResource(BASE_URI + "Commit");
    public static final Resource COORDINATION_MESSAGE_ABORT_AND_COMPENSATE = m.createResource(BASE_URI + "AbortAndCompensate");


    public static final Resource MESSAGE_CANCEL = m.createResource(BASE_URI + "MessageCancel");
    public static final Resource MESSAGE_CANCEL = m.createResource(BASE_URI + "MessageCancel");
    public static final Resource MESSAGE_CANCEL = m.createResource(BASE_URI + "MessageCancel");
    public static final Resource MESSAGE_CANCEL = m.createResource(BASE_URI + "MessageCancel");

    /** returns the URI for this schema
     * @return the URI for this schema
     */
    public static String getURI() {
        return BASE_URI;
    }
}
