package won.owner.web.rest;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.RDF;
import org.codehaus.jackson.map.util.LRUMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import won.matcher.protocol.impl.MatcherProtocolNeedServiceClient;
import won.owner.pojo.NeedPojo;
import won.owner.protocol.impl.OwnerProtocolNeedServiceClient;
import won.owner.service.impl.DataReloadService;
import won.owner.service.impl.URIService;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.IllegalNeedContentException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.model.Match;
import won.protocol.model.Need;
import won.protocol.model.NeedState;
import won.protocol.owner.OwnerProtocolNeedService;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.MatchRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.rest.LinkedDataRestClient;
import won.protocol.vocabulary.GEO;
import won.protocol.vocabulary.GR;
import won.protocol.vocabulary.WON;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: MrM
 * Date: 21.04.13
 * Time: 11:56
 * To change this template use File | Settings | File Templates.
 */
@Service
@Path("/")
public class RestController {


    final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private OwnerProtocolNeedService ownerService;

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

    private LRUMap<URI, NeedPojo> cachedNeeds = new LRUMap<URI, NeedPojo>(200,5000);


    public void setDataReloadService(DataReloadService dataReloadService) {
        this.dataReloadService = dataReloadService;
    }

    public URIService getUriService() {
        return uriService;
    }

    public void setUriService(final URIService uriService) {
        this.uriService = uriService;
    }

    public void setOwnerService(OwnerProtocolNeedService ownerService) {
        this.ownerService = ownerService;
    }

    public void setConnectionRepository(ConnectionRepository connectionRepository) {
        this.connectionRepository = connectionRepository;
    }

    public void setMatchRepository(MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    public void setNeedRepository(NeedRepository needRepository) {
        this.needRepository = needRepository;
    }

    @GET
    @Path("/{needId}/matches")
    @Produces(MediaType.APPLICATION_JSON)
    public List<NeedPojo> findMatches(@PathParam("needId") long needId)
    {

        logger.info("Looking for matches for Need: " + needId);

        List<NeedPojo> returnList = new ArrayList<NeedPojo>();

        List<Need> needs = needRepository.findById(needId);
        if(needs.isEmpty())
        {
            logger.warn("Need not found in db: " + needId);
            return returnList;
        }

        logger.info("Found need in DB: ");
        Need need = needs.get(0);


        LinkedDataRestClient linkedDataRestClient = new LinkedDataRestClient();
        NeedPojo fullNeed = new NeedPojo(need.getNeedURI(), linkedDataRestClient.readResourceData(need.getNeedURI()));

        //NeedPojo fullNeed = NeedFetcher.getNeedInfo(need);

        logger.info("Looking for matches for: " + need.getNeedURI());
        List<Match> matches =  matchRepository.findByFromNeed(need.getNeedURI());



        logger.info("Found Matches: " + matches.size());
        for(Match match : matches)
        {
            URI matchUri;
            logger.debug("using match: {} ", match);
            if(!match.getFromNeed().equals(need.getNeedURI()))
                matchUri = match.getFromNeed();
            else
                matchUri = match.getToNeed();
            logger.debug("using needUri: {} ", matchUri);
            NeedPojo matchedNeed = this.cachedNeeds.get(matchUri);
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
            if(matchedNeed != null && !NeedState.INACTIVE.equals(matchedNeed.getState())) {
                logger.debug("adding need {}", matchedNeed.getNeedURI());
                returnList.add(matchedNeed);
            }
        }

        return returnList;

    }


    @POST
    @Path("/create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public NeedPojo createNeed(NeedPojo needPojo)
    {

        logger.info("New Need:" + needPojo.getTextDescription() + "/" + needPojo.getCreationDate() + "/" +
                needPojo.getLongitude() + "/" + needPojo.getLatitude() + "/" + (needPojo.getState() == NeedState.ACTIVE));

        return resolve(needPojo);
    }


    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public List<NeedPojo> getAllNeeds()
    {

        logger.info("Getting all needs: ");

        LinkedDataRestClient linkedDataRestClient = new LinkedDataRestClient();
        List<NeedPojo> returnList = new ArrayList<NeedPojo>();

        Iterable<Need> needs = needRepository.findAll();
        for(Need need : needs)
        {
            NeedPojo needPojo = new NeedPojo(need.getNeedURI(), linkedDataRestClient.readResourceData(need.getNeedURI()));
            needPojo.setNeedId(need.getId());
            returnList.add(needPojo);
        }
        return returnList;
    }

    private NeedPojo resolve(NeedPojo needPojo)
    {


        if(needPojo.getNeedId() >= 0)
        {

            List<Need> needs = needRepository.findById(needPojo.getNeedId());
            if(!needs.isEmpty())
            {
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
                needContent.addProperty(WON.TITLE, needPojo.getTitle(), XSDDatatype.XSDstring);
            if (!needPojo.getTextDescription().isEmpty())
                needContent.addProperty(WON.TEXT_DESCRIPTION, needPojo.getTextDescription(), XSDDatatype.XSDstring);
            needModel.add(needModel.createStatement(needResource, WON.HAS_CONTENT, needContent));

            // owner
            if (needPojo.isAnonymize()) {
                needModel.add(needModel.createStatement(needResource, WON.HAS_OWNER, WON.ANONYMIZED_OWNER));
            }

            // need modalities
            Resource needModality = needModel.createResource(WON.NEED_MODALITY);

            needModel.add(needModel.createStatement(needModality, WON.AVAILABLE_DELIVERY_METHOD, GR.toResource(needPojo.getDeliveryMethod())));

            // TODO: store need modalities in separate objects to enable easier checking and multiple instances
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
                Resource timeConstraint = needModel.createResource(WON.TIME_CONSTRAINT)
                        .addProperty(WON.RECUR_INFINITE_TIMES, Boolean.toString(needPojo.getRecurInfiniteTimes()), XSDDatatype.XSDboolean);
                if (!needPojo.getStartTime().isEmpty())
                    timeConstraint.addProperty(WON.START_TIME, needPojo.getStartTime(), XSDDatatype.XSDdateTime);
                if (!needPojo.getEndTime().isEmpty())
                    timeConstraint.addProperty(WON.END_TIME, needPojo.getEndTime(), XSDDatatype.XSDdateTime);
                if (needPojo.getRecurIn() != null)
                    timeConstraint.addProperty(WON.RECUR_IN, Long.toString(needPojo.getRecurIn()));
                if (needPojo.getRecurTimes() != null)
                    timeConstraint.addProperty(WON.RECUR_TIMES, Integer.toString(needPojo.getRecurTimes()));
                needModel.add(needModel.createStatement(needModality, WON.AVAILABLE_AT_TIME, timeConstraint));
            }

            needModel.add(needModel.createStatement(needResource, WON.HAS_NEED_MODALITY, needModality));

            if (needPojo.getWonNode().equals("")) {
                //TODO: this is a temporary hack, please fix. The protocol expects boolean and we have an enum for needState
                needURI = ownerService.createNeed(ownerURI, needModel, needPojo.getState() == NeedState.ACTIVE);
            } else {
                needURI = ((OwnerProtocolNeedServiceClient) ownerService).createNeed(ownerURI, needModel, needPojo.getState() == NeedState.ACTIVE, needPojo.getWonNode());
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


            if(needs.size() == 1)
                return fullNeed;

            // return viewNeed(need.getId().toString(), model);
        } catch (IllegalNeedContentException e) {
            e.printStackTrace();
        }


        return new NeedPojo();
    }

}
