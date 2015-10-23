package won.owner.web.rest;

import com.hp.hpl.jena.query.Dataset;
import org.apache.jena.riot.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import won.owner.model.User;
import won.owner.model.UserNeed;
import won.owner.service.impl.WONUserDetailService;
import won.protocol.util.RdfUtils;
import won.protocol.util.linkeddata.LinkedDataSource;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

/**
 * User: ypanchenko
 * Date: 03.09.2015
 *
 * This controller at Owner server-side serves as a bridge for Owner client-side to obtain linked data from a Node:
 * because the linked data on a Node can have restricted access based on WebID, only Owner server-side can provide the
 * client's certificate as proof of having the private key from client's published WebID. Because of this, Owner
 * client-side has to ask Owner-server side to query Node for it, instead of querying directly from Owner client-side.
 */
@Controller
@RequestMapping("/rest/linked-data")
public class BridgeForLinkedDataController {

  @Autowired
  private WONUserDetailService wonUserDetailService;
  @Autowired
  private LinkedDataSource linkedDataSource;

  final Logger logger = LoggerFactory.getLogger(getClass());

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

  /**
   * Obtain linked data on behalf of the requester with given WebID. The request will include Certificate of the
   * requester. This is necessary for obtaining linked data has restricted access based on WebID access control and
   * verification.
   *
   * @param resourceUri
   * @param requesterWebId
   * @return
   */
  private ResponseEntity<String> fetchResourceProvidingIdentity(final String resourceUri, final String requesterWebId) {
    Dataset dataset = linkedDataSource.getDataForResource(URI.create(resourceUri), URI.create(requesterWebId));
    String result = RdfUtils.writeDatasetToString(dataset, Lang.JSONLD);
    return new ResponseEntity(result, HttpStatus.OK);
  }

  /**
   * Obtain linked data without providing Certificate/WebID - will work for linked data that does not have restricted
   * access.
   *
   * @param resourceUri
   * @return
   */
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

