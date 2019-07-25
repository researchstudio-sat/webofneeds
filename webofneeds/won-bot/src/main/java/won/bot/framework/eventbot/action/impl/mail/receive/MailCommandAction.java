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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import won.bot.framework.eventbot.event.impl.command.deactivate.DeactivateAtomCommandEvent;
import won.bot.framework.eventbot.event.impl.mail.MailCommandEvent;
import won.bot.framework.eventbot.event.impl.mail.SubscribeUnsubscribeEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionModelMapper;
import won.protocol.model.AtomState;
import won.protocol.util.DefaultAtomModelWrapper;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;

/**
 * Created by fsuda on 18.10.2016.
 */
public class MailCommandAction extends BaseEventBotAction {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private MailContentExtractor mailContentExtractor;

    public MailCommandAction(EventListenerContext eventListenerContext, MailContentExtractor mailContentExtractor) {
        super(eventListenerContext);
        this.mailContentExtractor = mailContentExtractor;
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        EventListenerContext ctx = getEventListenerContext();
        if (event instanceof MailCommandEvent && ctx.getBotContextWrapper() instanceof MailBotContextWrapper) {
            MailBotContextWrapper botContextWrapper = (MailBotContextWrapper) ctx.getBotContextWrapper();
            MimeMessage message = ((MailCommandEvent) event).getMessage();
            String referenceId = MailContentExtractor.getMailReference(message);
            WonURI wonUri = botContextWrapper.getWonURIForMailId(referenceId);
            // determine if the mail is referring to some other mail/atom/connection or not
            if (wonUri != null) {
                processReferenceMailCommands(message, wonUri);
            } else {
                processNonReferenceMailCommand(message);
            }
        }
    }

    private void processNonReferenceMailCommand(MimeMessage message) throws IOException, MessagingException {
        EventBus bus = getEventListenerContext().getEventBus();
        ActionType mailAction = mailContentExtractor.getMailAction(message);
        switch (mailAction) {
            case SUBSCRIBE:
                bus.publish(new SubscribeUnsubscribeEvent(message, SubscribeStatus.SUBSCRIBED));
                break;
            case UNSUBSCRIBE:
                bus.publish(new SubscribeUnsubscribeEvent(message, SubscribeStatus.UNSUBSCRIBED));
                break;
            case CLOSE_ATOM:
                /*
                 * An atom can be closed with a mail that matches the takenCmdPattern in its
                 * subject and has the same title as a previously created atom by the user
                 */
                URI atomUri = retrieveCorrespondingAtomUriFromMailByTitle(message);
                if (atomUri != null) {
                    bus.publish(new DeactivateAtomCommandEvent(atomUri));
                }
                break;
            case NO_ACTION:
            default:
                // INVALID COMMAND
                logger.error("No command was given or assumed");
                break;
        }
    }

    private void processReferenceMailCommands(MimeMessage message, WonURI wonUri) {
        MailBotContextWrapper botContextWrapper = ((MailBotContextWrapper) getEventListenerContext()
                        .getBotContextWrapper());
        EventBus bus = getEventListenerContext().getEventBus();
        try {
            if (wonUri == null) {
                throw new NullPointerException("No corresponding wonUri found");
            }
            URI atomUri;
            URI targetAtomUri = null;
            Dataset connectionRDF = null;
            switch (wonUri.getType()) {
                case CONNECTION:
                    connectionRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(wonUri.getUri());
                    atomUri = WonRdfUtils.ConnectionUtils.getLocalAtomURIFromConnection(connectionRDF, wonUri.getUri());
                    targetAtomUri = WonRdfUtils.ConnectionUtils.getTargetAtomURIFromConnection(connectionRDF,
                                    wonUri.getUri());
                    break;
                case ATOM:
                default:
                    atomUri = wonUri.getUri();
                    break;
            }
            MimeMessage originalMessage = botContextWrapper.getMimeMessageForURI(atomUri);
            if (originalMessage == null) {
                throw new NullPointerException("no originalmessage found");
            }
            logger.debug("Validate mailorigin with originalmail:");
            logger.debug("Command Message Sender: " + message.getFrom());
            logger.debug("Original Message Sender: " + originalMessage.getFrom());
            String senderNew = ((InternetAddress) message.getFrom()[0]).getAddress();
            String senderOriginal = ((InternetAddress) originalMessage.getFrom()[0]).getAddress();
            if (!senderNew.equals(senderOriginal)) {
                throw new AccessControlException("Sender of original and command mail are not equal");
            } else {
                logger.debug("Sender of original and command mail are not equal, continue with command processing");
            }
            ActionType actionType = determineAction(getEventListenerContext(), message, wonUri);
            logger.debug("Executing " + actionType + " on uri: " + wonUri.getUri() + " of type " + wonUri.getType());
            Connection con;
            switch (actionType) {
                case CLOSE_CONNECTION:
                    con = RdfUtils.findFirst(connectionRDF, x -> new ConnectionModelMapper().fromModel(x));
                    bus.publish(new CloseCommandEvent(con));
                    break;
                case OPEN_CONNECTION:
                    bus.publish(new ConnectCommandEvent(atomUri, targetAtomUri));
                    break;
                case IMPLICIT_OPEN_CONNECTION:
                    bus.publish(new ConnectCommandEvent(atomUri, targetAtomUri,
                                    mailContentExtractor.getTextMessage(message)));
                    break;
                case SENDMESSAGE:
                    con = RdfUtils.findFirst(connectionRDF, x -> new ConnectionModelMapper().fromModel(x));
                    Model messageModel = WonRdfUtils.MessageUtils
                                    .textMessage(mailContentExtractor.getTextMessage(message));
                    bus.publish(new ConnectionMessageCommandEvent(con, messageModel));
                    break;
                case CLOSE_ATOM:
                    bus.publish(new DeactivateAtomCommandEvent(atomUri));
                    break;
                case NO_ACTION:
                default:
                    // INVALID COMMAND
                    logger.error("No command was given or assumed");
                    break;
            }
        } catch (AccessControlException ace) {
            logger.error("ACCESS RESTRICTION: sender of original and command mail are not equal, command will be blocked");
        } catch (Exception e) {
            logger.error("no reply mail was set or found: " + e.getMessage());
        }
    }

