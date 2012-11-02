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

package won.matcher.protocol.local;

import won.protocol.exception.NoSuchNeedException;
import won.protocol.matcher.MatcherToNodeSender;
import won.protocol.matcher.NodeFromMatcherReceiver;

import java.net.URI;
import java.util.Collection;

/**
 * Implementation for testing purposes; communicates only with partners within the same VM.
 */
public class MatcherToNodeSenderLocalMatchEverythingImpl implements MatcherToNodeSender
{
  private NodeFromMatcherReceiver receiver;

  @Override
  public void sendHint(final URI needURI, final URI otherNeed, final double score, final URI originator) throws NoSuchNeedException
  {
    this.receiver.hint(needURI,otherNeed,score,originator);
  }

  @Override
  public Collection<URI> sendListNeedURIs()
  {
    return receiver.listNeedURIs();
  }

  @Override
  public Collection<URI> sendListConnectionURIs(final URI needURI) throws NoSuchNeedException
  {
    return receiver.listConnectionURIs(needURI);
  }

  public void setReceiver(final NodeFromMatcherReceiver receiver)
  {
    this.receiver = receiver;
  }
}
