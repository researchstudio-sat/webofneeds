import angular from "angular";
import won from "../../won-es6.js";
import sendRequestModule from "../send-request.js";
import postMessagesModule from "../post-messages.js";
import groupAdministrationModule from "../group-administration.js";
import postInfoModule from "../post-info.js";
import connectionsOverviewModule from "../connections-overview.js";
import createPostModule from "../create-post.js";
import createSearchModule from "../create-search.js";
import usecasePickerModule from "../usecase-picker.js";
import usecaseGroupModule from "../usecase-group.js";
import { attach, getIn, get } from "../../utils.js";
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
        selectedPostUri && getIn(state, ["needs", selectedPostUri]);

      const showGroupPostAdministration = getIn(state, [
        "router",
        "currentParams",
        "groupPostAdminUri",
      ]);
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

      const ownedNeeds = getOwnedNeeds(state);

      const hasOwnedNeeds = ownedNeeds && ownedNeeds.size > 0;

      const theme = getIn(state, ["config", "theme"]);
      const themeName = get(theme, "name");
      const appTitle = get(theme, "title");
      const welcomeTemplate = get(theme, "welcomeTemplate");

      return {
        appTitle,
        welcomeTemplatePath: "./skin/" + themeName + "/" + welcomeTemplate,

        open,
        showModalDialog: getIn(state, ["view", "showModalDialog"]),
        showWelcomeSide:
          !useCase &&
          !useCaseGroup &&
          !selectedPost &&
          !showGroupPostAdministration &&
          (!selectedConnection || selectedConnectionState === won.WON.Closed),
        showContentSide:
          useCase ||
          useCaseGroup ||
          selectedPost ||
          showGroupPostAdministration ||
          (selectedConnection && selectedConnectionState !== won.WON.Closed),

        showUseCasePicker:
          !useCase &&
          !!useCaseGroup &&
          useCaseGroup === "all" &&
          !selectedPost &&
          !selectedConnection,
        showUseCaseGroups:
          !useCase &&
          !!useCaseGroup &&
          useCaseGroup !== "all" &&
          !selectedPost &&
          !selectedConnection,
        showCreatePost:
          !!useCase &&
          useCase !== "search" &&
          !selectedPost &&
          !selectedConnection,
        showCreateSearch:
          !!useCase &&
          useCase === "search" &&
          !selectedPost &&
          !selectedConnection,
        showPostMessages:
          !selectedPost &&
          !useCaseGroup &&
          !showGroupPostAdministration &&
          (selectedConnectionState === won.WON.Connected ||
            selectedConnectionState === won.WON.RequestReceived ||
            selectedConnectionState === won.WON.RequestSent ||
            selectedConnectionState === won.WON.Suggested),
        showPostInfo:
          selectedPost && !useCaseGroup && !showGroupPostAdministration,
        showGroupPostAdministration: showGroupPostAdministration,

        hideListSideInResponsive:
          !hasOwnedNeeds ||
          selectedConnection ||
          selectedPost ||
          !!useCaseGroup ||
          !!useCase,
        hideWelcomeSideInResponsive: hasOwnedNeeds,
        hideFooterInResponsive: selectedConnection,
      };
    };

    const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(
      this
    );
    this.$scope.$on("$destroy", disconnect);
  }

  selectNeed(needUri) {
    this.router__stateGoCurrent({
      connectionUri: undefined,
      postUri: needUri,
      useCase: undefined,
      useCaseGroup: undefined,
      groupPostAdminUri: undefined,
    });
  }

  selectConnection(connectionUri) {
    this.markAsRead(connectionUri);
    this.router__stateGoCurrent({
      connectionUri: connectionUri,
      postUri: undefined,
      useCase: undefined,
      useCaseGroup: undefined,
      groupPostAdminUri: undefined,
    });
  }

  selectGroupChat(needUri) {
    //TODO: Mark all groupconnections as read
    this.router__stateGoCurrent({
      connectionUri: undefined,
      postUri: undefined,
      useCase: undefined,
      useCaseGroup: undefined,
      groupPostAdminUri: needUri,
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
    groupAdministrationModule,
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
