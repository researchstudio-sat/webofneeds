package won.node.camel.service;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import won.node.service.persistence.AtomService;
import won.protocol.jms.MessagingService;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;

@Component
public class CamelWonMessageService {
    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    AtomService atomService;
    @Autowired
    MessagingService messagingService;

    /**
     * Processes the system message (allowing socket implementations) and delivers
     * it, depending on its receiver settings.
     *
     * @param message
     */
    public void sendSystemMessage(WonMessage message) {
        Map headerMap = new HashMap<String, Object>();
        headerMap.put(WonCamelConstants.MESSAGE_HEADER, message);
        if (logger.isDebugEnabled()) {
            logger.debug("sending system message: {}", message.toStringForDebug(true));
        }
        messagingService.sendInOnlyMessage(null, headerMap, null, "seda:SystemMessageIn");
    }
}
