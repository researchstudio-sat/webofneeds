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

import com.hp.hpl.jena.rdf.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.node.facet.impl.FacetRegistry;
import won.node.facet.impl.WON_BA;
import won.protocol.exception.IllegalMessageForConnectionStateException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.model.*;
import won.protocol.repository.ConnectionRepository;
import won.protocol.service.ConnectionCommunicationService;
import won.protocol.util.DataAccessUtils;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.WON;

import java.net.URI;


/**
 * User: fkleedorfer
 * Date: 02.11.12
 */
public class OwnerFacingConnectionCommunicationServiceImpl implements ConnectionCommunicationService
{
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private FacetRegistry reg;
  private DataAccessService dataService;
  @Autowired
  private ConnectionRepository connectionRepository;
  @Autowired
  private URIService URIService;

  @Override
  public void open(final URI connectionURI, final Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    logger.info("OPEN received from the owner side for connection {0} with content {1}", connectionURI, content);

    Connection con = dataService.nextConnectionState(connectionURI, ConnectionEventType.OWNER_OPEN);

    ConnectionEvent event = dataService.createConnectionEvent(connectionURI, con.getRemoteConnectionURI(), ConnectionEventType.OWNER_OPEN);

    dataService.saveAdditionalContentForEvent(content, con, event);

    //invoke facet implementation
    reg.get(con).openFromOwner(con, content);
  }

  @Override
  public void close(final URI connectionURI, final Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    logger.info("CLOSE received from the owner side for connection {0} with content {1}", connectionURI, content);

    Connection con = dataService.nextConnectionState(connectionURI, ConnectionEventType.OWNER_CLOSE);

    ConnectionEvent event = dataService.createConnectionEvent(connectionURI, con.getRemoteConnectionURI(), ConnectionEventType.OWNER_CLOSE);

    dataService.saveAdditionalContentForEvent(content, con, event);

    //invoke facet implementation
    reg.get(con).closeFromOwner(con, content);
  }
  @Override
    public void textMessage(final URI connectionURI, final Model message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException{

        Connection con = DataAccessUtils.loadConnection(connectionRepository, connectionURI);
        Resource baseRes = message.getResource(message.getNsPrefixURI(""));
        StmtIterator stmtIterator = baseRes.listProperties(WON.HAS_TEXT_MESSAGE);

        //TODO: security? stability? use the first property we find - what if there are more?
        String textMessage = null;
        while (stmtIterator.hasNext()){
            RDFNode obj = stmtIterator.nextStatement().getObject();
            if (obj.isLiteral()) {
                textMessage = obj.asLiteral().getLexicalForm();
                break;
            }
        }
        if (textMessage == null){
            logger.debug("could not extract text message from RDF content of message");
            textMessage = "[could not extract text message]";
        }

        dataService.saveChatMessage(con,textMessage);
        //create ConnectionEvent in Database

        ConnectionEvent event = dataService.createConnectionEvent(con.getConnectionURI(), con.getRemoteConnectionURI(), ConnectionEventType.CHAT_MESSAGE);
        Resource eventNode = message.createResource(this.URIService.createEventURI(con, event).toString());
        RdfUtils.replaceBaseResource(message, eventNode);
        //create rdf content for the ConnectionEvent and save it to disk
        dataService.saveAdditionalContentForEvent(message, con, event);

        //invoke facet implementation
        reg.get(con).textMessageFromOwner(con, message);
        //todo: the method shall return an object that informs the owner that processing the message on the node side was done successfully.
        //return con.getConnectionURI();


    }

    /*
  @Override
  public void textMessage(final URI connectionURI, final Model message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    logger.info("SEND_TEXT_MESSAGE received from the owner side for connection {} with message '{}'", connectionURI, message);
    Connection con = dataService.saveChatMessage(connectionURI,message);

    //invoke facet implementation
    reg.get(con).textMessageFromOwner(con, message);

  }
     */
  public void setReg(FacetRegistry reg) {
    this.reg = reg;
  }

  public void setDataService(DataAccessService dataService) {
    this.dataService = dataService;
  }
}
