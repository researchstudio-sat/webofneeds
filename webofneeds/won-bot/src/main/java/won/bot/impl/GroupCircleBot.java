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

package won.bot.impl;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.DC;
import won.bot.framework.bot.base.EventBot;
import won.bot.framework.bot.context.GroupBotContextWrapper;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.impl.counter.CounterImpl;
import won.bot.framework.eventbot.action.impl.counter.TargetCountReachedEvent;
import won.bot.framework.eventbot.action.impl.counter.TargetCounterDecorator;
import won.bot.framework.eventbot.behaviour.BehaviourBarrier;
import won.bot.framework.eventbot.behaviour.BotBehaviour;
import won.bot.framework.eventbot.behaviour.ExecuteWonMessageCommandBehaviour;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.cmd.CommandEvent;
import won.bot.framework.eventbot.event.impl.command.connect.ConnectCommandEvent;
import won.bot.framework.eventbot.event.impl.command.connect.ConnectCommandResultEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.event.impl.command.create.CreateNeedCommandEvent;
import won.bot.framework.eventbot.event.impl.command.create.CreateNeedCommandResultEvent;
import won.bot.framework.eventbot.event.impl.command.open.OpenCommandEvent;
import won.bot.framework.eventbot.event.impl.lifecycle.InitializeEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherNeedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherNeedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.OpenFromOtherNeedEvent;
import won.bot.framework.eventbot.filter.EventFilter;
import won.bot.framework.eventbot.filter.impl.CommandResultFilter;
import won.bot.framework.eventbot.filter.impl.NeedUriEventFilter;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnFirstEventListener;
import won.protocol.message.WonMessage;
import won.protocol.model.Connection;
import won.protocol.model.FacetType;
import won.protocol.model.NeedContentPropertyType;
import won.protocol.model.NeedGraphType;
import won.protocol.util.NeedModelWrapper;
import won.protocol.util.WonRdfUtils;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Bot that creates three groupchats, links them to each other and causes an
 * endless echo. Used for verifying that the group facet suppresses echos.
 */
public class GroupCircleBot extends EventBot
{
    private final int NUMBER_OF_GROUPMEMBERS = 3;
    private final int NUMBER_OF_GROUPS = 3;
    private final ConnectionHolder connectionForFirstMessage = new ConnectionHolder();

    @Override
    protected void initializeEventListeners() {
        EventListenerContext ctx = getEventListenerContext();

        // understand message commands
        BotBehaviour messageCommandBehaviour = new ExecuteWonMessageCommandBehaviour(ctx);
        messageCommandBehaviour.activate();

        // if we receive a connection message, log it
        BotBehaviour logConnectionMessageBehaviour = new LogConnectionMessageBehaviour(ctx);
        logConnectionMessageBehaviour.activate();

        // create group 1, its members, and connect them
        CreateGroupBehaviour group1Create = new CreateGroupBehaviour(ctx);
        OpenOnConnectBehaviour group1openOnConnect = new OpenOnConnectBehaviour(ctx);
        CreateGroupMembersBehaviour group1Members = new CreateGroupMembersBehaviour(ctx);
        group1Create.onDeactivateActivate(group1openOnConnect, group1Members);

        // connect group 2, its members, and connect them
        CreateGroupBehaviour group2Create = new CreateGroupBehaviour(ctx);
        OpenOnConnectBehaviour group2openOnConnect = new OpenOnConnectBehaviour(ctx);
        CreateGroupMembersBehaviour group2Members = new CreateGroupMembersBehaviour(ctx);
        group2Create.onDeactivateActivate(group2openOnConnect, group2Members);

        // connect group 3, its members, and connect them
        CreateGroupBehaviour group3Create = new CreateGroupBehaviour(ctx);
        OpenOnConnectBehaviour group3openOnConnect = new OpenOnConnectBehaviour(ctx);
        CreateGroupMembersBehaviour group3Members = new CreateGroupMembersBehaviour(ctx);
        group3Create.onDeactivateActivate(group3openOnConnect, group3Members);

        // wait for both groups to finish being set up, then connect the groups
        BehaviourBarrier barrier = new BehaviourBarrier(ctx);
        BotBehaviour connectGroupsBehaviour = new ConnectGroupsBehaviour(ctx);
        barrier.waitFor(group2Members);
        barrier.waitFor(group1Members);
        barrier.waitFor(group3Members);
        barrier.thenStart(connectGroupsBehaviour);
        barrier.activate();

        //after connecting the groups, send one message on behalf of one of the group members
        BotBehaviour sendInitialMessageBehaviour = new SendOneMessageBehaviour(ctx);
        connectGroupsBehaviour.onDeactivateActivate(sendInitialMessageBehaviour);


        // wait for the initialize event and trigger group creation
        ctx.getEventBus().subscribe(InitializeEvent.class, new ActionOnFirstEventListener(ctx, new BaseEventBotAction(ctx) {
            @Override
            protected void doRun(Event event, EventListener executingListener) throws Exception {
                group1Create.activate();
                group2Create.activate();
                group3Create.activate();
            }
        }));
    }

