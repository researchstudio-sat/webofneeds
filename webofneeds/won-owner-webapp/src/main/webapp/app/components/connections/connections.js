import angular from "angular";
import ngAnimate from "angular-animate";
import won from "../../won-es6.js";
import sendRequestModule from "../send-request.js";
import postMessagesModule from "../post-messages.js";
import groupPostMessagesModule from "../group-post-messages.js";
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
import "style/_atom-overlay.scss";
import "style/_connection-overlay.scss";

const serviceDependencies = ["$element", "$ngRedux", "$scope", "$state"];

class ConnectionsController {
  constructor() {
    attach(this, serviceDependencies, arguments);
    Object.assign(this, srefUtils);
    this.WON = won.WON;
    this.open = {};

    const selectFromState = state => {
      const viewConnUri = generalSelectors.getViewConnectionUriFromRoute(state);
      const viewAtomUri = generalSelectors.getViewAtomUriFromRoute(state);

      const useCase = generalSelectors.getUseCaseFromRoute(state);
      const useCaseGroup = generalSelectors.getUseCaseGroupFromRoute(state);

      const selectedConnectionUri = generalSelectors.getConnectionUriFromRoute(
        state
      );
      const fromAtomUri = generalSelectors.getFromAtomUriFromRoute(state);
      const mode = generalSelectors.getModeFromRoute(state);

      const atom =
        selectedConnectionUri &&
        generalSelectors.getOwnedAtomByConnectionUri(
          state,
          selectedConnectionUri
        );
      const selectedConnection = getIn(atom, [
        "connections",
        selectedConnectionUri,
      ]);
      const isSelectedConnectionGroupChat = isChatToGroup(
        state.get("atoms"),
        get(atom, "uri"),
        selectedConnectionUri
      );

      const selectedConnectionState = getIn(selectedConnection, ["state"]);

      const ownedAtoms = generalSelectors.getOwnedAtoms(state);

      const hasOwnedAtoms = ownedAtoms && ownedAtoms.size > 0;

      const theme = getIn(state, ["config", "theme"]);
      const themeName = get(theme, "name");
      const appTitle = get(theme, "title");
      const welcomeTemplate = get(theme, "welcomeTemplate");

      const showCreateFromPost = !!(fromAtomUri && mode);

      return {
        appTitle,
        welcomeTemplatePath: "./skin/" + themeName + "/" + welcomeTemplate,

        open,
        showModalDialog: getIn(state, ["view", "showModalDialog"]),
        showWelcomeSide:
          !showCreateFromPost &&
          !useCase &&
          !useCaseGroup &&
          (!selectedConnection || selectedConnectionState === won.WON.Closed),
        showContentSide:
          showCreateFromPost ||
          useCase ||
          useCaseGroup ||
          (selectedConnection && selectedConnectionState !== won.WON.Closed),

        showUseCasePicker:
          !useCase &&
          !!useCaseGroup &&
          useCaseGroup === "all" &&
          !selectedConnection,
        showUseCaseGroups:
          !useCase &&
          !!useCaseGroup &&
          useCaseGroup !== "all" &&
          !selectedConnection,
        showCreatePost:
          showCreateFromPost ||
          (!!useCase && useCase !== "search" && !selectedConnection),
        showCreateSearch:
          !!useCase && useCase === "search" && !selectedConnection,
        showPostMessages:
          !useCaseGroup &&
          !isSelectedConnectionGroupChat &&
          (selectedConnectionState === won.WON.Connected ||
            selectedConnectionState === won.WON.RequestReceived ||
            selectedConnectionState === won.WON.RequestSent ||
            selectedConnectionState === won.WON.Suggested),
        showGroupPostMessages:
          !useCaseGroup &&
          isSelectedConnectionGroupChat &&
          (selectedConnectionState === won.WON.Connected ||
            selectedConnectionState === won.WON.RequestReceived ||
            selectedConnectionState === won.WON.RequestSent ||
            selectedConnectionState === won.WON.Suggested),
        showSlideIns:
          viewSelectors.hasSlideIns(state) && viewSelectors.showSlideIns(state),
        showAtomOverlay: !!viewAtomUri,
        showConnectionOverlay: !!viewConnUri,
        viewAtomUri,
        viewConnUri,
        hideListSideInResponsive:
          showCreateFromPost ||
          !hasOwnedAtoms ||
          selectedConnection ||
          !!useCaseGroup ||
          !!useCase,
        hideWelcomeSideInResponsive: hasOwnedAtoms,
        hideFooterInResponsive: selectedConnection,
      };
    };

    const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(
      this
    );
    this.$scope.$on("$destroy", disconnect);
  }

  selectConnection(connectionUri) {
    this.markAsRead(connectionUri);
    this.router__stateGoCurrent({
      connectionUri: connectionUri,
      useCase: undefined,
      useCaseGroup: undefined,
      fromAtomUri: undefined,
      mode: undefined,
    });
  }

  markAsRead(connectionUri) {
    const atom = generalSelectors.getOwnedAtomByConnectionUri(
      this.$ngRedux.getState(),
      connectionUri
    );

    const connUnread = getIn(atom, ["connections", connectionUri, "unread"]);
    const connNotConnected =
      getIn(atom, ["connections", connectionUri, "state"]) !==
      won.WON.Connected;

    if (connUnread && connNotConnected) {
      const payload = {
        connectionUri: connectionUri,
        atomUri: atom.get("uri"),
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
