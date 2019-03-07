package won.bot.framework.eventbot.action.impl.mail.receive;

import java.io.IOException;
import java.net.URI;
import java.security.AccessControlException;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang.StringUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;

import won.bot.framework.bot.context.MailBotContextWrapper;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.impl.mail.model.ActionType;
import won.bot.framework.eventbot.action.impl.mail.model.SubscribeStatus;
import won.bot.framework.eventbot.action.impl.mail.model.WonURI;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.close.CloseCommandEvent;
import won.bot.framework.eventbot.event.impl.command.connect.ConnectCommandEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.event.impl.command.deactivate.DeactivateNeedCommandEvent;
import won.bot.framework.eventbot.event.impl.mail.MailCommandEvent;
import won.bot.framework.eventbot.event.impl.mail.SubscribeUnsubscribeEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionModelMapper;
import won.protocol.model.NeedState;
import won.protocol.util.DefaultNeedModelWrapper;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;

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
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        EventListenerContext ctx = getEventListenerContext();
        if(event instanceof MailCommandEvent && ctx.getBotContextWrapper() instanceof MailBotContextWrapper){
            MailBotContextWrapper botContextWrapper = (MailBotContextWrapper) ctx.getBotContextWrapper();

            MimeMessage message = ((MailCommandEvent) event).getMessage();
            String referenceId = MailContentExtractor.getMailReference(message);

            WonURI wonUri = botContextWrapper.getWonURIForMailId(referenceId);

            // determine if the mail is referring to some other mail/need/connection or not
            if(wonUri != null){
                processReferenceMailCommands(message, wonUri);
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
                /*A need can be closed with a mail that matches the takenCmdPattern in its subject and has the same title
                 as a previously created need by the user*/
                URI needUri = retrieveCorrespondingNeedUriFromMailByTitle(message);
                if(needUri != null) {
                    bus.publish(new DeactivateNeedCommandEvent(needUri));
                }
                break;
            case NO_ACTION:
            default:
                //INVALID COMMAND
                logger.error("No command was given or assumed");
                break;
        }
    }

    private void processReferenceMailCommands(MimeMessage message, WonURI wonUri) {
        MailBotContextWrapper botContextWrapper = ((MailBotContextWrapper) getEventListenerContext().getBotContextWrapper());
        EventBus bus = getEventListenerContext().getEventBus();
        try{
            if(wonUri == null){
                throw new NullPointerException("No corresponding wonUri found");
            }

            URI needUri;
            URI remoteNeedUri = null;
            Dataset connectionRDF = null;

            switch(wonUri.getType()){
                case CONNECTION:
                    connectionRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(wonUri.getUri());
                    needUri = WonRdfUtils.ConnectionUtils.getLocalNeedURIFromConnection(connectionRDF, wonUri.getUri());
                    remoteNeedUri = WonRdfUtils.ConnectionUtils.getRemoteNeedURIFromConnection(connectionRDF, wonUri.getUri());
                    break;
                case NEED:
                default:
                    needUri = wonUri.getUri();
                    break;
            }

            MimeMessage originalMessage = botContextWrapper.getMimeMessageForURI(needUri);

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

            Connection con;

            switch(actionType) {
                case CLOSE_CONNECTION:
                    con = RdfUtils.findFirst(connectionRDF, x -> new ConnectionModelMapper().fromModel(x));
                    bus.publish(new CloseCommandEvent(con));
                    break;
                case OPEN_CONNECTION:
                    bus.publish(new ConnectCommandEvent(needUri, remoteNeedUri));
                    break;
                case IMPLICIT_OPEN_CONNECTION:
                    bus.publish(new ConnectCommandEvent(needUri, remoteNeedUri, mailContentExtractor.getTextMessage(message)));
                    break;
                case SENDMESSAGE:
                    con = RdfUtils.findFirst(connectionRDF, x -> new ConnectionModelMapper().fromModel(x));
                    Model messageModel = WonRdfUtils.MessageUtils.textMessage(mailContentExtractor.getTextMessage(message));
                    bus.publish(new ConnectionMessageCommandEvent(con, messageModel));
                    break;
                case CLOSE_NEED:
                    bus.publish(new DeactivateNeedCommandEvent(needUri));
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

    /**
     * This Method tries to find a corresponding open need uri from a user(given by the from adress) and returns the
     * corresponding need uri if there was an open need with the same title
     * @param message used to extract sender adress and subject(title)
     * @return
     */
    private URI retrieveCorrespondingNeedUriFromMailByTitle(MimeMessage message){
        try {
            MailBotContextWrapper botContextWrapper = ((MailBotContextWrapper) getEventListenerContext().getBotContextWrapper());
            String sender = ((InternetAddress) message.getFrom()[0]).getAddress();
            URI needURI = null;
            String titleToClose = mailContentExtractor.getTitle(message).trim();

            if (sender != null) {
                List<WonURI> needUris = botContextWrapper.getWonURIsForMailAddress(sender);

                for(WonURI u : needUris) {
                    Dataset needRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(u.getUri());
                    DefaultNeedModelWrapper needModelWrapper = new DefaultNeedModelWrapper(needRDF);
                    String needTitle = StringUtils.trim(needModelWrapper.getSomeTitleFromIsOrAll("en","de"));
                    if(titleToClose.equals(needTitle) && needModelWrapper.getNeedState().equals(NeedState.ACTIVE)){
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
