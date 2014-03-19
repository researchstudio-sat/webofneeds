package won.bot.framework.events.listener.baStateBots;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 12.3.14.
 * Time: 18.13
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

    //Business Activities
    public static final Resource MESSAGE_CANCEL = m.createResource(BASE_URI + "MessageCancel");
    public static final Resource MESSAGE_CLOSE = m.createResource(BASE_URI + "MessageClose");
    public static final Resource MESSAGE_COMPENSATE = m.createResource(BASE_URI + "MessageCompensate");
    public static final Resource MESSAGE_FAILED = m.createResource(BASE_URI + "MessageFailed");
    public static final Resource MESSAGE_EXITED = m.createResource(BASE_URI + "MessageExited");
    public static final Resource MESSAGE_NOTCOMPLETED = m.createResource(BASE_URI + "MessageNotCompleted");

    public static final Resource MESSAGE_EXIT = m.createResource(BASE_URI + "MessageExit");
    public static final Resource MESSAGE_COMPLETE = m.createResource(BASE_URI + "MessageComplete");
    public static final Resource MESSAGE_COMPLETED = m.createResource(BASE_URI + "MessageCompleted");
    public static final Resource MESSAGE_FAIL = m.createResource(BASE_URI + "MessageFail");
    public static final Resource MESSAGE_CANNOTCOMPLETE= m.createResource(BASE_URI + "MessageCanNotComplete");
    public static final Resource MESSAGE_CANCELED = m.createResource(BASE_URI + "MessageCanceled");
    public static final Resource MESSAGE_CLOSED = m.createResource(BASE_URI + "MessageClosed");
    public static final Resource MESSAGE_COMPENSATED = m.createResource(BASE_URI + "MessageCompensated");


    public static final Property STATE = m.createProperty(BASE_URI + "hasState");

    //Business Activities  -PC
    public static final Resource STATE_ACTIVE = m.createResource(BASE_URI + "StateActive");
    public static final Resource STATE_COMPLETING = m.createResource(BASE_URI + "StateCompleting");
    public static final Resource STATE_COMPLETED = m.createResource(BASE_URI + "StateCompleted");
    public static final Resource STATE_CLOSING= m.createResource(BASE_URI + "StateClosing");
    public static final Resource STATE_ENDED = m.createResource(BASE_URI + "StateEnded");
    public static final Resource STATE_EXITING = m.createResource(BASE_URI + "StateExiting");
    public static final Resource STATE_COMPENSATING = m.createResource(BASE_URI + "StateCompensating");
    public static final Resource STATE_CANCELING_COMPLETING = m.createResource(BASE_URI + "CancelingCompleting");
    public static final Resource STATE_NOT_COMPLETING = m.createResource(BASE_URI + "StateNotCompleting");
    public static final Resource STATE_FAILING_COMPENSATING = m.createResource(BASE_URI + "FailingCompensating");
    public static final Resource STATE_FAILING_ACTIVE_CANCELING_COMPLETING = m.createResource(BASE_URI + "FailingActiveCancelingCompleting");
    public static final Resource STATE_CANCELING_ACTIVE = m.createResource(BASE_URI + "CancelingActive");

    //Business Activities -CC
    public static final Resource STATE_CANCELING = m.createResource(BASE_URI + "Canceling");
    public static final Resource STATE_FAILING_ACTIVE_CANCELING = m.createResource(BASE_URI + "FailingActiveCanceling");


















    /** returns the URI for this schema
     * @return the URI for this schema
     */
    public static String getURI() {
        return BASE_URI;
    }
}