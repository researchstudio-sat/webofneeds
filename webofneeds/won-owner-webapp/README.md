# Architecture

We're using the redux-architecture for the client.

**Before you read anything else, check out [redux](http://redux.js.org/) and [ng-redux](https://github.com/wbuchwalter/ng-redux)**. The documentation on these two pages is very good and there's no need to repeat them here. Thus anything below will assume you have a basic understanding of actions, reducers, the store and components, as well as how they look like in angular. The purpose of this text is to document additional architectural details, specific coding style and learnings not documented in these two as well as any cases in which our architecture diverges from theirs.

![redux architecture in client-side owner-app](http://researchstudio-sat.github.io/webofneeds/images/owner_app_redux_architecture.svg)



## Action Creators

Can be found in `app/actions/actions.js`

Anything that can cause **side-effects** or is **asynchronous** should happen in these (tough it needn't necessarily - see `INJ_DEFAULT`)
They should only be triggered by either the user or a push from the server via the `messagingAgent.js`.
In both cases they cause a **single**(!) action to be dispatched (~= passed as input to the reducer).

If you want to **add new action-creators** do so by adding to the `actionHierarcy`-object in `actions.js`.
From that two objects are generated at the moment:

* `actionTypes`, which contains string-constants (e.g. actionTypes.needs.close === 'needs.close')
* `actionCreators`, which houses the action creators. for the sake of injecting them with ng-redux, they are organised with `__` as seperator (e.g. `actionCreators.needs__close()`)

Btw, the easiest way for actions without sideffects is to just placing an `myAction: INJ_DEFAULT`.
This results in an action-creator that just dispatches all function-arguments as payload,
i.e. `actionCreators.myAction = argument => ({type: 'myAction', payload: argument})`

Action(Creators) should always be **high-level user stories/interactions** - no `matches.add` please!
Action-creators encapsule all sideeffectful computation, as opposed to the reducers which (within the limits of javascript)
are guaranteed to be side-effect-free. Thus we should do **as much as possible within the reducers**.
This decreases the suprise-factor/coupling/bug-proneness of our code and increases its maintainability.

## Actions

Can be found in `app/reducers/reducers.js`

They are objects like `{type: 'drafts.change.title', payload: 'some title'}` and serve as input for the reducers.

See: [Actions/Stores and Synching](https://github.com/researchstudio-sat/webofneeds/issues/342)

## Reducers

Can be found in `app/reducers/reducers.js`

These are **side-effect-free**. Thus as much of the implementation as possible should be here instead of in the action-creators (see "Action Creators" above for more detail)

## Components

They live in `app/components/`.

Top-level components (views in the angular-sense) have their own folders (e.g. `app/components/create-need/` and are split in two files).
You'll need to add them to the routing (see below) to be able to switch the routing-state to these.

Non-top-level components are implemented as directives.

In both cases open up the latest implemented component and use the boilerplate from these, if you want to implement your own.
Once a refined/stable boilerplate version has emerged, it should be documented here.

## Routing

We use [ui-router](https://github.com/angular-ui/ui-router/wiki/Quick-Reference) and in particular the [redux-wrapper for it](https://github.com/neilff/redux-ui-router)

Routing(-states, aka URLs) are configured in `configRouting.js`.
State changes can be triggered via `actionCreators.router__stateGo(stateName)`.
The current routing-state and -parameters can be found in our app-state:

 ```javascript
$ngRedux.getState().get('router')
/* =>
{
  currentParams: {...},
  currentState: {...},
  prevParams: {...},
  prevState: {...}
}
*/
```

Also see: [Routing and Redux](https://github.com/researchstudio-sat/webofneeds/issues/344)

## State Structure
 ```javascript
$ngRedux.getState()
/* =>
{
  config: {
    defaultNodeUri: [nodeuri]
  },
  events: {
    events: {...},
    unreadEventUris: {...},
  },
  initialLoadFinished: true|false,
  lastUpdateTime: timeinmillis,
  messages: {
    enqueued: {...},
    lostConnection: true|false,
    reconnecting: true|false,
    waitingForAnswer: {...}
  },
  needs: {
    [needUri]: {
        connections: { //Immutable.Map() containing all corresponding Connections to this need
            [connectionUri]: {
                creationDate: date, //creationDate of the connection
                lastUpdateDate: date, //date of lastUpdate of this connection (last date of the message that was added)
                messages: { //Immutable.Map() of all the TextMessages sent over this connection
                    [messageUri]: {
                        connectMessage: true|false, //whether or not this was the connectMessage(a.k.a firstMessage)
                        date: date, //creation Date of this message
                        unread: true|false, //whether or not this message is new (or already seen if you will)
                        outgoingMessage: true|false, //flag to indicate if this was an outgoing or incoming message
                        text: string, //message text
                        uri: string //unique identifier of this message
                    }
                    ...
                },
                isLoading: true|false, //default is false, whether or not this connection is currently loading messages or processing agreements
                unread: true|false, //whether or not this connection is new (or already seen if you will)
                isRated: true|false, //whether or not this connection has been rated yet
                remoteNeedUri: string, //corresponding remote Need identifier
                state: string, //state of the connection
                uri: string //unique identifier of this connection
            }
            ...
        },
        creationDate: Date, //creationDate of this need
        lastUpdateDate: date, //date of lastUpdate of this need (last date of the message or connection that was added)
        unread: true|false, //whether or not this need has new information that has not been read yet
        description: string, //description of the need as a string (non mandatory, empty if not present)
        location: { //non mandatory but if present it contains all elements below
            address: string, //address as human readable string
            lat: float, //latitude of address
            lng: float, //longitude of address
            nwCorner: { //north west corner of the boundingbox
                lat: float,
                lng: float,
            },
            seCorner: { //south east corner of the boundingbox
                lat: float,
                lng: float,
            }
        },
        ownNeed: true|false, //whether this need is owned or not
        state: "won:Active" | "won:Inactive", //state of the need
        tags: Array of strings, //array of strings (non mandatory, empty if not present)
        title: string, //title of the need
        type: "won:Supply" | "won:Demand" | "won:Offer", //type of the need
        uri: string //unique identifier of this need
    },
    ...
  },
  router: {
    currentParams: {...},
    currentState: {...},
    prevParams: {...},
    prevState: {...}
  },
  toasts: {...},
  user: {...},
  showRdf: true|false, //flag that is true if rawData mode is on (enables rdf view and rdf links) (default is false)
  showClosedNeeds: true|false, //flag whether the drawer of the closedNeeds is open or closed (default is false)
  showMainMenu: true|false, //flag whether the mainmenu dropdown is open or closed (default is false)
}
*/
```
As you can see in this State all "visible" Data is stored within the needs and the corresponding connections and messages are stored within this tree.
Example: If you want to retrieve all present connections for a given need you will access it by ````$ngRedux.getState().getIn(["needs", [needUri], "connections"])````.

All The DataParsing happens within the ```need-reducer.js``` and should only be implemented here, in their respective Methods ```parseNeed(jsonLdNeed, ownNeed)```, ```parseConnection(jsonLdConnection, unread)``` and ```parseMessage(jsonLdMessage, outgoingMessage, unread)```.
It is very important to not parse needs/connections/messages in any other place or in any other way to make sure that the structure of the corresponding items is always the same, and so that the Views don't have to implement fail-safes when accessing elements, e.g. a Location is only present if the whole location data can be parsed/stored within the state, otherwise the location will stay empty.
This is also true for every message connection and need, as soon as the data is in the state you can be certain that all the mandatory values are set correctly.

## Server-Interaction

If it's **REST**-style, just use `fetch(...).then(...dispatch...)` in an action-creator.

If it's **linked-data-related**, use the utilities in `linkeddata-service-won.js`.
They'll do standard HTTP(S) but will make sure to cache as much as possible via the local triplestore.

If needs to **push to the web-socket**, add a hook for the respective *user(!)*-action in `message-reducers.js`.
The `messaging-agent.js` will pick up any messages in `$ngRedux.getState().getIn(['messages', 'enqueued'])`
and push them to it's websocket. This solution appears rather hacky to me (see 'high-level interactions' under 'Action Creators') and I'd be thrilled to hear any alternative solutions :)

If you want to **receive stuff the web-socket**, go to `actions.js` and add your handlers to the `messages__messageReceived`-actioncreator. The same I said about pushing to the web-socket also holds here.

# Tooling

See:

* [Angular 2.0](https://github.com/researchstudio-sat/webofneeds/issues/300) -> it wasn't ready at the time of the decision
* [Precompilation and Tooling (Bundling, CSS, ES6)](https://github.com/researchstudio-sat/webofneeds/issues/314)

