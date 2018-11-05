/**
 * Created by ksinger on 24.09.2015.
 */

import { actionTypes } from "../actions/actions.js";
import Immutable from "immutable";
import { combineReducersStable } from "../redux-utils.js";
import { messagesReducer } from "./message-reducers.js";
import { isChatConnection } from "../connection-utils.js";
import reduceReducers from "reduce-reducers";
import needReducer from "./need-reducer/need-reducer-main.js";
import userReducer from "./user-reducer.js";
import toastReducer from "./toast-reducer.js";
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

  user: userReducer,
  needs: needReducer,
  messages: messagesReducer,
  toasts: toastReducer,

  // contains the Date.now() of the last action
  // lastUpdateTime: (state = Date.now(), action = {}) => Date.now(),
  lastUpdateTime: () => Date.now(),

  loginInProcessFor: (loginInProcessFor = undefined, action = {}) => {
    switch (action.type) {
      case actionTypes.loginStarted:
        return getIn(action, ["payload", "email"]);

      case actionTypes.login:
        if (getIn(action, ["payload", "loginFinished"])) {
          return undefined;
        } else {
          return loginInProcessFor;
        }

      case actionTypes.loginFailed:
        return undefined;

      default:
        return loginInProcessFor;
    }
  },

  logoutInProcess: (logoutInProcess = undefined, action = {}) => {
    switch (action.type) {
      case actionTypes.logoutStarted:
        return true;

      case actionTypes.logout:
        return undefined;

      default:
        return logoutInProcess;
    }
  },

  initialLoadFinished: (state = false, action = {}) =>
    state ||
    (action.type === actionTypes.initialPageLoad &&
      getIn(action, ["payload", "initialLoadFinished"])),

  showRdf: (isShowingRdf = false, action = {}) => {
    switch (action.type) {
      case actionTypes.toggleRdfDisplay:
        return !isShowingRdf;
      default:
        return isShowingRdf;
    }
  },
  showClosedNeeds: (isShowingClosed = false, action = {}) => {
    switch (action.type) {
      case actionTypes.toggleClosedNeedsDisplay:
        return !isShowingClosed;
      case actionTypes.hideClosedNeedsDisplay:
        return false;
      case actionTypes.showClosedNeedsDisplay:
        return true;
      default:
        return isShowingClosed;
    }
  },

  showMainMenu: (isShowingMainMenu = false, action = {}) => {
    switch (action.type) {
      case actionTypes.loginFailed:
      case actionTypes.showMainMenuDisplay:
        return true;

      case actionTypes.logout:
      case actionTypes.toggleRdfDisplay:
      case actionTypes.login:
      case actionTypes.hideMainMenuDisplay:
        return false;
      default:
        return isShowingMainMenu;
    }
  },

  showAddMessageContent: (isShowingAddMessageContent = false, action = {}) => {
    switch (action.type) {
      case actionTypes.toggleAddMessageContentDisplay:
        return !isShowingAddMessageContent;
      case actionTypes.selectAddMessageContent:
      case actionTypes.showAddMessageContentDisplay:
        return true;
      case actionTypes.hideAddMessageContentDisplay:
        return false;
      default:
        return isShowingAddMessageContent;
    }
  },

  selectedAddMessageContent: (
    selectedAddMessageContent = undefined,
    action = {}
  ) => {
    switch (action.type) {
      case actionTypes.selectAddMessageContent:
        return getIn(action, ["payload", "selectedDetail"]);
      case actionTypes.toggleAddMessageContentDisplay:
      case actionTypes.hideAddMessageContentDisplay:
      case actionTypes.removeAddMessageContent:
        return undefined;
      default:
        return selectedAddMessageContent;
    }
  },

  showModalDialog: (isShowingModalDialog = false, action = {}) => {
    switch (action.type) {
      case actionTypes.openModalDialog:
        return true;
      case actionTypes.closeModalDialog:
        return false;
      default:
        return isShowingModalDialog;
    }
  },

  modalDialog: (modalDialog = undefined, action = {}) => {
    switch (action.type) {
      case actionTypes.openModalDialog:
        return Immutable.fromJS(action.payload);
      case actionTypes.closeModalDialog:
      default:
        return modalDialog;
    }
  },

  creatingWhatsX: (creatingWhatsX = false, action = {}) => {
    switch (action.type) {
      case actionTypes.needs.whatsNew:
      case actionTypes.needs.whatsAround:
        return true;
      case actionTypes.failedToGetLocation:
      case actionTypes.needs.createSuccessful:
        return false;
      default:
        return creatingWhatsX;
    }
  },

  //config: createReducer(
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
      case actionTypes.login:
      case actionTypes.messages.connectMessageSent:
      case actionTypes.messages.connectMessageReceived:
      case actionTypes.messages.hintMessageReceived:
        return deleteChatConnectionsBetweenOwnNeeds(state);

      case actionTypes.mainViewScrolled:
        return state.set("mainViewScroll", action.payload);
      /*
              * TODO try to resolve a lot of the AC-dispatching so only
              * high-level actions are left there. avoid actions that
              * trigger other actions. also, actions shouldn't have a
              * 1:1 mapping to state.
              * see: https://github.com/rackt/redux/issues/857#issuecomment-146021839
              * see: https://github.com/rackt/redux/issues/857#issuecomment-146269384
              */
      default:
        return state;
    }
  }
  //-------------------- </cross-cutting-reducer> -------------------
);

window.Immutable4dbg = Immutable;

function deleteChatConnectionsBetweenOwnNeeds(state) {
  let needs = state.get("needs");

  if (needs) {
    needs = needs.map(function(need) {
      let connections = need.get("connections");

      connections =
        connections &&
        connections.filter(function(conn) {
          //Any connection that is not of type chatFacet will be exempt from deletion
          if (isChatConnection(conn)) {
            //Any other connection will be checked if it would be connected to the ownNeed, if so we remove it.
            return !state.getIn([
              "needs",
              conn.get("remoteNeedUri"),
              "ownNeed",
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
