package won.owner.web.need;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import won.owner.pojo.NeedPojo;
import won.owner.service.impl.DataReloadService;
import won.owner.service.impl.URIService;
import won.protocol.exception.ConnectionAlreadyExistsException;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.IllegalNeedContentException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.model.Facet;
import won.protocol.model.Match;
import won.protocol.model.Need;
import won.protocol.model.NeedState;
import won.protocol.owner.OwnerProtocolNeedServiceClientSide;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.FacetRepository;
import won.protocol.repository.MatchRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.rest.LinkedDataRestClient;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.GEO;
import won.protocol.vocabulary.WON;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created with IntelliJ IDEA.
 * User: Gabriel
 * Date: 17.12.12
 * Time: 13:38
 */

@Controller
@RequestMapping("/need")
public class NeedController
{
  final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  @Qualifier("ownerProtocolNeedServiceClient")
  private OwnerProtocolNeedServiceClientSide ownerService;

  @Autowired
  private NeedRepository needRepository;

  @Autowired
  private FacetRepository facetRepository;

  @Autowired
  private MatchRepository matchRepository;

  @Autowired
  private ConnectionRepository connectionRepository;

  @Autowired
  private URIService uriService;

  @Autowired
  private DataReloadService dataReloadService;

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

  @RequestMapping(value = "/create", method = RequestMethod.GET)
  public String createNeedGet(Model model)
  {
    model.addAttribute("command", new NeedPojo());
    return "createNeed";
  }

  //TODO use NeedModelBuilder here instead
  @RequestMapping(value = "/create", method = RequestMethod.POST)
  public String createNeedPost(@ModelAttribute("SpringWeb") NeedPojo needPojo, Model model) throws ExecutionException, InterruptedException, IOException, URISyntaxException {
    URI needURI;

    try {
      URI ownerURI = this.uriService.getOwnerProtocolOwnerServiceEndpointURI();

      com.hp.hpl.jena.rdf.model.Model needModel = ModelFactory.createDefaultModel();

      Resource needResource = needModel.createResource(ownerURI.toString(), WON.NEED);

      // need type
      needModel.add(needModel.createStatement(needResource, WON.HAS_BASIC_NEED_TYPE, WON.toResource(needPojo.getBasicNeedType())));

      // need content
      Resource needContent = needModel.createResource(WON.NEED_CONTENT);
      if (!needPojo.getTitle().isEmpty())
        needContent.addProperty(DC.title, needPojo.getTitle(), XSDDatatype.XSDstring);
      if (!needPojo.getTextDescription().isEmpty())
        needContent.addProperty(WON.HAS_TEXT_DESCRIPTION, needPojo.getTextDescription(), XSDDatatype.XSDstring);
      if (!needPojo.getContentDescription().isEmpty())
        attachRdfToModelViaBlanknode(needPojo.getContentDescription(), "TTL", needContent, WON.HAS_CONTENT_DESCRIPTION, needModel);
      if (!needPojo.getTags().isEmpty()) {
        String[] tags = needPojo.getTags().split(",");
        for (String tag : tags) {
          needModel.add(needModel.createStatement(needContent, WON.HAS_TAG, tag.trim()));
        }
      }

      needModel.add(needModel.createStatement(needResource, WON.HAS_CONTENT, needContent));

      for(String ft : needPojo.getFacetTypes()) {
          needModel.add(needModel.createStatement(needResource, WON.HAS_FACET, needModel.createResource(ft)));
      }


      // need modalities
      Resource needModality = needModel.createResource(WON.NEED_MODALITY);

      //price and currency
      if (needPojo.getUpperPriceLimit() != null || needPojo.getLowerPriceLimit() != null) {
        Resource priceSpecification = needModel.createResource(WON.PRICE_SPECIFICATION);
        if (needPojo.getLowerPriceLimit() != null)
          priceSpecification.addProperty(WON.HAS_LOWER_PRICE_LIMIT, Double.toString(needPojo.getLowerPriceLimit()), XSDDatatype.XSDfloat);
        if (needPojo.getUpperPriceLimit() != null)
          priceSpecification.addProperty(WON.HAS_UPPER_PRICE_LIMIT, Double.toString(needPojo.getUpperPriceLimit()), XSDDatatype.XSDfloat);
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
          Future<URI> futureResult = ownerService.createNeed(ownerURI, needModel, needPojo.getState() == NeedState.ACTIVE,URI.create(needPojo.getWonNode()));
          needURI = futureResult.get();
      }

      List<Need> needs = needRepository.findByNeedURI(needURI);
      //TODO: race condition between need saving logic and redirect. adapt interface.
      if (needs.size() == 1)
        return "redirect:/need/" + needs.get(0).getId().toString();
      // return viewNeed(need.getId().toString(), model);
    } catch (IllegalNeedContentException e) {
      logger.warn("caught IllegalNeedContentException:", e);
    }

    model.addAttribute("command", new NeedPojo());

    return "createNeed";
  }

  private void attachRdfToModelViaBlanknode(final String rdfAsString, final String rdfLanguage, final Resource resourceToLinkTo, final Property propertyToLinkThrough, final com.hp.hpl.jena.rdf.model.Model modelToModify)
  {
    com.hp.hpl.jena.rdf.model.Model model = RdfUtils.readRdfSnippet(rdfAsString, rdfLanguage);
    RdfUtils.attachModelByBaseResource(resourceToLinkTo,propertyToLinkThrough, model);
  }

  @RequestMapping(value = "/", method = RequestMethod.GET)
  public String listNeeds(Model model)
  {

    model.addAttribute("needs", needRepository.findAll());

    return "listNeeds";
  }

  @RequestMapping(value = "reload", method = RequestMethod.GET)
  public String reload(Model model)
  {

    dataReloadService.reload();
    return "redirect:/need/";
  }

