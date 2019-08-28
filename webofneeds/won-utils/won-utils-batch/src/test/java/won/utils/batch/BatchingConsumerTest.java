package won.utils.batch;

import org.junit.Assert;
import org.junit.Test;
import won.utils.batch.BatchingConsumer.Config;
import won.utils.batch.BatchingConsumer.ConfigBuilder;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class BatchingConsumerTest {
    @Test
    public void testConsumeAllOneKey() {
        BatchingConsumer<String, String> c = new BatchingConsumer<>();
        AtomicInteger counter = new AtomicInteger(0);
        c.accept("key1", "first item", items -> counter.addAndGet(items.size()));
        Assert.assertEquals(0, counter.get());
        c.accept("key1", "second item", items -> counter.set(-1));
        Assert.assertEquals(0, counter.get());
        c.accept("key1", "third item", items -> counter.set(-2));
        Assert.assertEquals(0, counter.get());
        c.accept("key1", "fourth item", items -> counter.set(-3));
        Assert.assertEquals(0, counter.get());
        c.consumeAllBatches();
        Assert.assertEquals(4, counter.get());
    }

    @Test
    public void testConsumeAllOneKeyOneDuplicate() {
        BatchingConsumer<String, String> c = new BatchingConsumer<>();
        AtomicInteger counter = new AtomicInteger(0);
        c.accept("key1", "first item", "a", items -> counter.addAndGet(items.size()));
        Assert.assertEquals(0, counter.get());
        c.accept("key1", "second item", "a", items -> counter.set(-1));
        Assert.assertEquals(0, counter.get());
        c.accept("key1", "third item", "b", items -> counter.set(-2));
        Assert.assertEquals(0, counter.get());
        c.accept("key1", "fourth item", "c", items -> counter.set(-3));
        Assert.assertEquals(0, counter.get());
        c.consumeAllBatches();
        Assert.assertEquals(3, counter.get());
    }

    @Test
    public void testConsumeAllOneKeyNoDuplicatesOneNull() {
        BatchingConsumer<String, String> c = new BatchingConsumer<>();
        AtomicInteger counter = new AtomicInteger(0);
        c.accept("key1", "first item", "a", items -> counter.addAndGet(items.size()));
        Assert.assertEquals(0, counter.get());
        c.accept("key1", "second item", null, items -> counter.set(-1));
        Assert.assertEquals(0, counter.get());
        c.accept("key1", "third item", "b", items -> counter.set(-2));
        Assert.assertEquals(0, counter.get());
        c.accept("key1", "fourth item", "c", items -> counter.set(-3));
        Assert.assertEquals(0, counter.get());
        c.consumeAllBatches();
        Assert.assertEquals(4, counter.get());
    }

    @Test
    public void testConsumeAllTwoKeys() {
        BatchingConsumer<String, String> c = new BatchingConsumer<>();
        AtomicInteger counter1 = new AtomicInteger(0);
        AtomicInteger counter2 = new AtomicInteger(0);
        c.accept("key1", "first item", items -> counter1.addAndGet(items.size()));
        Assert.assertEquals(0, counter1.get());
        Assert.assertEquals(0, counter2.get());
        c.accept("key2", "second item", items -> counter2.addAndGet(items.size()));
        Assert.assertEquals(0, counter1.get());
        Assert.assertEquals(0, counter2.get());
        c.accept("key1", "third item", items -> counter1.addAndGet(items.size()));
        c.accept("key2", "fourth item", items -> counter2.addAndGet(items.size()));
        Assert.assertEquals(0, counter1.get());
        Assert.assertEquals(0, counter2.get());
        c.consumeAllBatches();
        Assert.assertEquals(2, counter1.get());
        Assert.assertEquals(2, counter2.get());
    }

    @Test
    public void testConsumeAllTwoKeysTwoDuplicates() {
        BatchingConsumer<String, String> c = new BatchingConsumer<>();
        AtomicInteger counter1 = new AtomicInteger(0);
        AtomicInteger counter2 = new AtomicInteger(0);
        c.accept("key1", "first item", "a", items -> counter1.addAndGet(items.size()));
        Assert.assertEquals(0, counter1.get());
        Assert.assertEquals(0, counter2.get());
        c.accept("key2", "second item", "b", items -> counter2.addAndGet(items.size()));
        Assert.assertEquals(0, counter1.get());
        Assert.assertEquals(0, counter2.get());
        c.accept("key1", "third item", "a", items -> counter1.addAndGet(items.size()));
        c.accept("key2", "fourth item", "b", items -> counter2.addAndGet(items.size()));
        Assert.assertEquals(0, counter1.get());
        Assert.assertEquals(0, counter2.get());
        c.consumeAllBatches();
        Assert.assertEquals(1, counter1.get());
        Assert.assertEquals(1, counter2.get());
    }

    @Test
    public void testConsumeAllTwoKeysTwoDuplicatesSameDedupkeyDifferentBatches() {
        BatchingConsumer<String, String> c = new BatchingConsumer<>();
        AtomicInteger counter1 = new AtomicInteger(0);
        AtomicInteger counter2 = new AtomicInteger(0);
        c.accept("key1", "first item", "a", items -> counter1.addAndGet(items.size()));
        Assert.assertEquals(0, counter1.get());
        Assert.assertEquals(0, counter2.get());
        c.accept("key2", "second item", "a", items -> counter2.addAndGet(items.size()));
        Assert.assertEquals(0, counter1.get());
        Assert.assertEquals(0, counter2.get());
        c.accept("key1", "third item", "a", items -> counter1.addAndGet(items.size()));
        c.accept("key2", "fourth item", "a", items -> counter2.addAndGet(items.size()));
        Assert.assertEquals(0, counter1.get());
        Assert.assertEquals(0, counter2.get());
        c.consumeAllBatches();
        Assert.assertEquals(1, counter1.get());
        Assert.assertEquals(1, counter2.get());
    }

    @Test
    public void testCancelOneConsumeAllTwoKeys() {
        BatchingConsumer<String, String> c = new BatchingConsumer<>();
        AtomicInteger counter1 = new AtomicInteger(0);
        AtomicInteger counter2 = new AtomicInteger(0);
        c.accept("key1", "first item", items -> counter1.addAndGet(items.size()));
        Assert.assertEquals(0, counter1.get());
        Assert.assertEquals(0, counter2.get());
        c.accept("key2", "second item", items -> counter2.addAndGet(items.size()));
        Assert.assertEquals(0, counter1.get());
        Assert.assertEquals(0, counter2.get());
        c.cancelBatch("key2");
        c.accept("key1", "third item", items -> counter1.addAndGet(items.size()));
        c.accept("key2", "fourth item", items -> counter2.addAndGet(items.size()));
        Assert.assertEquals(0, counter1.get());
        Assert.assertEquals(0, counter2.get());
        c.consumeAllBatches();
        Assert.assertEquals(2, counter1.get());
        Assert.assertEquals(1, counter2.get());
    }

    @Test
    public void testConsumeConsumeFirstTwoKeys() throws Exception {
        BatchingConsumer<String, String> c = new BatchingConsumer<>();
        AtomicInteger counter1 = new AtomicInteger(0);
        AtomicInteger counter2 = new AtomicInteger(0);
        CountDownLock countDown = new CountDownLock();
        Config conf = new ConfigBuilder().consumeFirst(true).build();
        countDown.newRound(1);
        c.accept("key1", "first item", items -> countItems(counter1, countDown, items), conf);
        countDown.await();
        Assert.assertEquals(1, counter1.get());
        Assert.assertEquals(0, counter2.get());
        countDown.newRound(1);
        c.accept("key2", "second item", items -> countItems(counter2, countDown, items), conf);
        countDown.await();
        Assert.assertEquals(1, counter1.get());
        Assert.assertEquals(1, counter2.get());
        c.accept("key1", "third item", items -> countItems(counter1, countDown, items));
        Assert.assertEquals(1, counter1.get());
        Assert.assertEquals(1, counter2.get());
        countDown.newRound(2);
        c.accept("key2", "fourth item", items -> countItems(counter2, countDown, items));
        c.consumeAllBatches();
        countDown.await();
        Assert.assertEquals(2, counter1.get());
        Assert.assertEquals(2, counter2.get());
    }

    @Test
    public void testMaxBatchSizeTwoKeys() throws Exception {
        BatchingConsumer<String, String> c = new BatchingConsumer<>();
        AtomicInteger counter1 = new AtomicInteger(0);
        AtomicInteger counter2 = new AtomicInteger(0);
        CountDownLock countDown = new CountDownLock();
        Config conf = new ConfigBuilder().maxBatchSize(2).build();
        c.accept("key1", "first item", items -> countItems(counter1, countDown, items), conf);
        Assert.assertEquals(0, counter1.get());
        Assert.assertEquals(0, counter2.get());
        c.accept("key2", "second item", items -> countItems(counter2, countDown, items), conf);
        Assert.assertEquals(0, counter1.get());
        Assert.assertEquals(0, counter2.get());
        countDown.newRound(1);
        c.accept("key1", "third item", items -> countItems(counter1, countDown, items));
        countDown.await();
        Assert.assertEquals(2, counter1.get());
        Assert.assertEquals(0, counter2.get());
        countDown.newRound(1);
        c.accept("key2", "fourth item", items -> countItems(counter2, countDown, items));
        countDown.await();
        Assert.assertEquals(2, counter1.get());
        Assert.assertEquals(2, counter2.get());
        c.consumeAllBatches();
        countDown.await();
        Assert.assertEquals(2, counter1.get());
        Assert.assertEquals(2, counter2.get());
    }

    @Test
    public void testMaxBatchAgeTwoKeys() throws Exception {
        BatchingConsumer<String, String> c = new BatchingConsumer<>();
        AtomicInteger counter1 = new AtomicInteger(0);
        AtomicInteger counter2 = new AtomicInteger(0);
        CountDownLock countDown = new CountDownLock();
        Config conf = new ConfigBuilder().maxBatchAge(Duration.ofMillis(100)).build();
        countDown.newRound(2);
        c.accept("key1", "first item", items -> countItems(counter1, countDown, items), conf);
        Assert.assertEquals(0, counter1.get());
        Assert.assertEquals(0, counter2.get());
        c.accept("key2", "second item", items -> countItems(counter2, countDown, items), conf);
        Assert.assertEquals(0, counter1.get());
        Assert.assertEquals(0, counter2.get());
        Thread.sleep(200);
        countDown.await();
        Assert.assertEquals(1, counter1.get());
        Assert.assertEquals(1, counter2.get());
        c.accept("key1", "third item", items -> countItems(counter1, countDown, items));
        Assert.assertEquals(1, counter1.get());
        Assert.assertEquals(1, counter2.get());
        c.accept("key2", "fourth item", items -> countItems(counter2, countDown, items));
        Assert.assertEquals(1, counter1.get());
        Assert.assertEquals(1, counter2.get());
        countDown.newRound(2);
        c.consumeAllBatches();
        countDown.await();
        Assert.assertEquals(2, counter1.get());
        Assert.assertEquals(2, counter2.get());
    }

    @Test
    public void testMaxItemIntervalTwoKeys() throws Exception {
        BatchingConsumer<String, String> c = new BatchingConsumer<>();
        AtomicInteger counter1 = new AtomicInteger(0);
        AtomicInteger counter2 = new AtomicInteger(0);
        CountDownLock countDown = new CountDownLock();
        Config conf = new ConfigBuilder().maxItemInterval(Duration.ofMillis(50)).build();
        countDown.newRound(2);
        c.accept("key1", "first item", items -> countItems(counter1, countDown, items), conf);
        Assert.assertEquals(0, counter1.get());
        Assert.assertEquals(0, counter2.get());
        c.accept("key2", "second item", items -> countItems(counter2, countDown, items), conf);
        Assert.assertEquals(0, counter1.get());
        Assert.assertEquals(0, counter2.get());
        Thread.sleep(200);
        countDown.await();
        Assert.assertEquals(1, counter1.get());
        Assert.assertEquals(1, counter2.get());
        countDown.newRound(1);
        c.accept("key1", "third item", items -> countItems(counter1, countDown, items));
        Thread.sleep(100);
        countDown.await();
        Assert.assertEquals(2, counter1.get());
        Assert.assertEquals(1, counter2.get());
        c.accept("key2", "fourth item", items -> countItems(counter2, countDown, items));
        Assert.assertEquals(2, counter1.get());
        Assert.assertEquals(1, counter2.get());
        countDown.newRound(1);
        c.consumeAllBatches();
        countDown.await();
        Assert.assertEquals(2, counter1.get());
        Assert.assertEquals(2, counter2.get());
    }

    @Test
    public void testMaxItemIntervalMinTimeBetweenChunksTwoKeys() throws Exception {
        BatchingConsumer<String, String> c = new BatchingConsumer<>();
        AtomicInteger counter1 = new AtomicInteger(0);
        AtomicInteger counter2 = new AtomicInteger(0);
        CountDownLock countDown = new CountDownLock();
        Config conf = new ConfigBuilder()
                        .maxItemInterval(Duration.ofMillis(50))
                        .minChunkInterval(Duration.ofMillis(200))
                        .build();
        countDown.newRound(2);
        c.accept("key1", "first item", items -> countItems(counter1, countDown, items), conf);
        Assert.assertEquals(0, counter1.get());
        Assert.assertEquals(0, counter2.get());
        c.accept("key2", "second item", items -> countItems(counter2, countDown, items), conf);
        Assert.assertEquals(0, counter1.get());
        Assert.assertEquals(0, counter2.get());
        Thread.sleep(100);
        Assert.assertEquals(0, counter1.get());
        Assert.assertEquals(0, counter2.get());
        Thread.sleep(150);
        countDown.await();
        Assert.assertEquals(1, counter1.get());
        Assert.assertEquals(1, counter2.get());
        countDown.newRound(1);
        c.accept("key1", "third item", items -> countItems(counter1, countDown, items));
        Thread.sleep(250);
        countDown.await();
        Assert.assertEquals(2, counter1.get());
        Assert.assertEquals(1, counter2.get());
        c.accept("key2", "fourth item", items -> countItems(counter2, countDown, items));
        Assert.assertEquals(2, counter1.get());
        Assert.assertEquals(1, counter2.get());
        countDown.newRound(1);
        c.consumeAllBatches();
        countDown.await();
        Assert.assertEquals(2, counter1.get());
        Assert.assertEquals(2, counter2.get());
    }

    private void countItems(AtomicInteger counter1, CountDownLock countDown, Collection<String> items) {
        counter1.addAndGet(items.size());
        countDown.done();
    }

    private class CountDownLock {
        private CountDownLatch latch;

        public CountDownLock() {
        }

        public void newRound(int count) {
            this.latch = new CountDownLatch(count);
        }

        public void done() {
            this.latch.countDown();
        }

        public void await() throws InterruptedException {
            this.latch.await(1, TimeUnit.SECONDS);
        }
    }
}
