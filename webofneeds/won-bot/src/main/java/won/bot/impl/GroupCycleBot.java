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

import org.apache.jena.query.Dataset;
import won.bot.framework.bot.base.EventBot;
import won.bot.framework.bot.context.GroupBotContextWrapper;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.impl.counter.*;
import won.bot.framework.eventbot.behaviour.BehaviourBarrier;
import won.bot.framework.eventbot.behaviour.BotBehaviour;
import won.bot.framework.eventbot.behaviour.ExecuteWonMessageCommandBehaviour;
import won.bot.framework.eventbot.event.BaseEvent;
import won.bot.framework.eventbot.event.BaseNeedSpecificEvent;
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
import won.protocol.util.DefaultNeedModelWrapper;
import won.protocol.util.WonRdfUtils;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Bot that creates NUMBER_OF_GROUPS groupchats, adds NUMBER_OF_GROUPMEMBERS members to each,
 * links them to each other and sends a message on behalf of one of the members, potentially causing an
 * endless echo. Used for verifying that the group facet suppresses echos.
 */
public class GroupCycleBot extends EventBot {
  private final int NUMBER_OF_GROUPMEMBERS = 3;
  private final int NUMBER_OF_GROUPS = 10;
  private final ConnectionHolder connectionForFirstMessage = new ConnectionHolder();

  @Override protected void initializeEventListeners() {
    EventListenerContext ctx = getEventListenerContext();

    //start with a friendly message
    ctx.getEventBus().subscribe(InitializeEvent.class, new ActionOnFirstEventListener(ctx, new BaseEventBotAction(ctx) {
      @Override protected void doRun(Event event, EventListener executingListener) throws Exception {
        logger.info("");
        logger.info("We will create {} groups with {} members each.", NUMBER_OF_GROUPS, NUMBER_OF_GROUPMEMBERS);
        logger.info("The groups all be connected to each other, resulting in {} group-group connections",
            NUMBER_OF_GROUPS * (NUMBER_OF_GROUPS - 1) / 2);
        logger.info(
            "Then, one group member will send a message to its group, which should reach all other group members exactly once");
        logger.info("This will result in {} messages being received.", NUMBER_OF_GROUPS * NUMBER_OF_GROUPMEMBERS - 1);
        logger.info("The groups will forward {} messages and suppress {} duplicates",
            NUMBER_OF_GROUPS * (NUMBER_OF_GROUPS + NUMBER_OF_GROUPMEMBERS - 2),
            (int) Math.pow(NUMBER_OF_GROUPS, 2) - 3 * NUMBER_OF_GROUPS + 2);
        logger.info("");
      }
    }));

    // understand message commands
    BotBehaviour messageCommandBehaviour = new ExecuteWonMessageCommandBehaviour(ctx);
    messageCommandBehaviour.activate();

    // if we receive a connection message, log it
    BotBehaviour logConnectionMessageBehaviour = new LogConnectionMessageBehaviour(ctx);
    logConnectionMessageBehaviour.activate();

    // log other important events (group/member creation and conneciton)
    BotBehaviour infoBehaviour = new OutputInfoMessagesBehaviour(ctx);
    infoBehaviour.activate();

    // wait for both groups to finish being set up, then connect the groups
    BehaviourBarrier barrier = new BehaviourBarrier(ctx);

    for (int i = 0; i < NUMBER_OF_GROUPS; i++) {
      // create group 1, its members, and connect them
      CreateGroupBehaviour groupCreate = new CreateGroupBehaviour(ctx);
      OpenOnConnectBehaviour groupOpenOnConnect = new OpenOnConnectBehaviour(ctx);
      CreateGroupMembersBehaviour groupMembers = new CreateGroupMembersBehaviour(ctx);
      groupCreate.onDeactivateActivate(groupOpenOnConnect, groupMembers);
      barrier.waitFor(groupMembers);
      // wait for the initialize event and trigger group creation
      ctx.getEventBus()
          .subscribe(InitializeEvent.class, new ActionOnFirstEventListener(ctx, new BaseEventBotAction(ctx) {
            @Override protected void doRun(Event event, EventListener executingListener) throws Exception {
              groupCreate.activate();
            }
          }));
    }

    BotBehaviour connectGroupsBehaviour = new ConnectGroupsBehaviour(ctx);
    barrier.thenStart(connectGroupsBehaviour);
    barrier.activate();

    // after connecting the groups, send one message on behalf of one of the group members
    // and count the messages that group members receive

    // when all groups are connected, start the count behaviour
    CountReceivedMessagesBehaviour countReceivedMessagesBehaviour = new CountReceivedMessagesBehaviour(ctx);
    connectGroupsBehaviour.onDeactivateActivate(countReceivedMessagesBehaviour);

    // wait for the count behaviour to have started, then send the group message
    BotBehaviour sendInitialMessageBehaviour = new SendOneMessageBehaviour(ctx);
    countReceivedMessagesBehaviour.onActivateActivate(sendInitialMessageBehaviour);

  }

