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

import com.arjuna.ats.arjuna.common.Uid;
import com.arjuna.mw.wst11.BusinessActivityManagerFactory;
import com.arjuna.wst.SystemException;
import com.arjuna.wst11.BAParticipantManager;
import com.arjuna.mw.wst11.BusinessActivityManager;

import javax.jws.*;
import javax.jws.soap.SOAPBinding;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author Fabian Salcher
 * @version 2012/10/29
 */

@WebService(serviceName = "NeedProtocolWSClientService", portName = "NeedProtocolWSImpl",
    name = "IProtocolService", targetNamespace = "http://webofneeds.org/needProtocol",
    wsdlLocation = "/WEB-INF/wsdl/NeedProtocol.wsdl")
@HandlerChain(file = "/context-handlers.xml", name = "Context Handlers")
@SOAPBinding(style = SOAPBinding.Style.RPC)
public class NeedProtocolWSImpl implements NeedProtocolWS
{

  @WebMethod
  @WebResult(name = "connectResponse", partName = "connectResponse")
  public boolean connect(
      @WebParam(name = "remoteNeedUri", partName = "remoteNeedUri")
      String remoteNeedUriString,

      @WebParam(name = "remoteNeedProtocolEndpointUri", partName = "remoteNeedProtocolEndpointUri")
      String remoteNeedProtocolEndpointUriString)

  {

    System.out.println("[WS] connect has been called...");

    URI remoteNeedUri;
    URI remoteNeedProtocolEndpointUri;
    URI transactionID;
    try {
      remoteNeedUri = new URI(remoteNeedUriString);
      remoteNeedProtocolEndpointUri = new URI(remoteNeedProtocolEndpointUriString);
//      transactionID = new URI(transactionIDString);
    } catch (URISyntaxException e) {
      //ToDo: implement proper error handling
      e.printStackTrace();
      return false;
    }


    BusinessActivityManager activityManager = BusinessActivityManagerFactory.businessActivityManager();
    // get the transaction context of this thread:
    String transactionId = null;
    try {
      transactionId = activityManager.currentTransaction().toString();
    } catch (SystemException e) {
      e.printStackTrace(System.err);
      return false;
    }
    System.out.println("[WS] transactionId: " + transactionId);
    NeedProtocolBAParticipant needProtocolBAParticipant = NeedProtocolBAParticipant.getParticipant(transactionId);

    if (needProtocolBAParticipant != null) {

      System.err.println("[WS] request failed");
      return false;
    }

    BAParticipantManager participantManager;

    // enlist the Participant for this service:
    try {
      needProtocolBAParticipant = new NeedProtocolBAParticipant();
      participantManager = activityManager.enlistForBusinessAgreementWithParticipantCompletion(needProtocolBAParticipant, "org.webofneeds:needProtocol:" + new Uid().toString());
      NeedProtocolBAParticipant.recordParticipant(transactionId, needProtocolBAParticipant);
    } catch (Exception e) {
      System.err.println("Participant enlistment failed");
      e.printStackTrace(System.err);
      return false;
    }

    NeedProtocol needProtocol = NeedProtocol.getSingleton();
    needProtocol.receiveConnectionRequest(remoteNeedUri, remoteNeedProtocolEndpointUri, null);

    return true;
  }


//  @WebMethod
//  @WebResult(name = "acceptConnectResponse", partName = "acceptConnectResponse")
//  public boolean acceptConnect(
//      @WebParam(name = "transactionID", partName = "transactionID")
//      String transactionIDString)
//  {
//    URI transactionID;
//    try {
//      transactionID = new URI(transactionIDString);
//    } catch (URISyntaxException e) {
//      //ToDo: implement proper error handling
//      e.printStackTrace();
//      return false;
//    }
//
//    // ToDo: implement
//
//    return true;
//  }
//
//  @WebMethod
//  @WebResult(name = "refuseConnectResponse", partName = "refuseConnectResponse")
//  public boolean refuseConnect(
//      @WebParam(name = "transactionID", partName = "transactionID")
//      String transactionIDString)
//  {
//    URI transactionID;
//    try {
//      transactionID = new URI(transactionIDString);
//    } catch (URISyntaxException e) {
//      //ToDo: implement proper error handling
//      e.printStackTrace();
//      return false;
//    }
//
//    // ToDo: implement
//
//    return true;
//  }
}