    /**
     * Creates a need with an owner facet and a group facet, then stops.
     * Its deactivate message is the URI of the created group need.
     */
    private class CreateGroupBehaviour extends BotBehaviour
    {
        public CreateGroupBehaviour(EventListenerContext context) {
            super(context);
        }

        @Override
        protected void onActivate(Optional<Object> message) {
            GroupBotContextWrapper botContextWrapper = (GroupBotContextWrapper) getBotContextWrapper();
            Model model = createNeedModel("Group Need", "Used for testing if groups suppress echos");
            CommandEvent command = new CreateNeedCommandEvent(model, botContextWrapper.getGroupListName() , true, true, FacetType.OwnerFacet.getURI(), FacetType.GroupFacet.getURI());
            subscribeWithAutoCleanup(
                    CreateNeedCommandResultEvent.class,
                    new ActionOnFirstEventListener(context, new CommandResultFilter(command), new BaseEventBotAction(context) {
                        @Override
                        protected void doRun(Event event, EventListener executingListener) throws Exception {
                            CreateNeedCommandResultEvent resultEvent = (CreateNeedCommandResultEvent) event;
                            logger.debug("creating group need succeeded: {}, need uri: {}", resultEvent.isSuccess(), resultEvent.getNeedURI());
                            Optional<Object> uriMessage = resultEvent.isSuccess() ? Optional.of(resultEvent.getNeedURI()) : Optional.empty();
                            deactivate(uriMessage);
                        }
                    }));
            context.getEventBus().publish(command);
        }
    }

    private class CreateGroupMembersBehaviour extends BotBehaviour
    {
        public CreateGroupMembersBehaviour(EventListenerContext context) {
            super(context);
        }

        public CreateGroupMembersBehaviour(EventListenerContext context, String name) {
            super(context, name);
        }

