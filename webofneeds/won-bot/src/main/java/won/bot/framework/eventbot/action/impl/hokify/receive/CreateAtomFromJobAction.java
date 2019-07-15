package won.bot.framework.eventbot.action.impl.hokify.receive;

import java.math.RoundingMode;
import java.net.URI;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.datatypes.BaseDatatype;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;

import won.bot.framework.bot.context.HokifyJobBotContextWrapper;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.action.impl.atomlifecycle.AbstractCreateAtomAction;
import won.bot.framework.eventbot.action.impl.hokify.HokifyJob;
import won.bot.framework.eventbot.action.impl.hokify.util.HokifyBotsApi;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.atomlifecycle.AtomCreatedEvent;
import won.bot.framework.eventbot.event.impl.hokify.CreateAtomFromJobEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.FailureResponseEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.message.WonMessage;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.DefaultAtomModelWrapper;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.SCHEMA;
import won.protocol.vocabulary.WONCON;
import won.protocol.vocabulary.WONMATCH;
import won.protocol.vocabulary.WXCHAT;

/**
 * Created by MS on 18.09.2018.
 */
public class CreateAtomFromJobAction extends AbstractCreateAtomAction {
    private HokifyBotsApi hokifyBotsApi;
    private boolean createAllInOne;

    public CreateAtomFromJobAction(EventListenerContext eventListenerContext, boolean createAllInOne) {
        super(eventListenerContext);
        this.createAllInOne = createAllInOne;
    }

    protected void doRun(Event event, EventListener executingListener) throws Exception {
        EventListenerContext ctx = getEventListenerContext();
        if (event instanceof CreateAtomFromJobEvent
                        && ctx.getBotContextWrapper() instanceof HokifyJobBotContextWrapper) {
            HokifyJobBotContextWrapper botContextWrapper = (HokifyJobBotContextWrapper) ctx.getBotContextWrapper();
            this.hokifyBotsApi = ((CreateAtomFromJobEvent) event).getHokifyBotsApi();
            ArrayList<HokifyJob> hokifyJobs = ((CreateAtomFromJobEvent) event).getHokifyJobs();
            try {
                if (createAllInOne) {
                    logger.info("Create all job atoms");
                    for (HokifyJob hokifyJob : hokifyJobs) {
                        this.createAtomFromJob(ctx, botContextWrapper, hokifyJob);
                    }
                } else {
                    boolean created = false;
                    Random random = new Random();
                    while (!created) {
                        // Only one single random job
                        logger.info("Create 1 random job atom");
                        HokifyJob hokifyJob = hokifyJobs.get(random.nextInt(hokifyJobs.size()));
                        if (this.createAtomFromJob(ctx, botContextWrapper, hokifyJob)) {
                            created = true;
                        }
                    }
                }
            } catch (Exception me) {
                logger.error("messaging exception occurred: {}", me);
            }
        }
    }

