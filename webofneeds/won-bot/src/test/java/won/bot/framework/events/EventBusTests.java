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

package won.bot.framework.events;

import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import won.bot.framework.eventbot.bus.impl.AsyncEventBusImpl;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.listener.EventListener;

/**
 * User: fkleedorfer Date: 30.01.14
 */
public class EventBusTests {
    private Logger logger = LoggerFactory.getLogger(getClass());
    private ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();

    @Before
    public void setup() {
        taskScheduler.initialize();
    }

    @After
    public void tearDown() {
        taskScheduler.shutdown();
    }

    @Test
    public void testEventBusSimpleCase() throws InterruptedException {
        AsyncEventBusImpl bus = new AsyncEventBusImpl(this.taskScheduler);
        CountDownLatch countDownLatch = new CountDownLatch(9);
        bus.subscribe(TestEventA.class, new ListenerA(countDownLatch));
        bus.subscribe(TestEventB.class, new ListenerB(countDownLatch));
        bus.subscribe(TestEventB.class, new ListenerB(countDownLatch));
        bus.publish(new TestEventA());
        bus.publish(new TestEventB());
        bus.publish(new TestEventA());
        bus.publish(new TestEventB());
        bus.publish(new TestEventA());
        bus.publish(new TestEventB());
        countDownLatch.await();
    }

    private class TestEventA implements Event {
        @Override
        public String toString() {
            return "TestEventA";
        }
    }

    private class TestEventB implements Event {
        @Override
        public String toString() {
            return "TestEventB";
        }
    }

    private class ListenerA implements EventListener {
        private final CountDownLatch countDownLatch;

        public ListenerA(final CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void onEvent(final Event event) throws Exception {
            logger.debug("ListenerA: processing event {}", event);
            countDownLatch.countDown();
        }
    }

    private class ListenerB implements EventListener {
        private final CountDownLatch countDownLatch;

        public ListenerB(final CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void onEvent(final Event event) throws Exception {
            logger.debug("ListenerB: processing event {}", event);
            countDownLatch.countDown();
        }
    }
}
