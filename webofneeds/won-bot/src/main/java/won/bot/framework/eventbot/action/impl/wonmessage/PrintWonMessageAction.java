package won.bot.framework.eventbot.action.impl.wonmessage;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.MessageEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.WonMessageSentEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.message.WonMessage;
import won.protocol.util.WonRdfUtils;

public class PrintWonMessageAction extends BaseEventBotAction {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public PrintWonMessageAction(EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        if (!(event instanceof MessageEvent)) {
            return;
        }
        WonMessage msg = ((MessageEvent) event).getWonMessage();
        if (msg.getMessageType().isResponseMessage()) {
            // we don't print responses
            return;
        }
        if (event instanceof WonMessageSentEvent) {
            handleSentMessage(msg);
        } else {
            handleReceivedMessage(msg);
        }
    }

    private void handleReceivedMessage(WonMessage msg) {
        StringBuilder output = new StringBuilder();
        output.append("+-<<< Received " + msg.getMessageType() + " <<<---\n");
        output.append("| message uri: " + msg.getMessageURI() + "\n");
        output.append("| sender: " + getSender(msg) + "\n");
        Optional<String> textMessage = getTextMessage(msg);
        if (textMessage.isPresent()) {
            output.append("| message : \n");
            output.append(format(textMessage.get()));
        }
        output.append("+-<<< End of Message <<<---");
        System.out.println(output.toString());
    }

    private void handleSentMessage(WonMessage msg) {
        StringBuilder output = new StringBuilder();
        output.append("+->>> Sent " + msg.getMessageType() + ">>>---\n");
        output.append("| message uri: " + msg.getMessageURI() + "\n");
        output.append("| recipient: " + getRecipient(msg) + "\n");
        Optional<String> textMessage = getTextMessage(msg);
        if (textMessage.isPresent()) {
            output.append("| message : \n");
            output.append(format(textMessage.get()));
        }
        output.append("+->>> End of Message >>>---");
        System.out.println(output.toString());
    }

    private URI getSender(WonMessage msg) {
        URI ret = msg.getSenderURI();
        if (ret != null) {
            return ret;
        }
        ret = msg.getSenderSocketURI();
        if (ret != null) {
            return ret;
        }
        ret = msg.getSenderAtomURI();
        if (ret != null) {
            return ret;
        }
        ret = msg.getSenderNodeURI();
        if (ret != null) {
            return ret;
        }
        logger.warn("could not get a sender URI from message " + msg.getMessageURI());
        return null;
    }

    private URI getRecipient(WonMessage msg) {
        URI ret = msg.getRecipientURI();
        if (ret != null) {
            return ret;
        }
        ret = msg.getRecipientSocketURI();
        if (ret != null) {
            return ret;
        }
        ret = msg.getRecipientAtomURI();
        if (ret != null) {
            return ret;
        }
        ret = msg.getRecipientNodeURI();
        if (ret != null) {
            return ret;
        }
        logger.warn("could not get a recipient URI from message " + msg.getMessageURI());
        return null;
    }

    private Optional<String> getTextMessage(WonMessage msg) {
        return Optional.ofNullable(WonRdfUtils.MessageUtils.getTextMessage(msg));
    }

    private String format(String text) {
        int lineLength = 117;
        String[] lines = text.split("\\r?\\n");
        StringBuilder formatted = new StringBuilder();
        String lineToFormat = null;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.length() <= lineLength) {
                lineToFormat = line;
            } else {
                lineToFormat = line.substring(0, lineLength);
                lines[i] = line.substring(lineLength);
                i--; // process same array item again
            }
            formatted.append("|  " + lineToFormat + "\n");
        }
        return formatted.toString();
    }
}
