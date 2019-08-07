package won.bot.framework.eventbot.action.impl.mail.receive;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.bot.context.MailBotContextWrapper;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.action.impl.atomlifecycle.AbstractCreateAtomAction;
import won.bot.framework.eventbot.action.impl.mail.model.MailPropertyType;
import won.bot.framework.eventbot.action.impl.mail.model.UriType;
import won.bot.framework.eventbot.action.impl.mail.model.WonURI;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.mail.CreateAtomFromMailEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.FailureResponseEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.message.WonMessage;
import won.protocol.model.SocketType;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.DefaultAtomModelWrapper;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by fsuda on 30.09.2016.
 */
public class CreateAtomFromMailAction extends AbstractCreateAtomAction {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private MailContentExtractor mailContentExtractor;

    public CreateAtomFromMailAction(EventListenerContext eventListenerContext,
                    MailContentExtractor mailContentExtractor, URI... sockets) {
        super(eventListenerContext);
        this.mailContentExtractor = mailContentExtractor;
        if (sockets == null || sockets.length == 0) {
            // add the default socket if none is present.
            this.sockets = new ArrayList<URI>(1);
            this.sockets.add(SocketType.ChatSocket.getURI());
        } else {
            this.sockets = Arrays.asList(sockets);
        }
    }

    protected void doRun(Event event, EventListener executingListener) throws Exception {
        EventListenerContext ctx = getEventListenerContext();
        if (event instanceof CreateAtomFromMailEvent && ctx.getBotContextWrapper() instanceof MailBotContextWrapper) {
            MailBotContextWrapper botContextWrapper = (MailBotContextWrapper) ctx.getBotContextWrapper();
            MimeMessage message = ((CreateAtomFromMailEvent) event).getMessage();
            try {
                MailPropertyType type = mailContentExtractor.getMailType(message);
                String title = mailContentExtractor.getTitle(message);
                String description = mailContentExtractor.getDescription(message);
                String[] tags = mailContentExtractor.getTags(message);
                boolean isUsedForTesting = mailContentExtractor.isUsedForTesting(message);
                boolean isDoNotMatch = mailContentExtractor.isDoNotMatch(message);
                WonNodeInformationService wonNodeInformationService = ctx.getWonNodeInformationService();
                final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
                final URI atomURI = wonNodeInformationService.generateAtomURI(wonNodeUri);
                DefaultAtomModelWrapper atomModelWrapper = new DefaultAtomModelWrapper(atomURI);
                switch (type) {
                    case OFFER:
                        atomModelWrapper.setTitle(title);
                        atomModelWrapper.setDescription(description);
                        for (String tag : tags) {
                            atomModelWrapper.addTag(tag);
                        }
                        break;
                    case DEMAND:
                        atomModelWrapper.setSeeksTitle(title);
                        atomModelWrapper.setSeeksDescription(description);
                        for (String tag : tags) {
                            atomModelWrapper.addSeeksTag(tag);
                        }
                        break;
                    case BOTH:
                        atomModelWrapper.setTitle(title);
                        atomModelWrapper.setDescription(description);
                        atomModelWrapper.setSeeksTitle(title);
                        atomModelWrapper.setSeeksDescription(description);
                        for (String tag : tags) {
                            atomModelWrapper.addTag(tag);
                        }
                        for (String tag : tags) {
                            atomModelWrapper.addSeeksTag(tag);
                        }
                        break;
                }
                int i = 1;
                for (URI socket : sockets) {
                    atomModelWrapper.addSocket(atomURI.toString() + "#socket" + i, socket.toString());
                    i++;
                }
                Dataset dataset = atomModelWrapper.copyDataset();
                logger.debug("creating atom on won node {} with content {} ", wonNodeUri,
                                StringUtils.abbreviate(RdfUtils.toString(dataset), 150));
                WonMessage createAtomMessage = createWonMessage(wonNodeInformationService, atomURI, wonNodeUri, dataset,
                                isUsedForTesting, isDoNotMatch);
                EventBotActionUtils.rememberInList(ctx, atomURI, uriListName);
                botContextWrapper.addUriMimeMessageRelation(atomURI, message);
                EventListener successCallback = new EventListener() {
                    @Override
                    public void onEvent(Event event) throws Exception {
                        logger.debug("atom creation successful, new atom URI is {}", atomURI);
                        String sender = MailContentExtractor
                                        .getFromAddressString(botContextWrapper.getMimeMessageForURI(atomURI));
                        botContextWrapper.addMailAddressWonURIRelation(sender, new WonURI(atomURI, UriType.ATOM));
                        logger.debug("created atom was from sender: " + sender);
                    }
                };
                EventListener failureCallback = new EventListener() {
                    @Override
                    public void onEvent(Event event) throws Exception {
                        String textMessage = WonRdfUtils.MessageUtils
                                        .getTextMessage(((FailureResponseEvent) event).getFailureMessage());
                        logger.debug("atom creation failed for atom URI {}, original message URI {}: {}", new Object[] {
                                        atomURI, ((FailureResponseEvent) event).getOriginalMessageURI(), textMessage });
                        EventBotActionUtils.removeFromList(ctx, atomURI, uriListName);
                        botContextWrapper.removeUriMimeMessageRelation(atomURI);
                    }
                };
                EventBotActionUtils.makeAndSubscribeResponseListener(createAtomMessage, successCallback,
                                failureCallback, ctx);
                logger.debug("registered listeners for response to message URI {}", createAtomMessage.getMessageURI());
                ctx.getWonMessageSender().sendWonMessage(createAtomMessage);
                logger.debug("atom creation message sent with message URI {}", createAtomMessage.getMessageURI());
            } catch (MessagingException me) {
                logger.error("messaging exception occurred: {}", me);
            }
        }
    }
}
