package won.bot.framework.eventbot.action.impl.telegram.send;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.action.impl.mail.model.UriType;
import won.bot.framework.eventbot.action.impl.mail.model.WonURI;
import won.bot.framework.eventbot.action.impl.needlifecycle.AbstractCreateNeedAction;
import won.bot.framework.eventbot.action.impl.telegram.WonTelegramBotHandler;
import won.bot.framework.eventbot.action.impl.telegram.util.TelegramContentExtractor;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.telegram.TelegramCreateNeedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.FailureResponseEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.message.WonMessage;
import won.protocol.model.FacetType;
import won.protocol.model.NeedContentPropertyType;
import won.protocol.model.NeedGraphType;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.DefaultNeedModelWrapper;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;

import java.net.URI;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by fsuda on 15.12.2016.
 */
public class TelegramCreateAction extends AbstractCreateNeedAction {
    private WonTelegramBotHandler wonTelegramBotHandler;
    private TelegramContentExtractor telegramContentExtractor;

    private String uriChatIdListName;
    private String chatIdUriListName;
    private String messageIdUriListName;

    public TelegramCreateAction(EventListenerContext eventListenerContext, String uriListName, String uriChatIdListName, String chatIdUriListName, String messageIdUriListName, WonTelegramBotHandler wonTelegramBotHandler, TelegramContentExtractor telegramContentExtractor, URI... facets) {
        super(eventListenerContext, uriListName);
        this.wonTelegramBotHandler = wonTelegramBotHandler;
        this.telegramContentExtractor = telegramContentExtractor;
        this.uriChatIdListName = uriChatIdListName;
        this.chatIdUriListName = chatIdUriListName;
        this.messageIdUriListName = messageIdUriListName;

        if (facets == null || facets.length == 0) {
            //add the default facet if none is present.
            this.facets = new ArrayList<URI>(1);
            this.facets.add(FacetType.OwnerFacet.getURI());
        } else {
            this.facets = Arrays.asList(facets);
        }
    }

    @Override
    protected void doRun(Event event) throws Exception {
        if(event instanceof TelegramCreateNeedEvent){
            TelegramCreateNeedEvent telegramCreateNeedEvent = (TelegramCreateNeedEvent) event;
            String[] parameters = telegramCreateNeedEvent.getStrings();
            Long chatId = telegramCreateNeedEvent.getChat().getId();

            if(chatId == null){
                logger.error("no chatid present");
                return;
            }
            try{
                NeedContentPropertyType type = telegramContentExtractor.getNeedContentType(parameters[0]);

                if(type == null) {
                    throw new InvalidParameterException("no valid type was given");
                }

                String title = null;

                if(parameters.length > 1){
                    title = parameters[1];
                }

                if(title == null){
                    throw new InvalidParameterException("no valid title was given");
                }

                //MAKE THOSE ATTRIBUTES DIFFERENT AND EDITABLE
                boolean isUsedForTesting = true;
                boolean isDoNotMatch = false;

                EventListenerContext ctx = getEventListenerContext();
                WonNodeInformationService wonNodeInformationService = ctx.getWonNodeInformationService();

                final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
                final URI needURI = wonNodeInformationService.generateNeedURI(wonNodeUri);

                DefaultNeedModelWrapper wrapper = new DefaultNeedModelWrapper(needURI.toString());
                wrapper.setTitle(type, title);
                for (URI facet : facets) {
                    wrapper.addFacetUri(facet.toString());
                }

                Model model = wrapper.getNeedModel(NeedGraphType.NEED);
                logger.debug("creating need on won node {} with content {} ", wonNodeUri, StringUtils.abbreviate(RdfUtils.toString(model), 150));

                WonMessage createNeedMessage = createWonMessage(wonNodeInformationService, needURI, wonNodeUri,
                        model, isUsedForTesting, isDoNotMatch);
                EventBotActionUtils.rememberInList(ctx, needURI, uriListName);
                EventBotActionUtils.addChatIdWonURIRelation(ctx, chatIdUriListName, chatId, new WonURI(needURI, UriType.NEED));
                EventBotActionUtils.addURIChatIdRelation(ctx, uriChatIdListName, needURI, chatId);

                EventListener successCallback = new EventListener()
                {
                    @Override
                    public void onEvent(Event event) throws Exception {
                        logger.debug("need creation successful, new need URI is {}", needURI);
                        logger.debug("created need was from sender: " + EventBotActionUtils.getChatIdForURI(getEventListenerContext(), uriChatIdListName, needURI));

                        try{
                            Message message = telegramCreateNeedEvent.getAbsSender().sendMessage(wonTelegramBotHandler.getTelegramMessageGenerator().getCreatedNeedMessage(chatId, needURI));
                            EventBotActionUtils.addMessageIdWonURIRelation(getEventListenerContext(), messageIdUriListName, message.getMessageId(), new WonURI(needURI, UriType.NEED));
                        }catch (TelegramApiException te){
                            logger.error(te.getMessage());
                        }
                    }
                };

                EventListener failureCallback = new EventListener()
                {
                    @Override
                    public void onEvent(Event event) throws Exception {
                        String textMessage = WonRdfUtils.MessageUtils.getTextMessage(((FailureResponseEvent) event).getFailureMessage());
                        logger.error("need creation failed for need URI {}, original message URI {}: {}", new Object[]{needURI, ((FailureResponseEvent) event).getOriginalMessageURI(), textMessage});
                        EventBotActionUtils.removeFromList(getEventListenerContext(), needURI, uriListName);
                    }
                };
                EventBotActionUtils.makeAndSubscribeResponseListener(createNeedMessage, successCallback, failureCallback, getEventListenerContext());

                logger.debug("registered listeners for response to message URI {}", createNeedMessage.getMessageURI());
                getEventListenerContext().getWonMessageSender().sendWonMessage(createNeedMessage);
                logger.debug("need creation message sent with message URI {}", createNeedMessage.getMessageURI());
            }catch(Exception e){
                try{
                    logger.error(e.getMessage());
                    telegramCreateNeedEvent.getAbsSender().sendMessage(wonTelegramBotHandler.getTelegramMessageGenerator().getErrorMessage(chatId));
                }catch (TelegramApiException te){
                    logger.error(te.getMessage());
                }
            }
        }
    }
}
