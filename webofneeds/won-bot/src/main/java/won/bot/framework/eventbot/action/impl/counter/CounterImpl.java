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

package won.bot.framework.eventbot.action.impl.counter;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Counter that is intended to be shared between actions and listeners.
 */
public class CounterImpl implements Counter {
    private AtomicInteger count;
    private String name;

    public CounterImpl(String name, final int initialCount) {
        this.count = new AtomicInteger(initialCount);
        this.name = name;
    }

    public CounterImpl(String name) {
        this(name, 0);
    }

    @Override
    public int getCount() {
        return count.get();
    }

    @Override
    public int increment() {
        return count.incrementAndGet();
    }

    @Override
    public int decrement() {
        return count.decrementAndGet();
    }

    public String getName() {
        return name;
    }

    /**
     * Resets the counter to 0.
     * 
     * @return the counter's value obtained when resetting it.
     */
    public int reset() {
        return count.getAndSet(0);
    }
}
