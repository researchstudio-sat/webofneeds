package won.bot.framework.eventbot.action.impl.mail.receive;

import com.hp.hpl.jena.query.Dataset;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.action.impl.mail.model.ActionType;
import won.bot.framework.eventbot.action.impl.mail.model.SubscribeStatus;
import won.bot.framework.eventbot.action.impl.mail.model.WonURI;
import won.bot.framework.eventbot.action.impl.mail.send.WonMimeMessage;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.SendTextMessageOnConnectionEvent;
import won.bot.framework.eventbot.event.impl.mail.CloseConnectionEvent;
import won.bot.framework.eventbot.event.impl.mail.MailCommandEvent;
import won.bot.framework.eventbot.event.impl.mail.OpenConnectionEvent;
import won.bot.framework.eventbot.event.impl.mail.SubscribeUnsubscribeEvent;
import won.bot.framework.eventbot.event.impl.needlifecycle.NeedDeactivatedEvent;
import won.protocol.util.WonRdfUtils;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.AuthenticationException;
import java.io.IOException;
import java.net.URI;
import java.security.AccessControlException;
import java.util.List;

import static jdk.nashorn.internal.runtime.regexp.joni.Config.log;

/**
 * Created by fsuda on 18.10.2016.
 */
public class MailCommandAction extends BaseEventBotAction {
    private MailContentExtractor mailContentExtractor;

    public MailCommandAction(EventListenerContext eventListenerContext, MailContentExtractor mailContentExtractor) {
        super(eventListenerContext);
        this.mailContentExtractor = mailContentExtractor;
    }

    @Override
    protected void doRun(Event event) throws Exception {

        if(event instanceof MailCommandEvent) {

            MimeMessage message = ((MailCommandEvent) event).getMessage();
            String referenceId = MailContentExtractor.getMailReference(message);

            // determine if the mail is referring to some other mail/need/connection or not
            if (referenceId != null) {
                processReferenceMailCommands(message, referenceId);
            } else {
                processNonReferenceMailCommand(message);
            }
        }
    }

    private void processNonReferenceMailCommand(MimeMessage message) throws IOException, MessagingException {

        EventBus bus = getEventListenerContext().getEventBus();
        ActionType mailAction = mailContentExtractor.getMailAction(message);

        switch(mailAction) {
            case SUBSCRIBE:
                bus.publish(new SubscribeUnsubscribeEvent(message, SubscribeStatus.SUBSCRIBED));
                break;

            case UNSUBSCRIBE:
                bus.publish(new SubscribeUnsubscribeEvent(message, SubscribeStatus.UNSUBSCRIBED));
                break;
            case CLOSE_NEED:
                URI needUri = retrieveNeedUriFromMail(message);
                if(needUri != null) {
                    bus.publish(new NeedDeactivatedEvent(needUri));
                }
                break;
            case NO_ACTION:
            default:
                //INVALID COMMAND
                logger.error("No command was given or assumed");
                break;
        }
    }

