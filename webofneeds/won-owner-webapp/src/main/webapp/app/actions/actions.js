/**
 h Created by ksinger on 23.09.2015.
 *
 * Contains a list of actions to be used with the dispatcher and documentation
 * for their expected payloads.
 *
 * # Redux Primer - Actions
 *
 * Actions are small objects like:
 *
 * `{type: 'someaction', payload: {...}}`
 *
 * that are usually created via action-creators (ACs), e.g.:
 *
 * `function someaction(args) { return { type: 'someaction', payload: args }}`
 *
 * and then passed on to the reducer via `redux.dispatch(action)`.
 *
 * *Note:* The calls to `$ngRedux.connect` wrap the ACs in this call to `dispatch`
 *
 * # Best Practices
 *
 * Even though it's possible to have ACs trigger multiple ACs (which is
 * necessary asynchronous actions), try avoiding that. All actions are
 * broadcasted to all reducers anyway.  Mostly it's a symptom of actions
 * that aren't high-level enough. (high-level: `publish`,
 * low-level: `inDraftSetPublishPending`).
 *
 * ACs function is to do simple data-processing that is needed by multiple
 * reducers (e.g. creating the post-publish messages that are needed by
 * the drafts-reducer as well) and dealing with side-effects (e.g. routing,
 * http-calls)
 *
 * As a rule of thumb the lion's share of all processing should happen
 * in the reducers.
 */

import won from "../won-es6.js";

// <utils>

import { tree2constants, entries } from "../utils.js";
import { hierarchy2Creators } from "./action-utils.js";
import {
  buildCloseNeedMessage,
  buildOpenNeedMessage,
} from "../won-message-utils.js";

import {
  needCreate,
  createWhatsNew,
  createWhatsAround,
} from "./create-need-action.js";

import {
  needsConnect,
  fetchUnloadedNeeds,
  fetchSuggested,
} from "./needs-actions.js";

import {
  stateBack,
  stateGoAbs,
  stateGoCurrent,
  stateGoDefault,
  stateGoKeepParams,
  stateGoResetParams,
} from "./cstm-router-actions.js";

// </utils>

// <action-creators>

import {
  accountLogin,
  accountLogout,
  accountRegister,
  accountTransfer,
  accountAcceptDisclaimer,
  reconnect,
} from "./account-actions.js";

import * as cnct from "./connections-actions.js";
import * as messages from "./messages-actions.js";
import * as configActions from "./config-actions.js";

import { pageLoadAction } from "./load-action.js";
import { stateGo, stateReload } from "redux-ui-router";
import { createPersona, reviewPersona } from "./persona-actions.js";

// </action-creators>

/**
 * all values equal to this string will be replaced by action-creators that simply
 * passes it's argument on as payload on to the reducers
 */
