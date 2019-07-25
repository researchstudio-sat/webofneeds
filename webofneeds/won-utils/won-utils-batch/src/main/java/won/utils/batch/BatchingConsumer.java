package won.utils.batch;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Accepts items together with a key for grouping them, a
 * Consumer<Collection<Object>> for consuming them. The consumer's purpose is to
 * balance the following goals
 * <ul>
 * <li>the consumer should be called as soon as possible</li>
 * <li>frequent invocations of the consumer should be avoided</li>
 * </ul>
 * The BatchingConsumer passed <code>items</code> via its <code>accept()</code>
 * method. It consumes them in 'chunks' of one or multiple items at a time. The
 * following properties can be set for each key to govern this behaviour (higher
 * up on this list is more imporant):
 * <ul>
 * <li><code>consumeFirst (boolean)</code>: if set, the filter consumes the
 * accepted item immediately if there is not yet a batch for that key. However,
 * the batch is created and subsequent items with the same key are collected in
 * the batch.</li>
 * <li><code>maxBatchAge (Duration)</code>: the whole batch cannot become older
 * than this value. When it is reached, the whole batch is consumed.</li>
 * <li><code>minChunkInterval (Duration)</code>: wait at least for this duration
 * between chunks</li>
 * <li><code>maxItemInterval (Duration)</code>: consume the current chunk when
 * the instant the last item was accepted is longer ago than this value.</li>
 * <li><code>maxBatchSize (integer)</code>: consume whole batch if its size has
 * reached this value</li>
 * </ul>
 * 
 * @author fkleedorfer
 */
public class BatchingConsumer<K, I> {
    private final Map<K, Batch<K, I>> batches = new HashMap<K, Batch<K, I>>(10);
    ScheduledExecutorService executorSvc = Executors.newSingleThreadScheduledExecutor();
    private Config defaultConfig = new ConfigBuilder()
                    .maxBatchAge(Duration.ofHours(1))
                    .maxItemInterval(Duration.ofMinutes(5))
                    .consumeFirst(false)
                    .maxBatchSize(200)
                    .minChunkInterval(Duration.ofMinutes(15))
                    .build();

    public BatchingConsumer() {
    }

    public BatchingConsumer(Config config) {
        this.defaultConfig = config;
    }

    public void accept(K key, I item, Consumer<Collection<I>> consumer) {
        accept(key, item, consumer, Optional.empty());
    }

    public void accept(K key, I item, Consumer<Collection<I>> consumer, Config config) {
        accept(key, item, consumer, Optional.ofNullable(config));
    }

    /**
     * Offer an item to the BatchingConsumer, providing a consumer and an optional
     * config. The consumer must be specified just in case a new Batch is created in
     * this invocation. If that happens, the consumer is used, otherwise it will not
     * be. The config is optional because the BatchingConsumer has a defaultConfig
     * that is used if no config is specified. Again, the config presented when a
     * new Batch is instantiated will be used.
     * 
     * @param key
     * @param item
     * @param consumer
     * @param config
     */
    public void accept(K key, I item, Consumer<Collection<I>> consumer, Optional<Config> config) {
        Batch<K, I> batch = null;
        synchronized (batches) {
            batch = batches.get(key);
            if (batch == null || batch.isShuttingDown()) {
                batch = new Batch<K, I>(key, consumer, config.orElse(defaultConfig));
                batch.scheduleCleanup();
                batches.put(key, batch);
            }
        }
        batch.add(item);
        batch.rescheduleChunkConsumption();
    }

    /**
     * Invokes the consumers for all batches immediately and removes all batches.
     */
    public void consumeAllBatches() {
        synchronized (batches) {
            batches.values().forEach(batch -> batch.consumeAll(true));
            batches.clear();
        }
    }

    /**
     * Removes the batch, the consumer is not invoked.
     * 
     * @param key
     */
    public void cancelBatch(K key) {
        synchronized (batches) {
            Batch<K, I> batch = batches.get(key);
            if (batch != null) {
                batch.cancelAndCleanup();
            }
        }
    }

    public static class Config {
        public Config(Optional<Duration> maxBatchAge, Optional<Duration> minChunkInterval,
                        Optional<Duration> maxItemInterval, Optional<Boolean> consumeFirst,
                        Optional<Integer> maxBatchSize) {
            this.consumeFirst = consumeFirst;
            this.maxBatchAge = maxBatchAge;
            this.minChunkInterval = minChunkInterval;
            this.maxItemInterval = maxItemInterval;
            this.maxBatchSize = maxBatchSize;
        }

        public final Optional<Duration> maxBatchAge;
        public final Optional<Duration> maxItemInterval;
        public final Optional<Boolean> consumeFirst;
        public final Optional<Duration> minChunkInterval;
        public final Optional<Integer> maxBatchSize;
    }

    public static class ConfigBuilder {
        public ConfigBuilder() {
        }

        private Optional<Duration> maxBatchAge = Optional.empty();
        private Optional<Duration> maxItemInterval = Optional.empty();
        private Optional<Boolean> consumeFirst = Optional.empty();
        private Optional<Duration> minChunkInterval = Optional.empty();
        private Optional<Integer> maxBatchSize = Optional.empty();

        public ConfigBuilder maxBatchAge(Duration d) {
            this.maxBatchAge = Optional.ofNullable(d);
            return this;
        }

