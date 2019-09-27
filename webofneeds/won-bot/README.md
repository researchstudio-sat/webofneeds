# Bots

A bot is a so-called 'owner application'. It can communicate with WoN nodes and matchers, create and manage atoms and send messages to connected atoms. Further information on nodes, matchers and atoms can be found in the general [documentation on Github](https://github.com/researchstudio-sat/webofneeds).

Bots in the Web of Needs are generally reactive. Each bot uses [event listeners](src/main/java/won/bot/framework/eventbot/listener) to listen to events published on the bot's [event bus](src/main/java/won/bot/framework/eventbot/bus/EventBus.java). If an event occurs, a predefined [action](src/main/java/won/bot/framework/eventbot/action) can be executed. Event listeners and actions can also be combined into [behaviours](src/main/java/won/bot/framework/eventbot/behaviour) that determine how the bot acts.

## Base Bot

The java application that runs a bot is usually a spring-boot application that loads all the necessary config and then starts the bot. For examples, look at existing [Bot Apps](https://github.com/search?q=topic%3Awon-bot&type=Repositories).

The [base bot](src/main/java/won/bot/framework/bot/base/) consists of several interfaces and abstract classes that build upon each other. The `Bot` interface extends the `OwnerCallback` interface and is partially implemented in the abstract `BaseBot` class, which describes the methods needed for creating a minimal bot that is able to interact with atoms. This is extended by the `ScheduledTriggerBot`, which adds Spring task scheduling with [triggers](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/scheduling/Trigger.html). This is then extended by the `EventBot`, which adds the event listener structure described here.

Furthermore, the `FactoryBot` builds upon the `EventBot` and adds some additional functionality for creating `FactoryAtoms`. This results in a different structure in which a matching query can be used to filter which atoms are sent to the bot by the matcher. It also creates an additional atom representing the bot that may be visible to other users. See the [TaxiBot](https://github.com/researchstudio-sat/won-transport) for a possible use for `FactoryBot`.

<!-- TODO: bot lifecycle -->

## Components

### Events

Events may be generated in different situations:

- as a bot reaches certain [stages in its life cycle](src/main/java/won/bot/framework/eventbot/event/impl/lifecycle), e.g. the [InitializeEvent](src/main/java/won/bot/framework/eventbot/event/impl/lifecycle/InitializeEvent.java) or the [WorkDoneEvent](src/main/java/won/bot/framework/eventbot/event/impl/lifecycle/WorkDoneEvent.java).
- whenever a message is received for one of the atoms controlled by the bot.
- at regular intervals, by attaching a [trigger](src/main/java/won/bot/framework/eventbot/action/impl/trigger/BotTrigger.java) to the bot that publishes [ActEvents](src/main/java/won/bot/framework/eventbot/event/impl/lifecycle/ActEvent.java) at a regular interval that is specified when creating the `BotTrigger`.
- as part of an action that is executed by the bot.

<!-- TODO: maybe have some more concrete event examples here -->

### EventListeners/EventBus

Each bot has an [event bus](src/main/java/won/bot/framework/eventbot/bus/EventBus.java) that receives all events sent to the bot. To listen for a specific event, an `EventListener` needs to subscribe to the bus and specify an `Action` to be taken if that event is received.

<!-- TODO: extend this? -->

### Context

Each bot provides a [BotContext](src/main/java/won/bot/framework/bot/context/BotContext.java) and an [EventListenerContext](src/main/java/won/bot/framework/eventbot/EventListenerContext.java), both are initialized as single instances that can be accessed anywhere within the bot and are used to connect to various resources.

The `BotContext` is used by a bot to keep track of atoms, nodes or other objects it knows or is responsible for. This information can be used by both the bot itself and the framework to decide which events should be routed to which bot.

While it is necessary that each bot remembers which atoms it created, all other information stored within the `BotContext` depends on the intended purpose of any given bot. In addition to the `BotContext`, a [BotContextWrapper](src/main/java/won/bot/framework/bot/context/BotContextWrapper.java) provides access utility methods to interact with the `BotContext`.

---

The `EventListenerContext` is used to connect the bot to various resources it may use, including:

- The [LinkedDataSource](/webofneeds/won-core/src/main/java/won/protocol/util/linkeddata/LinkedDataSource.java) obtained via `EventListenerContext.getLinkedDataSource()`. It is used to query linked data.
- The [AtomProducer](src/main/java/won/bot/framework/component/atomproducer/AtomProducer.java) obtained via `EventListenerContext.getAtomProducer()`. It's a facility for creating atoms, much like a generator. Depending on the configured implementation, data can be read from any source and transformed into an atom. To see how this is done in practice, look at the [MailFileAtomProducer](src/main/java/won/bot/framework/component/atomproducer/impl/MailFileAtomProducer.java)
- The [WonMessageSender](/webofneeds/won-core/src/main/java/won/protocol/message/sender/WonMessageSender.java) obtained via `EventListenerContext.getWonMessageSender()`. This object allows the bot to send WoN messages.

### Actions

[Actions](src/main/java/won/bot/framework/eventbot/action) are predefined operations that determine the bot's behaviour. Each action is bound to an `EventListener` and triggered by the corresponding event. For example, an `InitializationAction` could listen for an `InitializeEvent` and, once triggered, perform various initialization tasks like creating an atom, setting up additional event listeners or sending out other events.

While event listeners and events generally provide the framework for when a bot acts, actions determine what a bot actually does.

### Behaviours

Behaviours act as a wrapper to `EventListener`s and `Action`s and can be activated and deactivated at any point. This is useful for `Event`/`Action` combinations that are used often or for `EventListener`s that should only trigger under certain conditions.

## Implementing a Bot

To create a new bot, we recommend to use the [bot skeleton](https://github.com/researchstudio-sat/bot-skeleton) as a template, providing all needed functions for a bot in the WoN.

### Starting from scratch

To write a bot from scratch, you'll need to create a new spring boot app. The main bot class has to establish a connection as a client to at least one node and provide an `EventBus`, `EventListenerContext` and `BotContext`. Refer to other bots and the framework described above to understand how all framework parts interact in determining a bots behaviour.

### Base Class

Bot implementations are best created by extending [EventBot](src/main/java/won/bot/framework/bot/base/EventBot.java) and implementing its `initializeEventListeners()` method. This method should then be used to set up all the event listeners required for the bot to perform its work.

```
public class MyBot extends EventBot
{

  @Override
  protected void initializeEventListeners()
  {
    //define your event listeners here
  }
}
```

### Registering Listeners

A listener is registered by using `EventBus.subscribe(Class<T> eventClazz, EventListener listener)`. Most functionality is implemented by instantiating a [ActionOnEventListener](src/main/java/won/bot/framework/eventbot/listener/impl) or one of its cousins, passing it some [Action](src/main/java/won/bot/framework/eventbot/action).

**Example**: This Bot will publish a `WorkDoneEvent` when the first `ActEvent` is published on the bot's event bus. The bot is basically telling the framework to stop it as soon as the bot has started up. Not very useful, but very simple:

```
//first, let's remember the bot's context and the bus - we'll need them often
final EventListenerContext ctx = getEventListenerContext();
EventBus bus = getEventBus();

// then, we define a new event listener
EventListener workDoneSignaller =
new ActionOnFirstNEventsListener(
  ctx,                                 // each listener needs the bot's context
  1,                                   // this listener needs to know how often it's supposed to be
                                       //   activated before stopping to listen
  "workDoneSignaller",                 // a name that is used in debug output. Useful if you have many listeners
    new PublishEventAction(            // the action that is executed when the listener is activated
     ctx,                              // each action needs the bot's context
     new WorkDoneEvent(this))          // the event that will be published by the action
  )
);
//subscribe the event listener
bus.subscribe(ActEvent.class, atomCreator);
```

### Sending WoN messages

For doing anything on the Web of Needs, a bot must send messages to a node or to specific atoms. This is done in two steps:

1. Creating a [WonMessage](/webofneeds/won-core/src/main/java/won/protocol/message/WonMessage.java) using the [WonMessageBuilder](/webofneeds/won-core/src/main/java/won/protocol/message/WonMessageBuilder.java)
2. Sending it using the `WonMessageSender` obtained from the `EventListenerContext`

Note that the WoN node will always answer with a similar message (`SuccessResponse` or `FailureResponse`) that will tell you if what you did worked or not. It's possible to add callbacks for both responses so as to be able to take the necessary measures.

### Creating Atoms

Atoms are created by sending a specific message. If using the dedicated [CreateAtomsWithSocketsAction](src/main/java/won/bot/framework/eventbot/action/impl/atomlifecycle/CreateAtomWithSocketsAction.java), most of the boilerplate code is taken care of. If the bot is meant to publish an existing collection of resources, e.g. files or database objects in the form of atoms, it's a question of configuring the `AtomProducer` to access that data and produce a Jena RDF Model that represents an atom. (For an example, look at the [MailFileAtomProducer](src/main/java/won/bot/framework/component/atomproducer/impl/MailFileAtomProducer.java)

For example, this is how [a bot creates a single Atom](src/main/java/won/bot/framework/eventbot/action/impl/atomlifecycle/AbstractCreateAtomAction.java):

```
//assuming
//   * wonNodeURI contains a valid WonNodeURI,
//   * atomURI has been newly created and conforms to the WoN node's URI pattern
WonMessageBuilder builder = WonMessageBuilder
 .setMessagePropertiesForCreate(
      wonNodeInformationService.generateEventURI(wonNodeURI),              // generate an URI for the 'create Atom' event
      atomURI,                                                             // pass the new Atom URI
      wonNodeURI)                                                          // pass the WoN node URI
      .addContent(atomModel, null);                                        // add the Atom's content
 WonMessage message = builder.build();                                     // build the Message object
 getEventListenerContext().getWonMessageSender().sendWonMessage(message);  //send it
```

## Additional Resources
A good starting point for understanding the framework is the [EchoBot](https://github.com/researchstudio-sat/won-echobot). This bot creates one atom at startup, and it registers with the WoN node configured with the `WON_NODE_URI` environment variable, so it is always notified when a new atom is created. When that happens, the bot attempts to establish a connection with the new atom and if the new atom accepts it, the bot echoes any text message received from the new atom.

For an example of a bot for creating atoms, the [AtomCreatorBot](https://github.com/researchstudio-sat/won-atomcreatorbot) is a good example. This bot reads atom `Model`s from the configured `AtomProducer` and creates new atoms on the configured WoN nodes. It does that until the `AtomProducer` is exhausted.

Other, more complex bots to look at are the [MailBot](https://github.com/researchstudio-sat/won-mailbot) and the [DebugBot](https://github.com/researchstudio-sat/won-debugbot). Click on the corresponding links to find out more.