const INJ_DEFAULT = "INJECT_DEFAULT_ACTION_CREATOR";
const actionHierarchy = {
  initialPageLoad: pageLoadAction,
  connections: {
    open: cnct.connectionsOpen,
    connectAdHoc: cnct.connectionsConnectAdHoc,
    close: cnct.connectionsClose,
    closeRemote: cnct.connectionsCloseRemote,
    rate: cnct.connectionsRate,
    sendChatMessage: cnct.connectionsChatMessage,
    sendChatMessageClaimOnSuccess: cnct.connectionsChatMessageClaimOnSuccess,
    sendChatMessageRefreshDataOnSuccess: INJ_DEFAULT, //will be dispatched solely within sendChatMessage (only if chatMessage that is sent contains References)
    sendChatMessageFailed: INJ_DEFAULT,
    showLatestMessages: cnct.showLatestMessages,
    showMoreMessages: cnct.showMoreMessages,
    markAsRead: INJ_DEFAULT,
    setLoadingAgreementData: INJ_DEFAULT,
    setLoadingPetriNetData: INJ_DEFAULT,
    setLoadingMessages: INJ_DEFAULT,
    showAgreementData: INJ_DEFAULT,
    showPetriNetData: INJ_DEFAULT,
    setMultiSelectType: INJ_DEFAULT,
    updateAgreementData: INJ_DEFAULT, //cnct.loadAgreementData,
    updatePetriNetData: INJ_DEFAULT,
  },
  needs: {
    received: INJ_DEFAULT,
    connectionsReceived: INJ_DEFAULT,
    clean: INJ_DEFAULT,
    create: needCreate,
    whatsNew: createWhatsNew,
    whatsAround: createWhatsAround,
    createSuccessful: INJ_DEFAULT,
    reopen: needsOpen,
    close: needsClose,
    closedBySystem: needsClosedBySystem,
    failed: INJ_DEFAULT,
    connect: needsConnect,
    fetchUnloadedNeeds: fetchUnloadedNeeds,
    fetchSuggested: fetchSuggested,
  },
  personas: {
    create: createPersona,
    createSuccessful: INJ_DEFAULT,
    review: reviewPersona,
  },
  router: {
    stateGo, // only overwrites parameters that are explicitly mentioned, unless called without queryParams object (which also resets "pervasive" parameters, that shouldn't be removed
    stateGoAbs, // reset's all parameters but the one passed as arguments
    stateGoResetParams, // goes to new state and resets all parameters (except for "pervasive" ones like `privateId`)
    stateGoKeepParams, // goes to new state and keeps listed parameters at their current values
    stateGoCurrent,
    stateGoDefault,
    stateReload,
    //stateTransitionTo, // should not be used directly
    back: stateBack,
    accessedNonLoadedPost: INJ_DEFAULT, //dispatched in configRouting.js
  },
  posts: {
    load: INJ_DEFAULT,
    clean: INJ_DEFAULT,
  },

  /**
   * Server triggered interactions (aka received messages)
   */
  messages: {
    /* websocket messages, e.g. post-creation, chatting */
    //TODO get rid of send and rename to receivedMessage

    send: INJ_DEFAULT, //TODO this should be part of proper, user-story-level actions (e.g. need.publish or sendCnctMsg)

    /*
         * posting things to the server should be optimistic and assume
         * success that is rolled back in case of a failure or timeout.
         */

    create: {
      success: messages.successfulCreate,
      //TODO failure: messages.failedCreate
    },
    open: {
      successRemote: INJ_DEFAULT, //2nd successResponse
      successOwn: INJ_DEFAULT, //1st successResponse
      failure: INJ_DEFAULT,
      //TODO failure: messages.failedOpen
    },
    close: {
      //TODO: NAME SEEMS GENERIC EVEN THOUGH IT IS ONLY USED FOR CLOSING CONNECITONS; REFACTOR THIS SOMEDAY
      success: messages.successfulCloseConnection,
      //TODO failure: messages.failedClose
    },
    connect: {
      //success: messages.successfulConnect,
      successRemote: INJ_DEFAULT, //2nd successResponse
      successOwn: INJ_DEFAULT, //1st successResponse
      failure: INJ_DEFAULT,
    },
    chatMessage: {
      //success: messages.successfulChatMessage,
      successRemote: INJ_DEFAULT, //2nd successResponse
      successOwn: INJ_DEFAULT, //1st successResponse
      failure: INJ_DEFAULT,
    },
    closeNeed: {
      success: messages.successfulCloseNeed,
      failure: messages.failedCloseNeed,
    },
    reopenNeed: {
      success: messages.successfulReopenNeed,
      failure: messages.failedReopenNeed,
    },
    markAsRead: INJ_DEFAULT,
    messageStatus: {
      markAsProposed: messages.markAsProposed,
      markAsClaimed: messages.markAsClaimed,
      markAsRetracted: messages.markAsRetracted,
      markAsRejected: messages.markAsRejected,
      markAsAccepted: messages.markAsAccepted,
      markAsCancelled: messages.markAsCancelled,
      markAsCancellationPending: messages.markAsCancellationPending,
    },
    viewState: {
      markAsCollapsed: INJ_DEFAULT,
      markAsSelected: INJ_DEFAULT,
      markShowActions: INJ_DEFAULT,
      markExpandReference: INJ_DEFAULT,
    },
    updateMessageStatus: messages.updateMessageStatus,
    processConnectionMessage: messages.processConnectionMessage,
    processAgreementMessage: messages.processAgreementMessage,
    needMessageReceived: messages.needMessageReceived,
    processConnectMessage: messages.processConnectMessage,
    connectMessageReceived: INJ_DEFAULT,
    connectMessageSent: INJ_DEFAULT,
    processHintMessage: messages.processHintMessage,
    hintMessageReceived: INJ_DEFAULT,
    openMessageReceived: INJ_DEFAULT,
    openMessageSent: INJ_DEFAULT,
    processOpenMessage: messages.processOpenMessage,

    // register a fully prepared action object to be dispatched
    // after a specific message (identified by URI)  has been processed
    // (when the respective success message is processed)
    dispatchActionOn: {
      // registers the action (in action.actionToDispatch)
      registerSuccessOwn: INJ_DEFAULT,
      registerFailureOwn: INJ_DEFAULT,
      registerSuccessRemote: INJ_DEFAULT,
      registerFailureRemote: INJ_DEFAULT,
      // the action creator dispatches the registered actions
      successOwn: messages.dispatchActionOnSuccessOwn,
      successRemote: messages.dispatchActionOnSuccessRemote,
      // failure actions clear the list of registered success actions
      // and vice versa
      failureOwn: messages.dispatchActionOnSuccessOwn,
      failureRemote: messages.dispatchActionOnSuccessOwn,
    },

    waitingForAnswer: INJ_DEFAULT,
  },
  //anonymousLogin: anonAccountLogin,
  loginStarted: INJ_DEFAULT,
  login: accountLogin, //loginSuccess
  loginFailed: INJ_DEFAULT,
  logoutStarted: INJ_DEFAULT,
  logout: accountLogout,
  register: accountRegister,
  transfer: accountTransfer,
  acceptDisclaimer: accountAcceptDisclaimer,
  acceptDisclaimerSuccess: INJ_DEFAULT,
  typedAtLoginCredentials: INJ_DEFAULT,
  registerReset: INJ_DEFAULT,
  registerFailed: INJ_DEFAULT,
  geoLocationDenied: INJ_DEFAULT,
  lostConnection: INJ_DEFAULT,
  failedToGetLocation: INJ_DEFAULT,

  reconnect: {
    start: reconnect,
    success: INJ_DEFAULT,
    startingToLoadConnectionData: INJ_DEFAULT,
    receivedConnectionData: INJ_DEFAULT,
    connectionFailedToLoad: INJ_DEFAULT,
  },

  toggleRdfDisplay: INJ_DEFAULT,
  toggleClosedNeedsDisplay: INJ_DEFAULT,
  hideClosedNeedsDisplay: INJ_DEFAULT,
  showClosedNeedsDisplay: INJ_DEFAULT,

  showMainMenuDisplay: INJ_DEFAULT,
  hideMainMenuDisplay: INJ_DEFAULT,

  toggleAddMessageContentDisplay: INJ_DEFAULT,
  showAddMessageContentDisplay: INJ_DEFAULT,
  hideAddMessageContentDisplay: INJ_DEFAULT,

  selectAddMessageContent: INJ_DEFAULT,
  removeAddMessageContent: INJ_DEFAULT,

  openModalDialog: INJ_DEFAULT,
  closeModalDialog: INJ_DEFAULT,

  toasts: {
    delete: INJ_DEFAULT,
    test: INJ_DEFAULT,
    push: INJ_DEFAULT,
  },
  config: {
    init: configActions.configInit,
    update: configActions.update,
  },
  tick: startTicking,
};

