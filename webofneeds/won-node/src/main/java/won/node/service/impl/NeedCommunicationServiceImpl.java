/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package won.node.service.impl;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import won.node.facet.impl.Facet;
import won.node.facet.impl.FacetRegistry;
import won.node.service.DataAccessService;
import won.protocol.exception.ConnectionAlreadyExistsException;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.WonMessageEncoder;
import won.protocol.model.*;
import won.protocol.need.NeedProtocolNeedService;
import won.protocol.owner.OwnerProtocolOwnerService;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.EventRepository;
import won.protocol.repository.MessageEventRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.repository.rdfstorage.RDFStorageService;
import won.protocol.service.MatcherFacingNeedCommunicationService;
import won.protocol.service.NeedFacingNeedCommunicationService;
import won.protocol.service.OwnerFacingNeedCommunicationService;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WON;

import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.ExecutorService;


@Component
public class NeedCommunicationServiceImpl implements
    OwnerFacingNeedCommunicationService,
    NeedFacingNeedCommunicationService,
    MatcherFacingNeedCommunicationService {

  final Logger logger = LoggerFactory.getLogger(NeedCommunicationServiceImpl.class);
  private FacetRegistry reg;
  private DataAccessService dataService;

  /**
   * Client talking to the owner side via the owner protocol
   */
  private OwnerProtocolOwnerService ownerProtocolOwnerService;
  /**
   * Client talking another need via the need protocol
   */
  private NeedProtocolNeedService needProtocolNeedService;

  /**
   * Client talking to this need service from the need side
   */
  private NeedFacingConnectionCommunicationServiceImpl needFacingConnectionCommunicationService;

  /**
   * Client talking to this need service from the owner side
   */
  private OwnerFacingConnectionCommunicationServiceImpl ownerFacingConnectionCommunicationService;

  private URIService URIService;

  private ExecutorService executorService;

  @Autowired
  private NeedRepository needRepository;
  @Autowired
  private ConnectionRepository connectionRepository;
  @Autowired
  private EventRepository eventRepository;
  @Autowired
  private RDFStorageService rdfStorageService;
  @Autowired
  private MessageEventRepository messageEventRepository;
  @Autowired
  private WonNodeInformationService wonNodeInformationService;

  @Override
  public void hint(final URI needURI, final URI otherNeedURI,
                   final double score, final URI originator,
                   final Model content, final WonMessage wonMessage)
          throws NoSuchNeedException, IllegalMessageForNeedStateException {

    // distinguish between the new message format (WonMessage) and the old parameters
    // ToDo (FS): remove this distinction if the old parameters not used anymore
    if (wonMessage != null) {
      logger.debug("STORING message with id {}", wonMessage.getMessageURI());
      rdfStorageService.storeDataset(wonMessage.getMessageURI(),
                                     WonMessageEncoder.encodeAsDataset(wonMessage));

      URI needURIFromWonMessage = wonMessage.getReceiverNeedURI();
      URI otherNeedURIFromWonMessage = URI.create(RdfUtils.findOnePropertyFromResource(
        wonMessage.getMessageContent(), wonMessage.getMessageURI(),
        WON.HAS_MATCH_COUNTERPART).asResource().getURI());
      double wmScore = RdfUtils.findOnePropertyFromResource(
        wonMessage.getMessageContent(), wonMessage.getMessageURI(),
        WON.HAS_MATCH_SCORE).asLiteral().getDouble();
      URI wmOriginator = wonMessage.getSenderNodeURI();
      if (wmScore < 0 || wmScore > 1) throw new IllegalArgumentException("score is not in [0,1]");
      if (wmOriginator == null)
        throw new IllegalArgumentException("originator is not set");

      //create Connection in Database
      Connection con = null;
      Model facetModel = ModelFactory.createDefaultModel();

      try {
        URI facet = dataService.getFacet(content);
        // ToDo (FS): adapt this part to the new message format (dont use content)
        if (facet == null) {
          //get the first one of the need's supported facets. TODO: implement some sort of strategy for choosing a facet here (and in the matcher)
          Collection<URI> facets = dataService.getSupportedFacets(needURIFromWonMessage);
          if (facets.isEmpty()) throw new IllegalArgumentException(
            "hint does not specify facets, falling back to using one of the need's supported facets failed as the need does not support any facets");
          //add the facet to the model.
          facet = facets.iterator().next();
        }
        con = dataService.createConnection(
          needURIFromWonMessage, otherNeedURIFromWonMessage,
          null, facet, ConnectionState.SUGGESTED, ConnectionEventType.MATCHER_HINT);
      } catch (ConnectionAlreadyExistsException e) {
        logger.warn("could not create connection", e);
      }


      messageEventRepository.save(new MessageEventPlaceholder(con.getConnectionURI(), wonMessage));

      //invoke facet implementation
      reg.get(con).hint(con, wmScore, wmOriginator, facetModel, wonMessage);

    } else {

      if (score < 0 || score > 1) throw new IllegalArgumentException("score is not in [0,1]");
      if (originator == null) throw new IllegalArgumentException("originator is not set");

      //create Connection in Database
      Connection con = null;
      try {
        //see if there is a facet specified in the content
        URI facet = dataService.getFacet(content);
        if (facet == null) {
          //get the first one of the need's supported facets. TODO: implement some sort of strategy for choosing a facet here (and in the matcher)
          Collection<URI> facets = dataService.getSupportedFacets(needURI);
          if (facets.isEmpty()) throw new IllegalArgumentException(
            "hint does not specify facets, falling back to using one of the need's supported facets failed as the need does not support any facets");
          //add the facet to the model.
          facet = facets.iterator().next();
        }
        con = dataService.createConnection(needURI, otherNeedURI, null, facet, ConnectionState.SUGGESTED,
                                           ConnectionEventType.MATCHER_HINT);
      } catch (ConnectionAlreadyExistsException e) {
        logger.warn("could not create connection", e);
      }

      //create ConnectionEvent in Database
      ConnectionEvent event = dataService
        .createConnectionEvent(con.getConnectionURI(), originator, ConnectionEventType.MATCHER_HINT);

      String baseURI = con.getConnectionURI().toString();
      RdfUtils.replaceBaseURI(content, baseURI);
      //create rdf content for the ConnectionEvent and save it to disk
      dataService.saveAdditionalContentForEvent(content, con, event, score);

      //invoke facet implementation
      reg.get(con).hint(con, score, originator, content, wonMessage);
    }
  }

  @Override
  public URI connect(final URI needURI, final URI otherNeedURI, final Model content, final WonMessage wonMessage)
          throws NoSuchNeedException,
    IllegalMessageForNeedStateException, ConnectionAlreadyExistsException {

    // distinguish between the new message format (WonMessage) and the old parameters
    // ToDo (FS): remove this distinction if the old parameters not used anymore
    if (wonMessage != null) {

      URI senderNeedURI = wonMessage.getSenderNeedURI();
      URI receiverNeedURI = wonMessage.getReceiverNeedURI();

      //TODO: when we introduce dedicated URIs for individual facets, this will be how
      URI facetURI = WonRdfUtils.FacetUtils.getFacet(content);

      //create Connection in Database
      Connection con =  dataService.createConnection(senderNeedURI, receiverNeedURI, null, facetURI,
                                                     ConnectionState.REQUEST_SENT,
                                                     ConnectionEventType.OWNER_OPEN);

      // add the connectionID to the wonMessage
      WonMessage newWonMessage = new WonMessageBuilder()
        .wrap(wonMessage)
        .setTimestamp(new Date().getTime())
        .setSenderURI(con.getConnectionURI())
        .build();
      messageEventRepository.save(
        new MessageEventPlaceholder(con.getConnectionURI(), newWonMessage));
      logger.debug("STORING message with id {}", newWonMessage.getMessageURI());
      rdfStorageService.storeDataset(newWonMessage.getMessageURI(),
        newWonMessage.getCompleteDataset());

      //invoke facet implementation
      Facet facet = reg.get(con);
      facet.connectFromOwner(con, content, newWonMessage);
      //reg.get(con).connectFromOwner(con, content);

      return con.getConnectionURI();

    } else {

      URI facetURI = WonRdfUtils.FacetUtils.getFacet(content);

      //create Connection in Database
      Connection con = dataService.createConnection(needURI, otherNeedURI, null, facetURI, ConnectionState.REQUEST_SENT,
                                                    ConnectionEventType.OWNER_OPEN);

      //create ConnectionEvent in Database
      ConnectionEvent event = dataService
        .createConnectionEvent(con.getConnectionURI(), con.getRemoteConnectionURI(), ConnectionEventType.OWNER_OPEN);

      String baseURI = con.getConnectionURI().toString();
      RdfUtils.replaceBaseURI(content, baseURI);
      //create rdf content for the ConnectionEvent and save it to disk
      dataService.saveAdditionalContentForEvent(content, con, event, null);

      //invoke facet implementation
      Facet facet = reg.get(con);
      facet.connectFromOwner(con, content, wonMessage);
      //reg.get(con).connectFromOwner(con, content);

      return con.getConnectionURI();

    }
  }

  @Override
  public URI connect(final URI needURI, final URI otherNeedURI,
                     final URI otherConnectionURI, final Model content,
                     final WonMessage wonMessage)
          throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException {

    // distinguish between the new message format (WonMessage) and the old parameters
    // ToDo (FS): remove this distinction if the old parameters not used anymore
    if (wonMessage != null) {

      // a need wants to connect.
      // get the required data from the message and create a connection
      URI needURIFromWonMessage = wonMessage.getReceiverNeedURI();
      URI otherNeedURIFromWonMessage = wonMessage.getSenderNeedURI();
      URI otherConnectionURIFromWonMessage = wonMessage.getSenderURI();
      URI facetURI = WonRdfUtils.FacetUtils.getFacet(wonMessage.getMessageURI(),
        wonMessage.getMessageContent());

      logger.debug("CONNECT received for need {} referring to need {} (connection {})",
                   new Object[]{needURIFromWonMessage,
                                otherNeedURIFromWonMessage,
                                otherConnectionURIFromWonMessage});
      if (otherConnectionURIFromWonMessage == null) throw new IllegalArgumentException("otherConnectionURI is not set");

      //create Connection in Database
      Connection con = dataService.createConnection(needURIFromWonMessage,
                                                    otherNeedURIFromWonMessage,
                                                    otherConnectionURIFromWonMessage,
                                                    facetURI,
                                                    ConnectionState.REQUEST_RECEIVED, ConnectionEventType.PARTNER_OPEN);
      // copy the message envelope
      // information about the newly created connection
      // to the message and pass it on to the owner.
      URI wrappedMessageURI = this.wonNodeInformationService.generateMessageEventURI();
      WonMessageBuilder builder = new WonMessageBuilder()
        .setMessageURI(wrappedMessageURI)
        .copyEnvelopeFromWonMessage(wonMessage)
        .copyContentFromMessageReplacingMessageURI(wonMessage)
        .setReceiverURI(con.getConnectionURI())
        .setTimestamp(new Date().getTime())
        .addRefersToURI(wonMessage.getMessageURI());
      WonMessage wrappedMessage = builder.build(wonMessage.getMessageContent());

      rdfStorageService.storeDataset(wrappedMessageURI, wrappedMessage.getCompleteDataset());

      messageEventRepository.save(new MessageEventPlaceholder(
        con.getConnectionURI(), wrappedMessage));

      //invoke facet implementation
      Facet facet = reg.get(con);
      // send an empty model until we remove this parameter
      facet.connectFromNeed(con, content, wrappedMessage);

      return con.getConnectionURI();

    } else {

      logger.debug("CONNECT received for need {} referring to need {} (connection {}) with content '{}'",
                   new Object[]{needURI, otherNeedURI, otherConnectionURI, content});
      if (otherConnectionURI == null) throw new IllegalArgumentException("otherConnectionURI is not set");

      URI facetURI = WonRdfUtils.FacetUtils.getFacet(content);

      //create Connection in Database
    Connection con = dataService.createConnection(needURI, otherNeedURI, otherConnectionURI, facetURI,
                                                    ConnectionState.REQUEST_RECEIVED, ConnectionEventType.PARTNER_OPEN);
      String baseURI = con.getConnectionURI().toString();
      RdfUtils.replaceBaseURI(content, baseURI);
      //create ConnectionEvent in Database
      ConnectionEvent event = dataService
        .createConnectionEvent(con.getConnectionURI(), con.getRemoteConnectionURI(), ConnectionEventType.PARTNER_OPEN);

      //create rdf content for the ConnectionEvent and save it to disk
      dataService.saveAdditionalContentForEvent(content, con, event, null);

      //invoke facet implementation
      Facet facet = reg.get(con);
      facet.connectFromNeed(con, content, wonMessage);

      return con.getConnectionURI();
    }
  }



  public void setReg(FacetRegistry reg) {
    this.reg = reg;
  }

  public void setDataService(DataAccessService dataService) {
    this.dataService = dataService;
  }
}
