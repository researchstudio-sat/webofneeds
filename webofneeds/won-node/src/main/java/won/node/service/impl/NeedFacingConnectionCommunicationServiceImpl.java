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
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import won.node.facet.impl.FacetRegistry;

import won.protocol.exception.IllegalMessageForConnectionStateException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEvent;
import won.protocol.model.ConnectionEventType;
import won.protocol.model.FacetType;
import won.protocol.repository.*;
import won.protocol.service.ConnectionCommunicationService;
import won.protocol.util.DataAccessUtils;
import won.protocol.vocabulary.WON;
import won.node.facet.impl.WON_BA;

import java.net.URI;
import java.util.concurrent.ExecutorService;

/**
 * User: fkleedorfer
 * Date: 02.11.12
 */
@Component
public class NeedFacingConnectionCommunicationServiceImpl implements ConnectionCommunicationService
{
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private FacetRegistry reg;
  private DataAccessService dataService;

  private ExecutorService executorService;

  @Autowired
  private ConnectionRepository connectionRepository;
  @Autowired
  private ChatMessageRepository chatMessageRepository;
  @Autowired
  private EventRepository eventRepository;
  @Autowired
  private RDFStorageService rdfStorageService;
  @Autowired
  private OwnerApplicationRepository ownerApplicationRepository;
  @Autowired
  private NeedRepository needRepository;

  @Override
  public void open(final URI connectionURI, final Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    logger.info("OPEN received from the need side for connection {0} with content {1}", connectionURI, content);

    Connection con = dataService.nextConnectionState(connectionURI, ConnectionEventType.PARTNER_OPEN);

    ConnectionEvent event = dataService.createConnectionEvent(connectionURI, con.getRemoteConnectionURI(), ConnectionEventType.PARTNER_OPEN);

    dataService.saveAdditionalContentForEvent(content, con, event);

    //invoke facet implementation
    reg.get(con).openFromNeed(con, content);
  }

  @Override
  public void close(final URI connectionURI, final Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    logger.info("CLOSE received from the need side for connection {0} with content {1}", connectionURI, content);
    Connection con = dataService.nextConnectionState(connectionURI, ConnectionEventType.PARTNER_CLOSE);

    ConnectionEvent event = dataService.createConnectionEvent(connectionURI, con.getRemoteConnectionURI(), ConnectionEventType.PARTNER_CLOSE);

    dataService.saveAdditionalContentForEvent(content, con, event);

    //invoke facet implementation
    reg.get(con).closeFromNeed(con, content);
  }
    @Override
    public void textMessage(final URI connectionURI, final Model message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
    {

        Connection con = DataAccessUtils.loadConnection(connectionRepository, connectionURI);
        //check for facet types:
        System.out.println("daki Facet type:"+con.getTypeURI());
        if(con.getTypeURI().equals(FacetType.BAPCCoordinatorFacet.getURI()) ||
                con.getTypeURI().equals(FacetType.BAPCParticipantFacet.getURI()) ||
                con.getTypeURI().equals(FacetType.BACCCoordinatorFacet.getURI())  ||
                con.getTypeURI().equals(FacetType.BACCParticipantFacet.getURI()))
        {
            System.out.println("Daki tu sam");
            Resource baseRes = message.getResource(message.getNsPrefixURI(""));
            StmtIterator stmtIterator = baseRes.listProperties(WON_BA.COORDINATION_MESSAGE);
            String coordinationMessage = stmtIterator.next().getObject().toString();
            dataService.saveChatMessage(con,coordinationMessage);
            //create ConnectionEvent in Database
            ConnectionEvent event = dataService.createConnectionEvent(con.getConnectionURI(), con.getRemoteConnectionURI(), ConnectionEventType.OWNER_OPEN);

            //create rdf content for the ConnectionEvent and save it to disk
            dataService.saveAdditionalContentForEvent(message, con, event, null);

            //invoke facet implementation
            reg.get(con).textMessageFromNeed(con, message);

        }
    /*    else if(con.getTypeURI().equals(FacetType.BAPCParticipantFacet.getURI()))
        {
            Resource baseRes = message.getResource(message.getNsPrefixURI(""));
            StmtIterator stmtIterator = baseRes.listProperties(WON_BA.COORDINATION_MESSAGE);
            String coordinationMessage = stmtIterator.next().getObject().toString();
            dataService.saveChatMessage(con,coordinationMessage);
            //create ConnectionEvent in Database
            ConnectionEvent event = dataService.createConnectionEvent(con.getConnectionURI(), con.getRemoteConnectionURI(), ConnectionEventType.OWNER_OPEN);

            //create rdf content for the ConnectionEvent and save it to disk
            dataService.saveAdditionalContentForEvent(message, con, event, null);

            //invoke facet implementation
            reg.get(con).textMessageFromNeed(con, message);
        } */
        else
        {
            System.out.println("Daki nisam tu");
            Resource baseRes = message.getResource(message.getNsPrefixURI(""));
            StmtIterator stmtIterator = baseRes.listProperties(WON.HAS_TEXT_MESSAGE);
            String textMessage = stmtIterator.next().getObject().toString();
            dataService.saveChatMessage(con,textMessage);
            //create ConnectionEvent in Database
            ConnectionEvent event = dataService.createConnectionEvent(con.getConnectionURI(), con.getRemoteConnectionURI(), ConnectionEventType.OWNER_OPEN);

            //create rdf content for the ConnectionEvent and save it to disk
            dataService.saveAdditionalContentForEvent(message, con, event, null);

            //invoke facet implementation
            reg.get(con).textMessageFromNeed(con, message);
        }
    }
  /*
  @Override
  public void textMessage(final URI connectionURI, final Model message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    logger.info("SEND_TEXT_MESSAGE received from the need side for connection {} with message '{}'", connectionURI, message);
    Connection con = dataService.saveChatMessage(connectionURI,message);

    //invoke facet implementation
    reg.get(con).textMessageFromNeed(con, message);


  }       */

  public void setReg(FacetRegistry reg) {
    this.reg = reg;
  }

  public void setDataService(DataAccessService dataService) {
    this.dataService = dataService;
  }
}