//as string constans, e.g. actionTypes.needs.close === "needs.close"
export const actionTypes = tree2constants(actionHierarchy);

/**
 * actionCreators are functions that take the payload and output
 * an action object, thus prebinding the action-type.
 * This object follows the structure of the actionTypes-object,
 * but is flattened for use with ng-redux. Thus calling
 * `$ngRedux.dispatch(actionCreators.drafts__new(myDraft))` will trigger an action
 * `{type: actionTypes.drafts.new, payload: myDraft}`
 *
 * e.g.:
 *
 * ```javascript
 * function newDraft(draft) {
 *   return { type: 'draft.new', payload: draft }
 * }
 * ```
 */
export const actionCreators = hierarchy2Creators(actionHierarchy);

/*
 * TODO deletme; for debugging
 */
window.actionCreators4Dbg = actionCreators;
window.actionTypes4Dbg = actionTypes;

//////////// STUFF THAT SHOULD BE IN OTHER FILES BELOW //////////////////

export function startTicking() {
  return dispatch =>
    setInterval(
      () => dispatch({ type: actionTypes.tick, payload: Date.now() }),
      60000
    );
}

/**
 * @param needUri
 * @param remoteNeedUri
 * @param connectionUri
 * @return {*}
 */
export function getConnectionRelatedData(
  needUri,
  remoteNeedUri,
  connectionUri
) {
  const remoteNeedP = won.getNeed(remoteNeedUri);
  const ownedNeedP = won.getNeed(needUri);
  const connectionP = won.getConnectionWithEventUris(connectionUri, {
    requesterWebId: needUri,
  });
  const eventsP = won
    .getEventsOfConnection(connectionUri, { requesterWebId: needUri })
    .then(eventsLookup => {
      const eventList = [];
      for (let [, event] of entries(eventsLookup)) {
        eventList.push(event);
      }
      return eventList;
    });

  const remotePersonaP = remoteNeedP.then(remoteNeed => {
    const remoteHeldBy = remoteNeed && remoteNeed["won:heldBy"];
    const remotePersonaUri = remoteHeldBy && remoteHeldBy["@id"];

    if (remotePersonaUri) {
      return won.getNeed(remotePersonaUri);
    } else {
      return Promise.resolve(undefined);
    }
  });

  const ownPersonaP = ownedNeedP.then(ownedNeed => {
    const ownHeldBy = ownedNeed && ownedNeed["won:heldBy"];
    const ownPersonaUri = ownHeldBy && ownHeldBy["@id"];

    if (ownPersonaUri) {
      return won.getNeed(ownPersonaUri);
    } else {
      return Promise.resolve(undefined);
    }
  });

  return Promise.all([
    remoteNeedP,
    ownedNeedP,
    connectionP,
    eventsP,
    remotePersonaP,
    ownPersonaP,
  ]).then(results => {
    return {
      remoteNeed: results[0],
      ownedNeed: results[1],
      connection: results[2],
      events: results[3],
      remotePersona: results[4],
      ownPersona: results[5],
    };
  });
}

