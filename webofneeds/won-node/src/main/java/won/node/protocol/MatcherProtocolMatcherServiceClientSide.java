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

package won.node.protocol;

import com.hp.hpl.jena.rdf.model.Model;

import java.net.URI;

/**
 * User: LEIH-NB
 * Date: 10.04.14
 */
public interface MatcherProtocolMatcherServiceClientSide
{

  public void matcherRegistered(final URI wonNodeURI);

  public void needCreated(final URI needURI, final Model content);

  public void needActivated(final URI needURI);

  public void needDeactivated(final URI needURI);
}
