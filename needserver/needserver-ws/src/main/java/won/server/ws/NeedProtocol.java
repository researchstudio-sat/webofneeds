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

package won.server.ws;

import java.net.URI;


public class NeedProtocol implements NeedProtocolExternalFacade, NeedProtocolInternalFacade
{

  private static NeedProtocol singleton;


  public static NeedProtocol getSingleton()
  {
    if (singleton == null) {
      singleton = new NeedProtocol();
    }
    return singleton;
  }

  public NeedTransaction receiveConnectionRequest(URI remoteNeedUri, URI remoteNeedProtocolEndpointUri, URI transactionID)
  {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public void compensate(URI transactionID)
  {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public void closed(URI transactionID)
  {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public void faulted(URI transactionID)
  {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public NeedTransaction connectWithNeed(URI needProtocolEndpointUri)
  {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public void abort(URI transactionID)
  {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public void close(URI transactionID)
  {
    //To change body of implemented methods use File | Settings | File Templates.
  }
}