        @Override
        protected void onActivate(Optional<Object> message) {
            //we expect the URI of the group in the message!
            if (!message.isPresent()) return;
            GroupBotContextWrapper botContextWrapper = (GroupBotContextWrapper) getBotContextWrapper();
            URI groupNeedURI = (URI) message.get();
            TargetCounterDecorator memberCreationCounter = new TargetCounterDecorator(context, new CounterImpl("memberCreationCounter", 0),NUMBER_OF_GROUPMEMBERS);
            TargetCounterDecorator membersConnectedCounter = new TargetCounterDecorator(context, new CounterImpl("membersConnectedCounter", 0),NUMBER_OF_GROUPMEMBERS);

            Set<URI> members = new HashSet<URI>();
            //create N group members
            for (int i = 0; i < NUMBER_OF_GROUPMEMBERS; i++) {
                Model model = createNeedModel("Group Memeber Need", "Used for testing if groups suppress echos");
                CommandEvent command = new CreateNeedCommandEvent(model, botContextWrapper.getGroupMembersListName(), true, true, FacetType.OwnerFacet.getURI());
                subscribeWithAutoCleanup(
                        CreateNeedCommandResultEvent.class,
                        new ActionOnFirstEventListener(context, new CommandResultFilter(command), new BaseEventBotAction(context) {
                            @Override
                            protected void doRun(Event event, EventListener executingListener) throws Exception {
                                CreateNeedCommandResultEvent resultEvent = (CreateNeedCommandResultEvent) event;
                                logger.debug("creating group member succeeded: {}, need uri: {}", resultEvent.isSuccess(), resultEvent.getNeedURI());
                                memberCreationCounter.increment();
                                if (resultEvent.isSuccess()){
                                    members.add(resultEvent.getNeedURI());
                                }
                            }
                        }));
                context.getEventBus().publish(command);
            }
            //wait for them to be created
            subscribeWithAutoCleanup(
                    TargetCountReachedEvent.class,
                    new ActionOnFirstEventListener(
                            context,
                            memberCreationCounter.makeEventFilter(),
                            new BaseEventBotAction(context) {
                @Override
                protected void doRun(Event event, EventListener executingListener) throws Exception {
                    //make sure all members were created successfully
                    if (members.size() != NUMBER_OF_GROUPMEMBERS){
                        logger.error("expected {} members to be successfully created, but {} were", NUMBER_OF_GROUPMEMBERS, members.size());
                        deactivate();
                    }
                    //now, connect all group members
                    members.forEach( memberURI -> {
                        //prepare the command
                        ConnectCommandEvent connectCommandEvent =
                                new ConnectCommandEvent(
                                        memberURI,
                                        groupNeedURI,
                                        FacetType.OwnerFacet.getURI(),
                                        FacetType.GroupFacet.getURI(),
                                        "Hello from your latest would-be member!");
                        //set up a listener for the result of the command
                        subscribeWithAutoCleanup(
                                ConnectCommandResultEvent.class,
                                new ActionOnFirstEventListener(context, new CommandResultFilter(connectCommandEvent),
                                        new BaseEventBotAction(context) {
                                            @Override
                                            protected void doRun(Event event, EventListener executingListener) throws Exception {
                                                ConnectCommandResultEvent resultEvent = (ConnectCommandResultEvent) event;
                                                logger.debug("sending connect to group need {} on behalf of member {}, succeeded: {}", new Object[]{
                                                        connectCommandEvent.getRemoteNeedURI(), connectCommandEvent.getNeedURI(), resultEvent.isSuccess()});
                                            }
                                        }));
                        //set up a listener for the response from the group need
                        subscribeWithAutoCleanup(
                                OpenFromOtherNeedEvent.class,
                                new ActionOnEventListener(context, new EventFilter() {
                                    @Override
                                    public boolean accept(Event event) {
                                        if (!(event instanceof OpenFromOtherNeedEvent)) return false;
                                        OpenFromOtherNeedEvent openEvent = (OpenFromOtherNeedEvent) event;
                                        if (!groupNeedURI.equals(openEvent.getRemoteNeedURI())) return false;
                                        if (!memberURI.equals(openEvent.getNeedURI())) return false;
                                        return true;
                                    }
                                },
                                new BaseEventBotAction(context) {
                                    @Override
                                    protected void doRun(Event event, EventListener executingListener) throws Exception {
                                        OpenFromOtherNeedEvent openEvent = (OpenFromOtherNeedEvent) event;
                                        logger.debug("received open from group need {} on behalf of member {}", new Object[]{
                                                openEvent.getRemoteNeedURI(), openEvent.getNeedURI()});
                                        membersConnectedCounter.increment();
                                        // remember the connection of the first open
                                        if (!connectionForFirstMessage.isSet()){
                                            connectionForFirstMessage.set(openEvent.getCon());
                                        }
                                    }
                                }));
                        //publish the command
                        context.getEventBus().publish(connectCommandEvent);
                    });

                }
            }));

            //when all members are connected, finish the behaviour
            subscribeWithAutoCleanup(
                    TargetCountReachedEvent.class,
                    new ActionOnFirstEventListener(context,
                            membersConnectedCounter.makeEventFilter(),
                            new BaseEventBotAction(context) {
                                @Override
                                protected void doRun(Event event, EventListener executingListener) throws Exception {
                                    logger.debug("finished connecting all members to group {} ", groupNeedURI);
                                    deactivate();
                                }
                            }));
        }
    }

