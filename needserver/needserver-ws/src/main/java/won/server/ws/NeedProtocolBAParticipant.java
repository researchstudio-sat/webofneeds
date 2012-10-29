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

import com.arjuna.wst.BusinessAgreementWithParticipantCompletionParticipant;
import com.arjuna.wst.FaultedException;
import com.arjuna.wst.SystemException;
import com.arjuna.wst.WrongStateException;
import com.arjuna.wst11.ConfirmCompletedParticipant;

import java.io.Serializable;
import java.util.HashMap;


/**
 * @author Fabian Salcher
 * @version 2012/10/29
 */

public class NeedProtocolBAParticipant implements
    BusinessAgreementWithParticipantCompletionParticipant,
    ConfirmCompletedParticipant,
    Serializable
{

  public void close() throws WrongStateException, SystemException
  {
    NeedProtocol np = NeedProtocol.getSingleton();
    np.closed(null);
    System.out.println("received close directive");
  }

  public void cancel() throws FaultedException, WrongStateException, SystemException
  {
    NeedProtocol np = NeedProtocol.getSingleton();
    np.faulted(null);
    System.out.println("received cancel directive");
  }

  public void compensate() throws FaultedException, WrongStateException, SystemException
  {
    NeedProtocol np = NeedProtocol.getSingleton();
    np.compensate(null);
    System.out.println("received compensate directive");
  }

  public String status() throws SystemException
  {
    System.out.println("received status directive");
    return null;
  }

  @Deprecated
  public void unknown() throws SystemException
  {
    System.out.println("received unknown directive");
  }

  public void error() throws SystemException
  {
    NeedProtocol np = NeedProtocol.getSingleton();
    np.faulted(null);
    System.out.println("received error directive");
  }

  public void confirmCompleted(final boolean b)
  {
  }

  public static synchronized void recordParticipant(String txID, NeedProtocolBAParticipant participant)
  {
    participants.put(txID, participant);
  }

  public static synchronized NeedProtocolBAParticipant removeParticipant(String txID)
  {
    return participants.remove(txID);
  }

  public static synchronized NeedProtocolBAParticipant getParticipant(String txID)
  {
    return participants.get(txID);
  }

  private static HashMap<String, NeedProtocolBAParticipant> participants = new HashMap<String, NeedProtocolBAParticipant>();

}
