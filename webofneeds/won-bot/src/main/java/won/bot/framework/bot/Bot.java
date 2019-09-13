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
package won.bot.framework.bot;

import org.apache.jena.query.Dataset;
import won.owner.protocol.message.OwnerCallback;

import java.net.URI;

/**
 * A bot that manipulates atoms. Note: Methods may throw runtime exceptions,
 * which will be handled by the execution framework.
 */
public interface Bot extends OwnerCallback {
    boolean knowsAtomURI(URI atomURI);

    boolean knowsNodeURI(URI wonNodeURI);

    void onNewAtomCreated(final URI atomUri, final URI wonNodeUri, final Dataset atomModel) throws Exception;

    void onMatcherRegistered(URI wonNodeUri);

    void onNewAtomCreatedNotificationForMatcher(final URI wonNodeURI, final URI atomURI,
                    final Dataset atomModel);

    void onAtomModifiedNotificationForMatcher(final URI wonNodeURI, final URI atomURI);

    void onAtomActivatedNotificationForMatcher(final URI wonNodeURI, final URI atomURI);

    void onAtomDeactivatedNotificationForMatcher(final URI wonNodeURI, final URI atomURI);

    /**
     * Init method, called exactly once by the framework before any other method is
     * invoked. The callee must make sure this call is thread-safe, e.g. by explicit
     * synchronizing.
     */
    void initialize() throws Exception;

    /**
     * Called by the framework to execute non-reactive tasks. The callee must make
     * sure this call is thread-safe, but explicit synchronization is strongly
     * discouraged.
     */
    void act() throws Exception;

    /**
     * Shutdown method called exactly once by the framework to allow the bot to free
     * resources. The callee must make sure this call is thread-safe, e.g. by
     * explicit synchronizing.
     */
    void shutdown() throws Exception;

    /**
     * The lifecycle phase the bot is currently in.
     * 
     * @return
     */
    BotLifecyclePhase getLifecyclePhase();

    /**
     * Indicates whether the bot considers its work done. If true, the bot is ok
     * with not receiving incoming messages and not having its act() method called.
     *
     * @return
     */
    boolean isWorkDone();
}
