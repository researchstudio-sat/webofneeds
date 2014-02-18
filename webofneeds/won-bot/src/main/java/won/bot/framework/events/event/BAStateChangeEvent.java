package won.bot.framework.events.event;

import com.hp.hpl.jena.rdf.model.Model;
import won.bot.framework.events.Event;
import won.protocol.model.ChatMessage;
import won.protocol.model.Connection;
import won.protocol.model.FacetType;


/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 13.2.14.
 * Time: 11.04
 * To change this template use File | Settings | File Templates.
 */


public class BAStateChangeEvent extends BaseEvent
{
    private final Connection con;
    private final ChatMessage message;
    private final FacetType facetType;
    private final Model content;

    public BAStateChangeEvent(final Connection con, final ChatMessage message, final FacetType facetType, final Model content)
    {
        this.con = con;
        this.message = message;
        this.facetType = facetType;
        this.content = content;
    }

    public Connection getCon()
    {
        return con;
    }

    public ChatMessage getMessage()
    {
        return message;
    }

    public FacetType getFacetType()
    {
        return facetType;
    }

    public Model getContent()
    {
        return content;
    }
}
