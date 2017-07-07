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

