import angular from "angular";
import won from "../../won-es6.js";
import Immutable from "immutable";
import sendRequestModule from "../send-request.js";
import postMessagesModule from "../post-messages.js";
import postInfoModule from "../post-info.js";
import connectionsOverviewModule from "../connections-overview.js";
import createPostModule from "../create-post.js";
import createSearchModule from "../create-search.js";
import usecasePickerModule from "../usecase-picker.js";
import usecaseGroupModule from "../usecase-group.js";
import { attach, getIn } from "../../utils.js";
import { isWhatsAroundNeed, isWhatsNewNeed } from "../../need-utils.js";
import { actionCreators } from "../../actions/actions.js";
import {
  getOwnedNeedByConnectionUri,
  getOwnedNeeds,
} from "../../selectors/general-selectors.js";
import * as srefUtils from "../../sref-utils.js";

import "style/_connections.scss";
import "style/_responsiveness-utils.scss";

const serviceDependencies = ["$element", "$ngRedux", "$scope", "$state"];

class ConnectionsController {
  constructor() {
    attach(this, serviceDependencies, arguments);
    Object.assign(this, srefUtils);
    this.WON = won.WON;
    this.open = {};

    const selectFromState = state => {
      const selectedPostUri = decodeURIComponent(
        getIn(state, ["router", "currentParams", "postUri"])
      );
      const selectedPost =
        selectedPostUri && state.getIn(["needs", selectedPostUri]);

      const useCase = getIn(state, ["router", "currentParams", "useCase"]);
      const useCaseGroup = getIn(state, [
        "router",
        "currentParams",
        "useCaseGroup",
      ]);

      const selectedConnectionUri = decodeURIComponent(
        getIn(state, ["router", "currentParams", "connectionUri"])
      );
      const need =
        selectedConnectionUri &&
        getOwnedNeedByConnectionUri(state, selectedConnectionUri);
      const selectedConnection = getIn(need, [
        "connections",
        selectedConnectionUri,
      ]);
      const selectedConnectionState = getIn(selectedConnection, ["state"]);

      const ownedNeeds = getOwnedNeeds(state).filter(
        //FIXME: THIS CAN BE REMOVED ONCE WE DELETE INSTEAD OF CLOSE THE WHATSX NEEDS
        post =>
          !(
            (isWhatsAroundNeed(post) || isWhatsNewNeed(post)) &&
            post.get("state") === won.WON.InactiveCompacted
          )
      );

      let connections = Immutable.Map();

      ownedNeeds &&
        ownedNeeds.map(function(need) {
          connections = connections.merge(need.get("connections"));
        });

      const themeName = getIn(state, ["config", "theme", "name"]);

      return {
        themeName,
        welcomeTemplate:
          "./skin/" +
          themeName +
          "/" +
          getIn(state, ["config", "theme", "welcomeTemplate"]),
        appTitle: getIn(state, ["config", "theme", "title"]),
        WON: won.WON,
        selectedPost,
        selectedConnection,
        selectedConnectionState,
        useCase,
        useCaseGroup,
        hasConnections: connections && connections.size > 0,
        hasOwnedNeeds: ownedNeeds && ownedNeeds.size > 0,
        open,
        showModalDialog: state.getIn(["view", "showModalDialog"]),
      };
    };

    const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(
      this
    );
    this.$scope.$on("$destroy", disconnect);

    /*this.$scope.$watch("self.mainViewScroll", newValue => {
      if (newValue !== undefined)
        requestAnimationFrame(() => (scrollArea.scrollTop = newValue));
    });*/
  }

  selectNeed(needUri) {
    this.router__stateGoCurrent({
      connectionUri: undefined,
      postUri: needUri,
      useCase: undefined,
      useCaseGroup: undefined,
    }); //TODO: Maybe leave the connectionUri in the parameters to go back when closing a selected need
  }

  selectConnection(connectionUri) {
    this.markAsRead(connectionUri);
    this.router__stateGoCurrent({
      connectionUri,
      postUri: undefined,
      useCase: undefined,
      useCaseGroup: undefined,
    });
  }

  markAsRead(connectionUri) {
    const need = getOwnedNeedByConnectionUri(
      this.$ngRedux.getState(),
      connectionUri
    );

    const connUnread = getIn(need, ["connections", connectionUri, "unread"]);
    const connNotConnected =
      getIn(need, ["connections", connectionUri, "state"]) !==
      won.WON.Connected;

    if (connUnread && connNotConnected) {
      const payload = {
        connectionUri: connectionUri,
        needUri: need.get("uri"),
      };

      this.connections__markAsRead(payload);
    }
  }
}

ConnectionsController.$inject = [];

export default angular
  .module("won.owner.components.connections", [
    sendRequestModule,
    postMessagesModule,
    postInfoModule,
    usecasePickerModule,
    usecaseGroupModule,
    createPostModule,
    createSearchModule,
    connectionsOverviewModule,
  ])
  .controller("ConnectionsController", [
    ...serviceDependencies,
    ConnectionsController,
  ]).name;