    protected boolean createAtomFromJob(EventListenerContext ctx, HokifyJobBotContextWrapper botContextWrapper,
                    HokifyJob hokifyJob) {
        if (botContextWrapper.getAtomUriForJobURL(hokifyJob.getUrl()) != null) {
            logger.info("Atom already exists for job: {}", hokifyJob.getUrl());
            return false;
        } else {
            final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
            WonNodeInformationService wonNodeInformationService = ctx.getWonNodeInformationService();
            final URI atomURI = wonNodeInformationService.generateAtomURI(wonNodeUri);
            Dataset dataset = this.generateJobAtomStructure(atomURI, hokifyJob);
            logger.debug("creating atom on won node {} with content {} ", wonNodeUri,
                            StringUtils.abbreviate(RdfUtils.toString(dataset), 150));
            WonMessage createAtomMessage = createWonMessage(wonNodeInformationService, atomURI, wonNodeUri, dataset,
                            false, false);
            EventBotActionUtils.rememberInList(ctx, atomURI, uriListName);
            botContextWrapper.addURIJobURLRelation(hokifyJob.getUrl(), atomURI);
            EventBus bus = ctx.getEventBus();
            EventListener successCallback = new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    logger.debug("atom creation successful, new atom URI is {}", atomURI);
                    bus.publish(new AtomCreatedEvent(atomURI, wonNodeUri, dataset, null));
                }
            };
            EventListener failureCallback = new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    String textMessage = WonRdfUtils.MessageUtils
                                    .getTextMessage(((FailureResponseEvent) event).getFailureMessage());
                    logger.error("atom creation failed for atom URI {}, original message URI {}: {}", new Object[] {
                                    atomURI, ((FailureResponseEvent) event).getOriginalMessageURI(), textMessage });
                    EventBotActionUtils.removeFromList(ctx, atomURI, uriListName);
                    botContextWrapper.removeURIJobURLRelation(atomURI);
                }
            };
            EventBotActionUtils.makeAndSubscribeResponseListener(createAtomMessage, successCallback, failureCallback,
                            ctx);
            logger.debug("registered listeners for response to message URI {}", createAtomMessage.getMessageURI());
            ctx.getWonMessageSender().sendWonMessage(createAtomMessage);
            logger.debug("atom creation message sent with message URI {}", createAtomMessage.getMessageURI());
            return true;
        }
    }

    private Dataset generateJobAtomStructure(URI atomURI, HokifyJob hokifyJob) {
        DefaultAtomModelWrapper atomModelWrapper = new DefaultAtomModelWrapper(atomURI);
        Resource atom = atomModelWrapper.getAtomModel().createResource(atomURI.toString());
        Resource seeksPart = atom.getModel().createResource();
        // @type
        atom.addProperty(RDF.type, SCHEMA.JOBPOSTING);
        // s:url
        atom.addProperty(SCHEMA.URL, "");
        // s:title
        atom.addProperty(SCHEMA.TITLE, hokifyJob.getTitle());
        // s:datePosted
        // TODO:convert to s:Date (ISO 8601)
        atom.addProperty(SCHEMA.DATEPOSTED, hokifyJob.getDate());
        // s:image
        Resource image = atom.getModel().createResource();
        image.addProperty(RDF.type, SCHEMA.URL);
        image.addProperty(SCHEMA.VALUE, hokifyJob.getImage());
        atom.addProperty(SCHEMA.IMAGE, image);
        // s:hiringOrganization
        Resource hiringOrganisation = atom.getModel().createResource();
        hiringOrganisation.addProperty(RDF.type, SCHEMA.ORGANIZATION);
        hiringOrganisation.addProperty(SCHEMA.NAME, hokifyJob.getCompany());
        atom.addProperty(SCHEMA.ORGANIZATION, hiringOrganisation);
        // s:jobLocation
        HashMap<String, String> location = hokifyBotsApi.fetchGeoLocation(hokifyJob.getCity(), hokifyJob.getCountry());
        if (location != null) {
            Resource jobLocation = atom.getModel().createResource();
            jobLocation.addProperty(RDF.type, SCHEMA.PLACE);
            // TODO look up lon/lat via nominatim
            atom.addProperty(SCHEMA.JOBLOCATION, jobLocation);
            DecimalFormat df = new DecimalFormat("##.######");
            df.setRoundingMode(RoundingMode.HALF_UP);
            df.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
            String nwlat = df.format(Double.parseDouble(location.get("nwlat")));
            String nwlng = df.format(Double.parseDouble(location.get("nwlng")));
            String selat = df.format(Double.parseDouble(location.get("selat")));
            String selng = df.format(Double.parseDouble(location.get("selng")));
            String lat = df.format(Double.parseDouble(location.get("lat")));
            String lng = df.format(Double.parseDouble(location.get("lng")));
            String name = location.get("name");
            Resource boundingBoxResource = atom.getModel().createResource();
            Resource nwCornerResource = atom.getModel().createResource();
            Resource seCornerResource = atom.getModel().createResource();
            Resource geoResource = atom.getModel().createResource();
            jobLocation.addProperty(SCHEMA.NAME, name);
            jobLocation.addProperty(SCHEMA.GEO, geoResource);
            geoResource.addProperty(RDF.type, SCHEMA.GEOCOORDINATES);
            geoResource.addProperty(SCHEMA.LATITUDE, lat);
            geoResource.addProperty(SCHEMA.LONGITUDE, lng);
            RDFDatatype bigdata_geoSpatialDatatype = new BaseDatatype(
                            "http://www.bigdata.com/rdf/geospatial/literals/v1#lat-lon");
            geoResource.addProperty(WONCON.geoSpatial, lat + "#" + lng, bigdata_geoSpatialDatatype);
            jobLocation.addProperty(WONCON.boundingBox, boundingBoxResource);
            boundingBoxResource.addProperty(WONCON.northWestCorner, nwCornerResource);
            nwCornerResource.addProperty(RDF.type, SCHEMA.GEOCOORDINATES);
            nwCornerResource.addProperty(SCHEMA.LATITUDE, nwlat);
            nwCornerResource.addProperty(SCHEMA.LONGITUDE, nwlng);
            boundingBoxResource.addProperty(WONCON.southEastCorner, seCornerResource);
            seCornerResource.addProperty(RDF.type, SCHEMA.GEOCOORDINATES);
            seCornerResource.addProperty(SCHEMA.LATITUDE, selat);
            seCornerResource.addProperty(SCHEMA.LONGITUDE, selng);
        }
        // s:description
        atom.addProperty(SCHEMA.DESCRIPTION, filterDescriptionString(hokifyJob.getDescription()));
        // s:baseSalary
        atom.addProperty(SCHEMA.BASESALARY, hokifyJob.getSalary());
        // s:employmentType
        atom.addProperty(SCHEMA.EMPLYOMENTTYPE, hokifyJob.getJobtype() != null ? hokifyJob.getJobtype() : "");
        // s:industry
        for (Object field : hokifyJob.getField()) {
            atom.addProperty(SCHEMA.INDUSTRY, parseField(field));
        }
        String[] tags = { "job", "hokify", "offer-job" };
        for (String tag : tags) {
            atom.addProperty(WONCON.tag, tag);
        }
        seeksPart.addProperty(RDF.type, SCHEMA.PERSON);
        seeksPart.addProperty(WONMATCH.seeks, SCHEMA.JOBPOSTING);
        atomModelWrapper.addSocket("#ChatSocket", WXCHAT.ChatSocketString);
        atomModelWrapper.setDefaultSocket("#ChatSocket");
        atomModelWrapper.addFlag(WONMATCH.NoHintForMe);
        atom.addProperty(WONMATCH.seeks, seeksPart);
        return atomModelWrapper.copyDataset();
    }

    private String filterDescriptionString(String description) {
        // TODO filter out contact information
        return description;
    }

    private String parseField(Object field) {
        // TODO parse the field string
        return field.toString();
    }
}
