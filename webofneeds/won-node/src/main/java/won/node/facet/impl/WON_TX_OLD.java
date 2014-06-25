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
public class WON_TX_OLD
{
   // public static final String BASE_URI = "http://purl.org/webofneeds/tx/model#";
    public static final String BASE_URI = "http://purl.org/webofneeds/tx/model#";


    public static final String DEFAULT_PREFIX= "won-tx";


    private static Model m = ModelFactory.createDefaultModel();

    public static final Property COORDINATION_MESSAGE = m.createProperty(BASE_URI + "coordinationMessage");
    public static final Property COORDINATOR_VOTE_REQUEST = m.createProperty(BASE_URI + "coordinatorVoteRequest");
    public static final Property BA_STATE = m.createProperty(BASE_URI + "hasBAState");

    public static final Resource COORDINATOR = m.createResource(BASE_URI + "Coordinator");
    public static final Resource PARTICIPANT = m.createResource(BASE_URI + "Participant");
    public static final Resource COORDINATION_MESSAGE_ABORT = m.createResource(BASE_URI + "Abort");
    public static final Resource COORDINATION_MESSAGE_COMMIT = m.createResource(BASE_URI + "Commit");
    public static final Resource COORDINATION_MESSAGE_ABORT_AND_COMPENSATE = m.createResource(BASE_URI + "AbortAndCompensate");

    //Business Activities
    public static final Resource MESSAGE_CANCEL = m.createResource(BASE_URI + "MessageCancel");
    public static final Resource MESSAGE_CLOSE = m.createResource(BASE_URI + "MessageClose");
    public static final Resource MESSAGE_COMPENSATE = m.createResource(BASE_URI + "MessageCompensate");
    public static final Resource MESSAGE_FAILED = m.createResource(BASE_URI + "MessageFailed");
    public static final Resource MESSAGE_EXITED = m.createResource(BASE_URI + "MessageExited");
    public static final Resource MESSAGE_NOTCOMPLETED = m.createResource(BASE_URI + "MessageNotCompleted");

    public static final Resource MESSAGE_EXIT = m.createResource(BASE_URI + "MessageExit");
    public static final Resource MESSAGE_COMPLETED = m.createResource(BASE_URI + "MessageCompleted");
    public static final Resource MESSAGE_FAIL = m.createResource(BASE_URI + "MessageFail");
    public static final Resource MESSAGE_CANNOTCOMPLETE= m.createResource(BASE_URI + "MessageCanNotComplete");
    public static final Resource MESSAGE_CANCELED = m.createResource(BASE_URI + "MessageCanceled");
    public static final Resource MESSAGE_CLOSED = m.createResource(BASE_URI + "MessageClosed");
    public static final Resource MESSAGE_COMPENSATED = m.createResource(BASE_URI + "MessageCompensated");

    public static final Resource STATE_CLOSING = m.createResource(BASE_URI + "Closing");
    public static final Resource STATE_ACTIVE = m.createResource(BASE_URI + "Active");
    public static final Resource STATE_COMPLETED = m.createResource(BASE_URI + "Completed");
    public static final Resource STATE_CANCELING = m.createResource(BASE_URI + "Canceling");
    public static final Resource STATE_COMPENSATING = m.createResource(BASE_URI + "Compensating");
    public static final Resource STATE_COMPLETING = m.createResource(BASE_URI + "Completing");
    public static final Resource STATE_CANCELING_ACTIVE = m.createResource(BASE_URI + "CancelingActive");
    public static final Resource STATE_CANCELING_COMPLETING = m.createResource(BASE_URI + "CancelingCompleting");



    /** returns the URI for this schema
     * @return the URI for this schema
     */
    public static String getURI() {
        return BASE_URI;
    }
}
