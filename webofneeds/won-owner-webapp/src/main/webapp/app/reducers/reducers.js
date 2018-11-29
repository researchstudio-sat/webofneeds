/**
 * Created by ksinger on 24.09.2015.
 */

import { actionTypes } from "../actions/actions.js";
import Immutable from "immutable";
import { messagesReducer } from "./message-reducers.js";
import { isChatConnection } from "../connection-utils.js";
import reduceReducers from "reduce-reducers";
import needReducer from "./need-reducer/need-reducer-main.js";
import accountReducer from "./account-reducer.js";
import toastReducer from "./toast-reducer.js";
import viewReducer from "./view-reducer.js";
import processReducer from "./process-reducer.js";
import { getIn } from "../utils.js";
/*
 * this reducer attaches a 'router' object to our state that keeps the routing state.
 */
import { router } from "redux-ui-router";

const reducers = {
  router,

  /**
     * Example for a simple reducer:
     *
    simplereducer: (state = initialState, action) => {
        switch(action.type) {
            case actionTypes.moreWub:
                return state.setIn(...);
            default:
                return state;
        }
    },
    */

  account: accountReducer,
  needs: needReducer,
  messages: messagesReducer,
  toasts: toastReducer,
  view: viewReducer,
  process: processReducer,

  // contains the Date.now() of the last action
  // lastUpdateTime: (state = Date.now(), action = {}) => Date.now(),
  lastUpdateTime: () => Date.now(),

  initialLoadFinished: (state = false, action = {}) =>
    state ||
    (action.type === actionTypes.initialPageLoad &&
      getIn(action, ["payload", "initialLoadFinished"])),

  config: (
    config = Immutable.fromJS({ theme: { name: "current" } }),
    action = {}
  ) => {
    switch (action.type) {
      case actionTypes.config.init:
      case actionTypes.config.update:
        return config.mergeDeep(action.payload);

      default:
        return config;
    }
  },
};

export default reduceReducers(
  //passes on the state from one reducer to another

  /* note that `combineReducers` is opinionated as a root reducer for the
     * sake of convenience and ease of first use. It takes an object
     * with seperate reducers and applies each to it's seperate part of the
     * store/model. e.g.: an reducers object `{ drafts: function(state = [], action){...} }`
     * would result in a store like `{ drafts: [...] }`
     */
  combineReducersStable(Immutable.Map(reducers)),

  /*--------------------- <cross-cutting-reducer> -------------------
     *
     * combineReducers above parcels out the state to individual
     * reducers that deal with their slice on their own. Sadly not
     * all updates can work like this. Occasionally there's cross-cutting
     * concerns between parts of the state. These can be addressed in
     * this reducer.
     *
     * **How to use me:**
     *
     * * Try not to create new branches of the state here (this is
     *   what combineReducers is for)
     * * Make sure the actual methods updating a respective part
     *   of the state are in the js-file responsible for that part
     *   of the state. This is to ensure that all write-accesses
     *   to the data can be discerned from that one file.
     *
     * @dependent state: https://github.com/rackt/redux/issues/749
     *
     * also, if you need to resolve the data-dependency just for
     * a component, you can use [memoized selectors]
     * (http://rackt.org/redux/docs/recipes/ComputingDerivedData.html)
     */
  (state, action) => {
    switch (action.type) {
      /**
       * Add all actions that load connections
       * and their events. The reducer here makes
       * sure that no connections between two needs
       * that both are owned by the user, remain
       * in the state.
       */
      case actionTypes.initialPageLoad:
      case actionTypes.account.login:
      case actionTypes.messages.connectMessageSent:
      case actionTypes.messages.connectMessageReceived:
      case actionTypes.messages.hintMessageReceived:
        return deleteChatConnectionsBetweenOwnedNeeds(state);

      default:
        return state;
    }
  }
  //-------------------- </cross-cutting-reducer> -------------------
);

window.Immutable4dbg = Immutable;

function deleteChatConnectionsBetweenOwnedNeeds(state) {
  let needs = state.get("needs");

  if (needs) {
    needs = needs.map(function(need) {
      let connections = need.get("connections");

      connections =
        connections &&
        connections.filter(function(conn) {
          //Any connection that is not of type chatFacet will be exempt from deletion
          if (isChatConnection(conn)) {
            //Any other connection will be checked if it would be connected to the ownedNeed, if so we remove it.
            return !state.getIn([
              "needs",
              conn.get("remoteNeedUri"),
              "isOwned",
            ]);
          }
          return true;
        });
      return need.set("connections", connections);
    });
    return state.set("needs", needs);
  }

  return state;
}

/**
 * Reducers are functions of type (state, action) => state.
 * `combineReducersStable` takes a `Map<String,Reducer>` where
 * the key should equal the name of the subtree the reducer applies to.
 * Thus the result of `combineReducersStable` is a combined reducer that
 * applies to the whole state but delegates to the individual reducers for
 * their respective 'domain'.
 *
 * The 'stable' in the function name signals that if none of the children-reducers
 * have modified their individual domains, the root-state-object is exactly the
 * same as the state before (down to the object-reference as simply the same object
 * is returned). This avoids e.g. unnecessary redraws on angular that just checks whether
 * or not the object reference of an observed variable has changed.
 *
 * The reducers should have a default-value for their state-parameter, as they
 * get called to initialize their domain.
 *
 * E.g.:
 *
 * ``` javascript
 * mapOfReducers = {
 *      counter: (state = 0, action) => {
 *          switch (action.type) {
 *          case 'INCREMENT':
 *            return state + 1;
 *          default:
 *            return state;
 *      },
 *      todos: (state = Immutable.List(), action) => ...
 * }
 * ```
 *
 * would lead to an initial state like the following:
 *
 * ```javascript
 * state = {
 *      counter: 0,
 *      todos: Imutable.List()
 * }
 * ```
 *
 * and on future actions the counter-reducer would always get called
 * with the integer as `state`-parameter and the todos-reducer
 * with the list as `state`-parameter (and is expected to return
 * that list or an updated version).
 *
 * @param {Immutable.Map} mapOfReducers
 * @returns {Function} a combined reducer that applies to the whole state.
 */
function combineReducersStable(mapOfReducers) {
  return (state = Immutable.Map(), action = {}) => {
    let updatedState = state;
    mapOfReducers.forEach((reducer, domainName) => {
      // the domain is the child-node the reducer is responsible for
      const domain = state.get(domainName);

      // update the domain. if the domain hasn't been created yet,
      // let the reducer handle that.
      const actionCurriedReducer = s => reducer(s, action);
      const updatedDomain = domain
        ? actionCurriedReducer(domain)
        : actionCurriedReducer();

      // only change the state object,
      if (domain !== updatedDomain)
        updatedState = updatedState.set(domainName, updatedDomain);
    });

    return updatedState;
  };
}
