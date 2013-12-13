package won.owner.web.rest;


import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DC;
import org.apache.commons.collections.map.LRUMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import won.owner.pojo.NeedPojo;
import won.owner.protocol.impl.OwnerProtocolNeedServiceClient;
import won.protocol.owner.OwnerProtocolNeedServiceClientSide;
import won.owner.service.impl.DataReloadService;
import won.owner.service.impl.URIService;
import won.protocol.exception.IllegalNeedContentException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.model.Match;
import won.protocol.model.Need;
import won.protocol.model.NeedState;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.MatchRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.rest.LinkedDataRestClient;
import won.protocol.vocabulary.GEO;
import won.protocol.vocabulary.WON;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Controller
@RequestMapping("/rest")
public class RestController
{

  final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  @Qualifier("ownerProtocolNeedServiceClientJMSBased")
  private OwnerProtocolNeedServiceClientSide ownerService;

  @Autowired
  private NeedRepository needRepository;

  @Autowired
  private MatchRepository matchRepository;

  @Autowired
  private ConnectionRepository connectionRepository;

  @Autowired
  private URIService uriService;

  @Autowired
  private DataReloadService dataReloadService;

  //TODO: this is a quick fix and the only reason for us to use commons-collections. Rework to use ehcache!
  private LRUMap cachedNeeds = new LRUMap(200, 1000);


  public void setDataReloadService(DataReloadService dataReloadService)
  {
    this.dataReloadService = dataReloadService;
  }

  public URIService getUriService()
  {
    return uriService;
  }

  public void setUriService(final URIService uriService)
  {
    this.uriService = uriService;
  }

  public void setOwnerService(OwnerProtocolNeedServiceClientSide ownerService)
  {
    this.ownerService = ownerService;
  }

  public void setConnectionRepository(ConnectionRepository connectionRepository)
  {
    this.connectionRepository = connectionRepository;
  }

  public void setMatchRepository(MatchRepository matchRepository)
  {
    this.matchRepository = matchRepository;
  }

  public void setNeedRepository(NeedRepository needRepository)
  {
    this.needRepository = needRepository;
  }

  @GET
  @Path("/{needId}/matches")
  @Produces(MediaType.APPLICATION_JSON)
  @ResponseBody
  @RequestMapping(
      value="/{needId}/matches",
      produces = MediaType.APPLICATION_JSON,
      method = RequestMethod.GET)
  public List<NeedPojo> findMatches(@PathParam("needId") long needId)
  {

    logger.info("Looking for matches for Need: " +  needId);

    List<NeedPojo> returnList = new ArrayList<NeedPojo>();

    List<Need> needs = needRepository.findById(needId);
    if (needs.isEmpty()) {
      logger.warn("Need not found in db: " + needId);
      return returnList;
    }

    logger.info("Found need in DB: ");
    Need need = needs.get(0);


    LinkedDataRestClient linkedDataRestClient = new LinkedDataRestClient();
    NeedPojo fullNeed = new NeedPojo(need.getNeedURI(), linkedDataRestClient.readResourceData(need.getNeedURI()));

    //NeedPojo fullNeed = NeedFetcher.getNeedInfo(need);

    logger.info("Looking for matches for: " + need.getNeedURI());
    List<Match> matches = matchRepository.findByFromNeed(need.getNeedURI());


    logger.info("Found Matches: " + matches.size());
    for (Match match : matches) {
      URI matchUri;
      logger.debug("using match: {} ", match);
      if (!match.getFromNeed().equals(need.getNeedURI()))
        matchUri = match.getFromNeed();
      else
        matchUri = match.getToNeed();
      logger.debug("using needUri: {} ", matchUri);
      NeedPojo matchedNeed = (NeedPojo) this.cachedNeeds.get(matchUri);
      if (matchedNeed == null) {
        List<Need> matchNeeds = needRepository.findByNeedURI(matchUri);
        logger.debug("found {} needs for uri {} in repo", matchNeeds.size(), matchUri);
        logger.debug("matchUri:{}, needURi:{}", matchUri, matchNeeds.get(0));
        logger.debug("fetching need {} from WON node", matchUri);
        matchedNeed = new NeedPojo(matchNeeds.get(0).getNeedURI(), linkedDataRestClient.readResourceData(matchNeeds.get(0).getNeedURI()));
        //matchedNeed = new NeedPojo(matchUri, linkedDataRestClient.readResourceData(matchUri));
        //NeedPojo matchedNeed = NeedFetcher.getNeedInfo(matchNeeds.get(0));
        this.cachedNeeds.put(matchUri, matchedNeed);
      }
      logger.debug("matched need's state: {}", matchedNeed.getState());
      if (matchedNeed != null && !NeedState.INACTIVE.equals(matchedNeed.getState())) {
        logger.debug("adding need {}", matchedNeed.getNeedURI());
        returnList.add(matchedNeed);
      }
    }

    return returnList;

  }


