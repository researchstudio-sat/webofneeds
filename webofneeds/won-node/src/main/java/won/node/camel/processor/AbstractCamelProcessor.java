/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.node.camel.processor;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import won.cryptography.service.RandomNumberService;
import won.node.facet.FacetService;
import won.node.protocol.MatcherProtocolMatcherServiceClientSide;
import won.node.service.DataAccessService;
import won.protocol.exception.IncompatibleFacetTypesException;
import won.protocol.jms.MessagingService;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Need;
import won.protocol.model.OwnerApplication;
import won.protocol.repository.ConnectionContainerRepository;
import won.protocol.repository.ConnectionEventContainerRepository;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.DatasetHolderRepository;
import won.protocol.repository.FacetRepository;
import won.protocol.repository.MessageEventRepository;
import won.protocol.repository.NeedEventContainerRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.repository.OwnerApplicationRepository;
import won.protocol.service.LinkedDataService;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.RdfUtils;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

/**
 * User: syim
 * Date: 02.03.2015
 */
public abstract class AbstractCamelProcessor implements Processor
{

  protected Logger logger = org.slf4j.LoggerFactory.getLogger(this.getClass());


  @Autowired
  protected MessagingService messagingService;
  @Autowired
  protected DataAccessService dataService;
  @Autowired
  protected DatasetHolderRepository datasetHolderRepository;
  @Autowired
  protected NeedRepository needRepository;
  @Autowired
  protected ConnectionContainerRepository connectionContainerRepository;
  @Autowired
  protected NeedEventContainerRepository needEventContainerRepository;
  @Autowired
  protected ConnectionRepository connectionRepository;
  @Autowired
  protected ConnectionEventContainerRepository connectionEventContainerRepository;
  @Autowired
  protected FacetRepository facetRepository;
  @Autowired
  protected OwnerApplicationRepository ownerApplicationRepository;
  @Autowired
  protected MessageEventRepository messageEventRepository;
  @Autowired
  protected LinkedDataService linkedDataService;
  @Autowired
  protected WonNodeInformationService wonNodeInformationService;
  @Autowired
  protected LinkedDataSource linkedDataSource;
  @Autowired
  protected MatcherProtocolMatcherServiceClientSide matcherProtocolMatcherClient;
  @Autowired
  protected RandomNumberService randomNumberService;
  @Autowired
  protected ExecutorService executorService;
  @Autowired
  protected FacetService facetService;





  protected void sendMessageToOwner(WonMessage message, URI needURI, String fallbackOwnerApplicationId){
    Need need = needRepository.findOneByNeedURI(needURI);
    List<OwnerApplication> ownerApplications = need != null ? need.getAuthorizedApplications() : Collections.EMPTY_LIST;
    List<String> ownerApplicationIds = toStringIds(ownerApplications);
    //if no owner application ids are authorized, we use the fallback specified (if any)
    if (ownerApplicationIds.isEmpty() && fallbackOwnerApplicationId != null) {
      ownerApplicationIds.add(fallbackOwnerApplicationId);
    }
    Map headerMap = new HashMap<String, Object>();
    headerMap.put(WonCamelConstants.OWNER_APPLICATIONS, ownerApplicationIds);
    messagingService.sendInOnlyMessage(null, headerMap, RdfUtils.writeDatasetToString(message.getCompleteDataset(),
        WonCamelConstants.RDF_LANGUAGE_FOR_MESSAGE),
                                       "seda:OwnerProtocolOut");
  }

  protected void sendMessageToOwner(WonMessage message, List<String> ownerApplicationIds){
    Map headerMap = new HashMap<String, Object>();
    headerMap.put("protocol","OwnerProtocol");
    headerMap.put(WonCamelConstants.OWNER_APPLICATIONS, ownerApplicationIds);
    messagingService.sendInOnlyMessage(null, headerMap, RdfUtils.writeDatasetToString(message.getCompleteDataset(),
        WonCamelConstants.RDF_LANGUAGE_FOR_MESSAGE),
                                       "seda:OwnerProtocolOut");
  }

