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

package won.owner.service.impl;

import com.hp.hpl.jena.rdf.model.Model;
import won.owner.service.OwnerProtocolOwnerServiceCallback;
import won.protocol.model.ChatMessage;
import won.protocol.model.Connection;
import won.protocol.model.Match;

/**
 * Handler implementation that does nothing. Useful for extending as well as pull-only cases
 * such as a simple Web application.
 */
public class NopOwnerProtocolOwnerServiceCallback implements OwnerProtocolOwnerServiceCallback
{
  @Override
  public void onHint(final Match match, final Model content)
  {

  }

  @Override
  public void onConnect(final Connection con, final Model content)
  {

  }

  @Override
  public void onOpen(final Connection con, final Model content)
  {

  }

  @Override
  public void onClose(final Connection con, final Model content)
  {

  }

  @Override
  public void onTextMessage(Connection con, final ChatMessage message, final Model content)
  {

  }
}
