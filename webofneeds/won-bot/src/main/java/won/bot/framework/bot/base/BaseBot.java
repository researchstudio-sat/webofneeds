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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.Assert;
import won.bot.framework.bot.Bot;
import won.bot.framework.bot.BotLifecyclePhase;
import won.bot.framework.bot.context.BotContextWrapper;
import won.bot.framework.component.atomproducer.AtomProducer;
import won.bot.framework.component.nodeurisource.NodeURISource;
import won.matcher.component.MatcherNodeURISource;
import won.matcher.protocol.impl.MatcherProtocolMatcherServiceImplJMSBased;
import won.protocol.matcher.MatcherProtocolAtomServiceClientSide;
import won.protocol.message.sender.WonMessageSender;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.linkeddata.LinkedDataSource;

import java.lang.invoke.MethodHandles;
import java.net.URI;

/**
 * Basic Bot implementation intended to be extended. Does nothing. Implements
 * Bot and OwnerCallback interfaces. Provides wrappers for initialize(),
 * shutdown() and setters/getters. Holds information needed for connecting to
 * nodes.
 */
public abstract class BaseBot implements Bot {
    // bot control variables
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private BotLifecyclePhase lifecyclePhase = BotLifecyclePhase.DOWN;
    private boolean workDone = false;
    // node connection variables
    private NodeURISource nodeURISource;
    private MatcherNodeURISource matcherNodeURISource;
    private AtomProducer atomProducer;
    private WonMessageSender wonMessageSender;
    private MatcherProtocolAtomServiceClientSide matcherProtocolAtomServiceClient;
    private MatcherProtocolMatcherServiceImplJMSBased matcherProtocolMatcherService;
    private LinkedDataSource linkedDataSource;
    private WonNodeInformationService wonNodeInformationService;
    // ================================================================================
    // Methods from OwnerCallback Interface
    // ================================================================================
    // void onConnectFromOtherAtom(Connection con, final WonMessage wonMessage);
    // void onOpenFromOtherAtom(Connection con, final WonMessage wonMessage);
    // void onCloseFromOtherAtom(Connection con, final WonMessage wonMessage);
    // void onAtomHintFromMatcher(final WonMessage wonMessage);
    // void onSocketHintFromMatcher(final WonMessage wonMessage);
    // void onMessageFromOtherAtom(Connection con, final WonMessage wonMessage);
    // void onFailureResponse(URI failedMessageUri, WonMessage wonMessage);
    // void onSuccessResponse(URI successfulMessageUri, WonMessage wonMessage);

    // ================================================================================
    // Methods from Bot Interface
    // ================================================================================
    // void onNewAtomCreated(URI atomUri, URI wonNodeUri, Dataset atomDataset)
    // throws Exception;
    // void onMatcherRegistered(URI wonNodeUri);
    // void act() throws Exception;
    @Override
    public boolean knowsAtomURI(final URI atomURI) {
        return this.botContextWrapper.isAtomKnown(atomURI);
    }

    @Override
    public boolean knowsNodeURI(final URI wonNodeURI) {
        return this.botContextWrapper.getBotContext().isNodeKnown(wonNodeURI);
    }

    /**
     * Override this method to add additional initialization routines.
     */
    @Override
    public synchronized void initialize() throws Exception {
        if (!this.lifecyclePhase.isDown())
            return;
        this.lifecyclePhase = BotLifecyclePhase.STARTING_UP;
        // try to connect with the bot context
        try {
            botContextWrapper.getBotContext().saveToObjectMap("temp", "temp", "temp");
            Object o = botContextWrapper.getBotContext().loadFromObjectMap("temp", "temp");
            Assert.state(o.equals("temp"), "Assertion failed.");
        } catch (IllegalStateException e) {
            logger.error("Bot cannot establish connection with bot context");
            throw e;
        }
        this.lifecyclePhase = BotLifecyclePhase.ACTIVE;
    }

    /**
     * Override this method to add additional shutdown routines.
     */
    @Override
    public synchronized void shutdown() throws Exception {
        if (!this.lifecyclePhase.isActive())
            return;
        this.lifecyclePhase = BotLifecyclePhase.SHUTTING_DOWN;
        this.lifecyclePhase = BotLifecyclePhase.DOWN;
    }

    @Override
    public synchronized BotLifecyclePhase getLifecyclePhase() {
        return this.lifecyclePhase;
    }

    // ================================================================================
    // Bot Control Methods
    // ================================================================================
    /**
     * Sets the workDone flag to true.
     */
    protected void workIsDone() {
        this.workDone = true;
    }

    @Override
    public boolean isWorkDone() {
        return this.workDone;
    }

    @Autowired
    private BotContextWrapper botContextWrapper;

    public void setBotContextWrapper(final BotContextWrapper botContextWrapper) {
        this.botContextWrapper = botContextWrapper;
    }

    protected BotContextWrapper getBotContextWrapper() {
        return botContextWrapper;
    }

    // ================================================================================
    // Node Connection Setters/Getters
    // ================================================================================
    protected NodeURISource getNodeURISource() {
        return nodeURISource;
    }

    @Qualifier("default")
    @Autowired()
    public void setNodeURISource(final NodeURISource nodeURISource) {
        this.nodeURISource = nodeURISource;
    }

    protected MatcherNodeURISource getMatcherNodeURISource() {
        return matcherNodeURISource;
    }

    @Qualifier("default")
    @Autowired()
    public void setMatcherNodeURISource(final MatcherNodeURISource matcherNodeURISource) {
        this.matcherNodeURISource = matcherNodeURISource;
    }

    protected WonMessageSender getWonMessageSender() {
        return wonMessageSender;
    }

    @Qualifier("default")
    @Autowired()
    public void setWonMessageSender(final WonMessageSender wonMessageSender) {
        this.wonMessageSender = wonMessageSender;
    }

    protected MatcherProtocolAtomServiceClientSide getMatcherProtocolAtomServiceClient() {
        return matcherProtocolAtomServiceClient;
    }

    @Qualifier("default")
    @Autowired()
    public void setMatcherProtocolAtomServiceClient(
            final MatcherProtocolAtomServiceClientSide matcherProtocolAtomServiceClient) {
        this.matcherProtocolAtomServiceClient = matcherProtocolAtomServiceClient;
    }

    protected MatcherProtocolMatcherServiceImplJMSBased getMatcherProtocolMatcherService() {
        return matcherProtocolMatcherService;
    }

    @Qualifier("default")
    @Autowired()
    public void setMatcherProtocolMatcherService(
            final MatcherProtocolMatcherServiceImplJMSBased matcherProtocolMatcherService) {
        this.matcherProtocolMatcherService = matcherProtocolMatcherService;
    }

    protected AtomProducer getAtomProducer() {
        return atomProducer;
    }

    @Qualifier("default")
    @Autowired()
    public void setAtomProducer(final AtomProducer atomProducer) {
        this.atomProducer = atomProducer;
    }

    public LinkedDataSource getLinkedDataSource() {
        return linkedDataSource;
    }

    @Qualifier("default")
    @Autowired()
    public void setLinkedDataSource(final LinkedDataSource linkedDataSource) {
        this.linkedDataSource = linkedDataSource;
    }

    public WonNodeInformationService getWonNodeInformationService() {
        return wonNodeInformationService;
    }

    @Autowired()
    public void setWonNodeInformationService(final WonNodeInformationService wonNodeInformationService) {
        this.wonNodeInformationService = wonNodeInformationService;
    }
}
