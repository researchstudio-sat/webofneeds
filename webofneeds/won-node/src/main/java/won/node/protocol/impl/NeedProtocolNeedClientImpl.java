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

package won.node.protocol.impl;

import com.hp.hpl.jena.rdf.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.node.rest.LinkedDataRestClient;
import won.protocol.exception.*;
import won.protocol.model.WON;
import won.protocol.need.NeedProtocolNeedService;

import java.net.URI;
import java.text.MessageFormat;

/**
 * User: fkleedorfer
 * Date: 28.11.12
 */
public class NeedProtocolNeedClientImpl implements NeedProtocolNeedService
{
  final Logger logger = LoggerFactory.getLogger(getClass());

  private LinkedDataRestClient linkedDataRestClient;


  @Override
  public URI connectionRequested(final URI needURI, final URI otherNeedURI, final URI otherConnectionURI, final String message) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException
  {
    logger.info(MessageFormat.format("need-facing: CONNECTION_REQUESTED called for own need {0}, other need {1}, other connection {2} and message {3}", needURI, otherNeedURI, otherConnectionURI, message));
    Model rdfModel = this.linkedDataRestClient.readResourceData(needURI);
    StmtIterator stmts = rdfModel.listStatements(new SimpleSelector(rdfModel.createResource(needURI.toString()), WON.NEED_PROTOCOL_ENDPOINT, (RDFNode) null));
    //assume only one endpoint
    if (!stmts.hasNext()) throw new NoSuchNeedException(needURI);
    Statement stmt = stmts.next();
    RDFNode needProtocolEndpoint = stmt.getObject();
    logger.info("requesting connection from need {} via need protocol endpoint {}",needURI.toString(), needProtocolEndpoint.toString());
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void accept(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    logger.info(MessageFormat.format("need-facing: ACCEPT called for connection {0}", connectionURI));
  }

  @Override
  public void deny(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    logger.info(MessageFormat.format("need-facing: DENY called for connection {0}", connectionURI));
  }

  @Override
  public void close(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    logger.info(MessageFormat.format("need-facing: CLOSE called for connection {0}", connectionURI));
  }

  @Override
  public void sendTextMessage(final URI connectionURI, final String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    logger.info(MessageFormat.format("need-facing: SEND_TEXT_MESSAGE called for connection {0} with message {1}", connectionURI,message));
  }










  public void setLinkedDataRestClient(final LinkedDataRestClient linkedDataRestClient)
  {
    this.linkedDataRestClient = linkedDataRestClient;
  }
}
