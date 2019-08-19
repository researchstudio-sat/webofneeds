/** @jsx h */

import angular from "angular";
import ngAnimate from "angular-animate";
import { get, getIn, sortByDate } from "../utils.js";
import { attach, classOnComponentRoot } from "../cstm-ng-utils.js";
import { connect2Redux } from "../configRedux.js";
import { actionCreators } from "../actions/actions.js";
import howToModule from "../components/howto.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as viewSelectors from "../redux/selectors/view-selectors.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as processUtils from "../redux/utils/process-utils.js";
import * as accountUtils from "../redux/utils/account-utils.js";
import * as viewUtils from "../redux/utils/view-utils.js";
import WonAtomCardGrid from "../components/atom-card-grid.jsx";
import WonAtomInfo from "../components/atom-info.jsx";
import WonAtomMessages from "../components/atom-messages.jsx";

import { h } from "preact";

import "~/style/_inventory.scss";
import "~/style/_connection-overlay.scss";

const template = (
  <container>
    <won-modal-dialog ng-if="self.showModalDialog" />
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
    <won-topnav page-title="::'Inventory'" />
    <won-menu ng-if="self.isLoggedIn" />
    <won-toasts />
    <won-slide-in ng-if="self.showSlideIns" />
    <main
      className="ownerloading"
      ng-if="self.isLoggedIn && self.isInitialLoadInProgress"
    >
      <svg className="ownerloading__spinner hspinner">
        <use xlinkHref="#ico_loading_anim" href="#ico_loading_anim" />
      </svg>
      <span className="ownerloading__label">Gathering your Atoms...</span>
    </main>
    <main className="ownerwelcome" ng-if="!self.isLoggedIn">
      <div
        className="ownerwelcome__text"
        ng-include="self.welcomeTemplatePath"
      />
      <won-how-to />
    </main>
    <main
      className="ownerinventory"
      ng-if="self.isLoggedIn && !self.isInitialLoadInProgress"
    >
      <div
        className="ownerinventory__personas"
        ng-if="self.hasOwnedActivePersonas"
      >
        <won-preact
          className="atomInfo ownerinventory__personas__persona"
          component="self.WonAtomInfo"
          props="{atomUri: personaUri}"
          ng-repeat="personaUri in self.sortedOwnedActivePersonaUriArray track by personaUri"
        />
      </div>
      <div className="ownerinventory__header">
        <div className="ownerinventory__header__title">
          Unassigned
          <span
            className="ownerinventory__header__title__count"
            ng-if="self.hasOwnedUnassignedAtomUris"
          >
            {"({{self.unassignedAtomSize}})"}
          </span>
        </div>
      </div>
      <div className="ownerinventory__content">
        <won-preact
          component="self.WonAtomCardGrid"
          props="{ atomUris: self.sortedOwnedUnassignedAtomUriArray, currentLocation: self.currentLocation, showSuggestions: true, showPersona: true, showCreate: true }"
        />
      </div>
      <div
        className="ownerinventory__header"
        ng-if="self.hasOwnedInactiveAtomUris"
      >
        <div className="ownerinventory__header__title">
          Archived
          <span className="ownerinventory__header__title__count">
            {"({{self.inactiveAtomUriSize}})"}
          </span>
        </div>
        <svg
          className="ownerinventory__header__carret"
          ng-click="self.toggleClosedAtoms()"
          ng-class="{
            'ownerinventory__header__carret--expanded': self.showClosedAtoms,
            'ownerinventory__header__carret--collapsed': !self.showClosedAtoms
          }"
        >
          <use xlinkHref="#ico16_arrow_down" href="#ico16_arrow_down" />
        </svg>
      </div>
      <div
        className="ownerinventory__content"
        ng-if="self.showClosedAtoms && self.hasOwnedInactiveAtomUris"
      >
        <won-preact
          component="self.WonAtomCardGrid"
          props="{ atomUris: self.sortedOwnedInactiveAtomUriArray, currentLocation: self.currentLocation, showSuggestions: false, showPersona: false, showCreate: false }"
        />
      </div>
    </main>
    <won-footer />
  </container>
);

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
class Controller {
  constructor() {
    attach(this, serviceDependencies, arguments);
    window.inventory4dbg = this;

    this.WonAtomCardGrid = WonAtomCardGrid;
    this.WonAtomInfo = WonAtomInfo;
    this.WonAtomMessages = WonAtomMessages;

    const selectFromState = state => {
      const viewConnUri = generalSelectors.getViewConnectionUriFromRoute(state);

      const ownedActivePersonas = generalSelectors
        .getOwnedPersonas(state)
        .filter(atom => atomUtils.isActive(atom));
      const ownedUnassignedActivePosts = generalSelectors
        .getOwnedPosts(state)
        .filter(atom => atomUtils.isActive(atom))
        .filter(atom => !atomUtils.isHeld(atom));
      const ownedInactiveAtoms = generalSelectors
        .getOwnedAtoms(state)
        .filter(atom => atomUtils.isInactive(atom));

      const sortedOwnedUnassignedActivePosts = sortByDate(
        ownedUnassignedActivePosts,
        "creationDate"
      );
      const sortedOwnedUnassignedAtomUriArray = sortedOwnedUnassignedActivePosts
        ? [
            ...sortedOwnedUnassignedActivePosts.flatMap(atom =>
              get(atom, "uri")
            ),
          ]
        : [];

      const sortedOwnedInactiveAtoms = sortByDate(
        ownedInactiveAtoms,
        "creationDate"
      );
      const sortedOwnedInactiveAtomUriArray = sortedOwnedInactiveAtoms
        ? [...sortedOwnedInactiveAtoms.flatMap(atom => get(atom, "uri"))]
        : [];

      const sortedOwnedActivePersonas = sortByDate(
        ownedActivePersonas,
        "modifiedDate"
      );

      const sortedOwnedActivePersonaUriArray = sortedOwnedActivePersonas && [
        ...sortedOwnedActivePersonas.flatMap(atom => get(atom, "uri")),
      ];

      const viewState = get(state, "view");

      const theme = getIn(state, ["config", "theme"]);
      const themeName = get(theme, "name");
      const welcomeTemplate = get(theme, "welcomeTemplate");

      const accountState = get(state, "account");
      const process = get(state, "process");

      return {
        isInitialLoadInProgress: processUtils.isProcessingInitialLoad(process),
        isLoggedIn: accountUtils.isLoggedIn(accountState),
        welcomeTemplatePath: "./skin/" + themeName + "/" + welcomeTemplate,
        currentLocation: generalSelectors.getCurrentLocation(state),
        sortedOwnedUnassignedAtomUriArray,
        sortedOwnedInactiveAtomUriArray,
        hasOwnedUnassignedAtomUris:
          sortedOwnedUnassignedAtomUriArray &&
          sortedOwnedUnassignedAtomUriArray.length > 0,
        hasOwnedInactiveAtomUris:
          sortedOwnedInactiveAtomUriArray &&
          sortedOwnedInactiveAtomUriArray.length > 0,
        unassignedAtomSize: sortedOwnedUnassignedAtomUriArray
          ? sortedOwnedUnassignedAtomUriArray.length
          : 0,
        inactiveAtomUriSize: sortedOwnedInactiveAtomUriArray
          ? sortedOwnedInactiveAtomUriArray.length
          : 0,
        hasOwnedActivePersonas:
          sortedOwnedActivePersonaUriArray &&
          sortedOwnedActivePersonaUriArray.length > 0,
        sortedOwnedActivePersonaUriArray,
        showSlideIns:
          viewSelectors.hasSlideIns(state) &&
          viewSelectors.isSlideInsVisible(state),
        showModalDialog: viewSelectors.showModalDialog(state),
        showConnectionOverlay: !!viewConnUri,
        viewConnUri,
        showClosedAtoms: viewUtils.showClosedAtoms(viewState),
      };
    };

    connect2Redux(selectFromState, actionCreators, [], this);
    classOnComponentRoot("won-signed-out", () => !this.isLoggedIn, this);
  }

  toggleClosedAtoms() {
    this.view__toggleClosedAtoms();
  }
}

Controller.$inject = [];

export default {
  module: angular
    .module("won.owner.components.inventory", [ngAnimate, howToModule])
    .controller("InventoryController", [...serviceDependencies, Controller])
    .name,
  controller: "InventoryController",
  template: template,
};
