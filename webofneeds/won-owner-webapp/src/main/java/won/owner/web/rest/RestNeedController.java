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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import won.owner.linkeddata.NeedPojoNeedModelBuilder;
import won.owner.model.Draft;
import won.owner.model.User;
import won.owner.model.UserNeed;
import won.owner.pojo.ConnectionPojo;
import won.owner.pojo.CreateDraftPojo;
import won.owner.pojo.DraftPojo;
import won.owner.pojo.NeedPojo;
import won.owner.repository.DraftRepository;
import won.owner.service.impl.DataReloadService;
import won.owner.service.impl.URIService;
import won.owner.service.impl.WONUserDetailService;
import won.protocol.exception.ConnectionAlreadyExistsException;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.model.Connection;
import won.protocol.model.FacetType;
import won.protocol.model.Need;
import won.protocol.model.NeedState;
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

import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/rest/needs")
public class RestNeedController {

	final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
  @Qualifier("default")
	private OwnerProtocolNeedServiceClientSide ownerService;

	@Autowired
	private NeedRepository needRepository;


  @Autowired
  private DraftRepository draftRepository;

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

  /**
   * returns a List containing needs belonging to the user
   * @return JSON List of need objects
   */
  @ResponseBody
  @RequestMapping(
    value = "/",
    produces = MediaType.APPLICATION_JSON,
    method = RequestMethod.GET
  )
  public List<URI> getAllNeedsOfUser() {
    logger.info("Getting all needs of user: ");

    User user = getCurrentUser();
    List<UserNeed> userNeeds = user.getUserNeeds();
    List<URI> needUris = new ArrayList(userNeeds.size());
    for(UserNeed userNeed: userNeeds){
      needUris.add(userNeed.getUri());
    }
    return needUris;
  }

  /**
   * Gets the current user. If no user is authenticated, an Exception is thrown
   * @return
   */
  public User getCurrentUser() {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    if (username == null) throw new AccessDeniedException("client is not authenticated");
    return (User) wonUserDetailService.loadUserByUsername(username);
  }

  @ResponseBody
  @RequestMapping(
    value = "/drafts",
    produces = MediaType.APPLICATION_JSON,
    method = RequestMethod.GET
  )
  //TODO: move transactionality annotation into the service layer
  @Transactional(propagation = Propagation.SUPPORTS)
  public List<CreateDraftPojo> getAllDrafts() {
    User user = getCurrentUser();

    List<CreateDraftPojo> createDraftPojos = new ArrayList<>();
    Set<URI> draftURIs = user.getDraftURIs();
   Iterator<URI> draftURIIterator = draftURIs.iterator();
    while(draftURIIterator.hasNext()){
      URI draftURI = draftURIIterator.next();
      Draft draft = draftRepository.findByDraftURI(draftURI).get(0);
      CreateDraftPojo createDraftPojo = new CreateDraftPojo(draftURI.toString(), draft.getContent());
      createDraftPojos.add(createDraftPojo);
    }
    return createDraftPojos ;

  }
  /**
   * saves draft of a draft
   * @param createDraftObject an object containing information of the need draft
   * @return a JSON object of the draft with its temprory id.
   */
  @ResponseBody
  @RequestMapping(
    value = "/drafts",
    consumes = MediaType.APPLICATION_JSON,
    produces = MediaType.APPLICATION_JSON,
    method = RequestMethod.POST
  )
  //TODO: move transactionality annotation into the service layer
  @Transactional(propagation = Propagation.SUPPORTS)
  public CreateDraftPojo createDraft(@RequestBody CreateDraftPojo createDraftObject) throws ParseException {

    User user = getCurrentUser();
    URI draftURI = URI.create(createDraftObject.getDraftURI());
    user.getDraftURIs().add(draftURI);
    wonUserDetailService.save(user);
    Draft draft = null;
    draft = draftRepository.findOneByDraftURI(draftURI);
    if(draft==null){
      draft = new Draft(draftURI, createDraftObject.getDraft());
    }
    draft.setContent(createDraftObject.getDraft());

    draftRepository.save(draft);

    return createDraftObject;
  }

  @ResponseBody
  @RequestMapping(
    value = "/drafts",
    method = RequestMethod.DELETE
  )
  //TODO: move transactionality annotation into the service layer
  @Transactional(propagation = Propagation.SUPPORTS)
  public ResponseEntity deleteDrafts() {
    try{
     /* User user = getCurrentUser();
      List<Draft> draftStates = draftRepository.findByUserName(user.getUsername());
      Iterator<Draft> draftIterator =  draftStates.iterator();
      List<URI> draftURIs = new ArrayList<>();
      while(draftIterator.hasNext()){
        draftURIs.add(draftIterator.next().getDraftURI());
      }         */
    }catch (Exception e){
      return new ResponseEntity(HttpStatus.CONFLICT);
    }
    return new ResponseEntity(HttpStatus.OK);
  }

  @ResponseBody
  @RequestMapping(
    value ="/drafts/{draftId}",
    produces = MediaType.APPLICATION_JSON,
    method = RequestMethod.GET
  )
  public DraftPojo getDraft(@PathVariable("draftId") long draftId){
    logger.debug("getting draft: "+draftId);

    List<Need> draftList = needRepository.findById(draftId);
    Need need = draftList.get(0);

    DraftPojo draftPojo = new DraftPojo(need.getNeedURI(),rdfStorage.loadModel(need.getNeedURI()),
                                        draftRepository.findByDraftURI(need.getNeedURI()).get(0));
    draftPojo.setNeedURI(need.getNeedURI().toString());
    return draftPojo;
  }

