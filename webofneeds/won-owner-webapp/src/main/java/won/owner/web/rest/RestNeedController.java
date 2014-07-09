package won.owner.web.rest;


import com.google.common.util.concurrent.ListenableFuture;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import org.apache.commons.collections.map.LRUMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import won.owner.linkeddata.NeedPojoNeedModelBuilder;
import won.owner.model.DraftState;
import won.owner.model.User;
import won.owner.pojo.DraftPojo;
import won.owner.pojo.NeedPojo;
import won.owner.repository.DraftStateRepository;
import won.owner.service.impl.DataReloadService;
import won.owner.service.impl.URIService;
import won.owner.service.impl.WONUserDetailService;
import won.protocol.exception.ConnectionAlreadyExistsException;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.model.*;
import won.protocol.owner.OwnerProtocolNeedServiceClientSide;
import won.protocol.repository.ChatMessageRepository;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.MatchRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.repository.rdfstorage.RDFStorageService;
import won.protocol.rest.LinkedDataRestClient;
import won.protocol.util.DataAccessUtils;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.util.linkeddata.LinkedDataSource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/rest/need")
public class RestNeedController {

	final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
  @Qualifier("default")
	private OwnerProtocolNeedServiceClientSide ownerService;

	@Autowired
	private NeedRepository needRepository;


  @Autowired
  private DraftStateRepository draftStateRepository;

	@Autowired
	private MatchRepository matchRepository;

  @Autowired
  private RDFStorageService rdfStorage;

	@Autowired
	private ChatMessageRepository chatMessageRepository;

	@Autowired
	private ConnectionRepository connectionRepository;

	@Autowired
	private URIService uriService;

	@Autowired
	private WONUserDetailService wonUserDetailService;

	@Autowired
	private DataReloadService dataReloadService;

  @Autowired
  private LinkedDataSource linkedDataSource;

	//TODO: this is a quick fix and the only reason for us to use commons-collections. Rework to use ehcache!
	private LRUMap cachedNeeds = new LRUMap(200, 1000);


	public void setDataReloadService(DataReloadService dataReloadService) {
		this.dataReloadService = dataReloadService;
	}

	public URIService getUriService() {
		return uriService;
	}

	public void setUriService(final URIService uriService) {
		this.uriService = uriService;
	}