	@ResponseBody
	@RequestMapping(
      value = "/create",
      consumes = MediaType.APPLICATION_JSON,
      produces = MediaType.APPLICATION_JSON,
      method = RequestMethod.POST
      )
  public NeedPojo createNeed(NeedPojo needPojo) throws Exception {

    logger.info("New Need:" + needPojo.getTextDescription() + "/" + needPojo.getCreationDate() + "/" +
        needPojo.getLongitude() + "/" + needPojo.getLatitude() + "/" + (needPojo.getState() == NeedState.ACTIVE));

    return resolve(needPojo);
  }


	@ResponseBody
	@RequestMapping(
      value = "/",
      produces = MediaType.APPLICATION_JSON,
      method = RequestMethod.GET
  )
  public List<NeedPojo> getAllNeeds()
  {

    logger.info("Getting all needs: ");

    LinkedDataRestClient linkedDataRestClient = new LinkedDataRestClient();
    List<NeedPojo> returnList = new ArrayList<NeedPojo>();

    Iterable<Need> needs = needRepository.findAll();
    for (Need need : needs) {
      NeedPojo needPojo = new NeedPojo(need.getNeedURI(), linkedDataRestClient.readResourceData(need.getNeedURI()));
      needPojo.setNeedId(need.getId());
      returnList.add(needPojo);
    }
    return returnList;
  }

