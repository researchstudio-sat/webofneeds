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

package won.node.maintenance;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Component;

import won.node.service.impl.NeedManagementService;
import won.protocol.model.Need;
import won.protocol.repository.NeedRepository;

/**
 * Uses a timer to check needs for inactivity and send them warnings or
 * deactivate them if they have been inactive for too long.
 */
@Component
public class NeedInactivityChecker implements InitializingBean, DisposableBean {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private Trigger trigger;

  private TaskScheduler taskScheduler;

  private int inactivityCheckInterval = -1;

  private int warnTimeout = -1;

  private int deactivateTimeout = -1;

  private int deactivateTimeoutDespiteEstablishedConnections = -1;

  private InactivityCheckTask inactivityCheckTask = null;

  @Autowired
  private NeedRepository needRepository;

  @Autowired
  private NeedManagementService needManagementService;

  @Override
  public void afterPropertiesSet() throws Exception {
    if (this.taskScheduler == null)
      throw new IllegalStateException("taskScheduler must be set");
    if (this.inactivityCheckInterval <= 0) {
      return;
    }
    PeriodicTrigger periodicTrigger = new PeriodicTrigger(this.inactivityCheckInterval, TimeUnit.SECONDS);
    periodicTrigger.setInitialDelay(this.inactivityCheckInterval);
    this.trigger = periodicTrigger;
    this.inactivityCheckTask = new InactivityCheckTask();
    taskScheduler.schedule(this.inactivityCheckTask, trigger);
    if (logger.isDebugEnabled()) {
      logger.debug(
          "setting up inactivity checker to check inactivity every {} seconds, warn after {} seconds and deactivate after {} seconds",
          new Object[] { inactivityCheckInterval, warnTimeout, deactivateTimeout });
    }
  }

  @Override
  public void destroy() throws Exception {
    if (this.inactivityCheckTask != null) {
      this.inactivityCheckTask.cancel();
    }
  }

  public void setInactivityCheckInterval(int inactivityCheckInterval) {
    this.inactivityCheckInterval = inactivityCheckInterval;
  }

  public void setWarnTimeout(int warnTimeout) {
    this.warnTimeout = warnTimeout;
  }

  public void setDeactivateTimeoutDespiteEstablishedConnections(int deactivateTimeoutDespiteEstablishedConnections) {
    this.deactivateTimeoutDespiteEstablishedConnections = deactivateTimeoutDespiteEstablishedConnections;
  }

  public void setDeactivateTimeout(int deactivateTimeout) {
    this.deactivateTimeout = deactivateTimeout;
  }

  public void setTaskScheduler(TaskScheduler taskScheduler) {
    this.taskScheduler = taskScheduler;
  }

  private class InactivityCheckTask implements Runnable {
    private AtomicBoolean cancelled = new AtomicBoolean(false);

