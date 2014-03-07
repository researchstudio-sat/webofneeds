package won.bot.framework.events.listener.baStateBots;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 5.3.14.
 * Time: 16.50
 * To change this template use File | Settings | File Templates.
 */
public class WON_BACC {
    // public static final String BASE_URI = "http://purl.org/webofneeds/tx/model#";
    public static final String BASE_URI = "http://purl.org/webofneeds/model#";


    public static final String DEFAULT_PREFIX= "won-tx";


    private static Model m = ModelFactory.createDefaultModel();

    public static final Property COORDINATION_MESSAGE = m.createProperty(BASE_URI + "transitMessage");
    public static final Property STATE = m.createProperty(BASE_URI + "hasState");

    //Business Activities
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






    /** returns the URI for this schema
     * @return the URI for this schema
     */
    public static String getURI() {
        return BASE_URI;
    }
}

