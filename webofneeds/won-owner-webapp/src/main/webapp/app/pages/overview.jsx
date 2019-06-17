/** @jsx h */

/**
 * Created by quasarchimaere on 04.04.2019.
 */
import angular from "angular";
import ngAnimate from "angular-animate";
import { getIn, get, delay, sortByDate } from "../utils.js";
import { attach } from "../cstm-ng-utils.js";
import { connect2Redux } from "../configRedux.js";
import { actionCreators } from "../actions/actions.js";
import postMessagesModule from "../components/post-messages.js";
import atomCardModule from "../components/atom-card.js";
import postHeaderModule from "../components/post-header.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as viewSelectors from "../redux/selectors/view-selectors.js";
import * as processUtils from "../redux/utils/process-utils.js";
import * as wonLabelUtils from "../won-label-utils.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import { h } from "preact";

import "~/style/_overview.scss";
import "~/style/_atom-overlay.scss";
import "~/style/_connection-overlay.scss";

const template = (
  <container>
    <won-modal-dialog ng-if="self.showModalDialog" />
    <div className="won-modal-atomview" ng-if="self.showAtomOverlay">
      <won-post-info atom-uri="self.viewAtomUri" />
    </div>
    <div
      className="won-modal-connectionview"
      ng-if="self.showConnectionOverlay"
    >
      <won-post-messages connection-uri="self.viewConnUri" />
    </div>
    <won-topnav page-title="::'What\'s New'" />
    <won-toasts />
    <won-slide-in ng-if="self.showSlideIns" />
    <main className="owneroverview">
      <div className="owneroverview__header">
        <div className="owneroverview__header__title">
          {"What's new? "}
          <span
            className="owneroverview__header__title__count"
            ng-if="!self.isOwnerAtomUrisLoading && self.hasVisibleAtomUris"
          >
            {"({{ self.sortedVisibleAtomUriSize }})"}
          </span>
        </div>
        <div className="owneroverview__header__updated">
          <div
            className="owneroverview__header__updated__time hide-in-responsive"
            ng-if="!self.isOwnerAtomUrisLoading"
          >
            Updated: {"{{ self.friendlyLastAtomUrisUpdateTimestamp }}"}
          </div>
          <div
            className="owneroverview__header__updated__loading hide-in-responsive"
            ng-if="self.isOwnerAtomUrisLoading"
          >
            Loading...
          </div>
          <div
            className="owneroverview__header__updated__reload won-button--filled red"
            ng-click="self.reload()"
            ng-disabled="self.isOwnerAtomUrisLoading"
          >
            Reload
          </div>
        </div>
      </div>
      <div className="owneroverview__content" ng-if="self.hasVisibleAtomUris">
        <won-atom-card
          className="owneroverview__content__atom"
          atom-uri="atomUri"
          current-location="self.currentLocation"
          ng-repeat="atomUri in self.sortedVisibleAtomUriArray track by atomUri"
          show-suggestions="::false"
          show-persona="::true"
        />
      </div>
      <div
        className="owneroverview__noresults"
        ng-if="!self.hasVisibleAtomUris"
      >
        <span className="owneroverview__noresults__label">
          Nothing new found.
        </span>
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

    const selectFromState = state => {
      const viewAtomUri = generalSelectors.getViewAtomUriFromRoute(state);
      const viewConnUri = generalSelectors.getViewConnectionUriFromRoute(state);

      const whatsNewMetaAtoms = getIn(state, ["owner", "whatsNew"])
        .filter(metaAtom => atomUtils.isActive(metaAtom))
        .filter(metaAtom => !atomUtils.isSearchAtom(metaAtom))
        .filter(metaAtom => !atomUtils.isDirectResponseAtom(metaAtom))
        .filter(metaAtom => !atomUtils.isInvisibleAtom(metaAtom))
        .filter(
          (metaAtom, metaAtomUri) =>
            !generalSelectors.isAtomOwned(state, metaAtomUri)
        );

      const sortedVisibleAtoms = sortByDate(whatsNewMetaAtoms, "creationDate");
      const sortedVisibleAtomUriArray = sortedVisibleAtoms && [
        ...sortedVisibleAtoms.flatMap(visibleAtom => get(visibleAtom, "uri")),
      ];
      const lastAtomUrisUpdateDate = getIn(state, [
        "owner",
        "lastWhatsNewUpdateTime",
      ]);

      const process = get(state, "process");
      const isOwnerAtomUrisLoading = processUtils.isProcessingWhatsNew(process);
      const isOwnerAtomUrisToLoad =
        !lastAtomUrisUpdateDate && !isOwnerAtomUrisLoading;

      return {
        currentLocation: generalSelectors.getCurrentLocation(state),
        lastAtomUrisUpdateDate,
        friendlyLastAtomUrisUpdateTimestamp:
          lastAtomUrisUpdateDate &&
          wonLabelUtils.relativeTime(
            generalSelectors.selectLastUpdateTime(state),
            lastAtomUrisUpdateDate
          ),
        sortedVisibleAtomUriArray,
        hasVisibleAtomUris:
          sortedVisibleAtomUriArray && sortedVisibleAtomUriArray.length > 0,
        sortedVisibleAtomUriSize: sortedVisibleAtomUriArray
          ? sortedVisibleAtomUriArray.length
          : 0,
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
      this.atoms__fetchWhatsNew();
    }
  }

  reload() {
    if (!this.isOwnerAtomUrisLoading) {
      const modifiedAfterDate =
        new Date(this.lastAtomUrisUpdateDate) ||
        new Date(Date.now() - 30 /*Days before*/ * 86400000);
      this.atoms__fetchWhatsNew(modifiedAfterDate);
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
