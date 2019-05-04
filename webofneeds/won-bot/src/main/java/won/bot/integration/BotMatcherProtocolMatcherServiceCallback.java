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
package won.bot.integration;

import java.net.URI;
import java.util.Date;
import java.util.List;

import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;

import won.bot.framework.bot.Bot;
import won.bot.framework.manager.BotManager;
import won.matcher.protocol.MatcherProtocolMatcherServiceCallback;

/**
 * OwnerProtocolOwnerServiceCallback that dispatches the calls to the bots.
 */
public class BotMatcherProtocolMatcherServiceCallback implements MatcherProtocolMatcherServiceCallback {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    BotManager botManager;
    TaskScheduler taskScheduler;

    public void setBotManager(BotManager botManager) {
        this.botManager = botManager;
    }

    public void setTaskScheduler(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    private Bot getBotForAtomUri(URI atomUri) {
        Bot bot = botManager.getBotForAtomURI(atomUri);
        if (bot == null)
            throw new IllegalStateException("No bot registered for uri " + atomUri);
        if (!bot.getLifecyclePhase().isActive()) {
            throw new IllegalStateException("bot responsible for atom " + atomUri
                            + " is not active (lifecycle phase is: " + bot.getLifecyclePhase() + ")");
        }
        return bot;
    }

    private List<Bot> getBotsForNodeUri(URI nodeUri) {
        List<Bot> bots = botManager.getBotsForNodeURI(nodeUri);
        if (bots.size() == 0)
            throw new IllegalStateException("No bot registered for uri " + nodeUri);
        for (int i = bots.size() - 1; i >= 0; i--) {
            Bot bot = bots.get(i);
            if (!bot.getLifecyclePhase().isActive()) {
                bots.remove(i);
                throw new IllegalStateException("bot registered for " + nodeUri + " is not active (lifecycle phase is: "
                                + bot.getLifecyclePhase() + ")");
            }
        }
        return bots;
    }

    @Override
    public void onRegistered(final URI wonNodeUri) {
        taskScheduler.schedule(new Runnable() {
            public void run() {
                try {
                    List<Bot> bots = getBotsForNodeUri(wonNodeUri);
                    for (int i = 0; i < bots.size(); i++) {
                        logger.debug("bot {} matcher registered on wonNode {}", bots.get(i), wonNodeUri.toString());
                        bots.get(i).onMatcherRegistered(wonNodeUri);
                    }
                } catch (Exception e) {
                    logger.warn("error while handling onRegistered()", e);
                }
            }
        }, new Date());
    }

    @Override
    public void onNewAtom(final URI wonNodeURI, final URI atomURI, final Dataset content) {
        taskScheduler.schedule(new Runnable() {
            public void run() {
                try {
                    List<Bot> bots = getBotsForNodeUri(wonNodeURI);
                    for (int i = 0; i < bots.size(); i++) {
                        logger.debug("bot {} matcher registered on wonNode {}", bots.get(i), wonNodeURI.toString());
                        bots.get(i).onNewAtomCreatedNotificationForMatcher(wonNodeURI, atomURI, content);
                    }
                } catch (Exception e) {
                    logger.warn("error while handling onRegistered()", e);
                }
            }
        }, new Date());
    }

    @Override
    public void onAtomModified(final URI wonNodeURI, final URI atomURI) {
        taskScheduler.schedule(new Runnable() {
            public void run() {
                try {
                    logger.debug("onAtomModified for atom {} ", atomURI.toString());
                    getBotForAtomUri(atomURI).onAtomModifiedNotificationForMatcher(wonNodeURI, atomURI);
                    // getBotForAtomUri(atomURI.getAtomURI()).onMessageFromOtherAtom(con, message,
                    // content);
                } catch (Exception e) {
                    logger.warn("error while handling onAtomModified()", e);
                }
            }
        }, new Date());
    }

    @Override
    public void onAtomActivated(final URI wonNodeURI, final URI atomURI) {
        taskScheduler.schedule(new Runnable() {
            public void run() {
                try {
                    logger.debug("onAtomActivated for atom {} ", atomURI.toString());
                    getBotForAtomUri(atomURI).onAtomActivatedNotificationForMatcher(wonNodeURI, atomURI);
                    // getBotForAtomUri(atomURI.getAtomURI()).onMessageFromOtherAtom(con, message,
                    // content);
                } catch (Exception e) {
                    logger.warn("error while handling onAtomActivated()", e);
                }
            }
        }, new Date());
    }

    @Override
    public void onAtomDeactivated(final URI wonNodeURI, final URI atomURI) {
        taskScheduler.schedule(new Runnable() {
            public void run() {
                try {
                    logger.debug("onAtomDeactivated for atom {} ", atomURI.toString());
                    getBotForAtomUri(atomURI).onAtomDeactivatedNotificationForMatcher(wonNodeURI, atomURI);
                    // getBotForAtomUri(atomURI.getAtomURI()).onMessageFromOtherAtom(con, message,
                    // content);
                } catch (Exception e) {
                    logger.warn("error while handling onAtomDeactivated()", e);
                }
            }
        }, new Date());
    }
}