    @Override
    public void run() {
      try {
        logger.debug("starting inactivity check");
        Date now = new Date();
        Calendar calendar = Calendar.getInstance();

        calendar.setTime(now);
        calendar.add(Calendar.SECOND, -warnTimeout);
        Date startWarningThreshold = calendar.getTime();

        calendar.add(Calendar.SECOND, inactivityCheckInterval);
        Date stopWarningThreshold = calendar.getTime();

        calendar.setTime(now);
        calendar.add(Calendar.SECOND, -deactivateTimeout);
        Date deactivateThreshold = calendar.getTime();

        calendar.setTime(now);
        calendar.add(Calendar.SECOND, -deactivateTimeoutDespiteEstablishedConnections);
        Date deactivateThresholdDespiteEstablishedConnections = calendar.getTime();

        // select needs that match our criteria:
        Pageable firstPage = new PageRequest(0, 100);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        logger.debug("warn start-date: {}, warn end-date: {}, deactivate cut-off date {}",
            new Object[] { simpleDateFormat.format(startWarningThreshold),
                simpleDateFormat.format(stopWarningThreshold), simpleDateFormat.format(deactivateThreshold) });

        String warningMessage = "This posting does not have active connections, nor has it seen any activity "
            + "from your side in the last " + getTimeString(warnTimeout)
            + ". It will be deactivated if there continues " + "to be no activity for more than "
            + getTimeString(deactivateTimeout - warnTimeout) + ". Automatic deactivation is "
            + "done to clean up abandoned postings. You can reactivate your posting at any time.";

        String deactivateMessage = "This posting is deactivated because it has no active connections, nor has "
            + "there been any activity from your side in the last " + getTimeString(deactivateTimeout)
            + ". Automatic deactivation is done to "
            + "clean up abandoned postings. You can reactivate your posting at any time.";

        String deactivateDespiteEstablishedConnectionsMessage = "This posting is deactivated because there "
            + "has not been any activity from your side in the last "
            + getTimeString(deactivateTimeoutDespiteEstablishedConnections) + ". Automatic deactivation is done to "
            + "clean up abandoned postings. You can reactivate your posting at any time.";

        final AtomicInteger warned = new AtomicInteger(0);
        final AtomicInteger deactivated = new AtomicInteger(0);
        Slice<Need> needsToWarn = needRepository.findNeedsInactiveBetweenAndNotConnected(startWarningThreshold,
            stopWarningThreshold, firstPage);
        do {
          if (cancelled.get()) {
            return;
          }
          needsToWarn.forEach(need -> {
            try {
              if (cancelled.get()) {
                return;
              }
              warned.incrementAndGet();
              logger.debug("Sending warning to need {} ", need.getNeedURI());
              needManagementService.sendTextMessageToOwner(need.getNeedURI(), warningMessage);
            } catch (Exception e) {
              logger.warn("Caught and swallowed exception during warning an inactive need", e);
            }
          });
          if (cancelled.get()) {
            return;
          }
          if (needsToWarn.hasNext()) {
            Pageable pageable = needsToWarn.nextPageable();
            needsToWarn = needRepository.findNeedsInactiveBetweenAndNotConnected(startWarningThreshold,
                stopWarningThreshold, pageable);
          } else {
            needsToWarn = null;
          }
        } while (needsToWarn != null && needsToWarn.hasContent());

        Slice<Need> needsToDeactivate = needRepository.findNeedsInactiveSinceAndNotConnected(deactivateThreshold,
            firstPage);
        do {
          if (cancelled.get()) {
            return;
          }
          needsToDeactivate.forEach(need -> {
            try {
              if (cancelled.get()) {
                return;
              }
              deactivated.incrementAndGet();
              logger.debug("Deactivating need {} ", need.getNeedURI());
              needManagementService.deactivateNeed(need.getNeedURI(), deactivateMessage);
            } catch (Exception e) {
              logger.warn("Caught and swallowed exception during deactivating an inactive need", e);
            }
          });
          if (cancelled.get()) {
            return;
          }
          if (needsToDeactivate.hasNext()) {
            Pageable pageable = needsToDeactivate.nextPageable();
            needsToDeactivate = needRepository.findNeedsInactiveSinceAndNotConnected(deactivateThreshold, pageable);
          } else {
            needsToDeactivate = null;
          }
        } while (needsToDeactivate != null && needsToDeactivate.hasContent());

        needsToDeactivate = needRepository.findNeedsInactiveSince(deactivateThresholdDespiteEstablishedConnections,
            firstPage);
        do {
          if (cancelled.get()) {
            return;
          }
          needsToDeactivate.forEach(need -> {
            try {
              if (cancelled.get()) {
                return;
              }
              deactivated.incrementAndGet();
              logger.debug("Deactivating need {} ", need.getNeedURI());
              needManagementService.deactivateNeed(need.getNeedURI(), deactivateDespiteEstablishedConnectionsMessage);
            } catch (Exception e) {
              logger.warn("Caught and swallowed exception during deactivating an inactive need", e);
            }
          });
          if (cancelled.get()) {
            return;
          }
          if (needsToDeactivate.hasNext()) {
            Pageable pageable = needsToDeactivate.nextPageable();
            needsToDeactivate = needRepository.findNeedsInactiveSince(deactivateThresholdDespiteEstablishedConnections,
                pageable);
          } else {
            needsToDeactivate = null;
          }
        } while (needsToDeactivate != null && needsToDeactivate.hasContent());
        logger.info("Inactivity check finished. Sent warning to {} needs, deactivated {} needs", warned.get(),
            deactivated.get());
      } catch (Throwable t) {
        logger.warn(
            "Caught an error during the inactivity check, which may have aborted the complete procedure, not just an individual check",
            t);
      }
    }

    public void cancel() {
      this.cancelled.set(true);
    }

    private String getTimeString(int seconds) {
      Duration duration = Duration.ofSeconds(seconds);
      if (seconds <= 0)
        return "bogus time";
      if (seconds < 60)
        return singularOrPlural(seconds, "second", "seconds");
      if (seconds < 3600)
        return singularOrPlural(duration.toMinutes(), "minute", "minutes");
      if (seconds < 86400)
        return singularOrPlural(duration.toHours(), "hour", "hours");
      else
        return singularOrPlural(duration.toDays(), "day", "days");
    }

    private String singularOrPlural(long value, String singular, String plural) {
      return value == 1 ? singular : value + " " + plural;
    }

  }

}
