# Mini HowTos
This document contains mini HowTos explaining how to use the WoN framework. *Bot HowTos* generally assume that you want to do something in a Bot's `initializeEventListeners()` method. Other HowTos are not bot-specific.

## Bot HowTo: Storing Data and Keeping Track of Atoms

Situation: you want to store some information - an atom URI, or another Object or a Collection of Objects - for later use, maybe in another part of your bot.

Use the BotContext. It has a number of features for storing data and the underlying data store can be switched out from in-memory to persisted via mongodb.

A few examples:

```
BotContext bc = getEventListenerContext().getBotContext();
// single value
bc.setSingleValue("mybot:APIKEY", "123abc");
// Collection
Collection<String> apples = Arrays.asList("grannysmith", "goldendelicious");
bc.addToListMap("mybot:fruits", "apples", objects.toArray());
```
Note the `mybot:` key prefix in the above example. It is generally a good idea to use a separate prefix for each bot you are running, as this avoids name clashes. Those are not a problem as long as the `InMemoryBotContext` is used, but they matter when sharing a mongodb database between multiple bots.

Remembering Atom URIs is a special case, it happens often. The concept used here is 'named' atom URI lists, to allow you to keep track of different types of atoms. For example, the ones your bot manages and the ones it is currently somehow interacting with.

```
// let's say you just connected to atomURI and you want to keep them in a list 'connected-atoms'
getEventListenerContext().getBotContext().appendToNamedAtomUriList(atomURI, "connected-atoms");
// because you are using a special atom URI list, you can do this:
boolean isKnown = getEventListenerContext().getBotContext().isAtomKnown(atomURI); // isKnown is true
```

If you implement a bit more complex behaviour, managing all the names of lists, such as 'connected-atoms' above can become confusing or cumbersome. For such cases, you may consider writing a custom `BotContextWrapper` that provides access to the various collections you might need through service methods. If interested, take a look at the `SkeletonBotContextWrapper`, which is configured for the bot in the spring xml file `src/main/resources/spring/bot/bot.xml`.

