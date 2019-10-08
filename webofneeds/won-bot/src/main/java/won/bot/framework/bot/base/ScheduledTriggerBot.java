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
package won.bot.framework.bot.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;

import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;

/**
 * Bot that has access to a scheduler for performing recurring or deferred work
 */
public abstract class ScheduledTriggerBot extends BaseBot {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private TaskScheduler taskScheduler;
    private Executor insideSchedulerExecutor = new InsideSchedulerExecutor();
    private Trigger trigger;
    private ScheduledFuture<?> scheduledExecution;

    @Override
    public synchronized void initialize() throws Exception {
        super.initialize();
        if (trigger != null) {
            this.scheduledExecution = getTaskScheduler().schedule(() -> {
                try {
                    ScheduledTriggerBot.this.act();
                } catch (Exception e) {
                    logger.warn("caught exception during triggered execution of act()", e);
                }
            }, trigger);
        } else {
            logger.info("This bot will not fire the ActEvent because no trigger was configured.");
        }
    }

    @Override
    public synchronized void shutdown() throws Exception {
        logger.info("bot is shutting down");
        this.scheduledExecution.cancel(true);
        super.shutdown();
    }

    /**
     * Overrides the inherited method so as to also cancel the trigger when
     * indicating that the bot's work is done.
     */
    @Override
    protected void workIsDone() {
        logger.info("triggered bot signalling workIsDone");
        this.cancelTrigger();
        super.workIsDone();
    }

    /**
     * Returns the TaskScheduler.
     */
    protected TaskScheduler getTaskScheduler() {
        return taskScheduler;
    }

    /**
     * Returns an executor that passes the tasks to the TaskScheduler for immediate
     * execution.
     */
    protected Executor getExecutor() {
        return this.insideSchedulerExecutor;
    }

    public void setTaskScheduler(final TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    /**
     * Returns true if the trigger won't cause any more executions (and none are
     * currently running).
     */
    protected boolean isTriggerDone() {
        return this.scheduledExecution.isDone();
    }

    protected void cancelTrigger() {
        logger.info("canceling trigger");
        scheduledExecution.cancel(true);
    }

    protected ScheduledFuture<?> getScheduledExecution() {
        return scheduledExecution;
    }

    public void setTrigger(final Trigger trigger) {
        this.trigger = trigger;
    }

    private class InsideSchedulerExecutor implements Executor {
        @Override
        public void execute(final Runnable command) {
            getTaskScheduler().schedule(command, new Date());
        }
    }
}
