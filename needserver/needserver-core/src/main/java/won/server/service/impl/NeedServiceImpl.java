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

package won.server.service.impl;

import com.hp.hpl.jena.graph.Graph;
import won.protocol.exception.*;
import won.protocol.model.*;
import won.protocol.owner.NodeToOwnerSender;
import won.server.service.NeedService;

import java.net.URI;
import java.util.Collection;

/**
 * User: fkleedorfer
 * Date: 02.11.12
 */
public class NeedServiceImpl implements NeedService
{
  private NodeToOwnerSender ownerSender;

  @Override
  public URI createNeed(final URI ownerURI, final Graph content, final boolean activate) throws IllegalNeedContentException
  {

    Need need = new Need();
    need.setState(activate ? NeedState.ACTIVE : NeedState.INACTIVE);
    need.setOwnerURI(ownerURI);
    //TODO: save need in db - this will set the need URI
    return need.getURI();
  }

  @Override
  public void activate(final URI needURI) throws NoSuchNeedException
  {
    Need need = loadNeed(needURI);
    need.setState(NeedState.ACTIVE);
    //TODO: save need!
  }

  @Override
  public void deactivate(final URI needURI) throws NoSuchNeedException
  {
    Need need = loadNeed(needURI);
    need.setState(NeedState.INACTIVE);
    //TODO: save need!
  }

  @Override
  public Collection<URI> listNeedURIs()
  {
    //TODO: list need URIs from the db
    return null;
  }

  @Override
  public void hint(final URI needURI, final URI otherNeed, final double score, final URI originator) throws NoSuchNeedException
  {
    ownerSender.sendHintReceived(needURI,otherNeed,score,originator);
  }

  @Override
  public Collection<Match> getMatches(final URI needURI) throws NoSuchNeedException
  {
    Need need = loadNeed(needURI);
    //TODO: list connections!
    return null;
  }

  @Override
  public URI connectTo(final URI needURI, final URI otherNeedURI, final String message) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException
  {
    //Load need (throws exception if not found)
    Need need = loadNeed(needURI);
    //Create new connection object
    Connection con = new Connection();
    con.setNeedURI(needURI);
    con.setState(ConnectionState.REQUEST_SENT);
    con.setRemoteNeedURI(otherNeedURI);
    //TODO: save con in database - this will set the connection URI
    //Set connection
    return null;
  }


  @Override
  public void connectionRequested(final URI needURI, final URI otherNeedURI, final URI otherConnectionURI, final String message) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException
  {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public Collection<URI> listConnectionURIs(final URI needURI) throws NoSuchNeedException
  {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  /**
   * Loads the specified need from the database and raises an exception if it is not found.
   *
   * @param needURI
   * @throws won.protocol.exception.NoSuchNeedException
   * @return the connection
   */
  private Need loadNeed(final URI needURI) throws NoSuchNeedException
  {
    //TODO: load need object from db
    Need need = new Need();
    if (need == null) throw new NoSuchNeedException(needURI);
    return need;
  }

}
