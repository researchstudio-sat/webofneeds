package won.owner.web.need;

import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import won.owner.pojo.NeedPojo;
import won.owner.protocol.impl.OwnerProtocolNeedServiceClient;
import won.owner.service.impl.DataReloadService;
import won.owner.service.impl.URIService;
import won.protocol.exception.ConnectionAlreadyExistsException;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.IllegalNeedContentException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.model.*;
import won.protocol.owner.OwnerProtocolNeedService;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.MatchRepository;
import won.protocol.repository.NeedRepository;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Gabriel
 * Date: 17.12.12
 * Time: 13:38
 */

@Controller
public class NeedController
{
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

  public void setOwnerService(OwnerProtocolNeedService ownerService)
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

  @RequestMapping(value = "/create", method = RequestMethod.POST)
  public String createNeedPost(@ModelAttribute("SpringWeb") NeedPojo needPojo, Model model)
  {
    URI needURI;

    try {
      URI ownerURI = this.uriService.getOwnerProtocolOwnerServiceEndpointURI();

      com.hp.hpl.jena.rdf.model.Model needModel = ModelFactory.createDefaultModel();

      Resource needResource = needModel.createResource(WON.NEED);

      // need type
      needModel.add(needModel.createStatement(needResource, WON.HAS_BASIC_NEED_TYPE, WON.toResource(needPojo.getBasicNeedType())));

      // need content
      Resource needContent = needModel.createResource(WON.NEED_CONTENT)
              .addProperty(WON.TITLE, needPojo.getTitle())
              .addProperty(WON.TEXT_DESCRIPTION, needPojo.getTextDescription());
      needModel.add(needModel.createStatement(needResource, WON.HAS_CONTENT, needContent));

      // owner
      if(needPojo.isAnonymize()) {
        needModel.add(needModel.createStatement(needResource, WON.HAS_OWNER, WON.ANONYMIZED_OWNER));
      }

      // need modalities
      Resource needModality = needModel.createResource(WON.NEED_MODALITY);

      // TODO: store need modalities in separate objects to enable easier checking and multiple instances
      //price and currency
      if(needPojo.getUpperPriceLimit() != null || needPojo.getLowerPriceLimit() != null || !needPojo.getCurrency().isEmpty()) {
        Resource priceSpecification = needModel.createResource(WON.PRICE_SPECIFICATION);
        if(needPojo.getLowerPriceLimit() != null)
          priceSpecification.addProperty(WON.HAS_LOWER_PRICE_LIMIT, Double.toString(needPojo.getLowerPriceLimit()));
        if(needPojo.getUpperPriceLimit() != null)
          priceSpecification.addProperty(WON.HAS_UPPER_PRICE_LIMIT, Double.toString(needPojo.getUpperPriceLimit()));
        if(!needPojo.getCurrency().isEmpty())
          priceSpecification.addProperty(WON.HAS_CURRENCY, needPojo.getCurrency());

        needModel.add(needModel.createStatement(needModality, WON.HAS_PRICE_SPECIFICATION, priceSpecification));
      }

      if(needPojo.getLatitude() != null && needPojo.getLongitude() != null) {
        Resource location = needModel.createResource(GEO.POINT)
            .addProperty(GEO.LATITUDE, Double.toString(needPojo.getLatitude()))
            .addProperty(GEO.LONGITUDE, Double.toString(needPojo.getLongitude()));

        needModel.add(needModel.createStatement(needModality, WON.AVAILABLE_AT_LOCATION, location));
      }

      // time constraint
      if(needPojo.getStartTime() != null && needPojo.getEndTime() != null) {
        Resource timeConstraint = needModel.createResource(WON.TIME)
                .addProperty(WON.START_TIME, needPojo.getStartTime())
                .addProperty(WON.END_TIME, needPojo.getEndTime());
          if(needPojo.getRecurIn() != null)
              timeConstraint.addProperty(WON.RECUR_IN, Long.toString(needPojo.getRecurIn()));
          if(needPojo.getRecurTimes() != null)
              timeConstraint.addProperty(WON.RECUR_TIMES, Integer.toString(needPojo.getRecurTimes()));
          //if(needPojo.getRecurInfiniteTimes() != null)
          //    timeConstraint.addProperty(WON.RECUR_INFINITE_TIMES, needPojo.getRecurInfiniteTimes());
          needModel.add(needModel.createStatement(needModality, WON.AVAILABLE_AT_TIME, timeConstraint));
      }

      needModel.add(needModel.createStatement(needResource, WON.HAS_NEED_MODALITY, needModality));

      if (needPojo.getWonNode().equals("")) {
        needURI = ownerService.createNeed(ownerURI, needModel, needPojo.isActive());
      } else {
        needURI = ((OwnerProtocolNeedServiceClient) ownerService).createNeed(ownerURI, needModel, needPojo.isActive(), needPojo.getWonNode());
      }

      List<Need> needs = needRepository.findByNeedURI(needURI);


      if (needs.size() == 1)
        return "redirect:/need/" + needs.get(0).getId().toString();
      // return viewNeed(need.getId().toString(), model);
    } catch (IllegalNeedContentException e) {
      e.printStackTrace();
    }

    model.addAttribute("command", new NeedPojo());

    return "createNeed";
  }

    @RequestMapping(value = "", method = RequestMethod.GET)
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
    model.addAttribute("active", (need.getState() != NeedState.ACTIVE ? "activate" : "deactivate"));
    model.addAttribute("needURI", need.getNeedURI());
    model.addAttribute("command", new NeedPojo());

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
      ownerService.connectTo(need1.getNeedURI(), new URI(needPojo.getNeedURI()), "");
      return "redirect:/need/" + need1.getId().toString();//viewNeed(need1.getId().toString(), model);
    } catch (URISyntaxException e) {
      e.printStackTrace();
    } catch (ConnectionAlreadyExistsException e) {
      e.printStackTrace();
    } catch (IllegalMessageForNeedStateException e) {
      e.printStackTrace();
    } catch (NoSuchNeedException e) {
      e.printStackTrace();
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
      e.printStackTrace();
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
        ownerService.connectTo(match.getFromNeed(), match.getToNeed(), "");
      }
    } catch (ConnectionAlreadyExistsException e) {
      e.printStackTrace();
    } catch (IllegalMessageForNeedStateException e) {
      e.printStackTrace();
    } catch (NoSuchNeedException e) {
      e.printStackTrace();
    }

    return ret;
  }
}
