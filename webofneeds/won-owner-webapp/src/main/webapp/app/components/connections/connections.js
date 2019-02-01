import angular from "angular";
import ngAnimate from "angular-animate";
import won from "../../won-es6.js";
import sendRequestModule from "../send-request.js";
import postMessagesModule from "../post-messages.js";
import groupPostMessagesModule from "../group-post-messages.js";
import groupAdministrationModule from "../group-administration.js";
import postInfoModule from "../post-info.js";
import connectionsOverviewModule from "../connections-overview.js";
import createPostModule from "../create-post.js";
import createSearchModule from "../create-search.js";
import usecasePickerModule from "../usecase-picker.js";
import usecaseGroupModule from "../usecase-group.js";
import { attach, getIn, get } from "../../utils.js";
import { actionCreators } from "../../actions/actions.js";
import * as generalSelectors from "../../selectors/general-selectors.js";
import { isChatToGroup } from "../../connection-utils.js";
import * as viewSelectors from "../../selectors/view-selectors.js";
import * as srefUtils from "../../sref-utils.js";

import "style/_connections.scss";
import "style/_responsiveness-utils.scss";
import "style/_need-overlay.scss";

const serviceDependencies = ["$element", "$ngRedux", "$scope", "$state"];

class ConnectionsController {
  constructor() {
    attach(this, serviceDependencies, arguments);
    Object.assign(this, srefUtils);
    this.WON = won.WON;
    this.open = {};

    const selectFromState = state => {
      const viewNeedUri = generalSelectors.getViewNeedUriFromRoute(state);
      const selectedPostUri = generalSelectors.getPostUriFromRoute(state);
      const selectedPost =
        selectedPostUri && getIn(state, ["needs", selectedPostUri]);

      const showGroupPostAdministration = !!generalSelectors.getGroupPostAdminUriFromRoute(
        state
      );
      const useCase = generalSelectors.getUseCaseFromRoute(state);
      const useCaseGroup = generalSelectors.getUseCaseGroupFromRoute(state);

      const selectedConnectionUri = generalSelectors.getConnectionUriFromRoute(
        state
      );
      const fromNeedUri = generalSelectors.getFromNeedUriFromRoute(state);
      const mode = generalSelectors.getModeFromRoute(state);

      const need =
        selectedConnectionUri &&
        generalSelectors.getOwnedNeedByConnectionUri(
          state,
          selectedConnectionUri
        );
      const selectedConnection = getIn(need, [
        "connections",
        selectedConnectionUri,
      ]);
      const isSelectedConnectionGroupChat = isChatToGroup(
        state.get("needs"),
        get(need, "uri"),
        selectedConnectionUri
      );

      const selectedConnectionState = getIn(selectedConnection, ["state"]);

      const ownedNeeds = generalSelectors.getOwnedNeeds(state);

      const hasOwnedNeeds = ownedNeeds && ownedNeeds.size > 0;

      const theme = getIn(state, ["config", "theme"]);
      const themeName = get(theme, "name");
      const appTitle = get(theme, "title");
      const welcomeTemplate = get(theme, "welcomeTemplate");

      const showCreateFromPost = !!(fromNeedUri && mode);

      return {
        appTitle,
        welcomeTemplatePath: "./skin/" + themeName + "/" + welcomeTemplate,

        open,
        showModalDialog: getIn(state, ["view", "showModalDialog"]),
        showWelcomeSide:
          !showCreateFromPost &&
          !useCase &&
          !useCaseGroup &&
          !selectedPost &&
          !showGroupPostAdministration &&
          (!selectedConnection || selectedConnectionState === won.WON.Closed),
        showContentSide:
          showCreateFromPost ||
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
          showCreateFromPost ||
          (!!useCase &&
            useCase !== "search" &&
            !selectedPost &&
            !selectedConnection),
        showCreateSearch:
          !!useCase &&
          useCase === "search" &&
          !selectedPost &&
          !selectedConnection,
        showPostMessages:
          !selectedPost &&
          !useCaseGroup &&
          !showGroupPostAdministration &&
          !isSelectedConnectionGroupChat &&
          (selectedConnectionState === won.WON.Connected ||
            selectedConnectionState === won.WON.RequestReceived ||
            selectedConnectionState === won.WON.RequestSent ||
            selectedConnectionState === won.WON.Suggested),
        showGroupPostMessages:
          !selectedPost &&
          !useCaseGroup &&
          !showGroupPostAdministration &&
          isSelectedConnectionGroupChat &&
          (selectedConnectionState === won.WON.Connected ||
            selectedConnectionState === won.WON.RequestReceived ||
            selectedConnectionState === won.WON.RequestSent ||
            selectedConnectionState === won.WON.Suggested),
        showPostInfo:
          selectedPost && !useCaseGroup && !showGroupPostAdministration,
        showGroupPostAdministration: showGroupPostAdministration,
        showSlideIns:
          viewSelectors.hasSlideIns(state) && viewSelectors.showSlideIns(state),
        showNeedOverlay: !!viewNeedUri,
        viewNeedUri,
        hideListSideInResponsive:
          !hasOwnedNeeds ||
          showGroupPostAdministration ||
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
      fromNeedUri: undefined,
      mode: undefined,
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
      fromNeedUri: undefined,
      mode: undefined,
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
      fromNeedUri: undefined,
      mode: undefined,
    });
  }

  markAsRead(connectionUri) {
    const need = generalSelectors.getOwnedNeedByConnectionUri(
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
    ngAnimate,
    sendRequestModule,
    postMessagesModule,
    groupPostMessagesModule,
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