    private class OpenOnConnectBehaviour extends BotBehaviour
    {
        public OpenOnConnectBehaviour(EventListenerContext context) {
            super(context);
        }

        public OpenOnConnectBehaviour(EventListenerContext context, String name) {
            super(context, name);
        }

        @Override
        protected void onActivate(Optional<Object> message) {
            if (!message.isPresent()) return;
            //expect group URI in message
            URI groupNeedURI = (URI) message.get();
            subscribeWithAutoCleanup(ConnectFromOtherNeedEvent.class,
                    new ActionOnEventListener(context,
                            new NeedUriEventFilter(groupNeedURI),
                            new BaseEventBotAction(context) {
                        @Override
                        protected void doRun(Event event, EventListener executingListener) throws Exception {
                            OpenCommandEvent openCommandEvent = new OpenCommandEvent(((ConnectFromOtherNeedEvent)event).getCon(),"Welcome from the group need");
                            getEventBus().publish(openCommandEvent);
                        }
                    }));
        }
    }

    private class ConnectGroupsBehaviour extends BotBehaviour
    {
        public ConnectGroupsBehaviour(EventListenerContext context) {
            super(context);
        }

        public ConnectGroupsBehaviour(EventListenerContext context, String name) {
            super(context, name);
        }

        @Override
        protected void onActivate(Optional<Object> message) {
            GroupBotContextWrapper botContextWrapper = (GroupBotContextWrapper) getBotContextWrapper();
            List<URI> groupNeeds = botContextWrapper.getBotContext().getNamedNeedUriList(botContextWrapper.getGroupListName());
            if (groupNeeds == null || groupNeeds.size() != NUMBER_OF_GROUPS) {
                logger.error("Expected {} group needs but found {}", NUMBER_OF_GROUPS, groupNeeds == null ? "null" : groupNeeds.size());
                return;
            }

            //connect each group to all other groups
            for (int i = 0; i < groupNeeds.size(); i++) {
                URI groupNeedURI = groupNeeds.get(i);
                for (int j = i + 1; j < groupNeeds.size(); j++) {
                    URI remoteGroupNeedURI = groupNeeds.get(j);
                    //prepare the command
                    ConnectCommandEvent connectCommandEvent =
                            new ConnectCommandEvent(
                                    groupNeedURI,
                                    remoteGroupNeedURI,
                                    FacetType.GroupFacet.getURI(),
                                    FacetType.GroupFacet.getURI(),
                                    "Hello from the other group!");

                    //set up a listener for the result of the command
                    subscribeWithAutoCleanup(
                            ConnectCommandResultEvent.class,
                            new ActionOnFirstEventListener(context, new CommandResultFilter(connectCommandEvent),
                                    new BaseEventBotAction(context) {
                                        @Override
                                        protected void doRun(Event event, EventListener executingListener) throws Exception {
                                            ConnectCommandResultEvent resultEvent = (ConnectCommandResultEvent) event;
                                            logger.debug("sending connect to group need {} on behalf of group need {}, succeeded: {}", new Object[]{
                                                    connectCommandEvent.getRemoteNeedURI(), connectCommandEvent.getNeedURI(), resultEvent.isSuccess()});
                                        }
                                    }));
                    //set up a listener for the response from the group need
                    subscribeWithAutoCleanup(
                            OpenFromOtherNeedEvent.class,
                            new ActionOnEventListener(context, new EventFilter() {
                                @Override
                                public boolean accept(Event event) {
                                    if (!(event instanceof OpenFromOtherNeedEvent)) return false;
                                    OpenFromOtherNeedEvent openEvent = (OpenFromOtherNeedEvent) event;
                                    if (!remoteGroupNeedURI.equals(openEvent.getRemoteNeedURI())) return false;
                                    if (!groupNeedURI.equals(openEvent.getNeedURI())) return false;
                                    return true;
                                }
                            },
                                    new BaseEventBotAction(context) {
                                        @Override
                                        protected void doRun(Event event, EventListener executingListener) throws Exception {
                                            OpenFromOtherNeedEvent openEvent = (OpenFromOtherNeedEvent) event;
                                            logger.debug("received open from group need {} on behalf of group need {}", new Object[]{
                                                    openEvent.getRemoteNeedURI(), openEvent.getNeedURI()});
                                            deactivate();
                                        }
                                    }));
                    //publish the command
                    context.getEventBus().publish(connectCommandEvent);
                } //inner loop
            } //outer loop
        }
    }