    private void processReferenceMailCommands(MimeMessage message, String referenceId) {

        EventBus bus = getEventListenerContext().getEventBus();
        try{
            WonURI wonUri = EventBotActionUtils.getWonURIForMailId(getEventListenerContext(), referenceId);

            if(wonUri == null){
                throw new NullPointerException("No corresponding wonUri found");
            }

            URI needUri;
            switch(wonUri.getType()){
                case CONNECTION:
                    Dataset connectionRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(wonUri.getUri());
                    needUri = WonRdfUtils.NeedUtils.getLocalNeedURIFromConnection(connectionRDF, wonUri.getUri());
                    break;
                case NEED:
                default:
                    needUri = wonUri.getUri();
                    break;
            }

            MimeMessage originalMessage = EventBotActionUtils.getMimeMessageForURI(getEventListenerContext(), needUri);

            if(originalMessage == null) {
                throw new NullPointerException("no originalmessage found");
            }

            logger.debug("Validate mailorigin with originalmail:");
            logger.debug("Command Message Sender: "+message.getFrom());
            logger.debug("Original Message Sender: "+originalMessage.getFrom());

            String senderNew = ((InternetAddress) message.getFrom()[0]).getAddress();
            String senderOriginal = ((InternetAddress) originalMessage.getFrom()[0]).getAddress();

            if(!senderNew.equals(senderOriginal)) {
                throw new AccessControlException("Sender of original and command mail are not equal");
            }else{
                logger.debug("Sender of original and command mail are not equal, continue with command processing");
            }

            ActionType actionType = determineAction(getEventListenerContext(), message, wonUri);
            logger.debug("Executing " + actionType + " on uri: " + wonUri.getUri() + " of type " + wonUri.getType());

            switch(actionType) {
                case CLOSE_CONNECTION:
                    bus.publish(new CloseConnectionEvent(wonUri.getUri()));
                    break;
                case OPEN_CONNECTION:
                    bus.publish(new OpenConnectionEvent(wonUri.getUri()));
                    break;
                case IMPLICIT_OPEN_CONNECTION:
                    bus.publish(new OpenConnectionEvent(wonUri.getUri(), mailContentExtractor.getTextMessage(message)));
                    break;
                case SENDMESSAGE:
                    bus.publish(new SendTextMessageOnConnectionEvent(mailContentExtractor.getTextMessage(message), wonUri.getUri()));
                    break;
                case CLOSE_NEED:
                    bus.publish(new NeedDeactivatedEvent(needUri));
                    break;
                case NO_ACTION:
                default:
                    //INVALID COMMAND
                    logger.error("No command was given or assumed");
                    break;
            }
        }catch (AccessControlException ace){
            logger.error("ACCESS RESTRICTION: sender of original and command mail are not equal, command will be blocked");
        } catch(Exception e){
            logger.error("no reply mail was set or found: "+e.getMessage());
        }
    }

    private URI retrieveNeedUriFromMail(MimeMessage message){
        try {
            String sender = ((InternetAddress) message.getFrom()[0]).getAddress();
            URI needURI = null;
            String titleToClose = mailContentExtractor.getTitle(message).trim();

            if (sender != null) {
                List<WonURI> needUris = EventBotActionUtils.getWonURIsForMailAddress(getEventListenerContext(), sender);

                for(WonURI u : needUris) {
                    Dataset needRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(u.getUri());
                    String needTitle = WonRdfUtils.NeedUtils.getNeedTitle(needRDF, u.getUri()).trim();

                    if(titleToClose.equals(needTitle) && WonRdfUtils.NeedUtils.isNeedActive(needRDF, u.getUri())){
                        return u.getUri();
                    }
                }
            }

            return needURI;
        }catch (MessagingException me){
            logger.error("could not extract information from mimemessage");
            return null;
        }
    }

    private ActionType determineAction(EventListenerContext ctx, MimeMessage message, WonURI wonUri) {
        try {

            ActionType mailAction = mailContentExtractor.getMailAction(message);
            switch(wonUri.getType()) {
                case CONNECTION:
                    boolean connected = WonRdfUtils.ConnectionUtils.isConnected(
                      ctx.getLinkedDataSource().getDataForResource(wonUri.getUri()), wonUri.getUri());
                    if (ActionType.CLOSE_CONNECTION.equals(mailAction)) {
                        return ActionType.CLOSE_CONNECTION;
                    }else if(!connected && ActionType.OPEN_CONNECTION.equals(mailAction)){
                        return ActionType.OPEN_CONNECTION;
                    }else if(connected){
                        return ActionType.SENDMESSAGE;
                    }else if(ActionType.CLOSE_NEED.equals(mailAction)){
                        return ActionType.CLOSE_NEED;
                    }else{
                        //if the connection is not connected yet and we do not parse any command we assume that the mailsender wants to establish a connection
                        return ActionType.IMPLICIT_OPEN_CONNECTION;
                    }
                case NEED:
                    if(ActionType.CLOSE_NEED.equals(mailAction)){
                        return ActionType.CLOSE_NEED;
                    }
                default:
                    return mailAction;
            }
        }catch(MessagingException me){
            logger.error("exception occurred checking command mail: {}", me);
            return ActionType.NO_ACTION;
        }catch(IOException ioe){
            logger.error("exception occurred checking command mail: {}", ioe);
            return ActionType.NO_ACTION;
        }
    }
}