  /**
   * Creates a need with a chat facet and a group facet, then stops.
   * Its deactivate message is the URI of the created group need.
   */
  private class CreateGroupBehaviour extends BotBehaviour {
    public CreateGroupBehaviour(EventListenerContext context) {
      super(context);
    }

    @Override protected void onActivate(Optional<Object> message) {
      GroupBotContextWrapper botContextWrapper = (GroupBotContextWrapper) getBotContextWrapper();
      Dataset needDataset = createNeedDataset("Group Need", "Used for testing if groups suppress echos");
      CommandEvent command = new CreateNeedCommandEvent(needDataset, botContextWrapper.getGroupListName(), true, true,
          FacetType.ChatFacet.getURI(), FacetType.GroupFacet.getURI());
      subscribeWithAutoCleanup(CreateNeedCommandResultEvent.class,
          new ActionOnFirstEventListener(context, new CommandResultFilter(command), new BaseEventBotAction(context) {
            @Override protected void doRun(Event event, EventListener executingListener) throws Exception {
              CreateNeedCommandResultEvent resultEvent = (CreateNeedCommandResultEvent) event;
              logger.debug("creating group need succeeded: {}, need uri: {}", resultEvent.isSuccess(),
                  resultEvent.getNeedURI());
              Optional<Object> uriMessage = resultEvent.isSuccess() ?
                  Optional.of(resultEvent.getNeedURI()) :
                  Optional.empty();
              context.getEventBus().publish(new GroupCreatedEvent(resultEvent.getNeedURI()));
              deactivate(uriMessage);
            }
          }));
      context.getEventBus().publish(command);
    }
  }

  private class CreateGroupMembersBehaviour extends BotBehaviour {
    public CreateGroupMembersBehaviour(EventListenerContext context) {
      super(context);
    }

    public CreateGroupMembersBehaviour(EventListenerContext context, String name) {
      super(context, name);
    }

