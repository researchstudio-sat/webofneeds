/** @jsx h */

/**
 * Created by quasarchimaere on 04.04.2019.
 */
import angular from "angular";
import ngAnimate from "angular-animate";
import { attach, getIn, get, delay } from "../utils.js";
import { connect2Redux } from "../won-utils.js";
import won from "../won-es6.js";
import { actionCreators } from "../actions/actions.js";
import postMessagesModule from "../components/post-messages.js";
import atomCardModule from "../components/atom-card.js";
import postHeaderModule from "../components/post-header.js";
import * as generalSelectors from "../selectors/general-selectors.js";
import * as viewSelectors from "../selectors/view-selectors.js";
import * as processUtils from "../process-utils.js";
import * as wonLabelUtils from "../won-label-utils.js";
import { h } from "preact";

import "style/_overview.scss";
import "style/_atom-overlay.scss";
import "style/_connection-overlay.scss";

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
      <won-topnav />
    </header>
    <won-toasts />
    <won-slide-in ng-if="self.showSlideIns" />
    <main className="owneroverview">
      <div className="owneroverview__header">
        <div className="owneroverview__header__title">
          {"What's new? "}
          <span
            className="owneroverview__header__title__count"
            ng-if="!self.isOwnerAtomUrisLoading"
          >
            {"{{ self.atomUrisSize }}"}
          </span>
        </div>
        <div
          className="owneroverview__header__loading"
          ng-if="self.isOwnerAtomUrisLoading"
        >
          Loading...
        </div>
        <div
          className="owneroverview__header__updated"
          ng-if="!self.isOwnerAtomUrisLoading"
        >
          <div className="owneroverview__header__updated__time">
            {"Updated: {{ self.friendlyLastAtomUrisUpdateTimestamp }}"}
          </div>
          <div
            className="owneroverview__header__updated__reload won-button--filled red"
            ng-click="self.reload()"
          >
            Reload
          </div>
        </div>
      </div>
      <div className="owneroverview__content">
        <won-atom-card
          class="owneroverview__content__atom"
          atom-uri="atomUri"
          ng-repeat="atomUri in self.atomUrisArray track by atomUri"
        />
      </div>
    </main>
    <won-footer />
  </container>
);

const serviceDependencies = ["$ngRedux", "$scope"];
class Controller {
  constructor() {
    attach(this, serviceDependencies, arguments);
    this.selection = 0;
    window.overview4dbg = this;
    this.WON = won.WON;

    const selectFromState = state => {
      const viewAtomUri = generalSelectors.getViewAtomUriFromRoute(state);
      const viewConnUri = generalSelectors.getViewConnectionUriFromRoute(state);

      const atomUris = getIn(state, ["owner", "atomUris"]);
      const lastAtomUrisUpdateDate = getIn(state, [
        "owner",
        "lastAtomUrisUpdateTime",
      ]);

      const process = get(state, "process");
      const isOwnerAtomUrisLoading = processUtils.isProcessingAtomUrisFromOwnerLoad(
        process
      );
      const isOwnerAtomUrisToLoad =
        !lastAtomUrisUpdateDate && !isOwnerAtomUrisLoading;

      return {
        lastAtomUrisUpdateDate,
        friendlyLastAtomUrisUpdateTimestamp:
          lastAtomUrisUpdateDate &&
          wonLabelUtils.relativeTime(
            generalSelectors.selectLastUpdateTime(state),
            lastAtomUrisUpdateDate
          ),
        atomUrisArray: atomUris && atomUris.toArray().splice(0, 200), //FIXME: CURRENTLY LIMIT TO 200 entries
        atomUrisSize: atomUris ? atomUris.size : 0,
        isOwnerAtomUrisLoading,
        isOwnerAtomUrisToLoad,
        showSlideIns:
          viewSelectors.hasSlideIns(state) && viewSelectors.showSlideIns(state),
        showModalDialog: viewSelectors.showModalDialog(state),
        showAtomOverlay: !!viewAtomUri,
        showConnectionOverlay: !!viewConnUri,
        viewAtomUri,
        viewConnUri,
      };
    };

    connect2Redux(selectFromState, actionCreators, [], this);

    this.$scope.$watch(
      () => this.isOwnerAtomUrisToLoad,
      () => delay(0).then(() => this.ensureAtomUrisLoaded())
    );
  }

  ensureAtomUrisLoaded() {
    if (this.isOwnerAtomUrisToLoad) {
      this.atoms__loadAllActiveAtomUrisFromOwner();
    }
  }

  reload() {
    if (!this.isOwnerAtomUrisLoading) {
      this.atoms__loadAllActiveAtomUrisFromOwner();
    }
  }
}

Controller.$inject = serviceDependencies;

export default {
  module: angular
    .module("won.owner.components.overview", [
      ngAnimate,
      postMessagesModule,
      atomCardModule,
      postHeaderModule,
    ])
    .controller("OverviewController", Controller).name,
  controller: "OverviewController",
  template: template,
};
