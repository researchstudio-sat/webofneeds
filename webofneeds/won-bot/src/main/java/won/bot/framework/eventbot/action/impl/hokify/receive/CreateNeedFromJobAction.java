package won.bot.framework.eventbot.action.impl.hokify.receive;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDF;

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
import won.protocol.vocabulary.WON;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

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
        if (event instanceof CreateNeedFromJobEvent
                && ctx.getBotContextWrapper() instanceof HokifyJobBotContextWrapper) {
            System.out.println("----------------------------------------------------------------- INsight");
            HokifyJobBotContextWrapper botContextWrapper = (HokifyJobBotContextWrapper) ctx.getBotContextWrapper();
            ArrayList<HokifyJob> hokifyJobs = ((CreateNeedFromJobEvent) event).getHokifyJobs();
            try {
                // for (HokifyJob hokifyJob : hokifyJobs) {
                for (int i = 0; i < 1; i++) {
                    
                    Random random = new Random();
                    
                    int rnd = random.nextInt(1000);
                    HokifyJob hokifyJob = hokifyJobs.get(rnd);

                    // Check if need already exists

                    if (botContextWrapper.getNeedUriForJobURL(hokifyJob.getUrl()) != null) {

                        System.out.println("---------------- ------------------- Need already exists for job: "
                                + hokifyJob.getUrl());
                    } else {
                        Model model = ModelFactory.createDefaultModel();
                        
                        
                        Property schema_name = model.createProperty("http://schema.org/name");
                        Property schema_organisation = model.createProperty("http://schema.org/Organization");
                        
                        Property type = model.createProperty("@type");
                        
                        final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
                        WonNodeInformationService wonNodeInformationService = ctx.getWonNodeInformationService();
                        final URI needURI = wonNodeInformationService.generateNeedURI(wonNodeUri);
                        //URI needURI = URI.create("https://localhost:8443/won/resource/event/" + "hokify_job" + rnd + "#need");
                        DefaultNeedModelWrapper needModelWrapper = new DefaultNeedModelWrapper(needURI.toString());
                        
                        Resource need = needModelWrapper.getNeedModel().createResource(needURI.toString());//model.createResource(needURI.toString());
                        Resource isPart = need.getModel().createResource();
                        //isPart.add
                        //isPart.addProperty(type, "s:JobPosting");
                        isPart.addProperty(DC.title, hokifyJob.getTitle() + " - ALLNEW");
                        String[] tags = { "job", "new", "debug" };
                        for (String tag : tags) {
                            isPart.addProperty(WON.HAS_TAG, tag);
                        }
                        
                        /*
                        Resource hiringOrganisation = isPart.getModel().createResource();
                        //hiringOrganisation.addLiteral(type, "s:Organization");
                        hiringOrganisation.addProperty(schema_name, hokifyJob.getCompany());
                        isPart.addProperty(schema_organisation, hiringOrganisation);
                        */
                        
                        for (URI facet : facets) {
                            //isPart.addProperty(WON.HAS_FACET, facet.toString());
                            needModelWrapper.addFacetUri(facet.toString());
                        }
                        //TODO VOCAB!
                        //isPart.addProperty(model.createProperty("http://schema.org/description"), hokifyJob.getDescription());
                        isPart.addProperty(DC.description, hokifyJob.getDescription());
                        need.addProperty(WON.IS, isPart);
                        
                        // TODO find and declare properties
                        /*
                        NeedContentPropertyType type = NeedContentPropertyType.IS;
                        String title = hokifyJob.getTitle() + " - Corner";
                        String description = hokifyJob.getDescription();
                        String[] tags = { "job", "hokify", "debug" };
                        boolean isUsedForTesting = false;
                        boolean isDoNotMatch = false;

                        
                        //System.out.println("-------------- ----------- NODE URI" + wonNodeUri);
                        
                        
                        //final URI needURI = wonNodeInformationService.generateNeedURI(wonNodeUri);
                        System.out.println("-------------- ----------- NODE URI" + wonNodeUri);*/
                        
                        //needModelWrapper.createContentNodeIfNonExist(NeedContentPropertyType.IS);
                        //needModelWrapper.setTitle(NeedContentPropertyType.IS, hokifyJob.getTitle() + " - 42");
                        //needModelWrapper.setDescription(NeedContentPropertyType.SEEKS, "lala lala laaa");
                        //needModelWrapper.addTag(NeedContentPropertyType.SEEKS, "test");
                        /*
                        for (String tag : tags) {
                            needModelWrapper.addTag(type, tag);
                        }
                        
                        
                        for (URI facet : facets) {
                            needModelWrapper.addFacetUri(facet.toString());
                        }
                        //DefaultNeedModelWrapper needModelWrapper = new DefaultNeedModelWrapper(needURI.toString());
                        //need.addProperty(RDF.type, WON.NEED);
                        need.addProperty(WON.HAS_FACET, WON.OWNER_FACET_STRING);
                       */
                        Dataset dataset = needModelWrapper.copyDataset();
                     
                        //HERE
                        dataset.setDefaultModel(model);

                        logger.info("creating need on won node {} with content {} ", wonNodeUri,
                                StringUtils.abbreviate(RdfUtils.toString(dataset), 150));

                        WonMessage createNeedMessage = createWonMessage(wonNodeInformationService, needURI, wonNodeUri,
                                dataset, false, false);
                        EventBotActionUtils.rememberInList(ctx, needURI, uriListName);
                        botContextWrapper.addURIJobURLRelation(hokifyJob.getUrl(), needURI);
                        EventBus bus = ctx.getEventBus();
                        EventListener successCallback = new EventListener() {
                            @Override
                            public void onEvent(Event event) throws Exception {
                                logger.info("need creation successful, new need URI is {}", needURI);

                                bus.publish(new NeedCreatedEvent(needURI, wonNodeUri, dataset, null));

                            }
                        };

                        EventListener failureCallback = new EventListener() {
                            @Override
                            public void onEvent(Event event) throws Exception {
                                String textMessage = WonRdfUtils.MessageUtils
                                        .getTextMessage(((FailureResponseEvent) event).getFailureMessage());
                                logger.info("need creation failed for need URI {}, original message URI {}: {}",
                                        new Object[] { needURI, ((FailureResponseEvent) event).getOriginalMessageURI(),
                                                textMessage });
                                System.out.println("The uriLisName : " + uriListName + " uri: " + needURI.toString());
                                EventBotActionUtils.removeFromList(ctx, needURI, uriListName);
                                botContextWrapper.removeURIJobURLRelation(needURI);
                            }
                        };
                        EventBotActionUtils.makeAndSubscribeResponseListener(createNeedMessage, successCallback,
                                failureCallback, ctx);

                        logger.info("registered listeners for response to message URI {}",
                                createNeedMessage.getMessageURI());
                        ctx.getWonMessageSender().sendWonMessage(createNeedMessage);
                        logger.info("need creation message sent with message URI {}",
                                createNeedMessage.getMessageURI());
                        System.out.println("-------------- ----------- need creation message sent with message URI {}"
                                + createNeedMessage.getMessageURI());
                    }
                }
            } catch (Exception me) {
                logger.error("messaging exception occurred: {}", me);
                System.out.println("-------------- ------- EXEPTION" + me);
            }
        }
    }
}