  protected void sendMessageToOwner(WonMessage message, String... ownerApplicationIds){
    Map headerMap = new HashMap<String, Object>();
    headerMap.put(WonCamelConstants.OWNER_APPLICATIONS, Arrays.asList(ownerApplicationIds));
    messagingService.sendInOnlyMessage(null, headerMap, RdfUtils.writeDatasetToString(message.getCompleteDataset(),WonCamelConstants.RDF_LANGUAGE_FOR_MESSAGE),
                                       "seda:OwnerProtocolOut");
  }

  protected void sendMessageToNode(WonMessage message){
    Map headerMap = new HashMap<String, Object>();
    headerMap.put(WonCamelConstants.MESSAGE_HEADER, message);
    messagingService.sendInOnlyMessage(null, headerMap, null,
                                       "seda:NeedProtocolOut");
  }

  /**
   * Processes the system message (allowing facet implementations) and delivers it, depending on its receiver settings.
   *
   * @param message
   */
  protected void sendSystemMessage(WonMessage message){
    Map headerMap = new HashMap<String, Object>();
    headerMap.put(WonCamelConstants.MESSAGE_HEADER, message);
    messagingService.sendInOnlyMessage(null, headerMap, null,
      "seda:SystemMessageIn");
  }

  /**
   * Sends a system message to the owner without facet processing. Useful for Response messages.
   * @param message
   */
  protected void sendSystemMessageToOwner(WonMessage message) {
    sendSystemMessageToOwner(message, null);
  }

  /**
   * Sends a system message to the owner without facet processing. Useful for Response messages.
   *
   * Allows for adding the ownerApplicationId to the exchange used during creation and
   * sending of the system message. This is useful for cases in which the owner application
   * cannot determined otherwise, which can happen when need creation fails.
   *
   * If that value is non-null, it is set as the 'ownerApplicationId' header, which is used in
   * AbstractCamelProcessor#sendMessageToOwner(..) as a fallback to determine the recipients of
   * the message to be sent.
   *
   * @param message
   * @param ownerApplicationId
   */
  protected void sendSystemMessageToOwner(WonMessage message, String ownerApplicationId){
    Map headerMap = new HashMap<String, Object>();
    headerMap.put(WonCamelConstants.MESSAGE_HEADER, message);
    if (ownerApplicationId != null){
      headerMap.put(WonCamelConstants.OWNER_APPLICATION_ID, ownerApplicationId);
    }
    messagingService.sendInOnlyMessage(null, headerMap, null,
            "seda:SystemMessageToOwner");
  }

  protected List<String> toStringIds(final List<OwnerApplication> ownerApplications) {
    List<String> ownerApplicationIds = new ArrayList<String>(ownerApplications.size());
    for(OwnerApplication app: ownerApplications){
      ownerApplicationIds.add(app.getOwnerApplicationId());
    }
    return ownerApplicationIds;
  }

    protected void failForIncompatibleFacets(URI facetURI, URI facetTypeURI, URI remoteFacetURI)
            throws IncompatibleFacetTypesException {
        Optional<URI> remoteFacetType = WonLinkedDataUtils.getTypeOfFacet(remoteFacetURI, linkedDataSource);
        if (!remoteFacetType.isPresent()) {
            throw new IllegalStateException("Could not determine type of remote facet " + remoteFacetURI);
        }
        if (!facetService.isConnectionAllowedToType(facetTypeURI, remoteFacetType.get())) {
            throw new IncompatibleFacetTypesException(facetURI, facetTypeURI, remoteFacetURI, remoteFacetType.get());
        }
    }

    
    protected void failIfIsNotFacetOfNeed(Optional<URI> facetURI, Optional<URI> needURI) {
        if (facetURI.isPresent() && needURI.isPresent() && !facetURI.get().toString().startsWith(needURI.get().toString())){
            throw new IllegalArgumentException("User-defined facet "+ facetURI +" is not a facet of need " + needURI);
        }
    }
}
