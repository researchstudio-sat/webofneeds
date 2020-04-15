/**
 * Created by ksinger on 07.08.2017.
 */

import { actionCreators } from "./actions.js";
import { resetParams, addConstParams } from "../configRouting.js";
import { getCurrentParamsFromRoute } from "../redux/selectors/general-selectors.js";

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
