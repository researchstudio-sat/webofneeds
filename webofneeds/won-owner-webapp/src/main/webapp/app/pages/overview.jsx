/** @jsx h */

/**
 * Created by quasarchimaere on 04.04.2019.
 */
import angular from "angular";
import ngAnimate from "angular-animate";
import { delay, get, getIn, sortByDate } from "../utils.js";
import { attach, classOnComponentRoot } from "../cstm-ng-utils.js";
import { connect2Redux } from "../configRedux.js";
import { actionCreators } from "../actions/actions.js";
import postMessagesModule from "../components/post-messages.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as viewSelectors from "../redux/selectors/view-selectors.js";
import * as processUtils from "../redux/utils/process-utils.js";
import * as wonLabelUtils from "../won-label-utils.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as useCaseUtils from "../usecase-utils.js";
import WonAtomCardGrid from "../components/atom-card-grid.jsx";
import { h } from "preact";

import "~/style/_overview.scss";
import "~/style/_connection-overlay.scss";
import * as accountUtils from "../redux/utils/account-utils";

const template = (
  <container>
    <won-modal-dialog ng-if="self.showModalDialog" />
    <div
      className="won-modal-connectionview"
      ng-if="self.showConnectionOverlay"
    >
      <won-post-messages connection-uri="self.viewConnUri" />
    </div>
    <won-topnav page-title="::'What\'s New'" />
    <won-menu ng-if="self.isLoggedIn" />
    <won-toasts />
    <won-slide-in ng-if="self.showSlideIns" />
    <main className="owneroverview">
      <div className="owneroverview__header">
        <div className="owneroverview__header__title">
          {"What's new? "}
          <span
            className="owneroverview__header__title__count"
            ng-if="!self.isOwnerAtomUrisLoading && self.hasVisibleAtomUris && !self.whatsNewUseCaseIdentifierArray"
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
      <div className="owneroverview__usecases" ng-if="self.hasVisibleAtomUris">
        <div
          className="owneroverview__usecases__usecase"
          ng-repeat="ucIdentifier in self.whatsNewUseCaseIdentifierArray track by ucIdentifier"
        >
          <div className="owneroverview__usecases__usecase__header">
            <svg
              className="owneroverview__usecases__usecase__header__icon"
              ng-if="self.getUseCaseIcon(ucIdentifier)"
            >
              <use
                xlinkHref="{{ self.getUseCaseIcon(ucIdentifier) }}"
                href="{{ self.getUseCaseIcon(ucIdentifier) }}"
              />
            </svg>
            <div className="owneroverview__usecases__usecase__header__title">
              {"{{self.getUseCaseLabel(ucIdentifier)}}"}
              <span className="owneroverview__usecases__usecase__header__title__count">
                ({"{{ self.getAtomsSizeByUseCase(ucIdentifier) }}"})
              </span>
            </div>
            <svg
              className="owneroverview__usecases__usecase__header__carret"
              ng-click="self.toggleUseCase(ucIdentifier)"
              ng-class="{
              'owneroverview__usecases__usecase__header__carret--expanded': self.isUseCaseExpanded(ucIdentifier),
              'owneroverview__usecases__usecase__header__carret--collapsed': !self.isUseCaseExpanded(ucIdentifier)
            }"
            >
              <use xlinkHref="#ico16_arrow_down" href="#ico16_arrow_down" />
            </svg>
          </div>
          <div
            className="owneroverview__usecases__usecase__atoms"
            ng-show="self.isUseCaseExpanded(ucIdentifier)"
          >
            <won-preact
              component="self.WonAtomCardGrid"
              props="{ atomUris: self.getSortedVisibleAtomUriArrayByUseCase(ucIdentifier), currentLocation: self.currentLocation, showSuggestions: false, showPersona: true, showCreate: false }"
            />
          </div>
        </div>
        <div
          className="owneroverview__usecases__usecase"
          ng-if="self.hasOtherAtoms()"
        >
          <div
            className="owneroverview__usecases__usecase__header"
            ng-if="self.whatsNewUseCaseIdentifierArray"
          >
            <div className="owneroverview__usecases__usecase__header__title">
              Other
              <span className="owneroverview__usecases__usecase__header__title__count">
                ({"{{ self.getOtherAtomsSize() }}"})
              </span>
            </div>
            <svg
              className="owneroverview__usecases__usecase__header__carret"
              ng-click="self.toggleUseCase(undefined)"
              ng-class="{
              'owneroverview__usecases__usecase__header__carret--expanded': self.isUseCaseExpanded(undefined),
              'owneroverview__usecases__usecase__header__carret--collapsed': !self.isUseCaseExpanded(undefined)
            }"
            >
              <use xlinkHref="#ico16_arrow_down" href="#ico16_arrow_down" />
            </svg>
          </div>
          <div
            className="owneroverview__usecases__usecase__atoms"
            ng-show="self.isUseCaseExpanded(undefined)"
          >
            <won-preact
              component="self.WonAtomCardGrid"
              props="{ atomUris: self.getSortedVisibleOtherAtomUriArray(), currentLocation: self.currentLocation, showSuggestions: false, showPersona: true, showCreate: false }"
            />
          </div>
        </div>
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

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
class Controller {
  constructor() {
    attach(this, serviceDependencies, arguments);
    window.overview4dbg = this;
    this.open = [];

    this.WonAtomCardGrid = WonAtomCardGrid;

    const selectFromState = state => {
      const viewConnUri = generalSelectors.getViewConnectionUriFromRoute(state);
      const whatsNewAtoms = generalSelectors
        .getWhatsNewAtoms(state)
        .filter(metaAtom => atomUtils.isActive(metaAtom))
        .filter(metaAtom => !atomUtils.isSearchAtom(metaAtom))
        .filter(metaAtom => !atomUtils.isDirectResponseAtom(metaAtom))
        .filter(metaAtom => !atomUtils.isInvisibleAtom(metaAtom))
        .filter(
          (metaAtom, metaAtomUri) =>
            !generalSelectors.isAtomOwned(state, metaAtomUri)
        );

      const whatsNewUseCaseIdentifierArray = whatsNewAtoms
        .map(atom => getIn(atom, ["matchedUseCase", "identifier"]))
        .filter(identifier => !!identifier)
        .toSet()
        .toArray();

      const sortedVisibleAtoms = sortByDate(whatsNewAtoms, "creationDate");
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

      const accountState = get(state, "account");

      return {
        whatsNewUseCaseIdentifierArray: whatsNewUseCaseIdentifierArray,
        whatsNewAtoms: whatsNewAtoms,
        isLoggedIn: accountUtils.isLoggedIn(accountState),
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
          viewSelectors.hasSlideIns(state) &&
          viewSelectors.isSlideInsVisible(state),
        showModalDialog: viewSelectors.showModalDialog(state),
        showConnectionOverlay: !!viewConnUri,
        viewConnUri,
      };
    };

    connect2Redux(selectFromState, actionCreators, [], this);
    classOnComponentRoot("won-signed-out", () => !this.isLoggedIn, this);

    this.$scope.$watch(
      () => this.isOwnerAtomUrisToLoad,
      () => delay(0).then(() => this.ensureAtomUrisLoaded())
    );
  }