	public void setOwnerService(OwnerProtocolNeedServiceClientSide ownerService) {
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
	@ResponseBody
	@RequestMapping(
			value = "/{needId}/matches",
			produces = MediaType.APPLICATION_JSON,
			method = RequestMethod.GET)
	public List<NeedPojo> findMatches(@PathVariable("needId") long needId) {

		logger.info("Looking for matches for Need: " + needId);

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
  //TODO: move transactionality annotation into the service layer
  @Transactional(propagation = Propagation.SUPPORTS)
	public NeedPojo createNeed(@RequestBody NeedPojo needPojo) {
		User user = (User) wonUserDetailService.loadUserByUsername(SecurityContextHolder.getContext().getAuthentication().getName());

		logger.info("New Need:" + needPojo.getTextDescription() + "/" + needPojo.getCreationDate() + "/" +
				needPojo.getLongitude() + "/" + needPojo.getLatitude() + "/" + (needPojo.getState() == NeedState.ACTIVE));
    //TODO: using fixed Facets - change this
    needPojo.setFacetTypes(new String[]{FacetType.OwnerFacet.getURI().toString()});
    NeedPojo createdNeedPojo = resolve(needPojo);
		List<Need> needs = needRepository.findByNeedURI(URI.create(createdNeedPojo.getNeedURI()));
		user.getNeeds().add(needs.get(0));
		wonUserDetailService.save(user);

		return createdNeedPojo;
	}


  @ResponseBody
  @RequestMapping(
    value = "/create/saveDraft",
    consumes = MediaType.APPLICATION_JSON,
    produces = MediaType.APPLICATION_JSON,
    method = RequestMethod.POST
  )
  //TODO: move transactionality annotation into the service layer
  @Transactional(propagation = Propagation.SUPPORTS)
  public DraftPojo createDraft(@RequestBody DraftPojo draftPojo) {

    User user = (User) wonUserDetailService.loadUserByUsername(SecurityContextHolder.getContext().getAuthentication().getName());

    user.getNeeds().size();

    DraftPojo createdDraftPojo = resolveDraft(draftPojo,user);
    NeedPojo createdNeedPojo = (NeedPojo) createdDraftPojo;

    List<Need> drafts = needRepository.findByNeedURI(URI.create(createdNeedPojo.getNeedURI()));

    user.getNeeds().add(drafts.get(0));
    wonUserDetailService.save(user);

    int currentStep = draftPojo.getCurrentStep();
    String userName = draftPojo.getUserName();
    DraftState draftState = new DraftState(URI.create(draftPojo.getNeedURI()),currentStep);
    draftStateRepository.save(draftState);
    return createdDraftPojo;

  }

	@ResponseBody
	@RequestMapping(
			value = "/",
			produces = MediaType.APPLICATION_JSON,
			method = RequestMethod.GET
	)
	public List<NeedPojo> getAllNeedsOfUser() {
		logger.info("Getting all needs of user: ");

		User user = (User) wonUserDetailService.loadUserByUsername(SecurityContextHolder.getContext().getAuthentication().getName());

		LinkedDataRestClient linkedDataRestClient = new LinkedDataRestClient();
		List<NeedPojo> returnList = new ArrayList<NeedPojo>();

		Iterable<Need> needs = user.getNeeds();
		for (Need need : needs) {
			NeedPojo needPojo = new NeedPojo(need.getNeedURI(), linkedDataRestClient.readResourceData(need.getNeedURI()));
			needPojo.setNeedId(need.getId());
			returnList.add(needPojo);
		}
		return returnList;
	}


	@ResponseBody
	@RequestMapping(
			value = "/need/{needId}",
			produces = MediaType.APPLICATION_JSON,
			method = RequestMethod.GET
	)
	public NeedPojo getNeed(@PathVariable("needId") long needId) {
		logger.info("Getting need: " + needId);

		LinkedDataRestClient linkedDataRestClient = new LinkedDataRestClient();
		List<NeedPojo> returnList = new ArrayList<NeedPojo>();

		Iterable<Need> needs = needRepository.findById(needId);
		Need need = needs.iterator().next();

		NeedPojo needPojo = new NeedPojo(need.getNeedURI(), linkedDataRestClient.readResourceData(need.getNeedURI()));
		needPojo.setNeedId(need.getId());

		return needPojo;
	}
  private DraftPojo resolveDraft(DraftPojo draftPojo, User user){
    URI needURI;
    //Draft needDraft2 = new Draft();

    Need needDraft = new Need();

    URI ownerURI = this.uriService.getOwnerProtocolOwnerServiceEndpointURI();
    int draftNumber = user.getNeeds().size()+1;
    URI draftId = URI.create(ownerURI.toString()+"/"+user.getUsername()+"/"+ draftNumber);

    NeedPojoNeedModelBuilder needPojoNeedModelBuilder = new NeedPojoNeedModelBuilder(draftPojo);
    needPojoNeedModelBuilder.setUri(draftId);
    Model needModel = needPojoNeedModelBuilder.build();

    needDraft.setNeedURI(draftId);

    needDraft.setState(NeedState.INACTIVE);
    needDraft.setOwnerURI(ownerURI);
    try{
      needRepository.save(needDraft);
      rdfStorage.storeContent(needDraft.getNeedURI(),needModel);
    } catch(Exception e){
      e.printStackTrace();
      int size = needRepository.findByNeedURI(needDraft.getNeedURI()).size();
      logger.debug(String.valueOf(size));
    }

    draftPojo.setNeedURI(draftId.toString());

    return draftPojo;
  }

	private NeedPojo resolve(NeedPojo needPojo) {
		if (needPojo.getNeedId() >= 0) {

			List<Need> needs = needRepository.findById(needPojo.getNeedId());
			if (!needs.isEmpty()) {
				logger.warn("Deactivating old need");
				try {
					ownerService.deactivate(needs.get(0).getNeedURI());
				} catch (Exception e) {
					logger.warn("Could not deactivate old Need: " + needs.get(0).getNeedURI());
				}
			}
		}
		URI needURI;

		try {
      URI ownerURI = this.uriService.getOwnerProtocolOwnerServiceEndpointURI();



      NeedPojoNeedModelBuilder needPojoNeedModelBuilder = new NeedPojoNeedModelBuilder(needPojo);
      needPojoNeedModelBuilder.setUri("no:uri");
      Model needModel = needPojoNeedModelBuilder.build();
      needModel.setNsPrefix("","no:uri");


      //needModel = configureNeedModel("",needModel,needPojo);

      if (needPojo.getWonNode().equals("")) {
        ListenableFuture<URI> futureResult = ownerService.createNeed(ownerURI, needModel, needPojo.getState() == NeedState.ACTIVE);
        needURI = futureResult.get();
      } else {
        ListenableFuture<URI> futureResult = ownerService.createNeed(ownerURI, needModel, needPojo.getState() == NeedState.ACTIVE, URI.create(needPojo.getWonNode()));
        needURI = futureResult.get();
      }

      Need need = DataAccessUtils.loadNeed(needRepository, needURI);
      if (need != null) {
        NeedPojo fullNeed = new NeedPojo(needURI, linkedDataSource.getModelForResource(need.getNeedURI()));
        fullNeed.setNeedId(need.getId());
        logger.info("Added need id:" + fullNeed.getNeedId() + "uri: " + needURI);
        return fullNeed;
      }
		} catch (Exception e) {
			logger.warn("Caught exception", e);
		}
		return new NeedPojo();
	}

	private void attachRdfToModelViaBlanknode(final String rdfAsString, final String rdfLanguage, final Resource resourceToLinkTo,
	                                          final Property propertyToLinkThrough, final com.hp.hpl.jena.rdf.model.Model modelToModify) {
		com.hp.hpl.jena.rdf.model.Model model = RdfUtils.readRdfSnippet(rdfAsString, rdfLanguage);
		RdfUtils.attachModelByBaseResource(resourceToLinkTo, propertyToLinkThrough, model);
	}

	// Matching and connecting

	@RequestMapping(
			value = "/match/{matchId}/connect",
			method = RequestMethod.GET
	)
  //TODO: move transactionality annotation into the service layer
  @Transactional(propagation = Propagation.SUPPORTS)
	public String connect(@PathVariable String matchId) {
		String ret = "noNeedFound";

		try {
			List<Match> matches = matchRepository.findById(Long.valueOf(matchId));
			if (!matches.isEmpty()) {
				Match match = matches.get(0);
				List<Need> needs = needRepository.findByNeedURI(match.getFromNeed());
				if (!needs.isEmpty())
					ret = "";
        //TODO: this connects only ownerFacets!!!
				ownerService.connect(match.getFromNeed(), match.getToNeed(),
          WonRdfUtils.FacetUtils.createFacetModelForHintOrConnect(FacetType.OwnerFacet.getURI(),
            FacetType.OwnerFacet.getURI()));
			}
		} catch (ConnectionAlreadyExistsException e) {
			logger.warn("caught ConnectionAlreadyExistsException:", e);
		} catch (IllegalMessageForNeedStateException e) {
			logger.warn("caught IllegalMessageForNeedStateException:", e);
		} catch (NoSuchNeedException e) {
			logger.warn("caught NoSuchNeedException:", e);
		} catch (Exception e) {
      logger.warn("caught Exception", e);
    }

		return ret;
	}

	@ResponseBody
	@RequestMapping(
			value = "/{needId}/listMatches",
			method = RequestMethod.GET,
			produces = MediaType.APPLICATION_JSON
	)
	public List<Match> listMatches(@PathVariable String needId) {
		List<Need> needs = needRepository.findById(Long.valueOf(needId));
		if (needs.isEmpty())
			return new ArrayList<>();

		Need need = needs.get(0);

		return matchRepository.findByFromNeed(need.getNeedURI());
	}

	@ResponseBody
	@RequestMapping(
			value = "/{needId}/listConnections",
			method = RequestMethod.GET,
			produces = MediaType.APPLICATION_JSON
	)
	public List<Connection> listConnections(@PathVariable String needId) {

		List<Need> needs = needRepository.findById(Long.valueOf(needId));
		if (needs.isEmpty())
			return new ArrayList<>();

		Need need = needs.get(0);

		return connectionRepository.findByNeedURI(need.getNeedURI());
	}

}