## Bot HowTo: Remembering Your Atoms Across Sessions
Situation: your bot creates atoms while it is running but forgets everything upon restart. You don't want that.
1. Setup MongoDB
    1. Using native installation: Install [Mongodb](https://www.mongodb.com/)
    2. Using [Docker](https://hub.docker.com/_/mongo)
    ```
   docker run -d --name won_mongo_db -p 27017:27017 mongo
    ```
2. Create a `won` database and in it a `won` user with password `won`:
using the mongo shell (`mongodb` or using docker: ```docker exec -it won_mongo_db mongo```)
    ```
    use won
    db.runCommand({createUser:"won", pwd:"won", roles:["dbOwner"]})
    ```
3. Edit the `src/main/resources/application.conf` file:

    1. Uncomment the lines containing  `botContext.mongodb.*` properties

    2. Replace
    
      ```botContext.impl=memoryBotContext```
      
      with 
      
      ```botContext.impl=mongoBotContext```

4. Rebuild the bot jar and restart the bot

## Bot HowTo: Creating an Atom
Situation: you want to create an atom with some simple data (E.g. Title, Description, Tags, ...)
What you have to do:
1. Make sure you have the `ExecuteMessageCommandBehaviour` activated (see [HowTo on sending messages](#bot-howto-sending-a-message-and-processing-the-result))
2. Create a new atom URI
3. Use the `DefaultAtomModelWrapper` to create an RDF dataset with the atom's data
4. Publish a `CreateAtomCommandEvent`.

```java
// Create a new atom URI
EventListenerContext ctx = getEventListenerContext();
URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
URI atomURI = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);

// Set atom data - here only shown for commonly used (hence 'default') properties
DefaultAtomModelWrapper atomWrapper = new DefaultAtomModelWrapper(atomURI);
atomWrapper.setTitle("Interested in H.P. Lovecraft");
atomWrapper.setDescription("Contact me for all things Cthulhu, Yogge-Sothothe and R'lyeh");
atomWrapper.addTag("Fantasy");
atomWrapper.addTag("Fiction");
atomWrapper.addTag("Lovecraft");

//publish command
CreateAtomCommandEvent createCommand = new CreateAtomCommandEvent(atomWrapper.getDataset(), "atom_uris");
ctx.getEventBus().publish(createCommand);
```
Note: reacting to successful creation is explained in the [HowTo on sending messages](#bot-howto-sending-a-message-and-processing-the-result).

## Bot HowTo: Modifying an Atom
Situation: you have created an atom, now you want to change its data.

This is essentially the same process as [creating](#bot-howto-creating-an-atom), the only difference is that you send a `Replace` message. However:
* All connected atoms receive a `ChangeNotificationMessage`
* You can only change a socket if it has no established connections. 

Here is how:
1. Retrieve the atom's content as a dataset
2. Extract the 'content' (as opposed to 'sysinfo', which is managed by the WoN node)
3. Make your changes to the content
4. Send the `Replace` message

```
EventListenerContext ctx = getEventListenerContext();
// retrieve the atom dataset
Dataset atomData = ctx.getLinkedDataSource().getDataForResource(atomURI);
DefaultAtomModelWrapper atomModelWrapper = new DefaultAtomModelWrapper(atomData);
// extract the content
Dataset content = atomModelWrapper.copyDatasetWithoutSysinfo();
// make changes
// ...
// send Replace message
ctx.getEventBus().publish(new ReplaceCommandEvent(content));
```

## Bot HowTo: Connecting to another Atom
Situation: you have two atom URIs. One of them is an atom you control (the other, maybe, too). You know which socket types you want to connect, for example the [holder socket](https://w3id.org/won/ext/hold#HolderSocket) and the [holdable socket](https://w3id.org/won/ext/hold#HoldableSocket). Follow the [HowTo on getting the Socket of an Atom](#bot-howto-obtaining-an-atoms-sockets) to find out how to get the Socket URIs. We'll assume you call them `senderSocketURI` and `recipientSocketURI`.

```
String message = "Hello, let's connect!"; //optional welcome message
ConnectCommandEvent connectCommandEvent = new ConnectCommandEvent(
                                    senderSocketURI,recipientSocketURI, message);
getEventBus().publish(connectCommandEvent);
```

Note: this only works if you have activated the  `MessageCommandBehaviour`. 

## Bot HowTo: Accepting a Connect from another Atom
Situation: you are expecting a Connect message from another atom and you want to accept the connection.

1. Register a listener for the `ConnectFromOtherAtomEvent`
2. Respond with a connect message.

```
// this code accepts any incoming connection request
getEventListenerContext().getEventBus().subscribe(
    ConnectFromOtherAtomEvent.class, 
    new ActionOnEventListener(ctx, "open-reactor",
                        new OpenConnectionAction(ctx, "Accepting Connection")));
```

## Bot HowTo: Reacting to a Hint 
Situation: you are expecting Hint messages and want to establish connections with the Atoms that the hint points to.

1. Register a listener for the `HintFromMatcherEvent`
2. React by sending a connect message

```
// this reacts with connect to any hint
getEventListenerContext().getEventBus().subscribe(
    HintFromMatcherEvent.class, 
    new ActionOnEventListener(ctx, "hint-reactor",
                        new OpenConnectionAction(ctx, "Connecting because of a Hint I got")));
```

## Bot Howto: Getting Hints from Matchers
Situation: You publish atoms that you want to connect to other atoms you do not control.

To get matches, embed a SPARQL query in your atom. A matcher will execute that query in an RDF database containing all atoms that the matcher has seen so far.
You do this while [creating](#bot-howto-creating-an-atom) or [modifying](#bot-howto-modifying-an-atom) your atom.

```
String query = // your sparql query
DefaultAtomModelWrapper atomWrapper = new DefaultAtomModelWrapper();
wrapper.addQuery(query);
// add your other atom data
```

For example, this query finds all personas:

```
prefix won:<https://w3id.org/won/core#>
select ?result where {
  ?result a won:Persona .               
}
```

Great! now you only need to [react to the hints](#bot-howto-reacting-to-a-hint) you receive.

## Bot HowTo: Sending a Message and Processing the Result
Situation: you want to send a message (e.g. to create a new Atom, connect, close, whatever). The easiest way to do this is to activate the `ExecuteMessageCommandBehaviour`:

```
ExecuteWonMessageCommandBehaviour wonMessageCommandBehaviour = new ExecuteWonMessageCommandBehaviour(ctx);
wonMessageCommandBehaviour.activate();
```

Then publish the command, for example `ConnectionMessageCommandEvent` or the `CreateAtomCommandEvent` as in the following example:

```
Dataset ds = ... // dataset holding the atom content 
CreateAtomCommandEvent createCommand = new CreateAtomCommandEvent(ds);
//
// ...(a) register result listener here, if needed (see (b) below)
//
getEventListenerContext().getEventBus().publish(createCommand);
```
The `ExecuteMessageCommandBehaviour` will send the messsage and publish events on the eventBus when responses are received. You can register a result listener for the result of the message command:

```
// (b) this registers a listener that is activated when the message has been successful
// insert at position (a) above, if needed (because you want to register the listener before you publish the command)
EventListenerContext ctx = getEventListenerContext();
ctx.getEventBus().subscribe(
    CreateAtomCommandSuccessEvent.class, 
    new ActionOnFirstEventListener( //note the 'onFIRSTevent' in the name: the listener is destroyed after being invoked once.
        ctx, 
        new CommandResultFilter(createCommand),  // only listen for success to the command we just made
        new BaseEventBotAction(ctx) { 
                @Override
                protected void doRun(Event event, EventListener executingListener) {
                                      //your action here
                }
        }));
```
Much in the same way, you can listen for Failure events (e.g. the `CreateAtomCommandFailureEvent`) or any kind of result (e.g., the `CreateAtomCommandResultEvent`).


## Bot HowTo: Obtaining an Atom's Socket(s)

Situation: you have the URI of an atom you want to connect to. You know which socketType you want to use, for example, the [chat Socket](https://w3id.org/won/ext/chat#ChatSocket). Here is how you find the URI of the socket:

```
URI atomURI = // your atom URI
URI socketURI = URI.create(WXCHAT.ChatSocket.getURI()); //or any other socket type you want
LinkedDataSource linkedDataSource = getEventListenerContext().getLinkedDataSource();
Collection<URI> sockets = WonLinkedDataUtils.getSocketsOfType(atomURI, socketURI, linkedDataSource);
//sockets should have 0 or 1 items
if (sockets.isEmpty()){
    //did not find a socket of that type
}
URI socket = sockets.iterator().next();
```


## Bot HowTo: Setting Non-Standard Data for Atom
Situation: you want to create an atom containing data that you cannot produce with the method explained in the [HowTo on creating atoms](#bot-howto-creating-an-atom).

Let's assume you want to create the following structure:

``` 
@prefix match: <https://w3id.org/won/matching#> .
@prefix schema: <http://schema.org/> .
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
# {atomURI} to be replaced by the actual atomURI.
{atomURI} match:seeks [
    schema:isbn "1234-1234-1234";
    schema:author [
        a schema:Person ;
        schema:name "H.P. Lovecraft";
    ] .
] .
```

Which is Turtle(TTL) format equivalent to the N-Triples form:

```
# omitted prefix declarations
# {atomURI} to be replaced by the actual atomURI.
{atomURI} match:seeks _:blank1 .
_:blank1 schema:isbn "1234-1234-1234" .
_:blank1 schema:author _:blank2 .
_:blank2 rdf:type s:Person .
_:blank2 s:name "H.P. Lovecraft" .
```

This means: you want to say the atom is connected via `match:seeks` to a blanknode (1), which in turn has an `schema:isbn` property and is connected to another blank node (2) via `schema:author`, which has properties `rdf:type` and `schema:name`.

Use the `DefaultAtomModelWrapper`. Refer to the [HowTo on creating atoms](#bot-howto-creating-an-atom) for how to make one with a new atom URI. Then:

1. Create the 'seeks' blank node (1). 
2. Add the `schema:isbn` property.
3. Create another blank node (2). 
4. Connect it to the first blank node via `schema:author`
5. Add the `rdf:type` and `schema:name` properties to the second blank node

```
// we assume you have created a new atom URI as 'atomURI'
DefaultAtomModelWrapper atomWrapper = new DefaultAtomModelWrapper(atomURI); 
Resource book = atomWrapper.createSeeksNode();  // create a blank node that represents a book
Resource author = book.getModel().createResource(); //create the second blank node (which represents an author)
//set the properties
book.addProperty(SCHEMA.ISBN, "1234-1234-1234");
book.addProperty(SCHEMA.AUTHOR, author);
author.addProperty(SCHEMA.NAME, "H.P. Lovecraft");	
author.addProperty(RDF.type, SCHEMA.PERSON);
```

## Bot HowTo: Getting Service AtomURI:
The Service Atom is the BotAtom, that is created by default implementing the ServiceAtomBehaiviour.
```
ServiceAtomBehaviour serviceAtomBehaviour = new ServiceAtomBehaviour(ctx);
serviceAtomBehaviour.activate();
```
Class needs to implement ServiceAtomContext interface and cast BotContextWrapper to ServiceAtomContext:
```
URI atomURI = ((ServiceAtomContext) ctx.getBotContextWrapper()).getServiceAtomUri()
```

## Bot HowTo: Getting Atom Data

First you need to cast your Event to a MatcherExtensionAtomCreatedEvent.
```MatcherExtensionAtomCreatedEvent atomCreatedEvent = (MatcherExtensionAtomCreatedEvent) event; ```
from this atom you need to get the data and parse it to the DefaultModelWrapper.
```DefaultAtomModelWrapper defaultAtomModelWrapper = new DefaultAtomModelWrapper(atomCreatedEvent.getAtomData());```
From here on you have basic information about the atom. For example ```defaultAtomModelWrapper.getAllTags()``` 
To get the information from the values in seek you need to get all seek nodes. If the resource is a string it can be retireved with the getContentPropertyStringValue Method from the DefaultAtomModelWrapper. It takes the resource containing the requested value and the value you want to get back.
If it is an RDFNode you can get it with getContentPropertyObjects. 

``` MatcherExtensionAtomCreatedEvent atomCreatedEvent = (MatcherExtensionAtomCreatedEvent) event;

        DefaultAtomModelWrapper defaultAtomModelWrapper = new DefaultAtomModelWrapper(atomCreatedEvent.getAtomData());
        System.out.println(defaultAtomModelWrapper.getAllTags());
        defaultAtomModelWrapper.getSeeksNodes().forEach(node -> {
            System.out.println(defaultAtomModelWrapper.getContentPropertyStringValue(node, DC.description));
            Coordinate locationCoordinate = defaultAtomModelWrapper.getLocationCoordinate(node);
            System.out.println(locationCoordinate.getLatitude()+", "+locationCoordinate.getLongitude());
            defaultAtomModelWrapper.getContentPropertyObjects(node,SCHEMA.LOCATION);
        }); 
```

## Bot HowTo: Getting an Atom's Data If You Only have its URI

Situation: you have an atom URI and you want to get its data:

```
    Dataset atomData = getEventListenerContext().getLinkedDataSource().getDataForResource(atomURI);
```

Now you have the atom's dataset. 

## Bot HowTo: Obtaining the Connection If You Only have the Socket URIs

Situation: you need the `Connection` for sending a message, but you only know the two sockets that are connected.

The connection can be obtained from the atom's linked data representation via `WonLinkedDataUtils` :

```
EventListenerContext ctx = getEventListenerContext();
Optional<URI> connectionURI = WonLinkedDataUtils.getConnectionURIForSocketAndTargetSocket(socket, targetSocket, ctx.getLinkedDataSource());
if (!connectionURI.isPresent()){
    // handle: connection not found
}
Optional<Connection> con = getConnectionForConnectionURI(connectionURI.get());
```

## Bot HowTo: Adding Data to a Message

Situation: you want to send some RDF triples as the content of a message.

Let's say you want to embed the following triples:

```
@prefix schema:<http://schema.org/> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
{messageURI}#meeting
            schema:validFrom     "2019-12-11T20:17:34+01:00"^^xsd:dateTime ;
            schema:validThrough  "2019-12-11T22:00:00+01:00"^^xsd:dateTime ;
            schema:title        "Our Meeting" .
```

which is the shorter way of saying
 
```
@prefix schema:<http://schema.org/> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
{messageURI}#meeting schema:validFrom "2019-12-11T20:00:00+01:00"^^xsd:dateTime .
{messageURI}#meeting schema:validThrough "2019-12-11T22:00:00+01:00"^^xsd:dateTime .
{messageURI}#meeting schema:title "Our Meeting" .
```
 
So we are saying there is a meeting, identified by `{messageURI}#meeting`, where {messageURI} is eventually replaced by the actual URI of the message. We want to use a unique identifier for it that nobody can confuse with any other object. By using a fragment (`#message`) of the message URI, we can be quite sure it's going to be unique.

When authoring message content, a placeholder is used for the message URI whenever it is needed: `wm:/SELF`. So, the above triples have to be written as:

```
@prefix schema: <http://schema.org/> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
<wm:/SELF#meeting> schema:validFrom "2019-12-11T20:00:00+01:00"^^xsd:dateTime .
<wm:/SELF#meeting> schema:validThrough "2019-12-11T22:00:00+01:00"^^xsd:dateTime .
<wm:/SELF#meeting> schema:title "Our Meeting" .
```

Follwing the same method as in the [HowTo on sending messages](#bot-howto-sending-a-message-and-processing-the-result), you would use the `ConnectionMessageCommandEvent`:

```
// first, make an RDF model containing the triples:
Model model = ModelFactory.createDefaultModel();
Resource meeting = model.getResource("wm:/SELF#meeting");
meeting.addProperty(SCHEMA.VALID_FROM, "2019-12-11T20:00:00+01:00", XSDDatatype.XSDdateTime);
meeting.addProperty(SCHEMA.VALID_THROUGH, "2019-12-11T22:00:00+01:00", XSDDatatype.XSDdateTime);
meeting.addProperty(SCHEMA.TITLE, "Our Meeting");
// then, create the command and publish it
ConnectionMessageCommandEvent cmd = new ConnectionMessageCommandEvent(con, model);
getEventListenerContext().getEventBus().publish(cmd);
```


## HowTo: Getting the Message Content
Situation: you received a `WonMessage msg` and you want to use its content.

Options:

You are expecting only a text message: 

```
WonRdfUtils.MessageUtils.getTextMessage(msg);
```

You are expecting some other RDF triples: obtain the message's *content graphs* and analyze them:

```
Dataset content = msg.getMessageContent();  
// now process the content
```



## HowTo: Processing Non-Standard Atom/Message Data 
Situation: you have a `Dataset dataset` either obtained from a `WonMessage` or from an `Atom` (see respective How-Tos), and you want to extract some data structure from it. The generic approach is to use a SPARQL query on the dataset. Please refer to the [HowTo on processing RDF](#howto-processing-rdf).

## HowTo: Processing RDF
Situation: you have a `Model`(i.e. a set of `Statement`s), or a `Dataset`(i.e. a set of `Model`s) and you want to get at some of the data or write some into it.

1. Refer to the [Apache Jena documentation](https://jena.apache.org/documentation/rdf/index.html).
2. Look at `won.protocol.util.RdfUtils`- generic RDF utils (not  WoN-specific) 
3. Look at `won.protocol.util.WonRdfUtils`- WoN-specific utils, organized by type of entitiy (Atom/Socket/...)
4. Many URIs you need for creating RDF are found as constants in 
    - `won.protocol.vocabulary` (e.g. `WON.Atom`, `WONMSG.CreateMessage`, `WXCHAT.ChatSocket`)
    - `org.apache.jena.vocabulary`
    
**Print to Stdout**

Dataset: 

```
RDFDataMgr.write(System.out, dataset, Lang.TRIG);
```

Model: 

```
RDFDataMgr.write(System.out, model, Lang.TTL);
```

**Execute a SPARQL query**

Here, we use the query that finds all triples in the `Dataset dataset`:

```
String queryString = "select * where {?a ?b ?c}";
Query query = QueryFactory.create(queryString);
try (QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
    qexec.getContext().set(TDB.symUnionDefaultGraph, true); // use this unless you know why not to - it will execute the query over the union of all graphs. Makes things easier.
    ResultSet rs = qexec.execSelect();
    if (rs.hasNext()) {
        QuerySolution qs = rs.nextSolution();
        System.out.println(String.format("?a: %s, ?b: %s, ?c: %s", qs.get("a"), qs.get("b"), qs.get("c"));
    }
}
```