  getUseCaseLabel(ucIdentifier) {
    return useCaseUtils.getUseCaseLabel(ucIdentifier);
  }

  getUseCaseIcon(ucIdentifier) {
    return useCaseUtils.getUseCaseIcon(ucIdentifier);
  }

  getSortedVisibleAtomUriArrayByUseCase(ucIdentifier) {
    const useCaseAtoms = this.whatsNewAtoms.filter(
      atom => atomUtils.getMatchedUseCaseIdentifier(atom) === ucIdentifier
    );
    const sortedUseCaseAtoms = sortByDate(useCaseAtoms, "creationDate");
    return (
      sortedUseCaseAtoms && [
        ...sortedUseCaseAtoms.flatMap(atom => get(atom, "uri")),
      ]
    );
  }
  getSortedVisibleOtherAtomUriArray() {
    const useCaseAtoms = this.whatsNewAtoms.filter(
      atom => !atomUtils.hasMatchedUseCase(atom)
    );
    const sortedUseCaseAtoms = sortByDate(useCaseAtoms, "creationDate");
    return (
      sortedUseCaseAtoms && [
        ...sortedUseCaseAtoms.flatMap(atom => get(atom, "uri")),
      ]
    );
  }

  hasOtherAtoms() {
    return !!this.whatsNewAtoms.find(
      atom => !atomUtils.hasMatchedUseCase(atom)
    );
  }

  getOtherAtomsSize() {
    const useCaseAtoms = this.whatsNewAtoms.filter(
      atom => !atomUtils.hasMatchedUseCase(atom)
    );
    return useCaseAtoms ? useCaseAtoms.size : 0;
  }

  getAtomsSizeByUseCase(ucIdentifier) {
    const useCaseAtoms = this.whatsNewAtoms.filter(
      atom => atomUtils.getMatchedUseCaseIdentifier(atom) === ucIdentifier
    );
    return useCaseAtoms ? useCaseAtoms.size : 0;
  }

  toggleUseCase(ucIdentifier) {
    if (this.isUseCaseExpanded(ucIdentifier)) {
      this.open = this.open.filter(element => ucIdentifier !== element);
    } else {
      this.open.push(ucIdentifier);
    }
  }

  isUseCaseExpanded(ucIdentifier) {
    return this.open.includes(ucIdentifier);
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
    .module("won.owner.components.overview", [ngAnimate, postMessagesModule])
    .controller("OverviewController", Controller).name,
  controller: "OverviewController",
  template: template,
};
