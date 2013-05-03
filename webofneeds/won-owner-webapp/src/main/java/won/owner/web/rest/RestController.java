package won.owner.web.rest;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.RDF;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.http.client.utils.URIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import won.matcher.protocol.impl.MatcherProtocolNeedServiceClient;
import won.owner.pojo.NeedPojo;
import won.owner.service.impl.DataReloadService;
import won.owner.service.impl.URIService;
import won.owner.util.NeedFetcher;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.IllegalNeedContentException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.model.Match;
import won.protocol.model.Need;
import won.protocol.model.NeedState;
import won.protocol.model.WON;
import won.protocol.owner.OwnerProtocolNeedService;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.MatchRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.rest.LinkedDataRestClient;

import javax.management.remote.JMXConnectorFactory;
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
        NeedPojo fullNeed = NeedFetcher.getNeedInfo(need);

        logger.info("Looking for matches for: " + need.getNeedURI());
        List<Match> matches =  matchRepository.findByFromNeed(need.getNeedURI());



        logger.info("Found Matches: " + matches.size());
        for(Match match : matches)
        {
            URI matchUri;
            if(!match.getFromNeed().equals(need.getNeedURI()))
                matchUri = match.getFromNeed();
            else
                matchUri = match.getToNeed();


            List<Need> matchNeeds = needRepository.findByNeedURI(matchUri);
            NeedPojo matchedNeed = NeedFetcher.getNeedInfo(matchNeeds.get(0));
            if(matchNeeds.get(0).getState().equals(NeedState.ACTIVE))
                returnList.add(matchedNeed);
        }

        return returnList;

    }


    @POST
    @Path("/create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public NeedPojo createNeed(NeedPojo needPojo)
    {

        logger.info("New Need:" + needPojo.getTextDescription() + "/" + needPojo.getDate() + "/" +
                needPojo.getLongitude() + "/" + needPojo.getLatitude() + "/" + needPojo.isActive());

        return resolve(needPojo);
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
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");



        try {
            cal.setTime(sdf.parse(needPojo.getDate()));// all done
        } catch (ParseException e) {
            logger.warn("Could not parse Date String:" + needPojo.getDate() + " " + e.getMessage());
        }


        try {
            URI ownerURI = this.uriService.getOwnerProtocolOwnerServiceEndpointURI();
            com.hp.hpl.jena.rdf.model.Model m = ModelFactory.createDefaultModel();
            // use the FileManager to find the input file
            InputStream in = FileManager.get().open( "/offer.ttl" );
            if (in == null) {
                throw new IllegalArgumentException(
                        "File: offer.ttl not found");
            }
            m.read(in, null, "TTL");

            in.close();
            ResIterator it = m.listSubjectsWithProperty(RDF.type, WON.NEED_DESCRIPTION);
            if (it.hasNext()){
                Resource mainContentNode = it.next();
                m.add(m.createStatement(mainContentNode, WON.TEXT_DESCRIPTION, needPojo.getTextDescription()));
            }


            String whereUri = "http://www.w3.org/2003/01/geo/wgs84_pos#Point";
            String latUri = "http://www.w3.org/2003/01/geo/wgs84_pos#latitude";
            String longUri = "http://www.w3.org/2003/01/geo/wgs84_pos#longitude";
            Property whereProp = m.createProperty(whereUri);
            Property latitudeProp = m.createProperty(latUri);
            Property longitudeProp = m.createProperty(longUri);

            it = m.listSubjectsWithProperty(RDF.type, whereProp);
            if (it.hasNext()) {
                Resource mainContentNode = it.next();
                m.removeAll(mainContentNode, latitudeProp, (RDFNode) null);
                m.addLiteral(mainContentNode, latitudeProp, needPojo.getLatitude());
                m.removeAll(mainContentNode, longitudeProp, (RDFNode) null);
                m.addLiteral(mainContentNode, longitudeProp, needPojo.getLongitude());
            }

            String timeUri = "http://www.w3.org/2006/time#";
            Property timeProp = m.createProperty(timeUri
                    + "DateTimeDescription");
            Property minuteProp = m.createProperty(timeUri + "minute");
            Property hourProp = m.createProperty(timeUri + "hour");
            Property dayProp = m.createProperty(timeUri + "day");
            Property monthProp = m.createProperty(timeUri + "month");
            Property yearProp = m.createProperty(timeUri + "year");

            it = m.listSubjectsWithProperty(RDF.type, timeProp);
            if (it.hasNext()) {
                Resource mainContentNode = it.next();
                m.removeAll(mainContentNode, minuteProp, (RDFNode) null);
                m.addLiteral(mainContentNode, minuteProp, cal.get(Calendar.MINUTE));

                m.removeAll(mainContentNode, hourProp, (RDFNode) null);
                m.addLiteral(mainContentNode, hourProp, cal.get(Calendar.HOUR_OF_DAY));

                m.removeAll(mainContentNode, dayProp, (RDFNode) null);
                m.addLiteral(mainContentNode, dayProp, cal.get(Calendar.DATE));

                m.removeAll(mainContentNode, monthProp, (RDFNode) null);
                m.addLiteral(mainContentNode, monthProp, cal.get(Calendar.MONTH));

                m.removeAll(mainContentNode, yearProp, (RDFNode) null);
                m.addLiteral(mainContentNode, yearProp,
                        cal.get(Calendar.YEAR));
            }

            logger.info("Creating need, is active: " + needPojo.isActive());
            needURI = ownerService.createNeed(ownerURI, m, needPojo.isActive());

            List<Need> needs = needRepository.findByNeedURI(needURI);



            URI needUri = needs.get(0).getNeedURI();

            NeedPojo fullNeed = NeedFetcher.getNeedInfo(needs.get(0));
            logger.info("Added need id:" + fullNeed.getNeedId());


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
                if(iterNeed.getNeedURI().equals(needUri))
                    continue;

                try {
                    logger.info("Matching need: " + needUri + " to: " + iterNeed.getNeedURI());
                    client.hint(needUri, iterNeed.getNeedURI(), 0.5, new URI("http://www.agentdroid.com"));
                    client.hint( iterNeed.getNeedURI(),needUri, 0.5, new URI("http://www.agentdroid.com"));
                } catch (URISyntaxException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (IllegalMessageForNeedStateException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (NoSuchNeedException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }


            if(needs.size() == 1)
                return fullNeed;

            // return viewNeed(need.getId().toString(), model);
        } catch (IllegalNeedContentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return new NeedPojo();
    }

}
