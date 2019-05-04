/*
 * Copyright 2012 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.bot.framework.eventbot.action.impl.debugbot;

import java.net.URI;
import java.util.Date;

import won.bot.framework.eventbot.EventListenerContext;

/**
 * Created by fkleedorfer on 10.06.2016.
 */
public class MessageTimingManager {
    public static final String KEY_LAST_MESSAGE_IN_TIMESTAMPS = "lastMessageInTimestamps";
    public static final String KEY_LAST_MESSAGE_OUT_TIMESTAMPS = "lastMessageOutTimestamps";
    private EventListenerContext context;
    private int maxInstances;

    public MessageTimingManager(final EventListenerContext context, final int maxInstances) {
        this.context = context;
        this.maxInstances = maxInstances;
    }

    public static enum InactivityPeriod {
        ACTIVE(60 * 1000, 60 * 1000), SHORT(5 * 60 * 1000, 60 * 1000), LONG(10 * 60 * 1000, 120 * 1000),
        TOO_LONG(-1, 120 * 1000);
        InactivityPeriod(final long timeout, final long minimalPauseBetweenMessages) {
            this.timeout = timeout;
            this.minimalPauseBetweenMessages = minimalPauseBetweenMessages;
        }

        private long timeout;
        private long minimalPauseBetweenMessages;

        public long getTimeout() {
            return timeout;
        }

        public long getMinimalPauseBetweenMessages() {
            return minimalPauseBetweenMessages;
        }

        public boolean isWithin(long inactivityInMillis) {
            return inactivityInMillis <= timeout;
        }

        public boolean isPauseLongEnough(long pauseLengthInMIllis) {
            return pauseLengthInMIllis >= minimalPauseBetweenMessages;
        }

        public static InactivityPeriod getInactivityPeriod(Date lastAction) {
            if (lastAction == null)
                return TOO_LONG;
            return getInactivityPeriod(lastAction, new Date());
        }

        public static InactivityPeriod getInactivityPeriod(Date lastAction, Date timeToCompare) {
            if (lastAction == null)
                return TOO_LONG;
            if (timeToCompare == null)
                return TOO_LONG;
            long diff = timeToCompare.getTime() - lastAction.getTime();
            if (ACTIVE.isWithin(diff))
                return ACTIVE;
            if (SHORT.isWithin(diff))
                return SHORT;
            if (LONG.isWithin(diff))
                return LONG;
            return TOO_LONG;
        }
    }

    public boolean isWaitedLongEnough(URI connectionUri) {
        Date lastSent = (Date) context.getBotContext().loadFromObjectMap(KEY_LAST_MESSAGE_OUT_TIMESTAMPS,
                        connectionUri.toString());
        if (lastSent == null)
            return false; // avoid sending messages on every actEvent if too many atoms are connected
        return getInactivityPeriodOfPartner(connectionUri)
                        .isPauseLongEnough(System.currentTimeMillis() - lastSent.getTime());
    }

    public InactivityPeriod getInactivityPeriodOfPartner(URI connectionUri) {
        Date lastIn = (Date) context.getBotContext().loadFromObjectMap(KEY_LAST_MESSAGE_IN_TIMESTAMPS,
                        connectionUri.toString());
        return InactivityPeriod.getInactivityPeriod(lastIn);
    }

    public InactivityPeriod getInactivityPeriodOfSelf(URI connectionUri) {
        Date lastOut = (Date) context.getBotContext().loadFromObjectMap(KEY_LAST_MESSAGE_OUT_TIMESTAMPS,
                        connectionUri.toString());
        return InactivityPeriod.getInactivityPeriod(lastOut);
    }

    public void updateMessageTimeForMessageSent(URI connectionUri) {
        context.getBotContext().saveToObjectMap(KEY_LAST_MESSAGE_OUT_TIMESTAMPS, connectionUri.toString(), new Date());
    }

    public void updateMessageTimeForMessageReceived(URI connectionUri) {
        context.getBotContext().saveToObjectMap(KEY_LAST_MESSAGE_IN_TIMESTAMPS, connectionUri.toString(), new Date());
    }
}
