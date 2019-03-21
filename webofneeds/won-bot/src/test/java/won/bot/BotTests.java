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

package won.bot;

import org.junit.Assert;
import org.junit.Test;
import won.bot.framework.bot.Bot;
import won.bot.impl.DebugBot;

import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * User: fkleedorfer Date: 17.01.14
 */
public class BotTests {
  @Test
  public void testIsInitialized() {
    Bot bot = new DebugBot();
    try {
      bot.initialize();
    } catch (Exception e) {
      e.printStackTrace();
    }
    Assert.assertTrue(bot.getLifecyclePhase().isActive());
  }

  /**
   * Makes sure that the initialize cannot be entered by more than one thread.
   */
  @Test
  public void testIsInitializedMultiThreaded() throws BrokenBarrierException, InterruptedException {

    // set of threads that managed to enter the initialize method
    final Set<Thread> threadsInInit = Collections.synchronizedSet(new HashSet<Thread>());

    // bot impl that remembers which thread entered the initialize method
    final Bot bot = new DebugBot() {
      @Override
      protected void doInitialize() {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        threadsInInit.add(Thread.currentThread());
      }
    };

    // start 10 threads that initialize the bot
    int numThreads = 10;
    final Random rnd = new Random(System.currentTimeMillis());
    final CyclicBarrier barrier = new CyclicBarrier(numThreads + 1);
    for (int i = 0; i < numThreads; i++) {
      Thread thread = new Thread() {
        @Override
        public void run() {
          try {
            Thread.sleep(rnd.nextInt(100));
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          try {
            bot.initialize();
          } catch (Exception e) {
            e.printStackTrace();
          }
          try {
            barrier.await();
          } catch (InterruptedException e) {
            e.printStackTrace();
          } catch (BrokenBarrierException e) {
            e.printStackTrace();
          }
        }
      };
      thread.start();
    }
    barrier.await();

    // make sure the bot is initialized
    Assert.assertTrue(bot.getLifecyclePhase().isActive());
    // make sure it was initialized only once
    Assert.assertTrue(threadsInInit.size() == 1);
  }

  @Test
  public void testIsShutdown() {
    Bot bot = new DebugBot();
    try {
      bot.shutdown();
    } catch (Exception e) {
      e.printStackTrace();
    }
    Assert.assertTrue(bot.getLifecyclePhase().isDown());
  }

  /**
   * Makes sure that the shutdown cannot be entered by more than one thread.
   */
  @Test
  public void testIsShutdownMultiThreaded() throws BrokenBarrierException, InterruptedException {

    // set of threads that managed to enter the shutdown method
    final Set<Thread> threadsInShutdown = Collections.synchronizedSet(new HashSet<Thread>());

    // bot impl that remembers which thread entered the shutdown method
    final Bot bot = new DebugBot() {
      @Override
      protected void doShutdown() {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        threadsInShutdown.add(Thread.currentThread());
      }
    };

    // start 10 threads that shut down the bot
    int numThreads = 10;
    final Random rnd = new Random(System.currentTimeMillis());
    final CyclicBarrier barrier = new CyclicBarrier(numThreads + 1);
    for (int i = 0; i < numThreads; i++) {
      Thread thread = new Thread() {
        @Override
        public void run() {
          try {
            Thread.sleep(rnd.nextInt(100));
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          try {
            bot.shutdown();
          } catch (Exception e) {
            e.printStackTrace();
          }
          try {
            barrier.await();
          } catch (InterruptedException e) {
            e.printStackTrace();
          } catch (BrokenBarrierException e) {
            e.printStackTrace();
          }
        }
      };
      thread.start();
    }
    barrier.await();

    // make sure the bot is shutdownd
    Assert.assertTrue(bot.getLifecyclePhase().isDown());
    // make sure it was shutdownd only once
    Assert.assertTrue(threadsInShutdown.size() == 1);
  }
}
