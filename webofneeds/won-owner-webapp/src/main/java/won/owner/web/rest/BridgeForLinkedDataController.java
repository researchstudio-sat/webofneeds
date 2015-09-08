package won.owner.web.rest;

import com.hp.hpl.jena.query.Dataset;
import org.apache.jena.riot.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import won.cryptography.service.CryptographyUtils;
import won.cryptography.service.KeyStoreService;
import won.cryptography.service.TrustStoreService;
import won.owner.model.User;
import won.owner.model.UserNeed;
import won.owner.service.impl.WONUserDetailService;
import won.protocol.util.RdfUtils;
import won.protocol.util.linkeddata.LinkedDataSource;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * User: ypanchenko
 * Date: 03.09.2015
 */
@Controller
@RequestMapping("/rest/linked-data")
public class BridgeForLinkedDataController {

  @Autowired
  private KeyStoreService keyStoreService;
  @Autowired
  private TrustStoreService trustStoreService;
  @Autowired
  private WONUserDetailService wonUserDetailService;
  @Autowired
  private LinkedDataSource linkedDataSource;

  final Logger logger = LoggerFactory.getLogger(getClass());

  public BridgeForLinkedDataController() {
  }

  @ResponseBody
  @RequestMapping(
    value = "/",
    method = RequestMethod.GET,
    produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> fetchResource(@RequestParam("uri") String resourceUri, @RequestParam(value =
    "requester", required = false)
  String
    requesterWebId) {

    if (requesterWebId != null) {
      // verify that the current user has the requester webid among his identities
      if (!currentUserHasIdentity(requesterWebId)) {
        logger.warn("Current user does not have claimed identity " + requesterWebId);
        return new ResponseEntity("Could not get " + resourceUri + " for requester " + requesterWebId, HttpStatus.BAD_REQUEST);
      }
      return fetchResourceProvidingIdentity(resourceUri, requesterWebId);
    } else {
      return fetchResourceWithoutProvidingIdentity(resourceUri);
    }

  }

  private ResponseEntity<String> fetchResourceProvidingIdentity(final String resourceUri, final String requesterWebId) {
    // TODO talk with flo if it makes sense to implement this via an implementation of LinkedDataSource. The problem
    // with that is that internally it cannot reuse the httpclient anyway, because the alias/webid is different each
    // time - for retrieving the different key each time, i.e. the httpclient has to be created each time inside the
    // getresource, which is different from the current concept...
    try {
      //todo password handling
      RestTemplate restTemplate = CryptographyUtils.createRestTemplateWithSslContext(keyStoreService
                                                                                       .getUnderlyingKeyStore(),
                                                                                     "temp",
                                                                                     requesterWebId,
                                                                                     trustStoreService
                                                                                       .getUnderlyingKeyStore());

      HttpHeaders headers = new HttpHeaders();
      headers.setAccept(Collections.singletonList(new MediaType("application", "ld+json")));
      HttpEntity<String> entity = new HttpEntity<String>("parameters", headers);
      ResponseEntity<String> fetchedResult = restTemplate.exchange(resourceUri, HttpMethod.GET, entity, String.class);

      if (!fetchedResult.getStatusCode().is2xxSuccessful()) {
        logger.warn("Error getting resource " + resourceUri + " for " + requesterWebId + ": " + fetchedResult.toString());
        return new ResponseEntity("Could not get " + resourceUri + " for requester " + requesterWebId + ": " +
                                    fetchedResult.toString(), HttpStatus.BAD_REQUEST);
      }
      return new ResponseEntity(fetchedResult.getBody(), HttpStatus.OK);
    } catch (Exception e) {
      logger.warn("Could not get " + resourceUri + " for requester " + requesterWebId, e);
      return new ResponseEntity("Could not get " + resourceUri + " for requester " + requesterWebId, HttpStatus.BAD_REQUEST);
    }
  }

  private ResponseEntity<String> fetchResourceWithoutProvidingIdentity(final String resourceUri) {
    Dataset dataset = linkedDataSource.getDataForResource(URI.create(resourceUri));
    String result = RdfUtils.writeDatasetToString(dataset, Lang.JSONLD);
    return new ResponseEntity(result, HttpStatus.OK);
  }


  /**
   * Check if the current user has the claimed identity represented by web-id of the need.
   * I.e. if the identity is that of the need that belongs to the user - return true, otherwise - false.
   *
   * @param requesterWebId
   * @return
   */
  private boolean currentUserHasIdentity(final String requesterWebId) {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    User user = (User) wonUserDetailService.loadUserByUsername(username);
    Set<URI> needUris = getUserNeedUris(user);
    if (needUris.contains(URI.create(requesterWebId))) {
      return true;
    }
    return false;
  }

  private Set<URI> getUserNeedUris(final User user) {
    Set<URI> needUris = new HashSet<>();
    for (UserNeed userNeed : user.getUserNeeds()) {
      needUris.add(userNeed.getUri());
    }
    return  needUris;
  }


}

