package won.owner.protocol.impl;

import org.springframework.jms.core.MessageCreator;
import com.hp.hpl.jena.rdf.model.Model;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.Session;
import java.net.URI;

/**
 * User: LEIH-NB
 * Date: 18.10.13
 */
public interface NeedMessageCreator extends MessageCreator {
}
