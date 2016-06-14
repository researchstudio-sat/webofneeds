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

package won.bot.framework.eventbot.action.impl.debugbot;

import won.bot.framework.eventbot.EventListenerContext;

import java.net.URI;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by fkleedorfer on 10.06.2016.
 */
public class MessageTimingManager
{
  public static final String KEY_LAST_MESSAGE_IN_TIMESTAMPS = "lastMessageInTimestamps";
  public static final String KEY_LAST_MESSAGE_OUT_TIMESTAMPS = "lastMessageOutTimestamps";
  private EventListenerContext context;
  private int maxInstances;

  public MessageTimingManager(final EventListenerContext context, final int maxInstances) {
    this.context = context;
    this.maxInstances = maxInstances;
  }

  public static enum InactivityPeriod{
    ACTIVE(60*1000),SHORT(5*60*1000), LONG(10*60*1000), TOO_LONG(-1);

    InactivityPeriod(final long timeout) {
      this.timeout = timeout;
    }

    private long timeout;

    public long getTimeout() {
      return timeout;
    }

    public boolean isWithin(long inactivityInMillis){
      return  inactivityInMillis <= timeout;
    }

    public static InactivityPeriod getInactivityPeriod(Date lastAction){
      if (lastAction == null) return TOO_LONG;
      return getInactivityPeriod(lastAction, new Date());
    }

    public static InactivityPeriod getInactivityPeriod(Date lastAction, Date timeToCompare){
      if (lastAction == null) return TOO_LONG;
      if (timeToCompare == null) return TOO_LONG;
      long diff = timeToCompare.getTime() - lastAction.getTime();
      if (ACTIVE.isWithin(diff)) return ACTIVE;
      if (SHORT.isWithin(diff)) return SHORT;
      if (LONG.isWithin(diff)) return LONG;
      return TOO_LONG;
    }
  }

  public InactivityPeriod getInactivityPeriodOfPartner(URI connectionUri){
    Date lastIn = getTimestampMapForKey(KEY_LAST_MESSAGE_IN_TIMESTAMPS).get(connectionUri);
    return InactivityPeriod.getInactivityPeriod(lastIn);
  }

  public InactivityPeriod getInactivityPeriodOfSelf(URI connectionUri){
    Date lastOut = getTimestampMapForKey(KEY_LAST_MESSAGE_OUT_TIMESTAMPS).get(connectionUri);
    return InactivityPeriod.getInactivityPeriod(lastOut);
  }

  public void updateMessageTimeForMessageSent(URI connectionUri){
    getTimestampMapForKey(KEY_LAST_MESSAGE_OUT_TIMESTAMPS).put(connectionUri, new Date());
  }

  public void updateMessageTimeForMessageReceived(URI connectionUri){
    getTimestampMapForKey(KEY_LAST_MESSAGE_IN_TIMESTAMPS).put(connectionUri, new Date());
  }


  private Map<URI, Date> getTimestampMapForKey(String contextKey){
    Map<URI, Date> instances = (Map<URI, Date>) context.getBotContext().get(contextKey);
    if (instances == null){
      instances = new LinkedHashMap<URI, Date>(this.maxInstances, 0.8f, true);
      context.getBotContext().put(contextKey, instances);
    }
    return instances;
  }
}
