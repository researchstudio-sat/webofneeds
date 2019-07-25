package won.bot.framework.eventbot.action.impl.telegram.send;

import java.net.URI;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import won.bot.framework.bot.context.TelegramBotContextWrapper;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.action.impl.mail.model.UriType;
import won.bot.framework.eventbot.action.impl.mail.model.WonURI;
import won.bot.framework.eventbot.action.impl.atomlifecycle.AbstractCreateAtomAction;
import won.bot.framework.eventbot.action.impl.telegram.WonTelegramBotHandler;
import won.bot.framework.eventbot.action.impl.telegram.util.TelegramContentExtractor;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.telegram.TelegramCreateAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.FailureResponseEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.message.WonMessage;
import won.protocol.model.SocketType;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.DefaultAtomModelWrapper;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;

/**
 * Created by fsuda on 15.12.2016.
 */
public class TelegramCreateAction extends AbstractCreateAtomAction {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private WonTelegramBotHandler wonTelegramBotHandler;
    private TelegramContentExtractor telegramContentExtractor;

    public TelegramCreateAction(EventListenerContext eventListenerContext, WonTelegramBotHandler wonTelegramBotHandler,
                    TelegramContentExtractor telegramContentExtractor, URI... sockets) {
        super(eventListenerContext);
        this.wonTelegramBotHandler = wonTelegramBotHandler;
        this.telegramContentExtractor = telegramContentExtractor;
        if (sockets == null || sockets.length == 0) {
            // add the default socket if none is present.
            this.sockets = new ArrayList<URI>(1);
            this.sockets.add(SocketType.ChatSocket.getURI());
        } else {
            this.sockets = Arrays.asList(sockets);
        }
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        EventListenerContext ctx = getEventListenerContext();
        if (event instanceof TelegramCreateAtomEvent
                        && ctx.getBotContextWrapper() instanceof TelegramBotContextWrapper) {
            TelegramBotContextWrapper botContextWrapper = (TelegramBotContextWrapper) ctx.getBotContextWrapper();
            TelegramCreateAtomEvent telegramCreateAtomEvent = (TelegramCreateAtomEvent) event;
            String[] parameters = telegramCreateAtomEvent.getStrings();
            Long chatId = telegramCreateAtomEvent.getChat().getId();
            if (chatId == null) {
                logger.error("no chatid present");
                return;
            }
            try {
                MessagePropertyType type = telegramContentExtractor.getMessageContentType(parameters[0]);
                if (type == null) {
                    throw new InvalidParameterException("no valid type was given");
                }
                String title = null;
                if (parameters.length > 1) {
                    title = parameters[1];
                }
                if (title == null) {
                    throw new InvalidParameterException("no valid title was given");
                }
                // MAKE THOSE ATTRIBUTES DIFFERENT AND EDITABLE
                boolean isUsedForTesting = true;
                boolean isDoNotMatch = false;
                WonNodeInformationService wonNodeInformationService = ctx.getWonNodeInformationService();
                final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
                final URI atomURI = wonNodeInformationService.generateAtomURI(wonNodeUri);
                DefaultAtomModelWrapper wrapper = new DefaultAtomModelWrapper(atomURI);
                switch (type) {
                    case OFFER:
                        wrapper.setTitle(title);
                        break;
                    case DEMAND:
                        wrapper.setSeeksTitle(title);
                        break;
                    case BOTH:
                        wrapper.setTitle(title);
                        wrapper.setSeeksTitle(title);
                        break;
                }
                int i = 1;
                for (URI socket : sockets) {
                    wrapper.addSocket("#socket" + i, socket.toString());
                    i++;
                }
                Dataset atomDataset = wrapper.copyDataset();
                logger.debug("creating atom on won node {} with content {} ", wonNodeUri,
                                StringUtils.abbreviate(RdfUtils.toString(atomDataset), 150));
                WonMessage createAtomMessage = createWonMessage(wonNodeInformationService, atomURI, wonNodeUri,
                                atomDataset, isUsedForTesting, isDoNotMatch);
                EventBotActionUtils.rememberInList(ctx, atomURI, uriListName);
                botContextWrapper.addChatIdWonURIRelation(chatId, new WonURI(atomURI, UriType.ATOM));
                botContextWrapper.addURIChatIdRelation(atomURI, chatId);
                EventListener successCallback = new EventListener() {
                    @Override
                    public void onEvent(Event event) throws Exception {
                        logger.debug("atom creation successful, new atom URI is {}", atomURI);
                        logger.debug("created atom was from sender: " + botContextWrapper.getChatIdForURI(atomURI));
                        try {
                            Message message = telegramCreateAtomEvent.getAbsSender().sendMessage(wonTelegramBotHandler
                                            .getTelegramMessageGenerator().getCreatedAtomMessage(chatId, atomURI));
                            botContextWrapper.addMessageIdWonURIRelation(message.getMessageId(),
                                            new WonURI(atomURI, UriType.ATOM));
                        } catch (TelegramApiException te) {
                            logger.error(te.getMessage());
                        }
                    }
                };
                EventListener failureCallback = new EventListener() {
                    @Override
                    public void onEvent(Event event) throws Exception {
                        String textMessage = WonRdfUtils.MessageUtils
                                        .getTextMessage(((FailureResponseEvent) event).getFailureMessage());
                        logger.error("atom creation failed for atom URI {}, original message URI {}: {}", new Object[] {
                                        atomURI, ((FailureResponseEvent) event).getOriginalMessageURI(), textMessage });
                        EventBotActionUtils.removeFromList(getEventListenerContext(), atomURI, uriListName);
                    }
                };
                EventBotActionUtils.makeAndSubscribeResponseListener(createAtomMessage, successCallback,
                                failureCallback, getEventListenerContext());
                logger.debug("registered listeners for response to message URI {}", createAtomMessage.getMessageURI());
                getEventListenerContext().getWonMessageSender().sendWonMessage(createAtomMessage);
                logger.debug("atom creation message sent with message URI {}", createAtomMessage.getMessageURI());
            } catch (Exception e) {
                try {
                    logger.error(e.getMessage());
                    telegramCreateAtomEvent.getAbsSender().sendMessage(
                                    wonTelegramBotHandler.getTelegramMessageGenerator().getErrorMessage(chatId));
                } catch (TelegramApiException te) {
                    logger.error(te.getMessage());
                }
            }
        }
    }
}