        public ConfigBuilder maxItemInterval(Duration d) {
            this.maxItemInterval = Optional.ofNullable(d);
            return this;
        }

        public ConfigBuilder consumeFirst(Boolean b) {
            this.consumeFirst = Optional.ofNullable(b);
            return this;
        }

        public ConfigBuilder minChunkInterval(Duration d) {
            this.minChunkInterval = Optional.ofNullable(d);
            return this;
        }

        public ConfigBuilder maxBatchSize(Integer s) {
            this.maxBatchSize = Optional.ofNullable(s);
            return this;
        }

        public Config build() {
            return new Config(this.maxBatchAge, this.minChunkInterval, this.maxItemInterval, this.consumeFirst,
                            this.maxBatchSize);
        }
    }

    private class Batch<K, I> {
        private final K key;
        private final Queue<I> items = new LinkedList<>();
        private final Config config;
        private Optional<Instant> lastChunkInstant = Optional.empty();
        private Consumer<Collection<I>> consumer;
        private Optional<ScheduledFuture<?>> cleanupTask = Optional.empty();
        private Optional<ScheduledFuture<?>> consumeChunkTask = Optional.empty();
        private final AtomicBoolean firstAdd = new AtomicBoolean(true);
        private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
        private final Object monitor = new Object();

        Batch(K key, Consumer<Collection<I>> consumer, Config config) {
            this.key = key;
            this.config = config;
            this.consumer = consumer;
        }

        /**
         * Adds an item to this batch, possibly triggering consumption.
         * 
         * @param item
         */
        void add(I item) {
            synchronized (monitor) {
                this.lastChunkInstant = Optional.of(Instant.now());
                if (this.config.consumeFirst.orElse(false) && this.firstAdd.compareAndSet(true, false)) {
                    consume(Stream.of(item).collect(Collectors.toList()), false);
                } else {
                    items.add(item);
                    if (config.maxBatchSize.isPresent() && items.size() >= config.maxBatchSize.get()) {
                        consumeAll(false);
                    }
                }
            }
        }

        boolean isShuttingDown() {
            return shuttingDown.get();
        }

        /**
         * Invokes the consumer with the whole batch and removes the batch.
         */
        void consumeAllAndCleanup() {
            synchronized (monitor) {
                this.shuttingDown.set(true);
                consumeAll(false);
                synchronized (batches) {
                    batches.remove(this.key);
                }
                cancelTask(this.consumeChunkTask);
                cancelTask(this.cleanupTask);
            }
        }

        /**
         * Removes the batch without invoking the consumer.
         */
        void cancelAndCleanup() {
            synchronized (monitor) {
                this.shuttingDown.set(true);
                synchronized (batches) {
                    batches.remove(this.key);
                }
                cancelTask(this.consumeChunkTask);
                cancelTask(this.cleanupTask);
            }
        }

        void cancelTask(Optional<ScheduledFuture<?>> task) {
            synchronized (monitor) {
                if (task.isPresent()) {
                    task.get().cancel(false);
                }
            }
        }

        void consumeAll(boolean synchronous) {
            consume(items, synchronous);
        }

        boolean consumeChunk() {
            synchronized (monitor) {
                if (isTooEarlyForChunk()) {
                    return false;
                }
                consumeAll(false);
            }
            return true;
        }

        void consume(Collection<I> itemsToConsume, boolean synchronous) {
            synchronized (monitor) {
                this.lastChunkInstant = Optional.of(Instant.now());
                final Collection<I> consumed = new ArrayList<I>(itemsToConsume.size());
                consumed.addAll(itemsToConsume);
                itemsToConsume.clear();
                if (synchronous) {
                    consumer.accept(consumed);
                } else {
                    executorSvc.execute(() -> {
                        consumer.accept(consumed);
                    });
                }
            }
        }

        boolean isTooEarlyForChunk() {
            if (this.lastChunkInstant.isPresent() && config.minChunkInterval.isPresent()) {
                return Duration.between(this.lastChunkInstant.get(), Instant.now())
                                .compareTo(config.minChunkInterval.get()) < 0;
            } else {
                return false;
            }
        }

        /**
         * Schedules the consumption maxItemInterval in the future. If that does not
         * lead to a chunk consumption because of other configuration settings, it is
         * rescheduled then.
         * 
         * @param batch
         */
        void rescheduleChunkConsumption() {
            if (!this.config.maxItemInterval.isPresent()) {
                return;
            }
            synchronized (monitor) {
                cancelTask(this.consumeChunkTask);
                this.consumeChunkTask = Optional.of(executorSvc.schedule(() -> {
                    boolean consumed = consumeChunk();
                    if (!consumed) {
                        rescheduleChunkConsumption();
                    }
                }, this.config.maxItemInterval.get().toNanos(), TimeUnit.NANOSECONDS));
            }
        }

        /**
         * Schedules the consumption of the batch and its removal from the batches map.
         * 
         * @param batch
         */
        private void scheduleCleanup() {
            if (!this.config.maxBatchAge.isPresent()) {
                return;
            }
            this.cleanupTask = Optional.of(executorSvc.schedule(() -> {
                consumeAllAndCleanup();
            }, this.config.maxBatchAge.get().toNanos(), TimeUnit.NANOSECONDS));
        }
    }
}
