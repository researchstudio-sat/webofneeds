/** @jsx h */

import angular from "angular";
import ngAnimate from "angular-animate";
import won from "../won-es6.js";
import sendRequestModule from "../components/send-request.js";
import postMessagesModule from "../components/post-messages.js";
import groupPostMessagesModule from "../components/group-post-messages.js";
import postInfoModule from "../components/post-info.js";
import connectionsOverviewModule from "../components/connections-overview.js";
import createPostModule from "../components/create-post.js";
import createSearchModule from "../components/create-search.js";
import usecasePickerModule from "../components/usecase-picker.js";
import usecaseGroupModule from "../components/usecase-group.js";
import { attach, getIn, get } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import * as generalSelectors from "../selectors/general-selectors.js";
import * as connectionSelectors from "../selectors/connection-selectors.js";
import * as connectionUtils from "../connection-utils.js";
import * as viewSelectors from "../selectors/view-selectors.js";
import * as srefUtils from "../sref-utils.js";
import { h } from "preact";

import "~/style/_connections.scss";
import "~/style/_responsiveness-utils.scss";
import "~/style/_atom-overlay.scss";
import "~/style/_connection-overlay.scss";

const template = (
  <container>
    <won-modal-dialog ng-if="self.showModalDialog" />
    <div className="won-modal-atomview" ng-if="self.showAtomOverlay">
      <won-post-info include-header="true" atom-uri="self.viewAtomUri" />
    </div>
    <div
      className="won-modal-connectionview"
      ng-if="self.showConnectionOverlay"
    >
      <won-post-messages connection-uri="self.viewConnUri" />
    </div>
    <header>
      <won-topnav page-title="::'Chats'" />
    </header>
    <won-toasts />
    <won-slide-in ng-if="self.showSlideIns" />
    <aside
      className="overview__left"
      ng-class="{'hide-in-responsive': self.hideListSideInResponsive}"
    >
      <won-connections-overview
        on-selected-connection="::self.selectConnection(connectionUri)"
        on-selected-atom="::self.selectAtom(atomUri)"
        open="self.open"
      />
    </aside>
    {/* RIGHT SIDE */}
    <main
      className="overview__right"
      ng-if="self.showWelcomeSide"
      ng-class="{'hide-in-responsive' : self.hideWelcomeSideInResponsive}"
    >
      <div className="overview__right__welcome">
        <div
          className="overview__right__welcome__text"
          ng-include="self.welcomeTemplatePath"
        />
        <won-usecase-picker />
        <div className="overview__right__welcome__howto">
          <a
            className="overview__right__welcome__howto__button won-button--filled red"
            href="{{ ::self.absHRef(self.$state, 'about', {'aboutSection': 'aboutHowTo'}) }}"
          >
            <span>Learn how it works</span>
          </a>
        </div>
      </div>
    </main>
    <main className="overview__right" ng-if="self.showContentSide">
      <won-usecase-picker ng-if="self.showUseCasePicker" />
      <won-usecase-group ng-if="self.showUseCaseGroups" />
      <won-create-post ng-if="self.showCreatePost" />
      <won-create-search ng-if="self.showCreateSearch" />
      <won-post-messages ng-if="self.showPostMessages" />
      <won-group-post-messages ng-if="self.showGroupPostMessages" />
      <won-post-info include-header="::true" ng-if="self.showPostInfo" />
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
    Object.assign(this, srefUtils);
    this.WON = won.WON;
    this.open = {};

    const selectFromState = state => {
      const viewConnUri = generalSelectors.getViewConnectionUriFromRoute(state);
      const viewAtomUri = generalSelectors.getViewAtomUriFromRoute(state);
      const selectedPostUri = generalSelectors.getPostUriFromRoute(state);
      const selectedPost =
        selectedPostUri && getIn(state, ["atoms", selectedPostUri]);

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
      const isSelectedConnectionGroupChat = connectionSelectors.isChatToGroupConnection(get(state, "atoms"), selectedConnection);

      const ownedAtoms = generalSelectors.getOwnedAtoms(state);

      const hasOwnedAtoms = ownedAtoms && ownedAtoms.size > 0;

      const theme = getIn(state, ["config", "theme"]);
      const themeName = get(theme, "name");
      const welcomeTemplate = get(theme, "welcomeTemplate");

      const showCreateFromPost = !!(fromAtomUri && mode);

      return {
        welcomeTemplatePath: "./skin/" + themeName + "/" + welcomeTemplate,

        open,
        showModalDialog: getIn(state, ["view", "showModalDialog"]),
        showWelcomeSide:
          !showCreateFromPost &&
          !useCase &&
          !useCaseGroup &&
          !selectedPost &&
          (!selectedConnection || connectionUtils.isClosed(selectedConnection)),
        showContentSide:
          showCreateFromPost ||
          useCase ||
          useCaseGroup ||
          selectedPost ||
          (selectedConnection && !connectionUtils.isClosed(selectedConnection)),
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
          !isSelectedConnectionGroupChat &&
          (connectionUtils.isConnected(selectedConnection) ||
            connectionUtils.isRequestReceived(selectedConnection) ||
            connectionUtils.isRequestSent(selectedConnection) ||
            connectionUtils.isSuggested(selectedConnection)),
        showGroupPostMessages:
          !selectedPost &&
          !useCaseGroup &&
          isSelectedConnectionGroupChat &&
          (connectionUtils.isConnected(selectedConnection) ||
            connectionUtils.isRequestReceived(selectedConnection) ||
            connectionUtils.isRequestSent(selectedConnection) ||
            connectionUtils.isSuggested(selectedConnection)),
        showPostInfo: selectedPost && !useCaseGroup,
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
          selectedPost ||
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

  selectAtom(atomUri) {
    this.router__stateGoCurrent({
      connectionUri: undefined,
      postUri: atomUri,
      useCase: undefined,
      useCaseGroup: undefined,
      fromAtomUri: undefined,
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

export default {
  module: angular
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
    ]).name,
  controller: "ConnectionsController",
  template: template,
};