  @ResponseBody
  @RequestMapping(
    value ="/drafts/{draftId}",
    method = RequestMethod.DELETE
  )
  public ResponseEntity deleteDraft(@PathVariable long draftId){
    logger.debug("deleting draft: "+draftId);

    List<Need> draftList = needRepository.findById(draftId);
    if (draftList.size()==0){
      return new ResponseEntity(HttpStatus.CONFLICT);
    }
    Need need = draftList.get(0);
    User user = getCurrentUser();

    try{
      /*
      user.removeNeeds(draftList);
      wonUserDetailService.save(user);
      List<Draft> draftStates = draftStateRepository.findByDraftURI(need.getNeedURI());
      needRepository.delete(draftId);
      draftStateRepository.delete(draftStates);
      rdfStorage.removeContent(need.getNeedURI());     */
    }catch (Exception e){
      return new ResponseEntity(HttpStatus.CONFLICT);
    }
    return new ResponseEntity(HttpStatus.OK);


  }



  /**
   *
   * @param needId id of the need for which information shall be retrieved
   * @return a JSON need object
   */
	@ResponseBody
	@RequestMapping(
			value = "/{needId}",
			produces = MediaType.APPLICATION_JSON,
			method = RequestMethod.GET
	)
  @Deprecated
	public NeedPojo getNeed(@PathVariable("needId") long needId) {
		logger.info("Getting need: " + needId);

		LinkedDataRestClient linkedDataRestClient = new LinkedDataRestClient();
		List<NeedPojo> returnList = new ArrayList<NeedPojo>();

		Iterable<Need> needs = needRepository.findById(needId);
		Need need = needs.iterator().next();

		NeedPojo needPojo = new NeedPojo(need.getNeedURI(), linkedDataRestClient.readResourceData(need.getNeedURI()).getDefaultModel());
		needPojo.setNeedId(need.getId());

		return needPojo;
	}
  @ResponseBody
  @RequestMapping(
    value = "/{needId}",
    consumes = MediaType.APPLICATION_JSON,
    produces = MediaType.APPLICATION_JSON,
    method = RequestMethod.PUT
  )
  @Deprecated
  public NeedPojo updateNeed(@PathVariable("needId") long needId, @RequestBody NeedPojo needPojo) {
    //TODO: won node currently doesn't support updates.

    return needPojo;
  }


  /**
   * returns List of connections of a need with the needId
   * @param needId id of the need, for which list of connections shall be retrieved
   * @return a List of connections
   */
	@ResponseBody
	@RequestMapping(
			value = "/{needId}/connections",
			method = RequestMethod.GET,
			produces = MediaType.APPLICATION_JSON
	)
  @Deprecated
	public List<Connection> listConnections(@PathVariable String needId) {

		List<Need> needs = needRepository.findById(Long.valueOf(needId));
		if (needs.isEmpty())
			return new ArrayList<>();

		Need need = needs.get(0);

		return connectionRepository.findByNeedURI(need.getNeedURI());
	}

  /**
   *
   *
   *
   * @param needId
   * @param connectionPojo
   * @return
   */
  @ResponseBody
  @RequestMapping(
    value = "/{needId}/connections",
    method = RequestMethod.POST,
    consumes = MediaType.APPLICATION_JSON,
    produces = MediaType.APPLICATION_JSON
  )
  @Deprecated
  public ConnectionPojo connect(@PathVariable String needId, @RequestBody ConnectionPojo connectionPojo) {

    ConnectionPojo fullConnection = null;
    try {
      ListenableFuture<URI> futureResult = ownerService.connect(
              URI.create(connectionPojo.getNeedURI()),
              URI.create(connectionPojo.getRemoteNeedURI()),
              WonRdfUtils.FacetUtils.createFacetModelForHintOrConnect(FacetType.OwnerFacet.getURI(),
                    FacetType.OwnerFacet.getURI()),
              null);
      URI connectionURI = futureResult.get();
      Connection connection = DataAccessUtils.loadConnection(connectionRepository,connectionURI);
      if (connection != null){
        fullConnection = new ConnectionPojo(connectionURI, linkedDataSource.getDataForResource
          (connection.getConnectionURI()).getDefaultModel());
        fullConnection.setConnectionId(connection.getId());
        logger.debug("Added connection id:" + fullConnection.getConnectionId() + "uri: " + connectionURI);
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
    return fullConnection;
  }


  @Deprecated
  private NeedPojo resolve(NeedPojo needPojo) {
    if (needPojo.getNeedId() >= 0) {
      List<Need> needs = needRepository.findById(needPojo.getNeedId());
      if (!needs.isEmpty()) {
        logger.warn("Deactivating old need");
        try {
          ownerService.deactivate(needs.get(0).getNeedURI(), null);
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

      if (needPojo.getWonNode() == null || needPojo.getWonNode().equals("")) {
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

      Need need = DataAccessUtils.loadNeed(needRepository, needURI);
      if (need != null) {
        NeedPojo fullNeed = new NeedPojo(needURI, linkedDataSource.getDataForResource(need.getNeedURI()).getDefaultModel());
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

}
