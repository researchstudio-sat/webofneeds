package won.bot.framework.eventbot.action.impl.mail.receive;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import won.bot.framework.bot.context.MailBotContextWrapper;
import org.apache.jena.rdf.model.Model;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.action.impl.mail.model.UriType;
import won.bot.framework.eventbot.action.impl.mail.model.WonURI;
import won.bot.framework.eventbot.action.impl.needlifecycle.AbstractCreateNeedAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.mail.CreateNeedFromMailEvent;
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

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by fsuda on 30.09.2016.
 */
public class CreateNeedFromMailAction extends AbstractCreateNeedAction {
    private MailContentExtractor mailContentExtractor;

    public CreateNeedFromMailAction(EventListenerContext eventListenerContext,
                                    MailContentExtractor mailContentExtractor,
                                    URI... facets) {

        super(eventListenerContext);
        this.mailContentExtractor = mailContentExtractor;

        if (facets == null || facets.length == 0) {
            //add the default facet if none is present.
            this.facets = new ArrayList<URI>(1);
            this.facets.add(FacetType.OwnerFacet.getURI());
        } else {
            this.facets = Arrays.asList(facets);
        }
    }

    protected void doRun(Event event, EventListener executingListener) throws Exception {
        EventListenerContext ctx = getEventListenerContext();
        if(event instanceof CreateNeedFromMailEvent && ctx.getBotContextWrapper() instanceof MailBotContextWrapper){
            MailBotContextWrapper botContextWrapper = (MailBotContextWrapper) ctx.getBotContextWrapper();
            MimeMessage message = ((CreateNeedFromMailEvent) event).getMessage();

            try {
                NeedContentPropertyType type = mailContentExtractor.getNeedType(message);
                String title = mailContentExtractor.getTitle(message);
                String description = mailContentExtractor.getDescription(message);
                String[] tags = mailContentExtractor.getTags(message);
                boolean isUsedForTesting = mailContentExtractor.isUsedForTesting(message);
                boolean isDoNotMatch = mailContentExtractor.isDoNotMatch(message);

                WonNodeInformationService wonNodeInformationService = ctx.getWonNodeInformationService();

                final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
                final URI needURI = wonNodeInformationService.generateNeedURI(wonNodeUri);
                DefaultNeedModelWrapper needModelWrapper = new DefaultNeedModelWrapper(needURI.toString());
                needModelWrapper.setTitle(type, title);
                needModelWrapper.setDescription(type, description);

                for (String tag : tags) {
                    needModelWrapper.addTag(type, tag);
                }

                for (URI facet : facets) {
                    needModelWrapper.addFacetUri(facet.toString());
                }

                Model model = needModelWrapper.getNeedModel(NeedGraphType.NEED);

                logger.debug("creating need on won node {} with content {} ", wonNodeUri, StringUtils.abbreviate(RdfUtils.toString(model), 150));

                WonMessage createNeedMessage = createWonMessage(wonNodeInformationService, needURI, wonNodeUri,
                                                                model, isUsedForTesting, isDoNotMatch);
                EventBotActionUtils.rememberInList(ctx, needURI, uriListName);
                botContextWrapper.addUriMimeMessageRelation(needURI, message);

                EventListener successCallback = new EventListener()
                {
                    @Override
                    public void onEvent(Event event) throws Exception {
                        logger.debug("need creation successful, new need URI is {}", needURI);
                        String sender = MailContentExtractor.getFromAddressString(botContextWrapper.getMimeMessageForURI(needURI));
                        botContextWrapper.addMailAddressWonURIRelation(sender, new WonURI(needURI, UriType.NEED));
                        logger.debug("created need was from sender: " + sender);
                    }
                };

                EventListener failureCallback = new EventListener()
                {
                    @Override
                    public void onEvent(Event event) throws Exception {
                        String textMessage = WonRdfUtils.MessageUtils.getTextMessage(((FailureResponseEvent) event).getFailureMessage());
                        logger.debug("need creation failed for need URI {}, original message URI {}: {}", new Object[]{needURI, ((FailureResponseEvent) event).getOriginalMessageURI(), textMessage});
                        EventBotActionUtils.removeFromList(ctx, needURI, uriListName);
                        botContextWrapper.removeUriMimeMessageRelation(needURI);
                    }
                };
                EventBotActionUtils.makeAndSubscribeResponseListener(createNeedMessage, successCallback, failureCallback, ctx);

                logger.debug("registered listeners for response to message URI {}", createNeedMessage.getMessageURI());
                ctx.getWonMessageSender().sendWonMessage(createNeedMessage);
                logger.debug("need creation message sent with message URI {}", createNeedMessage.getMessageURI());
            }  catch (MessagingException me){
                logger.error("messaging exception occurred: {}", me);
            }
        }
    }
}
