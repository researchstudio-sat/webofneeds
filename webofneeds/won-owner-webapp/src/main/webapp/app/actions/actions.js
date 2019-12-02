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
// <utils>

import { atomCreate, atomEdit } from "./create-atom-action.js";

import {
  atomsClose,
  atomsClosedBySystem,
  atomsConnect,
  atomsConnectSockets,
  atomsDelete,
  atomsOpen,
  fetchUnloadedAtom,
} from "./atoms-actions.js";

import {
  stateBack,
  stateGoAbs,
  stateGoCurrent,
  stateGoDefault,
  stateGoKeepParams,
  stateGoResetParams,
} from "./cstm-router-actions.js";
import {
  accountAcceptDisclaimer,
  accountAcceptTermsOfService,
  accountChangePassword,
  accountLogin,
  accountLogout,
  accountRegister,
  accountResendVerificationEmail,
  accountSendAnonymousLinkEmail,
  accountTransfer,
  accountVerifyEmailAddress,
  reconnect,
} from "./account-actions.js";

import * as cnct from "./connections-actions.js";
import * as messages from "./messages-actions.js";
import * as configActions from "./config-actions.js";

import {
  fetchPersonas,
  fetchWhatsAround,
  fetchWhatsNew,
  pageLoadAction,
} from "./load-action.js";
import { stateGo, stateReload } from "redux-ui-router";
import {
  connectPersona,
  disconnectPersona,
  reviewPersona,
} from "./persona-actions.js";
import { deepFreeze } from "../utils.js";
import won from "../won-es6";

// </utils>

// <action-creators>

// </action-creators>

/**
 * all values equal to this string will be replaced by action-creators that simply
 * passes it's argument on as payload on to the reducers
 */
