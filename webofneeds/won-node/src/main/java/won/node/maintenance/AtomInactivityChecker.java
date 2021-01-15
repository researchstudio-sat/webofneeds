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
package won.node.maintenance;

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
import won.node.service.nodebehaviour.AtomManagementService;
import won.protocol.model.Atom;
import won.protocol.repository.AtomRepository;

import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Uses a timer to check atoms for inactivity and send them warnings or
 * deactivate them if they have been inactive for too long.
 */
@Component
public class AtomInactivityChecker implements InitializingBean, DisposableBean {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private Trigger trigger;
    private TaskScheduler taskScheduler;
    private int inactivityCheckInterval = -1;
    private int warnTimeout = -1;
    private int deactivateTimeout = -1;
    private int deactivateTimeoutDespiteEstablishedConnections = -1;
    private InactivityCheckTask inactivityCheckTask = null;
    @Autowired
    private AtomRepository atomRepository;
    @Autowired
    private AtomManagementService atomManagementService;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (this.taskScheduler == null) {
            throw new IllegalStateException("taskScheduler must be set");
        }
        if (this.inactivityCheckInterval <= 0) {
            return;
        }
        PeriodicTrigger periodicTrigger = new PeriodicTrigger(this.inactivityCheckInterval, TimeUnit.SECONDS);
        periodicTrigger.setInitialDelay(this.inactivityCheckInterval);
        this.trigger = periodicTrigger;
        this.inactivityCheckTask = new InactivityCheckTask();
        taskScheduler.schedule(this.inactivityCheckTask, trigger);
        if (logger.isDebugEnabled()) {
            logger.debug("setting up inactivity checker to check inactivity every {} seconds, warn after {} seconds and deactivate after {} seconds",
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
                // select atoms that match our criteria:
                Pageable firstPage = PageRequest.of(0, 100);
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
                logger.debug("warn start-date: {}, warn end-date: {}, deactivate cut-off date {}",
                                new Object[] { simpleDateFormat.format(startWarningThreshold),
                                                simpleDateFormat.format(stopWarningThreshold),
                                                simpleDateFormat.format(deactivateThreshold) });
                String warningMessage = "This posting does not have active connections, nor has it seen any activity "
                                + "from your side in the last " + getTimeString(warnTimeout)
                                + ". It will be deactivated if there continues " + "to be no activity for more than "
                                + getTimeString(deactivateTimeout - warnTimeout) + ". Automatic deactivation is "
                                + "done to clean up abandoned postings. You can reactivate your posting at any time.";
                String deactivateMessage = "This posting is deactivated because it has no active connections, nor has "
                                + "there been any activity from your side in the last "
                                + getTimeString(deactivateTimeout) + ". Automatic deactivation is done to "
                                + "clean up abandoned postings. You can reactivate your posting at any time.";
                String deactivateDespiteEstablishedConnectionsMessage = "This posting is deactivated because there "
                                + "has not been any activity from your side in the last "
                                + getTimeString(deactivateTimeoutDespiteEstablishedConnections)
                                + ". Automatic deactivation is done to "
                                + "clean up abandoned postings. You can reactivate your posting at any time.";
                final AtomicInteger warned = new AtomicInteger(0);
                final AtomicInteger deactivated = new AtomicInteger(0);
                Slice<Atom> atomsToWarn = atomRepository.findAtomsInactiveBetweenAndNotConnected(startWarningThreshold,
                                stopWarningThreshold, firstPage);
                do {
                    if (cancelled.get()) {
                        return;
                    }
                    atomsToWarn.forEach(atom -> {
                        try {
                            if (cancelled.get()) {
                                return;
                            }
                            warned.incrementAndGet();
                            logger.debug("Sending warning to atom {} ", atom.getAtomURI());
                            atomManagementService.sendTextMessageToOwner(atom.getAtomURI(), warningMessage);
                        } catch (Exception e) {
                            logger.warn("Caught and swallowed exception during warning an inactive atom", e);
                        }
                    });
                    if (cancelled.get()) {
                        return;
                    }
                    if (atomsToWarn.hasNext()) {
                        Pageable pageable = atomsToWarn.nextPageable();
                        atomsToWarn = atomRepository.findAtomsInactiveBetweenAndNotConnected(startWarningThreshold,
                                        stopWarningThreshold, pageable);
                    } else {
                        atomsToWarn = null;
                    }
                } while (atomsToWarn != null && atomsToWarn.hasContent());
                Slice<Atom> atomsToDeactivate = atomRepository
                                .findAtomsInactiveSinceAndNotConnected(deactivateThreshold, firstPage);
                do {
                    if (cancelled.get()) {
                        return;
                    }
                    atomsToDeactivate.forEach(atom -> {
                        try {
                            if (cancelled.get()) {
                                return;
                            }
                            deactivated.incrementAndGet();
                            logger.debug("Deactivating atom {} ", atom.getAtomURI());
                            atomManagementService.deactivateAtom(atom.getAtomURI(), deactivateMessage);
                        } catch (Exception e) {
                            logger.warn("Caught and swallowed exception during deactivating an inactive atom", e);
                        }
                    });
                    if (cancelled.get()) {
                        return;
                    }
                    if (atomsToDeactivate.hasNext()) {
                        Pageable pageable = atomsToDeactivate.nextPageable();
                        atomsToDeactivate = atomRepository.findAtomsInactiveSinceAndNotConnected(deactivateThreshold,
                                        pageable);
                    } else {
                        atomsToDeactivate = null;
                    }
                } while (atomsToDeactivate != null && atomsToDeactivate.hasContent());
                atomsToDeactivate = atomRepository
                                .findAtomsInactiveSince(deactivateThresholdDespiteEstablishedConnections, firstPage);
                do {
                    if (cancelled.get()) {
                        return;
                    }
                    atomsToDeactivate.forEach(atom -> {
                        try {
                            if (cancelled.get()) {
                                return;
                            }
                            deactivated.incrementAndGet();
                            logger.debug("Deactivating atom {} ", atom.getAtomURI());
                            atomManagementService.deactivateAtom(atom.getAtomURI(),
                                            deactivateDespiteEstablishedConnectionsMessage);
                        } catch (Exception e) {
                            logger.warn("Caught and swallowed exception during deactivating an inactive atom", e);
                        }
                    });
                    if (cancelled.get()) {
                        return;
                    }
                    if (atomsToDeactivate.hasNext()) {
                        Pageable pageable = atomsToDeactivate.nextPageable();
                        atomsToDeactivate = atomRepository.findAtomsInactiveSince(
                                        deactivateThresholdDespiteEstablishedConnections, pageable);
                    } else {
                        atomsToDeactivate = null;
                    }
                } while (atomsToDeactivate != null && atomsToDeactivate.hasContent());
                logger.info("Inactivity check finished. Sent warning to {} atoms, deactivated {} atoms", warned.get(),
                                deactivated.get());
            } catch (Throwable t) {
                logger.warn("Caught an error during the inactivity check, which may have aborted the complete procedure, not just an individual check",
                                t);
            }
        }

        public void cancel() {
            this.cancelled.set(true);
        }

        private String getTimeString(int seconds) {
            Duration duration = Duration.ofSeconds(seconds);
            if (seconds <= 0) {
                return "bogus time";
            }
            if (seconds < 60) {
                return singularOrPlural(seconds, "second", "seconds");
            }
            if (seconds < 3600) {
                return singularOrPlural(duration.toMinutes(), "minute", "minutes");
            }
            if (seconds < 86400) {
                return singularOrPlural(duration.toHours(), "hour", "hours");
            } else {
                return singularOrPlural(duration.toDays(), "day", "days");
            }
        }

        private String singularOrPlural(long value, String singular, String plural) {
            return value == 1 ? singular : value + " " + plural;
        }
    }
}
