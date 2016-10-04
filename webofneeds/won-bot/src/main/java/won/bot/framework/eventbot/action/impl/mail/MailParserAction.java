package won.bot.framework.eventbot.action.impl.mail;

import com.hp.hpl.jena.rdf.model.Model;
import org.apache.commons.lang3.StringUtils;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.action.impl.needlifecycle.AbstractCreateNeedAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.mail.MailReceivedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.FailureResponseEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.message.WonMessage;
import won.protocol.model.BasicNeedType;
import won.protocol.model.FacetType;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.NeedModelBuilder;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by fsuda on 30.09.2016.
 */
public class MailParserAction extends AbstractCreateNeedAction {
    private String uriMailRelationsName;

    public MailParserAction(EventListenerContext eventListenerContext, String uriListName, String uriMailRelationsName, URI... facets) {
        super(eventListenerContext, uriListName);
        this.uriMailRelationsName = uriMailRelationsName;

        this.usedForTesting = true;

        if (facets == null || facets.length == 0) {
            //add the default facet if none is present.
            this.facets = new ArrayList<URI>(1);
            this.facets.add(FacetType.OwnerFacet.getURI());
        } else {
            this.facets = Arrays.asList(facets);
        }
    }

    protected void doRun(Event event) throws Exception {
        logger.info("CREATENEEDFROMMAILACTION IS CALLED");
        if(event instanceof MailReceivedEvent){
            String title="";
            String description="";
            BasicNeedType type=BasicNeedType.DEMAND;

            MimeMessage message = ((MailReceivedEvent) event).getMessage();
            try {
                title = message.getSubject();
                description = message.getContent().toString();
                type = null;

                if(title.startsWith("[WANT]")) {
                    type = BasicNeedType.DEMAND;
                }else if(title.startsWith("[OFFER]")){
                    type = BasicNeedType.SUPPLY;
                }else {
                    logger.error("NO NEED TYPE DEFINED IN EMAIL MESSAGE");
                    return;
                }

                logger.info("Title: "+ title);
                logger.info("Content: "+ description);
                logger.info("Type: "+ type.toString());
            }catch (MessagingException me){
                logger.error("i had a messaging exception");
                me.printStackTrace();
            }
            EventListenerContext ctx = getEventListenerContext();
            WonNodeInformationService wonNodeInformationService = ctx.getWonNodeInformationService();

            final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
            final URI needURI = wonNodeInformationService.generateNeedURI(wonNodeUri);
            Model model = new NeedModelBuilder()
                    .setTitle(title)
                    .setBasicNeedType(type)
                    .setDescription(description)
                    .setUri(needURI)
                    .setFacetTypes(facets)
                    .build();

            logger.info("creating need on won node {} with content {} ", wonNodeUri, StringUtils.abbreviate(RdfUtils.toString(model), 150));

            WonMessage createNeedMessage = createWonMessage(wonNodeInformationService, needURI, wonNodeUri, model);
            EventBotActionUtils.rememberInListIfNamePresent(ctx, needURI, uriListName);
            EventBotActionUtils.addUriAddressRelation(ctx, uriMailRelationsName, needURI, message);

            EventListener successCallback = new EventListener()
            {
                @Override
                public void onEvent(Event event) throws Exception {
                    logger.info("need creation successful, new need URI is {}", needURI);
                    String sender = EventBotActionUtils.getAddressForURI(getEventListenerContext(), uriMailRelationsName, needURI);
                    logger.info("created need was from sender: " + sender);
                }
            };

            EventListener failureCallback = new EventListener()
            {
                @Override
                public void onEvent(Event event) throws Exception {
                    String textMessage = WonRdfUtils.MessageUtils.getTextMessage(((FailureResponseEvent) event).getFailureMessage());
                    logger.info("need creation failed for need URI {}, original message URI {}: {}", new Object[]{needURI, ((FailureResponseEvent) event).getOriginalMessageURI(), textMessage});
                    EventBotActionUtils.removeFromListIfNamePresent(getEventListenerContext(), needURI, uriListName);
                    EventBotActionUtils.removeUriAddressRelation(getEventListenerContext(), uriMailRelationsName, needURI);
                }
            };
            EventBotActionUtils.makeAndSubscribeResponseListener(createNeedMessage, successCallback, failureCallback, getEventListenerContext());

            logger.info("registered listeners for response to message URI {}", createNeedMessage.getMessageURI());
            getEventListenerContext().getWonMessageSender().sendWonMessage(createNeedMessage);
            logger.info("need creation message sent with message URI {}", createNeedMessage.getMessageURI());
        }else{
            logger.info("EVENT WAS NOT A MAIL RECEIVED EVENT");
        }
    }
}
