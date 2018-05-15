/**
 * Created by ksinger on 28.09.2015.
 */

import Immutable from "immutable";

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
export function combineReducersStable(mapOfReducers) {
  return (state = Immutable.Map(), action = {}) => {
    let hasChanged = false;
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
