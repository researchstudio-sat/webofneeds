/**
 * Created by ksinger on 04.08.2017.
 */

import { buildCreateMessage, buildEditMessage } from "../won-message-utils.js";

import { actionCreators, actionTypes } from "./actions.js";

import { ensureLoggedIn } from "./account-actions.js";

import {
  get,
  getIn,
  reverseSearchNominatim,
  nominatim2draftLocation,
} from "../utils.js";

import { isWhatsAroundNeed, isWhatsNewNeed } from "../need-utils.js";
import * as accountUtils from "../account-utils.js";

export function needEdit(draft, oldNeed, nodeUri) {
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
      const { message, eventUri, needUri } = await buildEditMessage(
        draft,
        oldNeed,
        nodeUri
      );

      dispatch({
        type: actionTypes.needs.edit,
        payload: { eventUri, message, needUri, need: draft, oldNeed },
      });

      dispatch(actionCreators.router__back());
    });
  };
}

export function needCreate(draft, persona, nodeUri) {
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

    return ensureLoggedIn(dispatch, getState)
      .then(() => {
        return dispatch(actionCreators.router__stateGoDefault());
      })
      .then(async () => {
        const { message, eventUri, needUri } = await buildCreateMessage(
          draft,
          nodeUri
        );
        if (persona) {
          const response = await fetch("rest/action/connect", {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
            },
            body: JSON.stringify([
              {
                pending: false,
                facet: `${persona}#holderFacet`,
              },
              {
                pending: true,
                facet: `${needUri}#holdableFacet`,
              },
            ]),
            credentials: "include",
          });
          if (!response.ok) {
            const errorMsg = await response.text();
            throw new Error(`Could not connect identity: ${errorMsg}`);
          }
        }
        dispatch({
          type: actionTypes.needs.create,
          payload: { eventUri, message, needUri, need: draft },
        });

        dispatch(
          actionCreators.router__stateGoAbs("connections", {
            postUri: undefined,
            connectionUri: undefined,
          })
        );
      });
  };
}

export function createWhatsNew() {
  return (dispatch, getState) => {
    const state = getState();
    const nodeUri = getIn(state, ["config", "defaultNodeUri"]);
    const defaultContext = getIn(state, ["config", "theme", "defaultContext"]);

    dispatch({ type: actionTypes.needs.whatsNew });

    const whatsNewObject = {
      content: {
        flags: ["won:WhatsNew", "won:NoHintForCounterpart"],
      },
      seeks: {},
      matchingContext: defaultContext,
    };

    getIn(state, ["needs"])
      .filter(need => isWhatsAroundNeed(need) || isWhatsNewNeed(need))
      .map(need => {
        dispatch(actionCreators.needs__delete(need.get("uri")));
      });

    dispatch(actionCreators.needs__create(whatsNewObject, null, nodeUri));
  };
}

export function createWhatsAround() {
  return (dispatch, getState) => {
    const state = getState();
    const nodeUri = getIn(state, ["config", "defaultNodeUri"]);
    const defaultContext = getIn(state, ["config", "theme", "defaultContext"]);

    dispatch({ type: actionTypes.needs.whatsAround });

    if ("geolocation" in navigator) {
      navigator.geolocation.getCurrentPosition(
        currentLocation => {
          const lat = currentLocation.coords.latitude;
          const lng = currentLocation.coords.longitude;
          const zoom = 13; // TODO use `currentLocation.coords.accuracy` to control coarseness of query / zoom-level

          reverseSearchNominatim(lat, lng, zoom).then(searchResult => {
            const location = nominatim2draftLocation(searchResult);

            getIn(state, ["needs"])
              .filter(need => isWhatsAroundNeed(need) || isWhatsNewNeed(need))
              .map(need => {
                dispatch(actionCreators.needs__delete(need.get("uri"))); //TODO action creators should not call other action creators, according to Moru
              });
            const whatsAroundObject = {
              content: {
                flags: ["won:WhatsAround", "won:NoHintForCounterpart"],
              },
              seeks: { location: location },
              matchingContext: defaultContext,
            };

            dispatch(
              actionCreators.needs__create(whatsAroundObject, null, nodeUri)
            );
          });
        },
        error => {
          //error handler
          dispatch({ type: actionTypes.failedToGetLocation });
          console.error(
            "Could not retrieve geolocation due to error: ",
            error.code,
            "fullerror:",
            error
          );
        },
        {
          //options
          enableHighAccuracy: true,
          maximumAge: 0,
        }
      );
    }
  };
}
