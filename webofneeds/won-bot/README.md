# Bots

A bot is a so-called 'owner application'. It can communicate with WoN nodes and matchers, create and manage atoms and send messages to connected atoms. Further information on nodes, matchers and atoms can be found in the general [documentation on Github](https://github.com/researchstudio-sat/webofneeds).

Bots in the Web of Needs are generally reactive. Each bot uses [event listeners](src/main/java/won/bot/framework/eventbot/listener) to listen to events published on the bot's [event bus](src/main/java/won/bot/framework/eventbot/bus/EventBus.java). If an event occurs, a predefined [action](src/main/java/won/bot/framework/eventbot/action) can be executed. Event listeners and actions can also be combined into [behaviours](src/main/java/won/bot/framework/eventbot/behaviour) that determine how the bot acts. 

<!-- TODO: bot lifecycle -->

## Components

### Events

Events can be generated in different situations:

- as the bot reaches certain [stages in its life cycle](src/main/java/won/bot/framework/eventbot/event/impl/lifecycle), e.g. the [InitializeEvent](src/main/java/won/bot/framework/eventbot/event/impl/lifecycle/InitializeEvent.java) or the [WorkDoneEvent](src/main/java/won/bot/framework/eventbot/event/impl/lifecycle/WorkDoneEvent.java).
- whenever a message is received for one of the atoms controlled by the bot.
- at regular intervals, by attaching a [trigger](src\main\java\won\bot\framework\eventbot\action\impl\trigger\BotTrigger.java) to the bot that publishes [ActEvents](src/main/java/won/bot/framework/eventbot/event/impl/lifecycle/ActEvent.java) at regular intervals.
- as part of an action that is executed by the bot.

<!-- TODO: maybe have some more concrete event examples here -->

### EventListeners/EventBus

### Context

Each bot provides a [BotContext](src/main/java/won/bot/framework/bot/context/BotContext.java) and an [EventListenerContext](src/main/java/won/bot/framework/eventbot/EventListenerContext.java), both are initialized as singe instances that can be accessed at any point within the bot and are used to connect to various resources. 

The `BotContext` is used by a bot to keep track of atoms, nodes or other objects it knows or is responsible for. This information can be used by both the bot itself and the framework to decide which events should be routed to which bot. 

While it is necessary that each bot remembers which atoms it created, all other information stored within the `BotContext` depends on the intended purpose of any given bot. In addition to the `BotContext`, a [BotContextWrapper](src/main/java/won/bot/framework/bot/context/BotContextWrapper.java) provides access utility methods to interact with the `BotContext`.

---

The `EventListenerContext` is used to connect the bot to various resources it may use, including:

- The [LinkedDataSource](/webofneeds/won-core/src/main/java/won/protocol/util/linkeddata/LinkedDataSource.java) obtained via `EventListenerContext.getLinkedDataSource()`. It is used to query linked data.
- The [AtomProducer](src/main/java/won/bot/framework/component/atomproducer/AtomProducer.java) obtained via `EventListenerContext.getAtomProducer()`. It's a facility for creating atoms, much like an iterator. Depending on the configured implementation, data can be read from any source and transformed into an atom. To see how this is done in practice, look at the [MailFileAtomProducer](src/main/java/won/bot/framework/component/atomproducer/impl/MailFileAtomProducer.java)
- The [WonMessageSender](/webofneeds/won-core/src/main/java/won/protocol/message/sender/WonMessageSender.java) obtained via `EventListenerContext.getWonMessageSender()`. This object allows the bot to send WoN messages.

### Actions

Actions are predefined operations that determine the bot's behaviour. Each action is bound to an event listener and triggered by the corresponding event. For example, an `InitializationAction` could listen for an `InitializeEvent` and, once triggered, perform various initialization tasks like creating an atom, setting up additional event listeners or sending out other events. 

<!--
to be added:
* information about bot inheritance: Bot -> BaseBot -> BasicServiceBot -> ScheduledActionBot -> TriggeredBot -> EventBot -> FactoryBot
* more info about EventBus, EventListener, Action and BotTrigger interactions
* more info about Behaviours - esp. activate/deactivate
* more info about variables and methods in bot base classes (tbd via javadoc)
* prepared bot template (maybe echo bot?)

to be done:
* think about deleting tests (outdated?)
* see if one or two of the base bots can be merged to others
* see if telegram bot and hokify bot can be moved to separate subfolders
* check for duplicate implementations of actions/behaviours
* think about making bots only interact with one node (nodeURI) and making a separate subclass for multi-node bots (uriSource)
* BotBehaviour uses its own subclass -> maybe merge those?
* draw diagram of bot class inheritance
-->


### Behaviours

Behaviours act as a wrapper to event listeners and actions and can be activated and deactivated. This is useful for event/action combinations that are used often or for event listeners that should only be active under certain conditions.


## Implementing a Bot

### The Bot's App

The java application that runs a bot is usually a spring-boot application that loads all the necessary config and then starts the bot. For examples, look at the existing [Bot Apps](src/main/java/won/bot/app).

To create a new app (which is what you want if you want to run a bot), it's easiest to copy it's app class and the corresponding spring config files, and then making modifications.

### Base Class

Bot implementations are best created by extending [EventBot](src/main/java/won/bot/base/EventBot.java) and implementing its `initializeEventListeners()` method. This method should then be used to set up all the event listeners required for the bot to perform its work.

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
//first, let't remember the bot's context and the bus - we'll need them often
final EventListenerContext ctx = getEventListenerContext();
EventBus bus = getEventBus();

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

For doing anything on the WoN, a bot must send messages. This is done in two steps:

1. Creating a [WonMessage](/webofneeds/won-core/src/main/java/won/protocol/message/WonMessage.java) using the [WonMessageBuilder](/webofneeds/won-core/src/main/java/won/protocol/message/WonMessageBuilder.java)
2. Sending it using the `WonMessageSender` obtained from the `EventListenerContext`
   For example, this is how [a bot creates an Atom](src/main/java/won/bot/framework/eventbot/action/impl/atomlifecycle/AbstractCreateAtomAction.java):

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

Note that the WoN node will always answer with a similar message (`SuccessResponse` or `FailureResponse`) that will tell you if what you did worked or not. It's possible to add callbacks for both responses so as to be able to take the necessary measures.

### Creating Atoms

Atoms are created by sending a specific message. If using the dedicated [CreateAtomsWithSocketsAction](src/main/java/won/bot/framework/eventbot/action/impl/atomlifecycle/CreateAtomWithSocketsAction.java), most of the boilerplate code is taken care of. If the bot is meant to publish an existing collection of resources, e.g. files or database objects in the form of atoms, it's a question of configuring the `AtomProducer` to access that data and produce a Jena RDF Model that represents an atom. (For an example, look at the [MailFileAtomProducer](src/main/java/won/bot/framework/component/atomproducer/impl/MailFileAtomProducer.java)

## Additional Resources

A good starting point for understanding the framework is the [EchoBot](src/main/java/won/bot/impl/EchoBot.java) and its corresponding [EchoBotApp](src/main/java/won/bot/app/EchoBotApp.java). This bot creates one atom at startup, and it registers with the WoN node configured in its [node-uri-source.properties](/webofneeds/conf/node-uri-source.properties) file so it is always notified when a new atom is created. When that happens, the bot attempts to establish a connection with the new atom and if the new atom accepts it, the bot echoes any text message received from the new atom.

For an example of a bot for creating atoms, the [AtomCreatorBot](src/main/java/won/bot/impl/AtomCreatorBot.java) is a good model. This bot reads atom `Model`s from the configured `AtomProducer` and creates new atoms on the configured WoN nodes. It does that until the `AtomProducer` is exhausted.

Other, more complex bots to look at are the [MailBot](src/main/java/won/bot/impl/Mail2WonBot.java) and the [DebugBot](src/main/java/won/bot/impl/DebugBot.java). They are described in more detail in the next section.

# Testing and debugging with bots

## Debug Bot

[DebugBotApp](src/main/java/won/bot/app/DebugBotApp.java) can be used to test if connections
can be established with the atoms you are creating and if messages can be sent via those connections. For each
created by you need the Bot will generate a connection request and a hint messages. Additionally, some actions can be
triggered by sending text messages on those connections. Check supported
[actions](src/main/java/won/bot/framework/events/action/impl/DebugBotIncomingMessageToEventMappingAction.java).

Run [DebugBotApp](src/main/java/won/bot/app/DebugBotApp.java) with the argument specifying the configuration
location, e.g:

    -DWON_CONFIG_DIR=C:/webofneeds/conf.local

Make sure this location contains the relevant property files, and you have specified the values of the properties
relevant for the system being tested, i.e.:

- in [node-uri-source.properties](../conf/node-uri-source.properties)
  - won.node.uris - specify values of nodes being tested - the bot will react to atoms published on those nodes
- in [owner.properties](../conf/owner.properties)
  - specify default node data (node.default.host/scheme/port) - the bot will create its own atoms on that node
  - make sure a path to keystore and truststore is specified (keystore/truststore.location) and their password
    (keystore/truststore.password)
    NOTE: Don't use at Bot the same keystore (and key pair) that you use for Owner Application, especially if you are
    running owner locally - the node won't deliver messages correctly to the Bot and Owner because the queues used
    for delivery are defined based on Owner and Bot certificates, and if they are the same there will be errors.
    NOTE: For the same reason as above, do not run several Bot applications at the same time, - stop one before
    running another or separate their configurations.
    NOTE: Keystore and trustore pathes have to be specified but the files themselves do not have to exist, they will
    be created automatically. In fact, in case the Bot's default Node was previously having a different certificate  
     this file must be deleted - otherwise the Bot won't trust the Node and won't be able to register and create atoms
    there.

## Mail2Won Bot

[Mail2WonBotApp](src/main/java/won/bot/app/Mail2WonBotApp.java) can be used to create Atoms retrieved from a given Email
Address. This Bot acts mostly like a Owner Application as it allows Users to create atoms, open/close connections or requests
and communicate with others. [Insert List of commands here]

Run [Mail2WonBotApp](src/main/java/won/bot/app/Mail2WonBotApp.java) with the argument specifying the configuration
location, e.g:
-DWON_CONFIG_DIR=C:/webofneeds/conf.local
Furthermore you need to set the following properties within mail-bot.properties to ensure a connection to an incoming and outgoing mail-server
mailbot.email.address=emailadress
mailbot.email.user=username
mailbot.email.password=pass
mailbot.email.imap.host=imap.gmail.com
mailbot.email.imap.port=993
mailbot.email.smtp.host=smtp.gmail.com
mailbot.email.smtp.port=587

Make sure this location contains the relevant property files, and you have specified the values of the properties
relevant for the system being tested, i.e.:

- in [node-uri-source.properties](../conf/node-uri-source.properties)
  - won.node.uris - specify values of nodes being tested - the bot will react to atoms published on those nodes
- in [owner.properties](../conf/owner.properties)

  - specify default node data (node.default.host/scheme/port) - the bot will create its own atoms on that node
  - make sure a path to keystore and truststore is specified (keystore/truststore.location) and their password
    (keystore/truststore.password)

  NOTE: Don't use at Bot the same keystore (and key pair) that you use for Owner Application or other Bots, especially if you are
  running owner locally - the node won't deliver messages correctly to the Bot and Owner because the queues used
  for delivery are defined based on Owner and Bot certificates, and if they are the same there will be errors.

  NOTE: For the same reason as above, do not run several Bot applications at the same time, - stop one before
  running another.

  NOTE: Keystore and trustore pathes have to be specified but the files themselves do not have to exist, they will
  be created automatically. In fact, in case the Bot's default Node was previously having a different certificate
  this file must be deleted - otherwise the Bot won't trust the Node and won't be able to register and create atoms
  there.

Usage:
You can send an e-mail with a Subject starting with either [WANT], [OFFER], [TOGETHER], [CRITIQUE] to the configured Mailadress,
to create an Atom of the given type. The Body or Content of the Mail will be used as the AtomDescription, the rest of the subject line
will be used as the Atom Title. Furthermore Tags (Strings starting with #) will be extracted from the e-mail and will
be stored within the created atom.

You will then receive e-mails when the Matcher finds Connections to this created Atom, you can answer those e-mails via
the reply function of your e-mail-Client, for now we support the following commands (which will be retrieved from the replymessage-body):
A Replymail that starts with:
-"close" or "deny" will close the respective connection
-"connect" sends a request to the other atom or opens the connection if the status is already request received
-answering a mail with anything that does not contain any of the keywords above will automatically open a connection and send the replymessage-body as a textmessage

A Replymail for an already open connection(connectionState: Connected) will send teh replymessage-body as a textmessage

Every Remote Message sent to this Connection will be sent to you as an e-mail as well.
