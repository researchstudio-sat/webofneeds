/**
 * Created by ksinger on 04.08.2017.
 */

import { buildCreateMessage, buildEditMessage } from "../won-message-utils.js";

import { actionCreators, actionTypes } from "./actions.js";

import { ensureLoggedIn } from "./account-actions.js";

import { get, getIn } from "../utils.js";

import * as accountUtils from "../account-utils.js";
import * as ownerApi from "../owner-api";

export function atomEdit(draft, oldAtom, nodeUri) {
  return (dispatch, getState) => {
    const state = getState();

    if (!nodeUri) {
      nodeUri = getIn(state, ["config", "defaultNodeUri"]);
    }

    let prevParams = getIn(state, ["router", "prevParams"]);

    if (
      !accountUtils.isLoggedIn(get(state, "account")) &&
      prevParams.privateId
    ) {
      /*
       * `ensureLoggedIn` will generate a new privateId. should
       * there be a previous privateId, we don't want to change
       * back to that later.
       */
      prevParams = Object.assign({}, prevParams);
      delete prevParams.privateId;
    }

    return ensureLoggedIn(dispatch, getState).then(async () => {
      const { message, eventUri, atomUri } = await buildEditMessage(
        draft,
        oldAtom,
        nodeUri
      );

      dispatch({
        type: actionTypes.atoms.edit,
        payload: { eventUri, message, atomUri, atom: draft, oldAtom },
      });

      dispatch(actionCreators.router__back());
    });
  };
}

export function atomCreate(draft, persona, nodeUri) {
  return (dispatch, getState) => {
    const state = getState();

    if (!nodeUri) {
      nodeUri = getIn(state, ["config", "defaultNodeUri"]);
    }

    let prevParams = getIn(state, ["router", "prevParams"]);

    if (
      !accountUtils.isLoggedIn(get(state, "account")) &&
      prevParams.privateId
    ) {
      /*
             * `ensureLoggedIn` will generate a new privateId. should
             * there be a previous privateId, we don't want to change
             * back to that later.
             */
      prevParams = Object.assign({}, prevParams);
      delete prevParams.privateId;
    }

    return ensureLoggedIn(dispatch, getState).then(async () => {
      const { message, eventUri, atomUri } = await buildCreateMessage(
        draft,
        nodeUri
      );
      if (persona) {
        return ownerApi
          .serverSideConnect(
            `${persona}#holderSocket`,
            `${atomUri}#holdableSocket`,
            false,
            true
          )
          .then(async response => {
            if (!response.ok) {
              const errorMsg = await response.text();
              throw new Error(`Could not connect identity: ${errorMsg}`);
            }
            dispatch({
              type: actionTypes.atoms.create,
              payload: { eventUri, message, atomUri, atom: draft },
            });
          });
      } else {
        dispatch({
          type: actionTypes.atoms.create,
          payload: { eventUri, message, atomUri, atom: draft },
        });
      }
    });
  };
}