    @Override protected void onActivate(Optional<Object> message) {
      //we expect the URI of the group in the message!
      if (!message.isPresent())
        return;
      GroupBotContextWrapper botContextWrapper = (GroupBotContextWrapper) getBotContextWrapper();
      URI groupNeedURI = (URI) message.get();
      TargetCounterDecorator memberCreationCounter = new TargetCounterDecorator(context,
          new CounterImpl("memberCreationCounter", 0), NUMBER_OF_GROUPMEMBERS);
      TargetCounterDecorator membersConnectedCounter = new TargetCounterDecorator(context,
          new CounterImpl("membersConnectedCounter", 0), NUMBER_OF_GROUPMEMBERS);

      Set<URI> members = new HashSet<URI>();
      //create N group members
      for (int i = 0; i < NUMBER_OF_GROUPMEMBERS; i++) {
        Dataset needDataset = createNeedDataset("Group Memeber Need", "Used for testing if groups suppress echos");
        CommandEvent command = new CreateNeedCommandEvent(needDataset, botContextWrapper.getGroupMembersListName(),
            true, true, FacetType.ChatFacet.getURI());
        subscribeWithAutoCleanup(CreateNeedCommandResultEvent.class,
            new ActionOnFirstEventListener(context, new CommandResultFilter(command), new BaseEventBotAction(context) {
              @Override protected void doRun(Event event, EventListener executingListener) throws Exception {
                CreateNeedCommandResultEvent resultEvent = (CreateNeedCommandResultEvent) event;
                logger.debug("creating group member succeeded: {}, need uri: {}", resultEvent.isSuccess(),
                    resultEvent.getNeedURI());
                if (resultEvent.isSuccess()) {
                  members.add(resultEvent.getNeedURI());
                  context.getEventBus().publish(new GroupMemberCreatedEvent(resultEvent.getNeedURI()));
                }
                memberCreationCounter.increment();
              }
            }));
        context.getEventBus().publish(command);
      }
      //wait for them to be created
      subscribeWithAutoCleanup(TargetCountReachedEvent.class,
          new ActionOnFirstEventListener(context, memberCreationCounter.makeEventFilter(),
              new BaseEventBotAction(context) {
                @Override protected void doRun(Event event, EventListener executingListener) throws Exception {
                  //make sure all members were created successfully
                  if (members.size() != NUMBER_OF_GROUPMEMBERS) {
                    logger.error("expected {} members to be successfully created, but {} were", NUMBER_OF_GROUPMEMBERS,
                        members.size());
                    deactivate();
                  }
                  //now, connect all group members
                  members.forEach(memberURI -> {
                    //prepare the command
                    ConnectCommandEvent connectCommandEvent = new ConnectCommandEvent(memberURI, groupNeedURI,
                        FacetType.ChatFacet.getURI(), FacetType.GroupFacet.getURI(),
                        "Hello from your latest would-be member!");
                    //set up a listener for the result of the command
                    subscribeWithAutoCleanup(ConnectCommandResultEvent.class,
                        new ActionOnFirstEventListener(context, new CommandResultFilter(connectCommandEvent),
                            new BaseEventBotAction(context) {
                              @Override protected void doRun(Event event, EventListener executingListener)
                                  throws Exception {
                                ConnectCommandResultEvent resultEvent = (ConnectCommandResultEvent) event;
                                logger.debug("sending connect to group need {} on behalf of member {}, succeeded: {}",
                                    new Object[] { connectCommandEvent.getRemoteNeedURI(),
                                        connectCommandEvent.getNeedURI(), resultEvent.isSuccess() });
                              }
                            }));
                    //set up a listener for the response from the group need
                    subscribeWithAutoCleanup(OpenFromOtherNeedEvent.class,
                        new ActionOnEventListener(context, new EventFilter() {
                          @Override public boolean accept(Event event) {
                            if (!(event instanceof OpenFromOtherNeedEvent))
                              return false;
                            OpenFromOtherNeedEvent openEvent = (OpenFromOtherNeedEvent) event;
                            if (!groupNeedURI.equals(openEvent.getRemoteNeedURI()))
                              return false;
                            if (!memberURI.equals(openEvent.getNeedURI()))
                              return false;
                            return true;
                          }
                        }, new BaseEventBotAction(context) {
                          @Override protected void doRun(Event event, EventListener executingListener)
                              throws Exception {
                            OpenFromOtherNeedEvent openEvent = (OpenFromOtherNeedEvent) event;
                            logger.debug("received open from group need {} on behalf of member {}",
                                new Object[] { openEvent.getRemoteNeedURI(), openEvent.getNeedURI() });
                            membersConnectedCounter.increment();
                            // remember the connection of the first open
                            if (!connectionForFirstMessage.isSet()) {
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
      subscribeWithAutoCleanup(TargetCountReachedEvent.class,
          new ActionOnFirstEventListener(context, membersConnectedCounter.makeEventFilter(),
              new BaseEventBotAction(context) {
                @Override protected void doRun(Event event, EventListener executingListener) throws Exception {
                  logger.debug("finished connecting all {} members to group {} ", NUMBER_OF_GROUPMEMBERS, groupNeedURI);
                  context.getEventBus().publish(new GroupMembersConnectedEvent());
                  deactivate();
                }
              }));
    }
  }

  private class OpenOnConnectBehaviour extends BotBehaviour {
    public OpenOnConnectBehaviour(EventListenerContext context) {
      super(context);
    }

    public OpenOnConnectBehaviour(EventListenerContext context, String name) {
      super(context, name);
    }

    @Override protected void onActivate(Optional<Object> message) {
      if (!message.isPresent())
        return;
      //expect group URI in message
      URI groupNeedURI = (URI) message.get();
      subscribeWithAutoCleanup(ConnectFromOtherNeedEvent.class,
          new ActionOnEventListener(context, new NeedUriEventFilter(groupNeedURI), new BaseEventBotAction(context) {
            @Override protected void doRun(Event event, EventListener executingListener) throws Exception {
              Connection con = ((ConnectFromOtherNeedEvent) event).getCon();
              logger.debug("received connect from need {} on behalf of need {}, responding with OPEN.",
                  con.getRemoteNeedURI(), con.getNeedURI());
              OpenCommandEvent openCommandEvent = new OpenCommandEvent(con, "Welcome from the group need");
              getEventBus().publish(openCommandEvent);
            }
          }));
    }
  }

  private class ConnectGroupsBehaviour extends BotBehaviour {
    public ConnectGroupsBehaviour(EventListenerContext context) {
      super(context);
    }

    public ConnectGroupsBehaviour(EventListenerContext context, String name) {
      super(context, name);
    }

    @Override protected void onActivate(Optional<Object> message) {
      GroupBotContextWrapper botContextWrapper = (GroupBotContextWrapper) getBotContextWrapper();
      List<URI> groupNeeds = botContextWrapper.getGroupNeedUris();
      if (groupNeeds == null || groupNeeds.size() != NUMBER_OF_GROUPS) {
        logger.error("Expected {} group needs but found {}", NUMBER_OF_GROUPS,
            groupNeeds == null ? "null" : groupNeeds.size());
        return;
      }

      // use a target counter to know when we are finished (and use an EventPublishingCounter as the
      // decorated counter so we can output status messages while counting
      EventPublishingCounter eachGroupConnectionCounter = new EventPublishingCounter("eachGroupConnectionCounter",
          context);
      TargetCounterDecorator groupConnectionCounter = new TargetCounterDecorator(context, eachGroupConnectionCounter,
          NUMBER_OF_GROUPS * (NUMBER_OF_GROUPS - 1) / 2);

      // react to each connection separately
      subscribeWithAutoCleanup(CountEvent.class,
          new ActionOnEventListener(context, eachGroupConnectionCounter.makeEventFilter(),
              new BaseEventBotAction(context) {
                @Override protected void doRun(Event event, EventListener executingListener) throws Exception {
                  logger.info("established group-group connection {} of {}", ((CountEvent) event).getCount(),
                      groupConnectionCounter.getTargetCount());
                }
              }));

      // react to reaching the target count
      subscribeWithAutoCleanup(TargetCountReachedEvent.class,
          new ActionOnEventListener(context, groupConnectionCounter.makeEventFilter(), new BaseEventBotAction(context) {
            @Override protected void doRun(Event event, EventListener executingListener) throws Exception {
              if (!(event instanceof TargetCountReachedEvent))
                return;
              TargetCountReachedEvent countEvent = (TargetCountReachedEvent) event;
              logger.info("successfully made {} connections between our {} groups", countEvent.getCount(),
                  NUMBER_OF_GROUPS);
              deactivate();
            }
          }));

      //connect each group to all other groups
      for (int i = 0; i < groupNeeds.size(); i++) {
        URI groupNeedURI = groupNeeds.get(i);
        for (int j = i + 1; j < groupNeeds.size(); j++) {
          URI remoteGroupNeedURI = groupNeeds.get(j);
          //prepare the command
          ConnectCommandEvent connectCommandEvent = new ConnectCommandEvent(groupNeedURI, remoteGroupNeedURI,
              FacetType.GroupFacet.getURI(), FacetType.GroupFacet.getURI(), "Hello from the other group!");

          //set up a listener for the result of the command
          subscribeWithAutoCleanup(ConnectCommandResultEvent.class,
              new ActionOnFirstEventListener(context, new CommandResultFilter(connectCommandEvent),
                  new BaseEventBotAction(context) {
                    @Override protected void doRun(Event event, EventListener executingListener) throws Exception {
                      ConnectCommandResultEvent resultEvent = (ConnectCommandResultEvent) event;
                      logger.debug("sending connect to group need {} on behalf of group need {}, succeeded: {}",
                          new Object[] { connectCommandEvent.getRemoteNeedURI(), connectCommandEvent.getNeedURI(),
                              resultEvent.isSuccess() });
                    }
                  }));
          //set up a listener for the response from the group need
          subscribeWithAutoCleanup(OpenFromOtherNeedEvent.class, new ActionOnEventListener(context, new EventFilter() {
                @Override public boolean accept(Event event) {
                  if (!(event instanceof OpenFromOtherNeedEvent))
                    return false;
                  OpenFromOtherNeedEvent openEvent = (OpenFromOtherNeedEvent) event;
                  if (!remoteGroupNeedURI.equals(openEvent.getRemoteNeedURI()))
                    return false;
                  if (!groupNeedURI.equals(openEvent.getNeedURI()))
                    return false;
                  return true;
                }
              }, new BaseEventBotAction(context) {
                @Override protected void doRun(Event event, EventListener executingListener) throws Exception {
                  OpenFromOtherNeedEvent openEvent = (OpenFromOtherNeedEvent) event;
                  logger.debug("received open from group need {} on behalf of group need {}",
                      new Object[] { openEvent.getRemoteNeedURI(), openEvent.getNeedURI() });
                  groupConnectionCounter.increment();
                }
              }));

          //publish the command
          context.getEventBus().publish(connectCommandEvent);
        } //inner loop
      } //outer loop
    }
  }

  private class LogConnectionMessageBehaviour extends BotBehaviour {
    public LogConnectionMessageBehaviour(EventListenerContext context) {
      super(context);
    }

    public LogConnectionMessageBehaviour(EventListenerContext context, String name) {
      super(context, name);
    }

    @Override protected void onActivate(Optional<Object> message) {
      subscribeWithAutoCleanup(MessageFromOtherNeedEvent.class,
          new ActionOnEventListener(context, new BaseEventBotAction(context) {
            @Override protected void doRun(Event event, EventListener executingListener) throws Exception {
              MessageFromOtherNeedEvent messageEvent = (MessageFromOtherNeedEvent) event;
              WonMessage message = ((MessageFromOtherNeedEvent) event).getWonMessage();
              String textMessage = WonRdfUtils.MessageUtils.getTextMessage(message);
              URI messageURI = message.getMessageURI();
              logger.debug("need {} received message from need {}, text: '{}', message uri: {}",
                  new Object[] { messageEvent.getNeedURI(), messageEvent.getRemoteNeedURI(), textMessage, messageURI });
            }
          }));
    }
  }

  private class SendOneMessageBehaviour extends BotBehaviour {
    public SendOneMessageBehaviour(EventListenerContext context) {
      super(context);
    }

    public SendOneMessageBehaviour(EventListenerContext context, String name) {
      super(context, name);
    }

    @Override protected void onActivate(Optional<Object> message) {
      //prepare the command
      ConnectionMessageCommandEvent initialMessageCommand = new ConnectionMessageCommandEvent(
          connectionForFirstMessage.getConnection(), WonRdfUtils.MessageUtils.textMessage("Hello, echo!"));
      context.getEventBus().publish(initialMessageCommand);
    }
  }

  /**
   * Class that encapsulates a connection, and allows us to
   * remember the one connection we want to use to send the only
   * connection message in this bot.
   */
  private class ConnectionHolder {
    private Connection connection;

    public ConnectionHolder() {
    }

    public boolean isSet() {
      return connection != null;
    }

    public synchronized void set(Connection connection) {
      if (isSet())
        return;
      this.connection = connection;
    }

    public Connection getConnection() {
      return connection;
    }
  }

  private class CountReceivedMessagesBehaviour extends BotBehaviour {
    public CountReceivedMessagesBehaviour(EventListenerContext context) {
      super(context);
    }

    public CountReceivedMessagesBehaviour(EventListenerContext context, String name) {
      super(context, name);
    }

    @Override protected void onActivate(Optional<Object> message) {
      GroupBotContextWrapper botContextWrapper = (GroupBotContextWrapper) getBotContextWrapper();

      EventPublishingCounter receivedMessagesCounter = new EventPublishingCounter("receivedMessagesCounter", context);
      //count connection messages received by group members coming from groups
      subscribeWithAutoCleanup(MessageFromOtherNeedEvent.class, new ActionOnEventListener(context, new EventFilter() {
            @Override public boolean accept(Event event) {
              if (!(event instanceof MessageFromOtherNeedEvent))
                return false;
              MessageFromOtherNeedEvent messageEvent = (MessageFromOtherNeedEvent) event;
              URI senderNeed = messageEvent.getRemoteNeedURI();
              URI recipientNeed = messageEvent.getNeedURI();
              if (!botContextWrapper.getGroupMemberNeedUris().contains(recipientNeed))
                return false;
              return botContextWrapper.getGroupNeedUris().contains(senderNeed);
            }
          }, new IncrementCounterAction(context, receivedMessagesCounter)));

      // produce log messages about the actual and expeced number of messages
      subscribeWithAutoCleanup(CountEvent.class,
          new ActionOnEventListener(context, receivedMessagesCounter.makeEventFilter(),
              new BaseEventBotAction(context) {
                @Override protected void doRun(Event event, EventListener executingListener) throws Exception {
                  if (!(event instanceof CountEvent))
                    return;
                  CountEvent countEvent = (CountEvent) event;
                  int currentCount = countEvent.getCount();
                  int targetCount = NUMBER_OF_GROUPMEMBERS * NUMBER_OF_GROUPS - 1;
                  if (currentCount < targetCount) {
                    logger.info("received group message {} of {} ...", currentCount, targetCount);
                  } else if (currentCount == targetCount) {
                    logger.info("received group message {} of {}, target count reached", currentCount, targetCount);
                  } else {
                    logger.warn("received group message {} but only expected {} - something is wrong!", currentCount,
                        targetCount);
                  }
                }
              }));
    }
  }

  private class OutputInfoMessagesBehaviour extends BotBehaviour {
    public OutputInfoMessagesBehaviour(EventListenerContext context) {
      super(context);
    }

    public OutputInfoMessagesBehaviour(EventListenerContext context, String name) {
      super(context, name);
    }

    @Override protected void onActivate(Optional<Object> message) {
      GroupBotContextWrapper botContextWrapper = (GroupBotContextWrapper) getBotContextWrapper();

      EventPublishingCounter counter = new EventPublishingCounter("groupCreationCounter", context);

      // count group creations
      subscribeWithAutoCleanup(GroupCreatedEvent.class,
          new ActionOnEventListener(context, new IncrementCounterAction(context, counter)));

      // produce log messages about created groups
      subscribeWithAutoCleanup(CountEvent.class,
          new ActionOnEventListener(context, counter.makeEventFilter(), new BaseEventBotAction(context) {
            @Override protected void doRun(Event event, EventListener executingListener) throws Exception {
              if (!(event instanceof CountEvent))
                return;
              CountEvent countEvent = (CountEvent) event;
              int currentCount = countEvent.getCount();
              int targetCount = NUMBER_OF_GROUPS;
              logger.info("created group {} of {} ", currentCount, targetCount);
            }
          }));

      // count when all members are created
      EventPublishingCounter membersCreatedCounter = new EventPublishingCounter("membersCreatedCounter", context);
      subscribeWithAutoCleanup(GroupMemberCreatedEvent.class,
          new ActionOnEventListener(context, new IncrementCounterAction(context, membersCreatedCounter)));

      // produce log messages about created members
      subscribeWithAutoCleanup(CountEvent.class,
          new ActionOnEventListener(context, membersCreatedCounter.makeEventFilter(), new BaseEventBotAction(context) {
            @Override protected void doRun(Event event, EventListener executingListener) throws Exception {
              if (!(event instanceof CountEvent))
                return;
              CountEvent countEvent = (CountEvent) event;
              int currentCount = countEvent.getCount();
              int targetCount = NUMBER_OF_GROUPS * NUMBER_OF_GROUPMEMBERS;
              logger.info("created group member {} of {}", currentCount, targetCount);
            }
          }));

      // count when all members are connected
      EventPublishingCounter membersConnectedCounter = new EventPublishingCounter("membersConnectedCounter", context);
      subscribeWithAutoCleanup(GroupMembersConnectedEvent.class,
          new ActionOnEventListener(context, new IncrementCounterAction(context, membersConnectedCounter)));

      // produce log messages about connected members
      subscribeWithAutoCleanup(CountEvent.class,
          new ActionOnEventListener(context, membersConnectedCounter.makeEventFilter(),
              new BaseEventBotAction(context) {
                @Override protected void doRun(Event event, EventListener executingListener) throws Exception {
                  if (!(event instanceof CountEvent))
                    return;
                  CountEvent countEvent = (CountEvent) event;
                  int currentCount = countEvent.getCount();
                  int targetCount = NUMBER_OF_GROUPS;
                  logger.info("connected all group members to group {} of {}", currentCount, targetCount);
                }
              }));
    }
  }

  private class GroupCreatedEvent extends BaseNeedSpecificEvent {
    public GroupCreatedEvent(URI needURI) {
      super(needURI);
    }
  }

  private class GroupMembersConnectedEvent extends BaseEvent {
    public GroupMembersConnectedEvent() {
    }
  }

  private class GroupMemberCreatedEvent extends BaseNeedSpecificEvent {
    public GroupMemberCreatedEvent(URI needURI) {
      super(needURI);
    }

  }

  private Dataset createNeedDataset(String title, String description) {
    URI needURI = getEventListenerContext().getWonNodeInformationService().generateNeedURI();
    DefaultNeedModelWrapper needModelWrapper = new DefaultNeedModelWrapper(needURI.toString());
    needModelWrapper.setTitle(title);
    needModelWrapper.setDescription(description);
    return needModelWrapper.copyDataset();
  }
}