    private class LogConnectionMessageBehaviour extends BotBehaviour
    {
        public LogConnectionMessageBehaviour(EventListenerContext context) {
            super(context);
        }

        public LogConnectionMessageBehaviour(EventListenerContext context, String name) {
            super(context, name);
        }

        @Override
        protected void onActivate(Optional<Object> message) {
            subscribeWithAutoCleanup(
                    MessageFromOtherNeedEvent.class,
                    new ActionOnEventListener(context, new BaseEventBotAction(context) {
                        @Override
                        protected void doRun(Event event, EventListener executingListener) throws Exception {
                            MessageFromOtherNeedEvent messageEvent = (MessageFromOtherNeedEvent) event;
                            WonMessage message = ((MessageFromOtherNeedEvent) event).getWonMessage();
                            String textMessage = WonRdfUtils.MessageUtils.getTextMessage(message);
                            URI messageURI = message.getMessageURI();
                            logger.info("need {} received message '{}', message uri: {}", new Object[]{messageEvent.getNeedURI(), textMessage, messageURI});
                        }
                    }));
        }
    }

    private class SendOneMessageBehaviour extends BotBehaviour
    {
        public SendOneMessageBehaviour(EventListenerContext context) {
            super(context);
        }

        public SendOneMessageBehaviour(EventListenerContext context, String name) {
            super(context, name);
        }

        @Override
        protected void onActivate(Optional<Object> message) {
            //prepare the command
            ConnectionMessageCommandEvent initialMessageCommand =
                    new ConnectionMessageCommandEvent(connectionForFirstMessage.getConnection(),
                            WonRdfUtils.MessageUtils.textMessage("Hello, echo!"));
            context.getEventBus().publish(initialMessageCommand);
        }
    }

    /**
     * Class that encapsulates a connection, and allows us to
     * remember the one connection we want to use to send the only
     * connection message in this bot.
     */
    private class ConnectionHolder
    {
        private Connection connection;

        public ConnectionHolder() {
        }

        public boolean isSet(){
            return connection != null;
        }

        public synchronized void set(Connection connection){
            if (isSet()) return;
            this.connection = connection;
        }

        public Connection getConnection() {
            return connection;
        }
    }

    private Model createNeedModel(String title, String description){
        URI needURI = getEventListenerContext().getWonNodeInformationService().generateNeedURI();
        NeedModelWrapper needModelWrapper = new NeedModelWrapper(needURI.toString());
        needModelWrapper.setContentPropertyStringValue(NeedContentPropertyType.IS, DC.title, title);
        needModelWrapper.setContentPropertyStringValue(NeedContentPropertyType.IS, DC.description, description);
        return needModelWrapper.getNeedModel(NeedGraphType.NEED);
    }
}
