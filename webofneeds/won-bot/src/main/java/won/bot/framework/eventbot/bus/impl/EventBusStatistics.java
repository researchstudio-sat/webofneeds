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

package won.bot.framework.eventbot.bus.impl;

import java.util.Map;


/**
 * Created by fkleedorfer on 30.03.2017.
 */
public class EventBusStatistics {
    private long listenerCount;
    private Map<Class<?>, Long> listenerCountPerEvent;
    private Map<Class<?>, Long> listenerCountPerListenerClass;
    private Map<Class<?>, Long> actionCountPerActionClass;

    public Map<Class<?>, Long> getActionCountPerActionClass() {
        return actionCountPerActionClass;
    }

    public void setActionCountPerActionClass(Map<Class<?>, Long> actionCountPerActionClass) {
        this.actionCountPerActionClass = actionCountPerActionClass;
    }

    public EventBusStatistics() {


    }

    public long getListenerCount() {
        return listenerCount;
    }

    public void setListenerCount(long listenerCount) {
        this.listenerCount = listenerCount;
    }

    public Map<Class<?>, Long> getListenerCountPerEvent() {
        return listenerCountPerEvent;
    }

    public void setListenerCountPerEvent(Map<Class<?>, Long> listenersCountPerEvent) {
        this.listenerCountPerEvent = listenersCountPerEvent;
    }

    public Map<Class<?>, Long> getListenerCountPerListenerClass() {
        return listenerCountPerListenerClass;
    }

    public void setListenerCountPerListenerClass(Map<Class<?>, Long> listenerCountPerListenerClass) {
        this.listenerCountPerListenerClass = listenerCountPerListenerClass;
    }
}
