package won.bot.framework.eventbot.action.impl.hokify.receive;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.datatypes.BaseDatatype;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import won.bot.framework.bot.context.HokifyJobBotContextWrapper;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.action.impl.hokify.HokifyJob;
import won.bot.framework.eventbot.action.impl.hokify.util.HokifyBotsApi;
import won.bot.framework.eventbot.action.impl.needlifecycle.AbstractCreateNeedAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.hokify.CreateNeedFromJobEvent;
import won.bot.framework.eventbot.event.impl.needlifecycle.NeedCreatedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.FailureResponseEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.message.WonMessage;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.DefaultNeedModelWrapper;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.SCHEMA;
import won.protocol.vocabulary.WON;

import java.math.RoundingMode;
import java.net.URI;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

/**
 * Created by MS on 18.09.2018.
 */
public class CreateNeedFromJobAction extends AbstractCreateNeedAction {

  private HokifyBotsApi hokifyBotsApi;
  private boolean createAllInOne;

  public CreateNeedFromJobAction(EventListenerContext eventListenerContext, boolean createAllInOne) {
    super(eventListenerContext);
    this.createAllInOne = createAllInOne;

  }

  protected void doRun(Event event, EventListener executingListener) throws Exception {
    EventListenerContext ctx = getEventListenerContext();
    if (event instanceof CreateNeedFromJobEvent && ctx.getBotContextWrapper() instanceof HokifyJobBotContextWrapper) {
      HokifyJobBotContextWrapper botContextWrapper = (HokifyJobBotContextWrapper) ctx.getBotContextWrapper();
      this.hokifyBotsApi = ((CreateNeedFromJobEvent) event).getHokifyBotsApi();
      ArrayList<HokifyJob> hokifyJobs = ((CreateNeedFromJobEvent) event).getHokifyJobs();

      try {
        if (createAllInOne) {
          logger.info("Create all job needs");
          for (HokifyJob hokifyJob : hokifyJobs) {
            this.createNeedFromJob(ctx, botContextWrapper, hokifyJob);
          }
        } else {
          boolean created = false;
          Random random = new Random();
          while (!created) {
            // Only one single random job
            logger.info("Create 1 random job need");
            HokifyJob hokifyJob = hokifyJobs.get(random.nextInt(hokifyJobs.size()));
            if (this.createNeedFromJob(ctx, botContextWrapper, hokifyJob)) {
              created = true;
            }
          }
        }
      } catch (Exception me) {
        logger.error("messaging exception occurred: {}", me);
      }
    }

  }

  protected boolean createNeedFromJob(EventListenerContext ctx, HokifyJobBotContextWrapper botContextWrapper,
      HokifyJob hokifyJob) {
    if (botContextWrapper.getNeedUriForJobURL(hokifyJob.getUrl()) != null) {
      logger.info("Need already exists for job: {}", hokifyJob.getUrl());
      return false;
    } else {
      final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
      WonNodeInformationService wonNodeInformationService = ctx.getWonNodeInformationService();
      final URI needURI = wonNodeInformationService.generateNeedURI(wonNodeUri);

      Dataset dataset = this.generateJobNeedStructure(needURI, hokifyJob);

      logger.debug("creating need on won node {} with content {} ", wonNodeUri,
          StringUtils.abbreviate(RdfUtils.toString(dataset), 150));

      WonMessage createNeedMessage = createWonMessage(wonNodeInformationService, needURI, wonNodeUri, dataset, false,
          false);
      EventBotActionUtils.rememberInList(ctx, needURI, uriListName);
      botContextWrapper.addURIJobURLRelation(hokifyJob.getUrl(), needURI);
      EventBus bus = ctx.getEventBus();
      EventListener successCallback = new EventListener() {
        @Override public void onEvent(Event event) throws Exception {
          logger.debug("need creation successful, new need URI is {}", needURI);

          bus.publish(new NeedCreatedEvent(needURI, wonNodeUri, dataset, null));

        }
      };

      EventListener failureCallback = new EventListener() {
        @Override public void onEvent(Event event) throws Exception {
          String textMessage = WonRdfUtils.MessageUtils
              .getTextMessage(((FailureResponseEvent) event).getFailureMessage());
          logger.error("need creation failed for need URI {}, original message URI {}: {}",
              new Object[] { needURI, ((FailureResponseEvent) event).getOriginalMessageURI(), textMessage });
          EventBotActionUtils.removeFromList(ctx, needURI, uriListName);
          botContextWrapper.removeURIJobURLRelation(needURI);

        }

      };
      EventBotActionUtils.makeAndSubscribeResponseListener(createNeedMessage, successCallback, failureCallback, ctx);

      logger.debug("registered listeners for response to message URI {}", createNeedMessage.getMessageURI());
      ctx.getWonMessageSender().sendWonMessage(createNeedMessage);
      logger.debug("need creation message sent with message URI {}", createNeedMessage.getMessageURI());

      return true;
    }
  }

