/**
 * Created by ksinger on 07.08.2017.
 */

import Immutable from "immutable";
import { getIn } from "../utils.js";
import { actionCreators } from "./actions.js";
import {
  defaultRoute,
  resetParams,
  resetParamsImm,
  addConstParams,
} from "../configRouting.js";
import { getCurrentParamsFromRoute } from "../redux/selectors/general-selectors.js";

/**
 * Action-Creator that goes back in the browser history
 * without leaving the app.
 * @param dispatch
 * @param getState
 */
export function stateBack() {
  return (dispatch, getState) => {
    const hasPreviousState = !!getIn(getState(), [
      "router",
      "prevState",
      "name",
    ]);
    if (hasPreviousState) {
      history.back();
    } else {
      dispatch(actionCreators.router__stateGoDefault());
    }
  };
}

/**
 * reset's all parameters but the one passed as arguments
 */
export function stateGoAbs(state, queryParams) {
  return (dispatch, getState) => {
    const currentParams = getCurrentParamsFromRoute(getState());
    return dispatch(
      actionCreators.router__stateGo(
        state,
        addConstParams(resetParamsImm.merge(queryParams), currentParams)
      )
    );
  };
}

/**
 * goes to new state and resets all parameters (except for "pervasive" ones like `privateId`)
 */
export function stateGoResetParams(state) {
  return (dispatch, getState) => {
    const currentParams = getCurrentParamsFromRoute(getState());
    return dispatch(
      actionCreators.router__stateGo(
        state,
        addConstParams(resetParams, currentParams)
      )
    );
  };
}

export function stateGoDefault() {
  return dispatch => {
    return dispatch(actionCreators.router__stateGoResetParams(defaultRoute));
  };
}

/**
 * goes to new state and keeps listed parameters at their current values
 */
export function stateGoKeepParams(state, queryParamsList) {
  return (dispatch, getState) => {
    const currentParams = getCurrentParamsFromRoute(getState());
    const params = Immutable.Map(
      // [[k,v]] -> Map
      queryParamsList.map(
        p => [p, currentParams[p]] // get value per param
      )
    );
    return dispatch(
      actionCreators.router__stateGo(
        state,
        addConstParams(params, currentParams)
      )
    );
  };
}

/**
 * goes to current state, but changes the parameters
 * passed to this function.
 * @param queryParams
 */
export function stateGoCurrent(queryParams) {
  return (dispatch, getState) => {
    const currentState = getIn(getState(), ["router", "currentState", "name"]);
    const currentParams = getCurrentParamsFromRoute(getState());
    return dispatch(
      actionCreators.router__stateGo(
        currentState,
        addConstParams(queryParams, currentParams)
      )
    );
  };
}
