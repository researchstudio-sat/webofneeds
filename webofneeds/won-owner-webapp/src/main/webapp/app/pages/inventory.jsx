/** @jsx h */

import angular from "angular";
import ngAnimate from "angular-animate";
import { get, sortByDate } from "../utils.js";
import { attach } from "../cstm-ng-utils.js";
import { connect2Redux } from "../configRedux.js";
import { actionCreators } from "../actions/actions.js";
import postMessagesModule from "../components/post-messages.js";
import atomCardModule from "../components/atom-card.js";
import howToModule from "../components/howto.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as viewSelectors from "../redux/selectors/view-selectors.js";
import * as atomUtils from "../redux/utils/atom-utils.js";

import { h } from "preact";

import "~/style/_inventory.scss";
import "~/style/_atom-overlay.scss";
import "~/style/_connection-overlay.scss";
import * as viewUtils from "../redux/utils/view-utils";
import { getIn } from "../utils";
import * as accountUtils from "../redux/utils/account-utils";

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
    <won-topnav />
    <won-menu ng-if="self.isLoggedIn" />
    <won-toasts />
    <won-slide-in ng-if="self.showSlideIns" />
    <main className="ownerwelcome" ng-if="!self.isLoggedIn">
      <div
        className="ownerwelcome__text"
        ng-include="self.welcomeTemplatePath"
      />
      <won-how-to />
    </main>
    <main className="ownerinventory" ng-if="self.isLoggedIn">
      <div className="ownerinventory__personas" ng-if="self.hasOwnedActivePersonas">
        <won-post-info
          class="ownerinventory__personas__persona"
          ng-repeat="personaUri in self.sortedOwnedActivePersonaUriArray track by personaUri"
          atom-uri="personaUri"
          hide-back-button="true"
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
      <div
        className="ownerinventory__content"
        ng-if="self.hasOwnedUnassignedAtomUris"
      >
        <won-atom-card
          className="ownerinventory__content__atom"
          atom-uri="atomUri"
          current-location="self.currentLocation"
          ng-repeat="atomUri in self.sortedOwnedUnassignedAtomUriArray track by atomUri"
          show-suggestions="::true"
          show-persona="::false"
        />
        <div
          className="ownerinventory__content__createatom"
          ng-click="self.router__stateGo('create')"
        >
          <svg
            className="ownerinventory__content__createatom__icon"
            title="Create a new post"
          >
            <use xlinkHref="#ico36_plus" href="#ico36_plus" />
          </svg>
          <span className="ownerinventory__content__createatom__label">New</span>
        </div>
      </div>
      <div
        className="ownerinventory__content"
        ng-if="!self.hasOwnedUnassignedAtomUris"
      >
        <div
          className="ownerinventory__content__createatom"
          ng-click="self.router__stateGo('create')"
        >
          <svg
            className="ownerinventory__content__createatom__icon"
            title="Create a new post"
          >
            <use xlinkHref="#ico36_plus" href="#ico36_plus" />
          </svg>
          <span className="ownerinventory__content__createatom__label">New</span>
        </div>
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
        <won-atom-card
          className="ownerinventory__content__atom"
          atom-uri="atomUri"
          current-location="self.currentLocation"
          ng-repeat="atomUri in self.sortedOwnedInactiveAtomUriArray track by atomUri"
          show-suggestions="::false"
          show-persona="::false"
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
    this.selection = 0;
    window.inventory4dbg = this;

    const selectFromState = state => {
      const viewAtomUri = generalSelectors.getViewAtomUriFromRoute(state);
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
      const sortedOwnedUnassignedAtomUriArray = sortedOwnedUnassignedActivePosts && [
        ...sortedOwnedUnassignedActivePosts.flatMap(atom => get(atom, "uri")),
      ];

      const sortedOwnedInactiveAtoms = sortByDate(
        ownedInactiveAtoms,
        "creationDate"
      );
      const sortedOwnedInactiveAtomUriArray = sortedOwnedInactiveAtoms && [
        ...sortedOwnedInactiveAtoms.flatMap(atom => get(atom, "uri")),
      ];

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

      return {
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
          viewSelectors.hasSlideIns(state) && viewSelectors.showSlideIns(state),
        showModalDialog: viewSelectors.showModalDialog(state),
        showAtomOverlay: !!viewAtomUri,
        showConnectionOverlay: !!viewConnUri,
        viewAtomUri,
        viewConnUri,
        showClosedAtoms: viewUtils.showClosedAtoms(viewState),
      };
    };

    connect2Redux(selectFromState, actionCreators, [], this);
  }

  toggleClosedAtoms() {
    this.view__toggleClosedAtoms();
  }
}

Controller.$inject = [];

export default {
  module: angular
    .module("won.owner.components.inventory", [
      ngAnimate,
      postMessagesModule,
      atomCardModule,
      howToModule,
    ])
    .controller("InventoryController", [...serviceDependencies, Controller])
    .name,
  controller: "InventoryController",
  template: template,
};
