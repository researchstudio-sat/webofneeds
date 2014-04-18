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

package won.bot.integration;

import com.hp.hpl.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import won.bot.framework.bot.Bot;
import won.bot.framework.manager.BotManager;
import won.matcher.protocol.MatcherProtocolMatcherServiceCallback;

import java.net.URI;
import java.util.Date;

/**
 * OwnerProtocolOwnerServiceCallback that dispatches the calls to the bots.
 */
public class BotMatcherProtocolMatcherServiceCallback implements MatcherProtocolMatcherServiceCallback
{
  private final Logger logger = LoggerFactory.getLogger(getClass());
  BotManager botManager;

  TaskScheduler taskScheduler;


  public void setBotManager(BotManager botManager) {
    this.botManager = botManager;
  }

  public void setTaskScheduler(TaskScheduler taskScheduler) {
    this.taskScheduler = taskScheduler;
  }


  private Bot getBotForNeedUri(URI needUri) {
    Bot bot = botManager.getBotForNeedURI(needUri);
    if (bot == null) throw new IllegalStateException("No bot registered for uri " + needUri);
    if (!bot.getLifecyclePhase().isActive()) {
      throw new IllegalStateException("bot responsible for need " + needUri + " is not active (lifecycle phase is: " +bot.getLifecyclePhase()+")");
    }
    return bot;
  }

  @Override
  public void onNewNeed(final URI needURI, final Model content) {
    taskScheduler.schedule(new Runnable(){
      public void run(){
        try {
          logger.debug("onNewNeed for need {} ",needURI.toString());
          getBotForNeedUri(needURI).onNewNeedCreatedNotificationForMatcher(needURI, content);
      //    getBotForNeedUri(needURI.getNeedURI()).onMessageFromOtherNeed(con, message, content);
        } catch (Exception e) {
          logger.warn("error while handling onNewNeed()",e);
        }
      }
    }, new Date());
  }

  @Override
  public void onNeedActivated(final URI needURI) {
    taskScheduler.schedule(new Runnable(){
      public void run(){
        try {
          logger.debug("onNeedActivated for need {} ",needURI.toString());
          getBotForNeedUri(needURI).onNeedActivatedNotificationForMatcher(needURI);
          //    getBotForNeedUri(needURI.getNeedURI()).onMessageFromOtherNeed(con, message, content);
        } catch (Exception e) {
          logger.warn("error while handling onNeedActivated()",e);
        }
      }
    }, new Date());
  }

  @Override
  public void onNeedDeactivated(final URI needURI) {
    taskScheduler.schedule(new Runnable(){
      public void run(){
        try {
          logger.debug("onNeedDeactivated for need {} ",needURI.toString());
          getBotForNeedUri(needURI).onNeedDeactivatedNotificationForMatcher(needURI);
          //    getBotForNeedUri(needURI.getNeedURI()).onMessageFromOtherNeed(con, message, content);
        } catch (Exception e) {
          logger.warn("error while handling onNeedDeactivated()",e);
        }
      }
    }, new Date());
  }
}