const INJ_DEFAULT = "INJECT_DEFAULT_ACTION_CREATOR";
const actionHierarchy = {
  initialPageLoad: pageLoadAction,
  initialLoadFinished: INJ_DEFAULT,
  connections: {
    connectAdHoc: cnct.connectionsConnectAdHoc,
    connectReactionAtom: cnct.connectionsConnectReactionAtom,
    close: cnct.connectionsClose,
    closeRemote: cnct.connectionsCloseRemote,
    rate: cnct.connectionsRate,
    sendChatMessage: cnct.connectionsChatMessage,
    sendChatMessageClaimOnSuccess: cnct.connectionsChatMessageClaimOnSuccess,
    sendChatMessageRefreshDataOnSuccess: INJ_DEFAULT, //will be dispatched solely within sendChatMessage (only if chatMessage that is sent contains References)
    sendChatMessageFailed: INJ_DEFAULT,
    showLatestMessages: cnct.showLatestMessages,
    showMoreMessages: cnct.showMoreMessages,
    fetchMessagesStart: INJ_DEFAULT,
    fetchMessagesEnd: INJ_DEFAULT,
    messageUrisInLoading: INJ_DEFAULT,
    fetchMessagesFailed: INJ_DEFAULT,
    fetchMessagesSuccess: INJ_DEFAULT,
    markAsRead: INJ_DEFAULT,
    setLoadingAgreementData: INJ_DEFAULT,
    setLoadingPetriNetData: INJ_DEFAULT,
    showAgreementData: INJ_DEFAULT,
    showPetriNetData: INJ_DEFAULT,
    setMultiSelectType: INJ_DEFAULT,
    updateAgreementData: INJ_DEFAULT, //cnct.loadAgreementData,
    updatePetriNetData: INJ_DEFAULT,

    storeActiveUrisInLoading: INJ_DEFAULT,
    storeMetaConnections: INJ_DEFAULT,
    storeActive: INJ_DEFAULT,

    storeUriFailed: INJ_DEFAULT,
  },
  atoms: {
    received: INJ_DEFAULT,
    connectionsReceived: INJ_DEFAULT,
    create: atomCreate,
    edit: atomEdit,
    editSuccessful: INJ_DEFAULT,
    editFailure: INJ_DEFAULT,
    createSuccessful: INJ_DEFAULT,
    reopen: atomsOpen,
    close: atomsClose,
    delete: atomsDelete,
    closedBySystem: atomsClosedBySystem,
    failed: INJ_DEFAULT,
    connect: atomsConnect,
    connectSockets: atomsConnectSockets,
    fetchUnloadedAtom: fetchUnloadedAtom,

    fetchMetaAtoms: INJ_DEFAULT,
    fetchWhatsNew: fetchWhatsNew,
    fetchPersonas: fetchPersonas,
    fetchWhatsAround: fetchWhatsAround,
    storeMetaAtoms: INJ_DEFAULT,
    storeWhatsNew: INJ_DEFAULT,
    storeWhatsAround: INJ_DEFAULT,

    storeOwnedMetaAtoms: INJ_DEFAULT,
    storeUriInLoading: INJ_DEFAULT,

    store: INJ_DEFAULT,

    storeUriFailed: INJ_DEFAULT,
    removeDeleted: INJ_DEFAULT,
    selectTab: INJ_DEFAULT,
  },
  personas: {
    review: reviewPersona,
    connect: connectPersona,
    disconnect: disconnectPersona,

    fetchPersonas: fetchPersonas,

    store: INJ_DEFAULT,
    storeUriInLoading: INJ_DEFAULT,

    storeUriFailed: INJ_DEFAULT,
    removeDeleted: INJ_DEFAULT,
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
  },

  /**
   * Server triggered interactions (aka received messages)
   */
  messages: {
    /* websocket messages, e.g. post-creation, chatting */
    //TODO get rid of send and rename to receivedMessage

    send: INJ_DEFAULT, //TODO this should be part of proper, user-story-level actions (e.g. atom.publish or sendCnctMsg)

    /*
         * posting things to the server should be optimistic and assume
         * success that is rolled back in case of a failure or timeout.
         */

    create: {
      success: messages.successfulCreate,
      //TODO failure: messages.failedCreate
    },
    edit: {
      success: messages.successfulEdit,
      //TODO failure: messages.failedEdit
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
    closeAtom: {
      success: messages.successfulCloseAtom,
      failure: messages.failedCloseAtom,
    },
    reopenAtom: {
      success: messages.successfulReopenAtom,
      failure: messages.failedReopenAtom,
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
    processChangeNotificationMessage: messages.processChangeNotificationMessage,
    processAgreementMessage: messages.processAgreementMessage,
    atomMessageReceived: messages.atomMessageReceived,
    processConnectMessage: messages.processConnectMessage,
    connectMessageReceived: INJ_DEFAULT,
    connectMessageSent: INJ_DEFAULT,
    processAtomHintMessage: messages.processAtomHintMessage,
    processSocketHintMessage: messages.processSocketHintMessage,

    waitingForAnswer: INJ_DEFAULT,
  },
  account: {
    login: accountLogin,
    loginStarted: INJ_DEFAULT, //will only be dispatched on login not on page reload
    loginFinished: INJ_DEFAULT, //will be dispatched when data has been loaded on login not on page reload
    loginFailed: INJ_DEFAULT,

    store: INJ_DEFAULT, //stores the retrieved account in the state
    reset: INJ_DEFAULT, //resets the retrieved account back to the initialState

    logout: accountLogout,
    logoutStarted: INJ_DEFAULT,
    logoutFinished: INJ_DEFAULT,

    register: accountRegister,
    transfer: accountTransfer,
    registerFailed: INJ_DEFAULT,

    acceptDisclaimer: accountAcceptDisclaimer,
    acceptDisclaimerSuccess: INJ_DEFAULT,

    changePassword: accountChangePassword,
    changePasswordSuccess: INJ_DEFAULT,
    changePasswordFailed: INJ_DEFAULT,

    acceptTermsOfService: accountAcceptTermsOfService,
    acceptTermsOfServiceStarted: INJ_DEFAULT,
    acceptTermsOfServiceSuccess: INJ_DEFAULT,
    acceptTermsOfServiceFailed: INJ_DEFAULT,

    verifyEmailAddress: accountVerifyEmailAddress,
    verifyEmailAddressStarted: INJ_DEFAULT,
    verifyEmailAddressSuccess: INJ_DEFAULT,
    verifyEmailAddressFailed: INJ_DEFAULT,

    resendVerificationEmail: accountResendVerificationEmail,
    resendVerificationEmailStarted: INJ_DEFAULT,
    resendVerificationEmailSuccess: INJ_DEFAULT,
    resendVerificationEmailFailed: INJ_DEFAULT,

    sendAnonymousLinkEmail: accountSendAnonymousLinkEmail,
    sendAnonymousLinkEmailStarted: INJ_DEFAULT,
    sendAnonymousLinkEmailSuccess: INJ_DEFAULT,
    sendAnonymousLinkEmailFailed: INJ_DEFAULT,

    copiedAnonymousLinkSuccess: INJ_DEFAULT,
  },

  geoLocationDenied: INJ_DEFAULT,
  lostConnection: INJ_DEFAULT,
  failedToGetLocation: INJ_DEFAULT,
  upgradeHttpSession: INJ_DEFAULT,
  downgradeHttpSession: INJ_DEFAULT,

  reconnect: {
    start: reconnect,
    success: INJ_DEFAULT,
  },

  view: {
    toggleRdf: INJ_DEFAULT,
    toggleDebugMode: toggleDebugMode,

    toggleClosedAtoms: INJ_DEFAULT,

    showMainMenu: INJ_DEFAULT,
    hideMainMenu: INJ_DEFAULT,

    showMenu: INJ_DEFAULT,
    hideMenu: INJ_DEFAULT,
    toggleMenu: INJ_DEFAULT,

    toggleAddMessageContent: INJ_DEFAULT,
    hideAddMessageContent: INJ_DEFAULT,

    selectAddMessageContent: INJ_DEFAULT,
    removeAddMessageContent: INJ_DEFAULT,

    showTermsDialog: INJ_DEFAULT,
    showModalDialog: INJ_DEFAULT,
    hideModalDialog: INJ_DEFAULT,

    clearLoginError: INJ_DEFAULT,
    clearRegisterError: INJ_DEFAULT,

    locationAccessDenied: INJ_DEFAULT,
    updateCurrentLocation: INJ_DEFAULT,

    anonymousSlideIn: {
      show: INJ_DEFAULT,
      hide: INJ_DEFAULT,
      showEmailInput: INJ_DEFAULT,
    },

    toggleSlideIns: INJ_DEFAULT,
  },

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

//****** SERVICE AND HELPER FUNCTIONS ********************
/*
 * @param obj an object-tree.
 *
 * @param prefix add a custom prefix to all generated constants.
 *
 * @returns a tree using the same structure as `o` but with
 *          all leaves being strings equal to their lookup path.
 * e.g.:
 * tree2constants({foo: null}) -> {foo: 'foo'}
 * tree2constants{{foo: {bar: null}}) -> {foo: {bar: 'foo.bar'}}
 * tree2constants{foo: null}, 'pfx') -> {foo: 'pfx.foo'}
 */
function tree2constants(obj, prefix = "") {
  //wrap prefix in array
  prefix = prefix === "" ? [] : [prefix];

  return deepFreeze(
    reduceAndMapTreeKeys(
      (acc, k) => acc.concat(k),
      acc => acc.join("."),
      prefix,
      obj
    )
  );
}

/**
 * Traverses an object-tree and produces an object
 * that is just one level deep but concatenating the
 * traversal path.
 *
 * ```
 * flattenTree({
 *   myInt: 1,
 *   myObj: {
 *      myProp: 2,
 *      myStr: 'asdf',
 *      foo: {
 *        bar: 3
 *      }
 *   }
 * });
 * // result:
 * // {
 * //   'myInt': 1,
 * //   'myObj__myProp' : 2,
 * //   'myObj__myStr' : 'asdf',
 * //   'myObj__foo__bar' : 3
 * // }
 * ```
 *
 * @param tree {object} the object-tree
 * @param delimiter {string} will be used to join the path. by default `__`
 * @returns {object} the flattened object
 */
function flattenTree(tree, delimiter = "__") {
  const accObj = {}; //the accumulator accObject
  function _flattenTree(node, pathAcc = []) {
    for (let k of Object.keys(node)) {
      const pathAccUpd = pathAcc.concat(k);
      if (typeof node[k] === "object" && node[k] !== null) {
        _flattenTree(node[k], pathAccUpd);
      } else {
        const propertyName = pathAccUpd.join(delimiter);
        accObj[propertyName] = node[k];
      }
    }
  }
  _flattenTree(tree);
  return accObj;
}

function hierarchy2Creators(actionHierarchy) {
  const actionTypes = tree2constants(actionHierarchy);
  return Object.freeze(
    flattenTree(
      reduceAndMapTreeKeys(
        (path, k) => path.concat(k), //construct paths, e.g. ['draft', 'new']
        path => {
          /* leaf can either be a defined creator or a
                 * placeholder asking to generate one.
                 */
          const potentialCreator = won.lookup(actionHierarchy, path);
          if (typeof potentialCreator === "function") {
            return potentialCreator; //already a defined creator. hopefully.
          } else {
            const type = won.lookup(actionTypes, path);
            return createActionCreator(type);
          }
        },
        [],
        actionHierarchy
      ),
      "__"
    )
  );
}

function createActionCreator(type) {
  return payload => {
    return { type, payload };
  };
}

/**
 * Traverses down an object, reducing the keys with the reducer
 * and then applying the mapper once it reaches the leaves.
 * The function doesn't modify the input-object.
 * @param obj
 * @param acc the initial accumulator
 * @param reducer (acc, key) => newAcc
 * @param mapper (acc) => newAcc
 * @returns {*}
 */
function reduceAndMapTreeKeys(reducer, mapper, acc, obj) {
  if (typeof obj === "object" && obj !== null) {
    const accObj = {};
    for (let k of Object.keys(obj)) {
      accObj[k] = reduceAndMapTreeKeys(
        reducer,
        mapper,
        reducer(acc, k),
        obj[k]
      );
    }
    return accObj;
  } else {
    return mapper(acc);
  }
}

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

//as string constans, e.g. actionTypes.atoms.close === "atoms.close"
export const actionTypes = tree2constants(actionHierarchy);

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

function toggleDebugMode() {
  return dispatch => {
    won.debugmode = !won.debugmode;
    dispatch({
      type: actionTypes.view.toggleDebugMode,
      payload: won.debugmode,
    });
  };
}
