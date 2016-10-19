package won.bot.framework.eventbot.action.impl.mail.receive;

import com.hp.hpl.jena.rdf.model.Model;
import org.apache.commons.lang3.StringUtils;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.action.impl.needlifecycle.AbstractCreateNeedAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.mail.CreateNeedFromMailEvent;
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

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;
import javax.mail.internet.MimeMessage;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by fsuda on 30.09.2016.
 */
public class CreateNeedFromMailAction extends AbstractCreateNeedAction {
    private String uriMailRelationsName;
    private String uriMimeMessageRelationsName;

    public CreateNeedFromMailAction(EventListenerContext eventListenerContext, String uriListName, String uriMailRelationsName, String uriMimeMessageRelationsName, URI... facets) {
        super(eventListenerContext, uriListName);
        this.uriMailRelationsName = uriMailRelationsName;
        this.uriMimeMessageRelationsName = uriMimeMessageRelationsName;

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
        if(event instanceof CreateNeedFromMailEvent){
            MimeMessage message = ((CreateNeedFromMailEvent) event).getMessage();

            try {
                String subject = message.getSubject();
                String description = message.getContent().toString();
                BasicNeedType type = retrieveBasicNeedType(subject);

                assert type != null; //Done as a failsafe, this Action should never be called if it is not a valid CreateNeed-Mail

                EventListenerContext ctx = getEventListenerContext();
                WonNodeInformationService wonNodeInformationService = ctx.getWonNodeInformationService();

                final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
                final URI needURI = wonNodeInformationService.generateNeedURI(wonNodeUri);
                Model model = new NeedModelBuilder()
                        .setTitle(subject)
                        .setBasicNeedType(type)
                        .setDescription(description)
                        .setUri(needURI)
                        .setFacetTypes(facets)
                        .build();

                logger.debug("creating need on won node {} with content {} ", wonNodeUri, StringUtils.abbreviate(RdfUtils.toString(model), 150));

                WonMessage createNeedMessage = createWonMessage(wonNodeInformationService, needURI, wonNodeUri, model);
                EventBotActionUtils.rememberInListIfNamePresent(ctx, needURI, uriListName);
                EventBotActionUtils.addUriAddressRelation(ctx, uriMailRelationsName, needURI, message);
                EventBotActionUtils.addUriMimeMessageRelation(ctx, uriMimeMessageRelationsName, needURI, message);

                EventListener successCallback = new EventListener()
                {
                    @Override
                    public void onEvent(Event event) throws Exception {
                        logger.debug("need creation successful, new need URI is {}", needURI);
                        String sender = EventBotActionUtils.getAddressForURI(getEventListenerContext(), uriMailRelationsName, needURI);
                        logger.debug("created need was from sender: " + sender);
                    }
                };

                EventListener failureCallback = new EventListener()
                {
                    @Override
                    public void onEvent(Event event) throws Exception {
                        String textMessage = WonRdfUtils.MessageUtils.getTextMessage(((FailureResponseEvent) event).getFailureMessage());
                        logger.debug("need creation failed for need URI {}, original message URI {}: {}", new Object[]{needURI, ((FailureResponseEvent) event).getOriginalMessageURI(), textMessage});
                        EventBotActionUtils.removeFromListIfNamePresent(getEventListenerContext(), needURI, uriListName);
                        EventBotActionUtils.removeUriAddressRelation(getEventListenerContext(), uriMailRelationsName, needURI);
                        EventBotActionUtils.removeUriMimeMessageRelation(getEventListenerContext(), uriMimeMessageRelationsName, needURI);
                    }
                };
                EventBotActionUtils.makeAndSubscribeResponseListener(createNeedMessage, successCallback, failureCallback, getEventListenerContext());

                logger.debug("registered listeners for response to message URI {}", createNeedMessage.getMessageURI());
                getEventListenerContext().getWonMessageSender().sendWonMessage(createNeedMessage);
                logger.debug("need creation message sent with message URI {}", createNeedMessage.getMessageURI());
            }catch (MessagingException me){
                logger.error("i had a messaging exception");
                me.printStackTrace();
            }
        }
    }

    public static boolean isCreateMail(MimeMessage message) {
        try{
            String subject = message.getSubject();

            return retrieveBasicNeedType(subject) != null;
        }catch(MessagingException me){
            //TODO: LOGGER SHOULD BE STATIC FINAL ETC TO PRINT IN STATIC CONTEXT
            me.printStackTrace();
            return false;
        }
    }

    public static BasicNeedType retrieveBasicNeedType(String subject){
        //TODO: IMPLEMENT VOCABULARY VIA ADMIN INTERFACE OR PROPERTIES FILE THAT LINKS CERTAIN REGULAREXPRESSIONS WITH A BasicNeedType

        if(subject.startsWith("[WANT]")){
            return BasicNeedType.DEMAND;
        }else if(subject.startsWith("[OFFER]")){
            return BasicNeedType.SUPPLY;
        }else{
            return null;
        }
    }
}
