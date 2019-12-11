Small HowTos:

## Bot HowTo: How to send any kind of message (and react when it succeeds/fails)
Situation: you want to send a message (e.g. to create a new Atom, connect, close, whatever). The easiest way to do this is to activate the `ExecuteMessageCommandBehaviour`:

```
ExecuteWonMessageCommandBehaviour wonMessageCommandBehaviour = new ExecuteWonMessageCommandBehaviour(ctx);
wonMessageCommandBehaviour.activate();
```
and then publish the command, for example the `CreateAtomCommandEvent`:

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
// insert at position (a) above, if needed
EventListenerContext ctx = getEventListenerContext();
ctx.getEventBus().subscribe(
    CreateAtomCommandSuccessEvent.class, 
    new ActionOnFirstEventListener( //note the 'onFIRSTevent' in the name: the listener is destroyed after being invoked once.
        ctx, 
        new CommandResultFilter(createCommand),  // only listen for success to the command we just made
        new BaseEventBotAction(ctx) { 
                @Override
                protected void doRun(Event event, EventListener executingListener) {
                                      //your action - whatever you want to do:  
                }
        }));
```
Much in the same way, you can listen for Failure events (e.g. the `CreateAtomCommandFailureEvent`) or any kind of result (e.g., the `CreateAtomCommandResultEvent`).


## Bot HowTo: How to connect to an Atom
Situation: you have two atom URIs. One of them is an atom you control (the other, maybe, too). You know which socket types you want to connect, for example the [holder socket](https://w3id.org/won/ext/hold#HolderSocket) and the [holdable socket](https://w3id.org/won/ext/hold#HoldableSocket). Follow the next HowTo to find out how to get the Socket URIs. We'll assume you call them `senderSocketURI` and `recipientSocketURI`.

```
String message = "Hello, let's connect!"; //optional welcome message
ConnectCommandEvent connectCommandEvent = new ConnectCommandEvent(
                                    senderSocketURI,recipientSocketURI, message);
getEventBus().publish(connectCommandEvent);
```

Note: this only works if you have activated the  `MessageCommandBehaviour`. 



## Bot HowTo: How to get Socket from Atom

Situation: you have the URI of an atom you want to connect to. You know which socketType you want to use, for example, the [chat Socket](https://w3id.org/won/ext/chat#ChatSocket). Here is how you find the URI of the socket:
```
URI atomURI = // your atom URI
URI socketURI = WXCHAT.ChatSocket; //or any other socket type you want
LinkedDataSource = getEventListenerContext().getLinkedDataSource();
Collection<URI> sockets = WonLinkedDataUtils.getSocketsOfType(atomURI, socketURI, linkedDataSource).
//sockets should have 0 or 1 items
if (sockets.isEmpty()){
    //did not find a socket of that type
}
URI socket = sockets.get(0);
```

## Bot HowTo: Get atom data from Atom

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
        }); ```
