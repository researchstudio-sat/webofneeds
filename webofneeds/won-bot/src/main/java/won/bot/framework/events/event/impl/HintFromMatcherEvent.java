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

package won.bot.framework.events.event.impl;

import com.hp.hpl.jena.rdf.model.Model;
import won.bot.framework.events.event.BaseEvent;
import won.protocol.model.Match;

/**
 *
 */
public class HintFromMatcherEvent extends BaseEvent
{
  private final Match match;
  final Model content;

  public HintFromMatcherEvent(final Match match, final Model content)
  {
    this.match = match;
    this.content = content;
  }

  public Match getMatch()
  {
    return match;
  }

  public Model getContent()
  {
    return content;
  }
}