  @RequestMapping(value = "/{needId}", method = RequestMethod.GET)
  public String viewNeed(@PathVariable String needId, Model model)
  {

    model.addAttribute("needId", needId);

    List<Need> needs = needRepository.findById(Long.valueOf(needId));
    if (needs.isEmpty())
      return "noNeedFound";

    Need need = needs.get(0);
    model.addAttribute("active", need.getState() != NeedState.ACTIVE ? "activate" : "deactivate");
    model.addAttribute("needURI", need.getNeedURI());
    List<Facet> facets = facetRepository.findByNeedURI(need.getNeedURI());
    model.addAttribute("command", new NeedPojo(facets));

    LinkedDataRestClient linkedDataRestClient = new LinkedDataRestClient();
    NeedPojo pojo = new NeedPojo(need.getNeedURI(), linkedDataRestClient.readResourceData(need.getNeedURI()));
    pojo.setState(need.getState());

    model.addAttribute("pojo", pojo);

    return "viewNeed";
  }


  @RequestMapping(value = "/{needId}/listMatches", method = RequestMethod.GET)
  public String listMatches(@PathVariable String needId, Model model)
  {
    List<Need> needs = needRepository.findById(Long.valueOf(needId));
    if (needs.isEmpty())
      return "noNeedFound";

    Need need = needs.get(0);
    model.addAttribute("matches", matchRepository.findByFromNeed(need.getNeedURI()));

    return "listMatches";
  }

  @RequestMapping(value = "/{needId}/listConnections", method = RequestMethod.GET)
  public String listConnections(@PathVariable String needId, Model model)
  {

    List<Need> needs = needRepository.findById(Long.valueOf(needId));
    if (needs.isEmpty())
      return "noNeedFound";

    Need need = needs.get(0);

    model.addAttribute("connections", connectionRepository.findByNeedURI(need.getNeedURI()));

    return "listConnections";
  }

  @RequestMapping(value = "/{needId}/connect", method = RequestMethod.POST)
  public String connect2Need(@PathVariable String needId, @ModelAttribute("SpringWeb") NeedPojo needPojo, Model model)
  {
    try {
      List<Need> needs = needRepository.findById(Long.valueOf(needId));
      if (needs.isEmpty())
        return "noNeedFound";

      Need need1 = needs.get(0);

      com.hp.hpl.jena.rdf.model.Model facetModel = ModelFactory.createDefaultModel();

      facetModel.setNsPrefix("", "no:uri");
      Resource baseRes = facetModel.createResource(facetModel.getNsPrefixURI(""));
      baseRes.addProperty(WON.HAS_FACET, facetModel.createResource(needPojo.getOwnFacetURI()));
      baseRes.addProperty(WON.HAS_REMOTE_FACET, facetModel.createResource(needPojo.getRemoteFacetURI()));

      ownerService.connect(need1.getNeedURI(), new URI(needPojo.getNeedURI()), facetModel);
      return "redirect:/need/" + need1.getId().toString();//viewNeed(need1.getId().toString(), model);
    } catch (URISyntaxException e) {
      logger.warn("caught URISyntaxException:", e);
    } catch (ConnectionAlreadyExistsException e) {
      logger.warn("caught ConnectionAlreadyExistsException:", e);
    } catch (IllegalMessageForNeedStateException e) {
      logger.warn("caught IllegalMessageForNeedStateException:", e);
    } catch (NoSuchNeedException e) {
      logger.warn("caught NoSuchNeedException:", e);
    }  catch (InterruptedException e) {
       logger.warn("caught InterruptedException", e);
    } catch (ExecutionException e) {
        logger.warn("caught ExcutionException", e);
    }

      return "noNeedFound";
  }

  @RequestMapping(value = "/{needId}/toggle", method = RequestMethod.POST)
  public String toggleNeed(@PathVariable String needId, Model model)
  {
    List<Need> needs = needRepository.findById(Long.valueOf(needId));
    if (needs.isEmpty())
      return "noNeedFound";
    Need need = needs.get(0);
    try {
      if (need.getState() == NeedState.ACTIVE) {
        ownerService.deactivate(need.getNeedURI());
      } else {
        ownerService.activate(need.getNeedURI());
      }
    } catch (NoSuchNeedException e) {
      logger.warn("caught NoSuchNeedException:", e);
    }
    return "redirect:/need/" + need.getId().toString();
    //return viewNeed(need.getId().toString(), model);
  }

  @RequestMapping(value = "/match/{matchId}/connect", method = RequestMethod.POST)
  public String connect(@PathVariable String matchId, Model model)
  {
    String ret = "noNeedFound";

    try {
      List<Match> matches = matchRepository.findById(Long.valueOf(matchId));
      if (!matches.isEmpty()) {
        Match match = matches.get(0);
        List<Need> needs = needRepository.findByNeedURI(match.getFromNeed());
        if (!needs.isEmpty())
          ret = "redirect:/need/" + needs.get(0).getId().toString();//viewNeed(needs.get(0).getId().toString(), model);
        ownerService.connect(match.getFromNeed(), match.getToNeed(), null);
      }
    } catch (ConnectionAlreadyExistsException e) {
      logger.warn("caught ConnectionAlreadyExistsException:", e);
    } catch (IllegalMessageForNeedStateException e) {
      logger.warn("caught IllegalMessageForNeedStateException:", e);
    } catch (NoSuchNeedException e) {
      logger.warn("caught NoSuchNeedException:", e);
    } catch (InterruptedException e) {
      logger.warn("caught InterruptedEception",e);
    } catch (ExecutionException e) {
      logger.warn("caught ExecutionException",e);
    }

      return ret;
  }
}
