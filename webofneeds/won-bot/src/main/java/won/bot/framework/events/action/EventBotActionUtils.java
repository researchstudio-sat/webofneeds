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

package won.bot.framework.events.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.events.EventListenerContext;

import java.net.URI;

/**
 * User: fkleedorfer
 * Date: 02.02.14
 */
public class EventBotActionUtils
{
  private static Logger logger = LoggerFactory.getLogger(EventBotActionUtils.class);
  public static void rememberInListIfNamePresent(EventListenerContext ctx ,URI uri, String uriListName) {
      if (uriListName != null && uriListName.trim().length() > 0){
          ctx.getBotContext().appendToNamedNeedUriList(uri, uriListName);
        logger.debug("remembering need in NamedNeedList {} ", uri);
      } else {
          ctx.getBotContext().rememberNeedUri(uri);
        logger.debug("remembering need in List {} ", uri);
      }
  }

  public static void rememberInNodeListIfNamePresent(EventListenerContext ctx, URI uri){
    ctx.getBotContext().rememberNodeUri(uri);
  }

  public static void removeFromListIfNamePresent(EventListenerContext ctx ,URI uri, String uriListName) {
    if (uriListName != null && uriListName.trim().length() > 0){
      ctx.getBotContext().removeNeedUriFromNamedNeedUriList(uri, uriListName);
      logger.debug("removing need from NamedNeedList {} ", uri);
    } else {
      ctx.getBotContext().removeNeedUri(uri);
      logger.debug("removed need from bot context {} ", uri);
    }
  }
}
