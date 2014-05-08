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

package won.bot.framework.events.action.impl;

import won.bot.framework.events.EventListenerContext;
import won.bot.framework.events.action.BaseEventBotAction;
import won.bot.framework.events.action.EventBotActionUtils;
import won.bot.framework.events.event.Event;

import java.net.URI;
import java.util.Iterator;

/**
* User: fkleedorfer
* Date: 28.03.14
*/
public class RegisterMatcherAction extends BaseEventBotAction
{
  public RegisterMatcherAction(final EventListenerContext eventListenerContext)
  {
    super(eventListenerContext);
  }


    @Override
  protected void doRun(Event event) throws Exception
  {
    if (getEventListenerContext().getNeedProducer().isExhausted()){
        logger.debug("bot's need procucer is exhausted.");
        return;
    }
    final Iterator wonNodeUriIterator = getEventListenerContext().getMatcherNodeURISource().getNodeURIIterator();

      while (wonNodeUriIterator.hasNext()){
        URI wonNodeUri = (URI)wonNodeUriIterator.next();
        logger.debug("registering matcher on won node {}",wonNodeUri);

        getEventListenerContext().getMatcherProtocolMatcherService().register( wonNodeUri );
        EventBotActionUtils.rememberInNodeListIfNamePresent(getEventListenerContext(),wonNodeUri);
        logger.debug("matcher registered on won node {}",wonNodeUri);
      }
  }


}