  private Dataset generateJobNeedStructure(URI needURI, HokifyJob hokifyJob) {
    DefaultNeedModelWrapper needModelWrapper = new DefaultNeedModelWrapper(needURI.toString());

    Resource need = needModelWrapper.getNeedModel().createResource(needURI.toString());
    Resource seeksPart = need.getModel().createResource();
    // @type
    need.addProperty(RDF.type, SCHEMA.JOBPOSTING);

    // s:url
    need.addProperty(SCHEMA.URL, "");

    // s:title
    need.addProperty(SCHEMA.TITLE, hokifyJob.getTitle());

    // s:datePosted
    // TODO:convert to s:Date (ISO 8601)
    need.addProperty(SCHEMA.DATEPOSTED, hokifyJob.getDate());

    // s:image
    Resource image = need.getModel().createResource();
    image.addProperty(RDF.type, SCHEMA.URL);
    image.addProperty(SCHEMA.VALUE, hokifyJob.getImage());
    need.addProperty(SCHEMA.IMAGE, image);

    // s:hiringOrganization
    Resource hiringOrganisation = need.getModel().createResource();
    hiringOrganisation.addProperty(RDF.type, SCHEMA.ORGANIZATION);
    hiringOrganisation.addProperty(SCHEMA.NAME, hokifyJob.getCompany());
    need.addProperty(SCHEMA.ORGANIZATION, hiringOrganisation);

    // s:jobLocation
    Resource jobLocation = need.getModel().createResource();
    jobLocation.addProperty(RDF.type, SCHEMA.PLACE);
    // TODO look up lon/lat via nominatim

    need.addProperty(SCHEMA.JOBLOCATION, jobLocation);

    HashMap<String, String> location = hokifyBotsApi.fetchGeoLocation(hokifyJob.getCity(), hokifyJob.getCountry());
    if (location != null) {
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

      Resource boundingBoxResource = need.getModel().createResource();
      Resource nwCornerResource = need.getModel().createResource();
      Resource seCornerResource = need.getModel().createResource();
      Resource geoResource = need.getModel().createResource();
      jobLocation.addProperty(SCHEMA.NAME, name);
      jobLocation.addProperty(SCHEMA.GEO, geoResource);
      geoResource.addProperty(RDF.type, SCHEMA.GEOCOORDINATES);
      geoResource.addProperty(SCHEMA.LATITUDE, lat);
      geoResource.addProperty(SCHEMA.LONGITUDE, lng);

      RDFDatatype bigdata_geoSpatialDatatype = new BaseDatatype(
          "http://www.bigdata.com/rdf/geospatial/literals/v1#lat-lon");

      geoResource.addProperty(WON.GEO_SPATIAL, lat + "#" + lng, bigdata_geoSpatialDatatype);
      jobLocation.addProperty(WON.HAS_BOUNDING_BOX, boundingBoxResource);
      boundingBoxResource.addProperty(WON.HAS_NORTH_WEST_CORNER, nwCornerResource);
      nwCornerResource.addProperty(RDF.type, SCHEMA.GEOCOORDINATES);
      nwCornerResource.addProperty(SCHEMA.LATITUDE, nwlat);
      nwCornerResource.addProperty(SCHEMA.LONGITUDE, nwlng);
      boundingBoxResource.addProperty(WON.HAS_SOUTH_EAST_CORNER, seCornerResource);
      seCornerResource.addProperty(RDF.type, SCHEMA.GEOCOORDINATES);
      seCornerResource.addProperty(SCHEMA.LATITUDE, selat);
      seCornerResource.addProperty(SCHEMA.LONGITUDE, selng);
    } else {
      String alternateLocation = hokifyJob.getCity() + " " + hokifyJob.getCountry();
      jobLocation.addProperty(SCHEMA.NAME, alternateLocation);
    }
    // s:description
    need.addProperty(SCHEMA.DESCRIPTION, filterDescriptionString(hokifyJob.getDescription()));

    // s:baseSalary
    need.addProperty(SCHEMA.BASESALARY, hokifyJob.getSalary());

    // s:employmentType
    need.addProperty(SCHEMA.EMPLYOMENTTYPE, hokifyJob.getJobtype() != null ? hokifyJob.getJobtype() : "");

    // s:industry
    for (Object field : hokifyJob.getField()) {
      need.addProperty(SCHEMA.INDUSTRY, parseField(field));
    }

    String[] tags = { "job", "hokify", "offer-job" };

    for (String tag : tags) {
      need.addProperty(WON.HAS_TAG, tag);
    }

    seeksPart.addProperty(RDF.type, SCHEMA.PERSON);
    seeksPart.addProperty(WON.SEEKS, SCHEMA.JOBPOSTING);

    needModelWrapper.addFacet("#ChatFacet", WON.CHAT_FACET_STRING);
    needModelWrapper.setDefaultFacet("#ChatFacet");
    needModelWrapper.addFlag(WON.NO_HINT_FOR_ME);
    need.addProperty(WON.SEEKS, seeksPart);

    return needModelWrapper.copyDataset();
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
