/**
 * Created by ksinger on 04.08.2017.
 */

import won from "../won-es6.js";
import { buildCreateMessage, buildEditMessage } from "../won-message-utils.js";

import { actionCreators, actionTypes } from "./actions.js";

import { ensureLoggedIn } from "./account-actions.js";

import { get, getIn } from "../utils.js";

import * as accountUtils from "../redux/utils/account-utils.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as ownerApi from "../api/owner-api.js";

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
      const { message, atomUri } = await buildEditMessage(
        draft,
        oldAtom,
        nodeUri
      );

      ownerApi.sendMessage(message).then(jsonResp => {
        dispatch({
          type: actionTypes.atoms.edit,
          payload: {
            eventUri: jsonResp.messageUri,
            message: jsonResp.message,
            atomUri: atomUri,
            atom: draft,
            oldAtom,
          },
        });

        dispatch(actionCreators.router__back());
      });
    });
  };
}

export function atomCreate(draft, personaUri, nodeUri) {
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
      const { message, atomUri } = await buildCreateMessage(draft, nodeUri);

      ownerApi.sendMessage(message).then(jsonResp => {
        dispatch({
          type: actionTypes.atoms.create,
          payload: {
            eventUri: jsonResp.messageUri,
            message: jsonResp.message,
            atomUri: atomUri,
            atom: draft,
          },
        });
      });

      const persona = getIn(state, ["atoms", personaUri]);
      if (persona) {
        return ownerApi
          .serverSideConnect(
            atomUtils.getSocketUri(persona, won.HOLD.HolderSocketCompacted),
            `${atomUri}#holdableSocket`,
            false,
            true
          )
          .then(async response => {
            if (!response.ok) {
              const errorMsg = await response.text();
              throw new Error(`Could not connect identity: ${errorMsg}`);
            }
          });
      }
    });
  };
}