    /**
     * This Method tries to find a corresponding open atom uri from a user(given by
     * the from adress) and returns the corresponding atom uri if there was an open
     * atom with the same title
     * 
     * @param message used to extract sender adress and subject(title)
     * @return
     */
    private URI retrieveCorrespondingAtomUriFromMailByTitle(MimeMessage message) {
        try {
            MailBotContextWrapper botContextWrapper = ((MailBotContextWrapper) getEventListenerContext()
                            .getBotContextWrapper());
            String sender = ((InternetAddress) message.getFrom()[0]).getAddress();
            URI atomURI = null;
            String titleToClose = mailContentExtractor.getTitle(message).trim();
            if (sender != null) {
                List<WonURI> atomUris = botContextWrapper.getWonURIsForMailAddress(sender);
                for (WonURI u : atomUris) {
                    Dataset atomRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(u.getUri());
                    DefaultAtomModelWrapper atomModelWrapper = new DefaultAtomModelWrapper(atomRDF);
                    String atomTitle = StringUtils.trim(atomModelWrapper.getSomeTitleFromIsOrAll("en", "de"));
                    if (titleToClose.equals(atomTitle) && atomModelWrapper.getAtomState().equals(AtomState.ACTIVE)) {
                        return u.getUri();
                    }
                }
            }
            return atomURI;
        } catch (MessagingException me) {
            logger.error("could not extract information from mimemessage");
            return null;
        }
    }

    private ActionType determineAction(EventListenerContext ctx, MimeMessage message, WonURI wonUri) {
        try {
            ActionType mailAction = mailContentExtractor.getMailAction(message);
            switch (wonUri.getType()) {
                case CONNECTION:
                    boolean connected = WonRdfUtils.ConnectionUtils.isConnected(
                                    ctx.getLinkedDataSource().getDataForResource(wonUri.getUri()), wonUri.getUri());
                    if (ActionType.CLOSE_CONNECTION.equals(mailAction)) {
                        return ActionType.CLOSE_CONNECTION;
                    } else if (!connected && ActionType.OPEN_CONNECTION.equals(mailAction)) {
                        return ActionType.OPEN_CONNECTION;
                    } else if (connected) {
                        return ActionType.SENDMESSAGE;
                    } else if (ActionType.CLOSE_ATOM.equals(mailAction)) {
                        return ActionType.CLOSE_ATOM;
                    } else {
                        // if the connection is not connected yet and we do not parse any command we
                        // assume that the mailsender wants to establish a connection
                        return ActionType.IMPLICIT_OPEN_CONNECTION;
                    }
                case ATOM:
                    if (ActionType.CLOSE_ATOM.equals(mailAction)) {
                        return ActionType.CLOSE_ATOM;
                    }
                default:
                    return mailAction;
            }
        } catch (MessagingException me) {
            logger.error("exception occurred checking command mail: {}", me);
            return ActionType.NO_ACTION;
        } catch (IOException ioe) {
            logger.error("exception occurred checking command mail: {}", ioe);
            return ActionType.NO_ACTION;
        }
    }
}
