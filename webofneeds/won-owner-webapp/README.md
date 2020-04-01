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

- `actionTypes`, which contains string-constants (e.g. actionTypes.atoms.close === 'atoms.close')
- `actionCreators`, which houses the action creators. for the sake of injecting them with ng-redux, they are organised with `__` as seperator (e.g. `actionCreators.atoms__close()`)

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

Top-level components (views in the angular-sense) have their own folders (e.g. `app/components/create-atom/` and are split in two files).
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
$ngRedux.getState().get("router");
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
$ngRedux.getState();
/* =>
{
 config: {
   defaultNodeUri: [nodeuri]
 },
 events: {
   events: {...},
   unreadEventUris: {...},
 },
 lastUpdateTime: timeinmillis,
 messages: {
   lostConnection: true|false,
   reconnecting: true|false,
   waitingForAnswer: {...}
 },
 atoms: {
   [atomUri]: {
       connections: { //Immutable.Map() containing all corresponding Connections to this atom
           [connectionUri]: {
               creationDate: date, //creationDate of the connection
               lastUpdateDate: date, //date of lastUpdate of this connection (last date of the message that was added)
               messages: { //Immutable.Map() of all the TextMessages sent over this connection
                   [messageUri]: {
                       messageType: //no default but is always set to the specific message type of the received/sent wonMessage
                       date: date, //creation Date of this message
                       unread: true|false, //whether or not this message is new (or already seen if you will)
                       forwardMessage: true|false, //default is false, flag to indicate if this is a message coming from another connection, and is referenced in the references->forwards of another message (this is a flag to indicate if the message is visible or not)
                       outgoingMessage: true|false, //flag to indicate if this was an outgoing or incoming message
                       systemMessage: true|false, //flag to indicate if this message came from the system (e.g. hint messages) !wonMessage.isFromOwner() && !wonMessage.getSenderAtom() && wonMessage.getSenderNode(),
                       senderUri: uri //to indicate which atom or node sent the message itself, wonMessage.getSenderAtom() || wonMessage.getSenderNode(),
                       content: {
                           text: wonMessage.getTextMessage(),
                           matchScore: wonMessage.getMatchScore(),
                           [and other details which are parsed from the detail-definitions are stored here]
                       },
                       references: {
                           //These references are parsed in a way that it will always be a list no matter if there is only a single element or an array
                           forwards: wonMessage.getForwardMessageUris(),
                           proposes: wonMessage.getProposedMessageUris(),
                           proposesToCancel: wonMessage.getProposedToCancelMessageUris(),
                           accepts: wonMessage.getAcceptsMessageUris(),
                           rejects: wonMessage.getRejectsMessageUris(),
                           retracts: wonMessage.getRetractsMessageUris(),
                           claims: wonMessage.getClaimsMessageUris(),
                       }
                       hasReferences: true|false //whether it contains any non-null/non-undefined references within the references block of the message
                       hasContent: true|false //whether it contains any non-null/non-undefined content within the content block of the message
                       injectInto: undefined or an array of connectionUris this message is injected into
                       originatorUri: undefined or the uri of the post/atom which initiated the forwardMessage (the one who injected the msg)
                       isParsable: true|false //true if hasReferences or hasContent is true
                       isMessageStatusUpToDate: true|false //true if the agreementData has been checked to define the status of the message
                       messageStatus: {
                           isProposed: true|false //if the message was proposed
                           isClaimed: true|false //if the message was claimed
                           isRetracted: true|false //if the message was retracted
                           isRejected: true|false //if the message was rejected
                           isAccepted: true|false //if the message was accepted
                           isAgreed: true|false //if the message is part of an agreement
                           isCancellationPending: true|false //if the message is pending to be cancelled
                           isCancelled: true|false //if the message was cancelled
                       },
                       viewState: {
                           //TODO: everything in this state should be extracted into its own (view/ui)-state, this is only here so we do not have a huge (fail prone refactoring shortly before the codefreeze)
                           isSelected: true|false //whether or not the message is Selected in the MultiSelect view
                           isCollapsed: true|false //default is false, whether or not the message should be displayed in a minimized fashion
                           showActions: true|false //default is false, whether or not the actionButtons of a message are visible
                           expandedReferences: {
                             forwards: true,
                             claims: false,
                             proposes: false,
                             proposesToCancel: false,
                             accepts: false,
                             rejects: false,
                             retracts: false,
                           }
                       },
                       uri: string //unique identifier of this message (same as messageUri)
                       isReceivedByOwn: true|false //whether the sent request/message is received by the own server or not (default: false, if its not an outgoingMessage the default is true)
                       isReceivedByRemote: true|false //whether the sent request/message is received by the remote server or not (default: false, if its not an outgoingMessage the default is true)
                       failedToSend: true|false //whether the sent message failed for whatever reason (default: false, only relevant in outgoingMessages)
                   }
                   ...
               },
               agreementData: { //contains agreementData that is necessary to display for the user
                   agreementUris: Immutable.Set(),
                   agreedMessageUris: Immutable.Set(),
                   pendingProposalUris: Immutable.Set(),
                   pendingCancellationProposalUris: Immutable.Set(),
                   cancellationPendingAgreementUris: Immutable.Set(),
                   acceptedCancellationProposalUris: Immutable.Set(),
                   cancelledAgreementUris: Immutable.Set(),
                   proposedMessageUris: Immutable.Set(),
                   claimedMessageUris: Immutable.Set(),
                   rejectedMessageUris: Immutable.Set(),
                   retractedMessageUris: Immutable.Set(),
               },
               petriNetData: Immutable.Map(),
               showAgreementData: true|false // default is false, whether or not the agreementDataPanel is active
               showPetriNetData: true|false // defautl is false, whether or not the petriNetDataPanel is active
               multiSelectType: String // default is undefined, indicates which action is supposed to happen for the multiselect messages
               unread: true|false, //whether or not this connection is new (or already seen if you will)
               isRated: true|false, //whether or not this connection has been rated yet
               targetAtomUri: string, //corresponding remote Atom identifier
               targetConnectionUri: string, //corresponding remote Connection uri
               state: string, //state of the connection
               uri: string //unique identifier of this connection
           }
           ...
       },
       creationDate: Date, //creationDate of this atom
       lastUpdateDate: date, //date of lastUpdate of this atom (last date of the message or connection that was added)
       nodeUri: string, //identifier of this atom's server
       isBeingCreated: true|false, //whether or not the creation of this atom was successfully completed yet
       state: "won:Active" | "won:Inactive", //state of the atom
       groupMembers: Immutable.List() // atomUris of participants of this atoms (won:groupMember) -> usually only set for groupChatAtoms
       holds: Immutable.List() // atomUris of the persona that holds these additional atoms
       unread: true|false, //whether or not this atom has new information that has not been read yet
       uri: string, //unique identifier of this atom
       humanReadable: string, //a human Readable String that parses the content from is or seeks and searchString and makes a title out of it based on a certain logic
       matchedUseCase: { //saves a matchedUseCase within the atom so we dont have to parse it multiple times
           identifier: undefined, //matched identifier that is set within the matched usecase
           icon: undefined, //matched icon that is set within the matched usecase
       },
       background: //generated background color based on a hash of the atom-uri, (similar to identiconSvg),
       content : {...},
       seeks: {...}
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
 account: {
    ...,
    acceptedDisclaimer: true|false, //flag whether the user has accepted the ToS etc. already. (default is false)
 },
 view: {
     showRdf: true|false, //flag that is true if rawData mode is on (enables rdf view and rdf links) (default is false)
     showClosedAtoms: true|false, //flag whether the drawer of the closedAtoms is open or closed (default is false)
     showMainMenu: true|false, //flag whether the mainmenu dropdown is open or closed (default is false)
     showModalDialog: true|false, //flag whether the omnipresent modal dialog is displayed or not (default is false)
     modalDialog: {
        caption: string, //header caption of the modal dialog
        text: string,
        buttons: [ //Array
            {
            caption: string //caption of the button
            callback: callback //action of the button
            }
            ...
        ]
    }
 },
 process: {
    processingPublish: true|false, //default false, flag that is true if an atom(or persona) is currently being created
    processingLogout: true|false, //default false, flag that indicates if a logout is currently in process
    processingInitialLoad: true|false //flag that indicates if the initialLoad is currently in process
    processingLogin: true|false, //default false flag that indicates if a login is currently in process
    processingLoginForEmail: undefined, //indicates which is user is currently being logged in undefined if nouser is currently being logged in, otherwise the email will be stored
    processingAcceptTermsOfService: false, //indicates if the rest-call to accept the terms of service is currently pending
    processingVerifyEmailAddress: false, //indicates if the rest-call to verify the email address is currently pending
    processingResendVerificationEmail: false, //indicates if the rest-call to resend the verification mail is currently pending
    atoms: {
        [atomUri]: {
            failedToLoad: true|false, //whether or not the atom has failed to load (due to delete or other)
            loading: true|false, //whether or not the atom is currently in the process of being loaded
            toLoad: true|false, //whether or not the atom is flagged as toLoad (for future loading purposes)
        }
    },
    connections: {
        [connUri]: {
           failedToLoad: true|false, //default is false, whether or not this connection was able to be loaded or not
           loadingMessages: true|false, //default is false, whether or not this connection is currently loading messages or processing agreements
           loading: true|false, //default is false, whether or not this connection is currently loading itself (similar to the loading in the atom)
           messages: {
            [messageUri]: {
                loading: true|false, //if the message is currently being loaded
                failedToLoad: true|false, //if the message failed to load (fetch failed from the backend)
                toLoad: true|false, //if the message is to be loaded (to figure out if a connection has messages that have not been loaded yet)
            }
           }
           petriNetData: {
                loading: true|false, //default is false, whether or not the petriNetData has been loaded,
                dirty: true|false //default is false, whether or not the currently stored petriNetData is (assumed to be) not correct anymore
                loaded: true|false //default is false, whether or not the petriNetData has been loaded already
           },
           agreementData: {
                loaded: true|false, //default is false, whether or not the agreementData has been loaded already
                loading: true|false, //default is false, whether or not the agreementData has been loaded,
           },
        }
    }

 }
}
*/
```

As you can see in this State all "visible" Data is stored within the atoms and the corresponding connections and messages are stored within this tree.
Example: If you want to retrieve all present connections for a given atom you will access it by `$ngRedux.getState().getIn(["atoms", [atomUri], "connections"])`.

All The DataParsing happens within the `atom-reducer.js` and should only be implemented here, in their respective Methods `parseAtom(jsonLdAtom)`, `parseConnection(jsonLdConnection, unread)` and `parseMessage(jsonLdMessage, outgoingMessage, unread)`.
It is very important to not parse atoms/connections/messages in any other place or in any other way to make sure that the structure of the corresponding items is always the same, and so that the Views don't have to implement fail-safes when accessing elements, e.g. a Location is only present if the whole location data can be parsed/stored within the state, otherwise the location will stay empty.
This is also true for every message connection and atom, as soon as the data is in the state you can be certain that all the mandatory values are set correctly.

### Data Structure

The `is` and `seeks` parts in the state displayed above store all details of a given atom. All available detail types are defined in `detail-definitions.js` and added to atoms via use cases defined in `usecase-definitions.js`. All details in `detail-definitions.js` have a default `parseToRDF({value, identifier})` and `parseFromRDF(jsonLDImm)` functions that are used for all data parsing.
There are also abstractDetails, which are not considered complete Details but provide a stub to a picker and a viewer component, e.g. abstractDetails.number, you have to write the parseToRDF, parseFromRDF functions, and set the identifier, label, and icon, as they do not have a default value.

To adjust details for individual use cases, the data parsing functions should be overwritten in `usecase-definitions.js`. For parsing, **all details defined in any use case** will be considered. To avoid unexpected behaviour, `detail.identifier` must be unique across all use cases and must not be "search", as this literal is used to recognise full-text searches. Additonally, if two or more use cases use the same `parseToRDF({value, identifier})` function or use the same RDF predicates, information may not be correctly recognised. E.g., if one use case parses a "description" to be saved as `dc:description`, and another use case parses a "biography" to also be saved as `dc:description`, "description" and "biography" can't be told apart while parsing. As a result, which `parseFromRDF(sonLDImm)` is used for parsing the information depends on the order of use case definitions.

Details are represented in the state as part of the atom or a branch `seeks`, for example:

```javascript
/*
seeks : {
           title: string, //title of the atom
           description: string, //description of the atom as a string (non mandatory, empty if not present)
           tags: Array of strings, //array of strings (non mandatory, empty if not present)
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
           travelAction: { //non mandatory, may contain half or all of the elements below
               fromAddress: string,
               fromLocation: {
                   lat: float, 
                   lng: float,
               },
               toAddress: string,
               toLocation: {
                   lat: float, 
                   lng: float, 
               }
           }
       }
*/
```

## Server-Interaction

If it's **REST**-style, just use `fetch(...).then(...dispatch...)` in an action-creator.

If it's **linked-data-related**, use the utilities in `linkeddata-service-won.js`.
They'll do standard HTTP(S) but will make sure to cache as much as possible via the local triplestore.

If you want to **receive stuff the web-socket**, go to `actions.js` and add your handlers to the `messages__messageReceived`-actioncreator. The same I said about pushing to the web-socket also holds here.

# Tooling

See:

- [Angular 2.0](https://github.com/researchstudio-sat/webofneeds/issues/300) -> it wasn't ready at the time of the decision
- [Precompilation and Tooling (Bundling, CSS, ES6)](https://github.com/researchstudio-sat/webofneeds/issues/314)
