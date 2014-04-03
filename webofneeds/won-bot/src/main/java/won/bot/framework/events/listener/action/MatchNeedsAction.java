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

package won.bot.framework.events.listener.action;

import won.bot.framework.events.listener.EventListenerContext;

import java.net.URI;
import java.util.List;

/**
 * EventBotAction that sends a hint message to the first need in the context to the second.
 */
public class MatchNeedsAction extends EventBotAction {
  public MatchNeedsAction(final EventListenerContext eventListenerContext)
  {
    super(eventListenerContext);
  }

  @Override
  protected void doRun() throws Exception{
    List<URI> needs = getEventListenerContext().getBotContext().listNeedUris();
    URI need1 = needs.get(0);
    URI need2 = needs.get(1);
    logger.debug("matching needs {} and {}",need1,need2);
    logger.debug("getEventListnerContext():"+getEventListenerContext());
    logger.debug("getMatcherService(): "+getEventListenerContext().getMatcherService());
    getEventListenerContext().getMatcherService().hint(need1,need2,1.0, URI.create("http://localhost:8080/matcher"),null);
  }
}