  private NeedPojo resolve(NeedPojo needPojo) throws Exception {


    if (needPojo.getNeedId() >= 0) {

      List<Need> needs = needRepository.findById(needPojo.getNeedId());
      if (!needs.isEmpty()) {
        logger.warn("Deactivating old need");
        try {
          ownerService.deactivate(needs.get(0).getNeedURI());
        } catch (NoSuchNeedException e) {
          logger.warn("Could not deactivate old Need: " + needs.get(0).getNeedURI());
        }
      }
    }
    URI needURI;

    try {
      URI ownerURI = this.uriService.getOwnerProtocolOwnerServiceEndpointURI();

      com.hp.hpl.jena.rdf.model.Model needModel = ModelFactory.createDefaultModel();

      Resource needResource = needModel.createResource(WON.NEED);

      // need type
      needModel.add(needModel.createStatement(needResource, WON.HAS_BASIC_NEED_TYPE, WON.toResource(needPojo.getBasicNeedType())));

      // need content
      Resource needContent = needModel.createResource(WON.NEED_CONTENT);
      if (!needPojo.getTitle().isEmpty())
        needContent.addProperty(DC.title, needPojo.getTitle(), XSDDatatype.XSDstring);
      if (!needPojo.getTextDescription().isEmpty())
        needContent.addProperty(WON.HAS_TEXT_DESCRIPTION, needPojo.getTextDescription(), XSDDatatype.XSDstring);
      needModel.add(needModel.createStatement(needResource, WON.HAS_CONTENT, needContent));

      // owner
      if (needPojo.isAnonymize()) {
        needModel.add(needModel.createStatement(needResource, WON.HAS_OWNER, WON.ANONYMIZED_OWNER));
      }

      // need modalities
      Resource needModality = needModel.createResource(WON.NEED_MODALITY);

      //price and currency
      if (needPojo.getUpperPriceLimit() != null || needPojo.getLowerPriceLimit() != null) {
        Resource priceSpecification = needModel.createResource(WON.PRICE_SPECIFICATION);
        if (needPojo.getLowerPriceLimit() != null)
          priceSpecification.addProperty(WON.HAS_LOWER_PRICE_LIMIT, Double.toString(needPojo.getLowerPriceLimit()), XSDDatatype.XSDdouble);
        if (needPojo.getUpperPriceLimit() != null)
          priceSpecification.addProperty(WON.HAS_UPPER_PRICE_LIMIT, Double.toString(needPojo.getUpperPriceLimit()), XSDDatatype.XSDdouble);
        if (!needPojo.getCurrency().isEmpty())
          priceSpecification.addProperty(WON.HAS_CURRENCY, needPojo.getCurrency(), XSDDatatype.XSDstring);

        needModel.add(needModel.createStatement(needModality, WON.HAS_PRICE_SPECIFICATION, priceSpecification));
      }

      if (needPojo.getLatitude() != null && needPojo.getLongitude() != null) {
        Resource location = needModel.createResource(GEO.POINT)
            .addProperty(GEO.LATITUDE, Double.toString(needPojo.getLatitude()))
            .addProperty(GEO.LONGITUDE, Double.toString(needPojo.getLongitude()));

        needModel.add(needModel.createStatement(needModality, WON.AVAILABLE_AT_LOCATION, location));
      }

      // time constraint
      if (!needPojo.getStartTime().isEmpty() || !needPojo.getEndTime().isEmpty()) {
        Resource timeConstraint = needModel.createResource(WON.TIME_SPECIFICATION)
            .addProperty(WON.HAS_RECUR_INFINITE_TIMES, Boolean.toString(needPojo.getRecurInfiniteTimes()), XSDDatatype.XSDboolean);
        if (!needPojo.getStartTime().isEmpty())
          timeConstraint.addProperty(WON.HAS_START_TIME, needPojo.getStartTime(), XSDDatatype.XSDdateTime);
        if (!needPojo.getEndTime().isEmpty())
          timeConstraint.addProperty(WON.HAS_END_TIME, needPojo.getEndTime(), XSDDatatype.XSDdateTime);
        if (needPojo.getRecurIn() != null)
          timeConstraint.addProperty(WON.HAS_RECURS_IN, Long.toString(needPojo.getRecurIn()));
        if (needPojo.getRecurTimes() != null)
          timeConstraint.addProperty(WON.HAS_RECURS_TIMES, Integer.toString(needPojo.getRecurTimes()));
        needModel.add(needModel.createStatement(needModality, WON.HAS_TIME_SPECIFICATION, timeConstraint));
      }

      needModel.add(needModel.createStatement(needResource, WON.HAS_NEED_MODALITY, needModality));

      if (needPojo.getWonNode().equals("")) {
        Future<URI> futureResult = ownerService.createNeed(ownerURI, needModel, needPojo.getState() == NeedState.ACTIVE);
        needURI = futureResult.get();
      } else {
        Future<URI> futureResult = ((OwnerProtocolNeedServiceClient) ownerService).createNeed(ownerURI, needModel, needPojo.getState() == NeedState.ACTIVE, URI.create(needPojo.getWonNode()));
        needURI = futureResult.get();
      }

      List<Need> needs = needRepository.findByNeedURI(needURI);


      LinkedDataRestClient linkedDataRestClient = new LinkedDataRestClient();
      NeedPojo fullNeed = new NeedPojo(needURI, linkedDataRestClient.readResourceData(needs.get(0).getNeedURI()));
      fullNeed.setNeedId(needs.get(0).getId());

      //NeedPojo fullNeed = NeedFetcher.getNeedInfo(needs.get(0));
      logger.info("Added need id:" + fullNeed.getNeedId() + "uri: " + needURI);


/*
//            if(!fullNeed.isActive())
//            {
//                logger.info("Need is not active returning");
//                return fullNeed;
//            }

            MatcherProtocolNeedServiceClient client = new MatcherProtocolNeedServiceClient();
            client.setLinkedDataRestClient(new LinkedDataRestClient());

            Iterable<Need> allNeeds =  needRepository.findAll();
            logger.info("Adding match to needs");
            for(Need iterNeed : allNeeds)
            {
                if(iterNeed.getState() == NeedState.INACTIVE)
                {
                    logger.info("Need: " + iterNeed.getNeedURI() + " is not active");
                    continue;

                }
                if(iterNeed.getNeedURI().equals(needURI))
                    continue;

                try {
                    logger.info("Matching need: " + needURI + " to: " + iterNeed.getNeedURI());
                    client.hint(needURI, iterNeed.getNeedURI(), 0.5, new URI("http://www.agentdroid.com"));
                    client.hint( iterNeed.getNeedURI(),needURI, 0.5, new URI("http://www.agentdroid.com"));
                } catch (URISyntaxException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (IllegalMessageForNeedStateException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (NoSuchNeedException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
*/


      if (needs.size() == 1)
        return fullNeed;

      // return viewNeed(need.getId().toString(), model);
    } catch (IllegalNeedContentException e) {
      e.printStackTrace();
    }


    return new NeedPojo();
  }

}
