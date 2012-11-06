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

import com.arjuna.mw.wst11.UserBusinessActivity;
import com.arjuna.mw.wst11.UserBusinessActivityFactory;
import com.arjuna.mw.wst11.client.JaxWSHeaderContextProcessor;
import com.arjuna.wst.SystemException;
import com.arjuna.wst.TransactionRolledBackException;
import com.arjuna.wst.UnknownTransactionException;
import com.arjuna.wst.WrongStateException;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.handler.Handler;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.List;

/**
 * @author Fabian Salcher
 * @version 2012/10/29
 */

public class NeedProtocol
{

  private static NeedProtocol singleton;
  private boolean initialized;

  public NeedProtocol()
  {

  }

  private synchronized void initialize()
  {


    initialized = true;
  }


  public static NeedProtocol getSingleton()
  {
    if (singleton == null) {
      singleton = new NeedProtocol();
    }
    return singleton;
  }

  public NeedTransaction receiveConnectionRequest(URI remoteNeedUri, URI remoteNeedProtocolEndpointUri, URI transactionID)
  {
    return null;
  }

  public void compensate(URI transactionID)
  {

  }

  public void closed(URI transactionID)
  {

  }

  public void faulted(URI transactionID)
  {

  }

  public NeedTransaction connectWithNeed(URI needProtocolEndpointUri)
  {

    UserBusinessActivity userBusinessActivity = UserBusinessActivityFactory.userBusinessActivity();

    try {

      userBusinessActivity.begin();
      System.out.println(userBusinessActivity.transactionIdentifier());

      NeedProtocolWS needProtocolWS = getService(needProtocolEndpointUri.toString());
      // ToDo: do we need the parameters?
      needProtocolWS.connect("http://localhost/need1", "http://localhost/needProtocol/");


    } catch (WrongStateException e) {
      e.printStackTrace();
      return null;
    } catch (SystemException e) {
      e.printStackTrace();
      return null;
    }

    return null;
  }

  public void abort(URI transactionID)
  {
    UserBusinessActivity userBusinessActivity = UserBusinessActivityFactory.userBusinessActivity();
    try {
      userBusinessActivity.cancel();
    } catch (UnknownTransactionException e) {
      e.printStackTrace();
    } catch (SystemException e) {
      e.printStackTrace();
    } catch (WrongStateException e) {
      e.printStackTrace();
    }
  }

  public void close(URI transactionID)
  {
    UserBusinessActivity userBusinessActivity = UserBusinessActivityFactory.userBusinessActivity();
    try {
      userBusinessActivity.close();
    } catch (UnknownTransactionException e) {
      e.printStackTrace();
    } catch (SystemException e) {
      e.printStackTrace();
    } catch (WrongStateException e) {
      e.printStackTrace();
    } catch (TransactionRolledBackException e) {
      e.printStackTrace();
    }
  }

  private NeedProtocolWS getService(String address)
  {


    QName serviceName = new QName(
        "http://webofneeds.org/needProtocol",
        "NeedProtocolWSClientService",
        "wonnp");

    QName endpointName = new QName(
        "http://webofneeds.org/needProtocol",
        "NeedProtocolWSImpl",
        "wonnp");

    URL url = NeedProtocol.class.getResource("/wsdl/NeedProtocol.wsdl");
    Service service = Service.create(url, serviceName);
    NeedProtocolWS port = service.getPort(endpointName, NeedProtocolWS.class);
    BindingProvider bindingProvider = ((BindingProvider) port);
    bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, address);

    Handler handler = new JaxWSHeaderContextProcessor();
    List<Handler> handlers = Collections.singletonList(handler);
    bindingProvider.getBinding().setHandlerChain(handlers);

    return port;
  }
}
