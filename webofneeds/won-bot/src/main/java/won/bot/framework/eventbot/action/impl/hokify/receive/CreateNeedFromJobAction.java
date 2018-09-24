package won.bot.framework.eventbot.action.impl.hokify.receive;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;

import won.bot.framework.bot.context.BotContextWrapper;
import won.bot.framework.bot.context.HokifyJobBotContextWrapper;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.action.impl.hokify.HokifyJob;
import won.bot.framework.eventbot.action.impl.mail.model.UriType;
import won.bot.framework.eventbot.action.impl.mail.model.WonURI;
import won.bot.framework.eventbot.action.impl.needlifecycle.AbstractCreateNeedAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.hokify.CreateNeedFromJobEvent;
import won.bot.framework.eventbot.event.impl.needlifecycle.NeedCreatedEvent;
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
 * Created by MS on 18.09.2018.
 */
public class CreateNeedFromJobAction extends AbstractCreateNeedAction {

    public CreateNeedFromJobAction(EventListenerContext eventListenerContext, URI... facets) {

        super(eventListenerContext);

        if (facets == null || facets.length == 0) {
            // add the default facet if none is present.
            this.facets = new ArrayList<URI>(1);
            this.facets.add(FacetType.OwnerFacet.getURI());
        } else {
            this.facets = Arrays.asList(facets);
        }
    }

    protected void doRun(Event event, EventListener executingListener) throws Exception {
        System.out.println("----------------------------------------------------------------- Started RUN");
        EventListenerContext ctx = getEventListenerContext();
        if (event instanceof CreateNeedFromJobEvent && ctx.getBotContextWrapper() instanceof HokifyJobBotContextWrapper) {
            System.out.println("----------------------------------------------------------------- INsight");
            HokifyJobBotContextWrapper botContextWrapper = (HokifyJobBotContextWrapper) ctx.getBotContextWrapper();
            ArrayList<HokifyJob> hokifyJobs = ((CreateNeedFromJobEvent) event).getHokifyJobs();
            try {
                //for (HokifyJob hokifyJob : hokifyJobs) {
                for (int i = 0; i < 2; i++) {
                    HokifyJob hokifyJob = hokifyJobs.get(i);
                    // TODO find and declare properties
                    NeedContentPropertyType type = NeedContentPropertyType.IS;
                    String title = hokifyJob.getTitle() + " Max debug - New Newest";
                    String description = hokifyJob.getDescription();
                    String[] tags = { "job", "hokify", "debug" };
                    boolean isUsedForTesting = false;
                    boolean isDoNotMatch = false;

                    WonNodeInformationService wonNodeInformationService = ctx.getWonNodeInformationService();

                    final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
                    System.out.println("-------------- ----------- NODE URI" + wonNodeUri);
                    final URI needURI = wonNodeInformationService.generateNeedURI(wonNodeUri);
                    System.out.println("-------------- ----------- NODE URI" + wonNodeUri);
                    DefaultNeedModelWrapper needModelWrapper = new DefaultNeedModelWrapper(needURI.toString());
                    needModelWrapper.setTitle(type, title);
                    needModelWrapper.setDescription(type, description);

                    for (String tag : tags) {
                        needModelWrapper.addTag(type, tag);
                    }

                    for (URI facet : facets) {
                        needModelWrapper.addFacetUri(facet.toString());
                    }

                    Dataset dataset = needModelWrapper.copyDataset();

                    logger.debug("creating need on won node {} with content {} ", wonNodeUri,
                            StringUtils.abbreviate(RdfUtils.toString(dataset), 150));

                    WonMessage createNeedMessage = createWonMessage(wonNodeInformationService, needURI, wonNodeUri,
                            dataset, isUsedForTesting, isDoNotMatch);
                    EventBotActionUtils.rememberInList(ctx, needURI, uriListName);
                    botContextWrapper.addURIJobURLRelation(needURI, hokifyJob.getUrl());
                    EventBus bus = ctx.getEventBus();
                    EventListener successCallback = new EventListener() {
                        @Override
                        public void onEvent(Event event) throws Exception {
                            logger.debug("need creation successful, new need URI is {}", needURI);
                            /*
                             * String sender = MailContentExtractor
                             * .getFromAddressString(botContextWrapper.getMimeMessageForURI(needURI));
                             * botContextWrapper.addMailAddressWonURIRelation(sender, new WonURI(needURI,
                             * UriType.NEED)); logger.debug("created need was from sender: " + sender);
                             */
                           
                             bus.publish(new NeedCreatedEvent(needURI, wonNodeUri, dataset, null));
                            
                        }
                    };

                    EventListener failureCallback = new EventListener() {
                        @Override
                        public void onEvent(Event event) throws Exception {
                            String textMessage = WonRdfUtils.MessageUtils
                                    .getTextMessage(((FailureResponseEvent) event).getFailureMessage());
                            logger.debug("need creation failed for need URI {}, original message URI {}: {}",
                                    new Object[] { needURI, ((FailureResponseEvent) event).getOriginalMessageURI(),
                                            textMessage });
                            EventBotActionUtils.removeFromList(ctx, needURI, uriListName);
                            botContextWrapper.removeURIJobURLRelation(needURI);
                        }
                    };
                    EventBotActionUtils.makeAndSubscribeResponseListener(createNeedMessage, successCallback,
                            failureCallback, ctx);

                    logger.debug("registered listeners for response to message URI {}",
                            createNeedMessage.getMessageURI());
                    ctx.getWonMessageSender().sendWonMessage(createNeedMessage);
                    logger.debug("need creation message sent with message URI {}", createNeedMessage.getMessageURI());
                    System.out.println("-------------- ----------- need creation message sent with message URI {}" + createNeedMessage.getMessageURI());
                }
            } catch (Exception me) {
                logger.error("messaging exception occurred: {}", me);
                System.out.println("-------------- ------- EXEPTION" + me);
            }
        }
    }
}
