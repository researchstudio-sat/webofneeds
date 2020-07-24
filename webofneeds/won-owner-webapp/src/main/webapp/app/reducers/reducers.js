/**
 * Created by ksinger on 24.09.2015.
 */

import { actionTypes } from "../actions/actions.js";
import Immutable from "immutable";
import { getUri } from "../utils.js";
import { messagesReducer } from "./message-reducers.js";
import reduceReducers from "reduce-reducers";
import externalDataReducer from "./external-data-reducer.js";
import atomReducer from "./atom-reducer/atom-reducer-main.js";
import accountReducer from "./account-reducer.js";
import toastReducer from "./toast-reducer.js";
import viewReducer from "./view-reducer.js";
import processReducer from "./process-reducer.js";

const initialOwnerState = Immutable.fromJS({
  whatsNewUris: Immutable.Set(),
  whatsAroundUris: Immutable.Set(),
  lastWhatsNewUpdateTime: undefined,
  lastWhatsAroundUpdateTime: undefined,
  lastWhatsAroundLocation: undefined,
  lastWhatsAroundMaxDistance: undefined,
});

const initialConfigState = Immutable.fromJS({ theme: { name: "current" } });

const reducers = {
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
  atoms: atomReducer,
  externalData: externalDataReducer,
  messages: messagesReducer,
  toasts: toastReducer,
  view: viewReducer,
  process: processReducer,

  // contains the Date.now() of the last action
  // lastUpdateTime: (state = Date.now(), action = {}) => Date.now(),
  lastUpdateTime: () => Date.now(),

  config: (config = initialConfigState, action = {}) => {
    switch (action.type) {
      case actionTypes.config.init:
      case actionTypes.config.update:
        return config.mergeDeep(action.payload);

      default:
        return config;
    }
  },
  owner: (owner = initialOwnerState, action = {}) => {
    switch (action.type) {
      case actionTypes.account.reset:
        return initialOwnerState;

      case actionTypes.atoms.storeWhatsNew: {
        const metaAtoms = action.payload.get("metaAtoms");

        metaAtoms &&
          metaAtoms.map((metaAtom, metaAtomUri) => {
            owner = owner.update("whatsNewUris", whatsNewUris =>
              whatsNewUris.add(metaAtomUri)
            );
          });

        return owner.set("lastWhatsNewUpdateTime", Date.now());
      }

      case actionTypes.atoms.storeWhatsAround: {
        const metaAtoms = action.payload.get("metaAtoms");
        const location = action.payload.get("location");
        const maxDistance = action.payload.get("maxDistance");

        metaAtoms &&
          metaAtoms.map((metaAtom, metaAtomUri) => {
            owner = owner.update("whatsAroundUris", whatsAroundUris =>
              whatsAroundUris.add(metaAtomUri)
            );
          });

        return owner
          .set("lastWhatsAroundMaxDistance", maxDistance)
          .set("lastWhatsAroundLocation", location)
          .set("lastWhatsAroundUpdateTime", Date.now());
      }

      case actionTypes.atoms.removeDeleted:
      case actionTypes.atoms.delete: {
        const atomUri = getUri(action.payload);
        const whatsAroundUris = owner.get("whatsAroundUris");
        const whatsNewUris = owner.get("whatsNewUris");

        return owner
          .set("whatsAroundUris", whatsAroundUris.remove(atomUri))
          .set("whatsNewUris", whatsNewUris.remove(atomUri));
      }

      default:
        return owner;
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
  combineReducersStable(Immutable.Map(reducers))
);

window.Immutable4dbg = Immutable;

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
