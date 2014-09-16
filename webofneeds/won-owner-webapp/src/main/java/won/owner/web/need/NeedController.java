package won.owner.web.need;

import com.google.common.util.concurrent.ListenableFuture;
import com.hp.hpl.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import won.owner.linkeddata.NeedPojoNeedModelBuilder;
import won.owner.pojo.NeedPojo;
import won.owner.service.impl.DataReloadService;
import won.owner.service.impl.URIService;
import won.owner.web.WonOwnerWebappUtils;
import won.protocol.exception.*;
import won.protocol.model.*;
import won.protocol.owner.OwnerProtocolNeedServiceClientSide;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.FacetRepository;
import won.protocol.repository.MatchRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.util.ProjectingIterator;
import won.protocol.util.WonRdfUtils;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;
import won.protocol.ws.fault.IllegalMessageForConnectionStateFault;
import won.protocol.ws.fault.NoSuchConnectionFault;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

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
  @Qualifier("default")
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

  @Autowired
  private LinkedDataSource linkedDataSource;

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

  @RequestMapping(value = "/import", method = RequestMethod.POST)
  public String importNeedPost(@RequestParam("needURI") URI needURI)
  {
    Need importedNeed = dataReloadService.importNeed(needURI);
    return "redirect:/need/" + importedNeed.getId();
  }

  @RequestMapping(value = "/create", method = RequestMethod.GET)
  public String createNeedGet(Model model)
  {
    model.addAttribute("command", new NeedPojo());
    return "createNeed";
  }
  public void configureNeedModel(NeedPojo needPojo){

  }
  //TODO use NeedModelBuilder here instead
  @RequestMapping(value = "/create", method = RequestMethod.POST)
  public String createNeedPost(@ModelAttribute("SpringWeb") NeedPojo needPojo, Model model) throws Exception {
    URI needURI;

    try {
      URI ownerURI = this.uriService.getOwnerProtocolOwnerServiceEndpointURI();



      NeedPojoNeedModelBuilder needPojoNeedModelBuilder = new NeedPojoNeedModelBuilder(needPojo);
      needPojoNeedModelBuilder.setUri("no:uri");
      com.hp.hpl.jena.rdf.model.Model needModel = needPojoNeedModelBuilder.build();
      needModel.setNsPrefix("","no:uri");

      if (needPojo.getWonNode().equals("")) {
          ListenableFuture<URI> futureResult = ownerService.createNeed(
                  needModel,
                  needPojo.getState() == NeedState.ACTIVE,
                  null);
          needURI = futureResult.get();
      } else {
          ListenableFuture<URI> futureResult = ownerService.createNeed(
                  needModel,
                  needPojo.getState() == NeedState.ACTIVE,
                  URI.create(needPojo.getWonNode()),
                  null);
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

    NeedPojo needCommandPojo = new NeedPojo(facets);
          model.addAttribute("command", needCommandPojo);

    NeedPojo pojo = new NeedPojo(need.getNeedURI(), linkedDataSource.getDataForResource(need.getNeedURI()).getDefaultModel());
    pojo.setState(need.getState());
    model.addAttribute("pojo", pojo);

    //set facets on 'command': (needed for the dropdown list in the 'connect' control TODO: deuglify
    needCommandPojo.setNeedFacetURIs(pojo.getNeedFacetURIs());


    return "viewNeed";
  }


  @RequestMapping(value = "/{needId}/listMatches", method = RequestMethod.GET)
  public String listMatches(@PathVariable String needId, Model model)
  {
    List<Need> needs = needRepository.findById(Long.valueOf(needId));
    if (needs.isEmpty())
      return "noNeedFound";

    Need need = needs.get(0);
    List<Match> matches = matchRepository.findByFromNeed(need.getNeedURI());
    model.addAttribute("matches", matches);

    //create an URI iterator from the matches and fetch the linked data descriptions for the needs.
    final Iterator<Match> matchIterator = matches.iterator();
    Iterator<Dataset> modelIterator = WonLinkedDataUtils.getModelForURIs(
      new ProjectingIterator<Match, URI>(matchIterator)
      {
        @Override
        public URI next() {
          return this.baseIterator.next().getToNeed();
        }
      }, this.linkedDataSource);
    Iterator<NeedPojo> needPojoIterator = WonOwnerWebappUtils.toNeedPojos(modelIterator);

    //create a list of models and add all the descriptions:
    List<NeedPojo> remoteNeeds = new ArrayList<NeedPojo>(matches.size());
    while(modelIterator.hasNext()){
      remoteNeeds.add(needPojoIterator.next());
    }
    model.addAttribute("remoteNeeds", remoteNeeds);
    return "listMatches";
  }

  @RequestMapping(value = "/{needId}/listConnections", method = RequestMethod.GET)
  public String listConnections(@PathVariable String needId, Model model)
  {

    List<Need> needs = needRepository.findById(Long.valueOf(needId));
    if (needs.isEmpty())
      return "noNeedFound";
    Need need = needs.get(0);
    List<Connection> connections = connectionRepository.findByNeedURI(need.getNeedURI());
    model.addAttribute("connections", connections);

    //create an URI iterator from the matches and fetch the linked data descriptions for the needs.
    final Iterator<Connection> connectionIterator = connections.iterator();
    Iterator<Dataset> datasetIterator = WonLinkedDataUtils.getModelForURIs(new ProjectingIterator<Connection,
      URI>(connectionIterator)
    {
      @Override
      public URI next() {
        return connectionIterator.next().getRemoteNeedURI();
      }
    }, this.linkedDataSource);
    Iterator<NeedPojo> needPojoIterator = WonOwnerWebappUtils.toNeedPojos(datasetIterator);

    //create a list of models and add all the descriptions:
    List<NeedPojo> remoteNeeds = new ArrayList<NeedPojo>(connections.size());
    while(connectionIterator.hasNext()){
      remoteNeeds.add(needPojoIterator.next());
    }
    model.addAttribute("remoteNeeds", remoteNeeds);
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

      com.hp.hpl.jena.rdf.model.Model facetModel =
        WonRdfUtils.FacetUtils.createFacetModelForHintOrConnect(
          URI.create(needPojo.getOwnFacetURI()),
          URI.create(needPojo.getRemoteFacetURI()));
      ownerService.connect(need1.getNeedURI(), new URI(needPojo.getNeedURI()), facetModel, null);
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
    } catch (CamelConfigurationFailedException e) {
        logger.warn("caught CameConfigurationException", e);
        logger.warn("caught CamelConfigurationFailedException",e);
    } catch (Exception e) {
        logger.warn("caught Exception",e);

    }

      return "noNeedFound";
  }

  @RequestMapping(value = "/{needId}/toggle", method = RequestMethod.POST)
  public String toggleNeed(@PathVariable String needId, Model model) throws NoSuchConnectionFault, IllegalMessageForConnectionStateFault {
    List<Need> needs = needRepository.findById(Long.valueOf(needId));
    if (needs.isEmpty())
      return "noNeedFound";
    Need need = needs.get(0);
    try {
      if (need.getState() == NeedState.ACTIVE) {
        ownerService.deactivate(need.getNeedURI(), null);
      } else {
        ownerService.activate(need.getNeedURI(), null);
      }
    } catch (NoSuchNeedException e) {
      logger.warn("caught NoSuchNeedException:", e);
    } catch (Exception e) {
        logger.warn("caught Exception",e);
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
        if (!needs.isEmpty()){
          ret = "redirect:/need/" + needs.get(0).getId().toString();//viewNeed(needs.get(0).getId().toString(), model);
        }
        //TODO: match object does not contain facet info, assume OwnerFacet.
        com.hp.hpl.jena.rdf.model.Model facetModel =
          WonRdfUtils.FacetUtils.createFacetModelForHintOrConnect(
            FacetType.OwnerFacet.getURI(),
            FacetType.OwnerFacet.getURI());
        ownerService.connect(match.getFromNeed(), match.getToNeed(), facetModel, null);
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
    } catch (CamelConfigurationFailedException e) {
        logger.warn("caught CamelConfigurationException", e); //To change body of catch statement use File | Settings | File Templates.
      logger.warn("caught CamelConfigurationFailedException");
    } catch (Exception e) {
        logger.warn("caught Exception",e);
    }

      return ret;
  }
}
