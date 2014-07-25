package won.owner.protocol.impl;

import com.hp.hpl.jena.rdf.model.Model;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.Session;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * User: LEIH-NB
 * Date: 18.10.13
 */
public class NeedMessageCreatorImpl implements NeedMessageCreator{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private URI ownerURI;
    private Model model;
    private boolean activate;

    public NeedMessageCreatorImpl(URI ownerURI, Model model, boolean activate){
        this.ownerURI = ownerURI;
        this.model = model;
        this.activate = activate;
    }

    public Message createMessage(Session session) throws JMSException {
        MapMessage message = session.createMapMessage();
        message.setObject("URI", ownerURI);
        message.setObject("Model", model);
        message.setBoolean("activation", activate);
        return message;
    }


}
