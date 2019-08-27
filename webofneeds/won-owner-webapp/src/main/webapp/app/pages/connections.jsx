/** @jsx h */

import angular from "angular";
import ngAnimate from "angular-animate";
import WonConnectionsOverview from "../components/connections-overview.jsx";
import WonAtomMessages from "../components/atom-messages.jsx";
import WonGroupAtomMessages from "../components/group-atom-messages.jsx";
import WonModalDialog from "../components/modal-dialog.jsx";
import WonToasts from "../components/toasts.jsx";
import WonMenu from "../components/menu.jsx";
import WonFooter from "../components/footer.jsx";
import { get, getIn } from "../utils.js";
import { attach, classOnComponentRoot } from "../cstm-ng-utils.js";
import { actionCreators } from "../actions/actions.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as connectionSelectors from "../redux/selectors/connection-selectors.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import * as accountUtils from "../redux/utils/account-utils.js";
import * as viewSelectors from "../redux/selectors/view-selectors.js";
import { h } from "preact";

import "~/style/_connections.scss";
import "~/style/_responsiveness-utils.scss";
import "~/style/_connection-overlay.scss";

const template = (
  <container>
    <won-preact
      className="modalDialog"
      component="self.WonModalDialog"
      props="{}"
      ng-if="self.showModalDialog"
    />
    <div
      className="won-modal-connectionview"
      ng-if="self.showConnectionOverlay"
    >
      <won-preact
        component="self.WonAtomMessages"
        props="{connectionUri: self.viewConnUri}"
        className="atomMessages"
      />
    </div>
    <won-topnav page-title="::'Chats'" />
    <won-preact
      className="menu"
      component="self.WonMenu"
      props="{}"
      ng-if="self.isLoggedIn"
    />
    <won-preact className="toasts" component="self.WonToasts" props="{}" />
    <won-slide-in ng-if="self.showSlideIns" />
    <aside
      className="overview__left"
      ng-class="{'hide-in-responsive': self.hideListSideInResponsive}"
      ng-if="self.showListSide"
    >
      <won-preact component="self.WonConnectionsOverview" props="{}" />
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
      <won-preact
        component="self.WonAtomMessages"
        ng-if="self.showPostMessages"
        props="{}"
        className="atomMessages"
      />
      <won-preact
        component="self.WonGroupAtomMessages"
        ng-if="self.showGroupPostMessages"
        props="{}"
        className="groupAtomMessages"
      />
    </main>
    <main className="overview__nochats" ng-if="!self.showListSide">
      <div className="overview__nochats__empty">
        <svg className="overview__nochats__empty__icon" title="Messages">
          <use xlinkHref="#ico36_message" href="#ico36_message" />
        </svg>
        <div className="overview__nochats__empty__text">
          No Open Chats available
        </div>
      </div>
    </main>
    {/* Connection view does not show the footer in responsive mode as there should not be two scrollable areas imho */}
    <won-preact
      className="footer"
      component="self.WonFooter"
      props="{}"
      ng-class="{'hide-in-responsive': self.hideFooterInResponsive }"
    />
  </container>
);

const serviceDependencies = ["$element", "$ngRedux", "$scope", "$state"];

class ConnectionsController {
  constructor() {
    attach(this, serviceDependencies, arguments);
    this.WonConnectionsOverview = WonConnectionsOverview;
    this.WonAtomMessages = WonAtomMessages;
    this.WonGroupAtomMessages = WonGroupAtomMessages;
    this.WonModalDialog = WonModalDialog;
    this.WonToasts = WonToasts;
    this.WonMenu = WonMenu;
    this.WonFooter = WonFooter;

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

      const accountState = get(state, "account");

      return {
        isLoggedIn: accountUtils.isLoggedIn(accountState),
        showModalDialog: getIn(state, ["view", "showModalDialog"]),
        showListSide: hasChatAtoms,
        showNoSelectionSide:
          (hasChatAtoms && !selectedConnection) ||
          connectionUtils.isClosed(selectedConnection),
        showContentSide:
          hasChatAtoms &&
          selectedConnection &&
          !connectionUtils.isClosed(selectedConnection),
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
          viewSelectors.hasSlideIns(state) &&
          viewSelectors.isSlideInsVisible(state),
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
    classOnComponentRoot("won-signed-out", () => !this.isLoggedIn, this);
    this.$scope.$on("$destroy", disconnect);
  }
}

ConnectionsController.$inject = [];

export default {
  module: angular
    .module("won.owner.components.connections", [ngAnimate])
    .controller("ConnectionsController", [
      ...serviceDependencies,
      ConnectionsController,
    ]).name,
  controller: "ConnectionsController",
  template: template,
};