export function needsOpen(needUri) {
  return (dispatch, getState) => {
    buildOpenNeedMessage(
      needUri,
      getState().getIn(["config", "defaultNodeUri"])
    )
      .then(data => {
        dispatch(
          actionCreators.messages__send({
            eventUri: data.eventUri,
            message: data.message,
          })
        );
      })
      .then(() =>
        // assume close went through successfully, update GUI
        dispatch({
          type: actionTypes.needs.reopen,
          payload: {
            ownedNeedUri: needUri,
          },
        })
      );
  };
}

export function needsClosedBySystem(event) {
  return (dispatch, getState) => {
    //first check if we really have the 'own' need in the state - otherwise we'll ignore the message
    const need = getState().getIn(["needs", event.getReceiverNeed()]);
    if (!need) {
      console.debug(
        "ignoring deactivateMessage for a need that is not ours:",
        event.getReceiverNeed()
      );
    }
    dispatch({
      type: actionTypes.needs.closedBySystem,
      payload: {
        needUri: event.getReceiverNeed(),
        humanReadable: need.get("humanReadable"),
        message: event.getTextMessage(),
      },
    });
  };
}

export function needsClose(needUri) {
  return (dispatch, getState) => {
    buildCloseNeedMessage(
      needUri,
      getState().getIn(["config", "defaultNodeUri"])
    )
      .then(data => {
        dispatch(
          actionCreators.messages__send({
            eventUri: data.eventUri,
            message: data.message,
          })
        );

        //Close all the open connections of the need
        getState()
          .getIn(["needs", needUri, "connections"])
          .map(function(con) {
            if (
              getState().getIn([
                "needs",
                needUri,
                "connections",
                con.get("uri"),
                "state",
              ]) === won.WON.Connected
            ) {
              dispatch(actionCreators.connections__close(con.get("uri")));
            }
          });
      })
      .then(() =>
        // assume close went through successfully, update GUI
        dispatch({
          type: actionTypes.needs.close,
          payload: {
            ownedNeedUri: needUri,
          },
        })
      );
  };
}
