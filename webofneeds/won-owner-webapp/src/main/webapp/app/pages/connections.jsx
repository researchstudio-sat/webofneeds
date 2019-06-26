/** @jsx h */

import angular from "angular";
import ngAnimate from "angular-animate";
import won from "../won-es6.js";
import postMessagesModule from "../components/post-messages.js";
import groupPostMessagesModule from "../components/group-post-messages.js";
import connectionsOverviewModule from "../components/connections-overview.js";
import { getIn, get } from "../utils.js";
import { attach } from "../cstm-ng-utils.js";
import { actionCreators } from "../actions/actions.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as connectionSelectors from "../redux/selectors/connection-selectors.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import * as viewSelectors from "../redux/selectors/view-selectors.js";
import { h } from "preact";

import "~/style/_connections.scss";
import "~/style/_responsiveness-utils.scss";
import "~/style/_connection-overlay.scss";

const template = (
  <container>
    <won-modal-dialog ng-if="self.showModalDialog" />
    <div
      className="won-modal-connectionview"
      ng-if="self.showConnectionOverlay"
    >
      <won-post-messages connection-uri="self.viewConnUri" />
    </div>
    <won-topnav page-title="::'Chats'" />
    <won-menu />
    <won-toasts />
    <won-slide-in ng-if="self.showSlideIns" />
    <aside
      className="overview__left"
      ng-class="{'hide-in-responsive': self.hideListSideInResponsive}"
      ng-if="self.showListSide"
    >
      <won-connections-overview on-selected-connection="::self.selectConnection(connectionUri)" />
    </aside>
    {/* RIGHT SIDE */}
    <main
      className="overview__rightempty"
      ng-if="self.showNoSelectionSide"
      ng-class="{'hide-in-responsive' : self.hideNoSelectionInResponsive}"
    >
      <div className="overview__rightempty__noselection">
        <svg
          className="overview__rightempty__noselection__icon"
          title="Messages"
        >
          <use xlinkHref="#ico36_message" href="#ico36_message" />
        </svg>
        <div className="overview__rightempty__noselection__text">
          No Chat selected
        </div>
        <div className="overview__rightempty__noselection__subtext">
          Click on a Chat on the left to open
        </div>
      </div>
    </main>
    <main className="overview__right" ng-if="self.showContentSide">
      <won-post-messages ng-if="self.showPostMessages" />
      <won-group-post-messages ng-if="self.showGroupPostMessages" />
    </main>
    <main className="overview__nochats" ng-if="!self.showListSide">
      <div className="overview__nochats__empty">
        <svg
          className="overview__nochats__empty__icon"
          title="Messages"
        >
          <use xlinkHref="#ico36_message" href="#ico36_message" />
        </svg>
        <div className="overview__nochats__empty__text">
          No Open Chats available
        </div>
      </div>
    </main>
    <won-footer ng-class="{'hide-in-responsive': self.hideFooterInResponsive }">
      {/* Connection view does not show the footer in responsive mode as there should not be two scrollable areas imho */}
    </won-footer>
  </container>
);

const serviceDependencies = ["$element", "$ngRedux", "$scope", "$state"];

class ConnectionsController {
  constructor() {
    attach(this, serviceDependencies, arguments);
    this.WON = won.WON;

    const selectFromState = state => {
      const viewConnUri = generalSelectors.getViewConnectionUriFromRoute(state);

      const selectedConnectionUri = generalSelectors.getConnectionUriFromRoute(
        state
      );

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
      const isSelectedConnectionGroupChat = connectionSelectors.isChatToGroupConnection(
        get(state, "atoms"),
        selectedConnection
      );

      const chatAtoms = generalSelectors.getChatAtoms(state);

      const hasChatAtoms = chatAtoms && chatAtoms.size > 0;

      return {
        showModalDialog: getIn(state, ["view", "showModalDialog"]),
        showListSide: hasChatAtoms,
        showNoSelectionSide:
          hasChatAtoms && !selectedConnection || connectionUtils.isClosed(selectedConnection),
        showContentSide:
          hasChatAtoms && selectedConnection && !connectionUtils.isClosed(selectedConnection),
        showPostMessages:
          !isSelectedConnectionGroupChat &&
          (connectionUtils.isConnected(selectedConnection) ||
            connectionUtils.isRequestReceived(selectedConnection) ||
            connectionUtils.isRequestSent(selectedConnection) ||
            connectionUtils.isSuggested(selectedConnection)),
        showGroupPostMessages:
          isSelectedConnectionGroupChat &&
          (connectionUtils.isConnected(selectedConnection) ||
            connectionUtils.isRequestReceived(selectedConnection) ||
            connectionUtils.isRequestSent(selectedConnection) ||
            connectionUtils.isSuggested(selectedConnection)),
        showSlideIns:
          viewSelectors.hasSlideIns(state) && viewSelectors.showSlideIns(state),
        showConnectionOverlay: !!viewConnUri,
        viewConnUri,
        hasChatAtoms,
        hideListSideInResponsive: !hasChatAtoms || selectedConnection,
        hideNoSelectionInResponsive: hasChatAtoms,
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
    this.router__stateGoCurrent({ connectionUri: connectionUri });
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

export default {
  module: angular
    .module("won.owner.components.connections", [
      ngAnimate,
      postMessagesModule,
      groupPostMessagesModule,
      connectionsOverviewModule,
    ])
    .controller("ConnectionsController", [
      ...serviceDependencies,
      ConnectionsController,
    ]).name,
  controller: "ConnectionsController",
  template: template,
};
