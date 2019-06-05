/** @jsx h */

import angular from "angular";
import ngAnimate from "angular-animate";
import { attach, get, sortByDate } from "../utils.js";
import { connect2Redux } from "../won-utils.js";
import { actionCreators } from "../actions/actions.js";
import postMessagesModule from "../components/post-messages.js";
import atomCardModule from "../components/atom-card.js";
import howToModule from "../components/howto.js";
import * as generalSelectors from "../selectors/general-selectors.js";
import * as viewSelectors from "../selectors/view-selectors.js";
import * as atomUtils from "../atom-utils.js";

import { h } from "preact";

import "~/style/_inventory.scss";
import "~/style/_atom-overlay.scss";
import "~/style/_connection-overlay.scss";
import * as viewUtils from "../view-utils";
import { getIn } from "../utils";
import * as accountUtils from "../account-utils";

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
      <div className="ownerinventory__header">
        <div className="ownerinventory__header__title">
          Active
          <span
            className="ownerinventory__header__title__count"
            ng-if="self.hasOwnedActiveAtomUris"
          >
            {"({{self.sortedOwnedActiveAtomUriSize}})"}
          </span>
        </div>
      </div>
      <div
        className="ownerinventory__content"
        ng-if="self.hasOwnedActiveAtomUris"
      >
        <won-atom-card
          className="ownerinventory__content__atom"
          atom-uri="atomUri"
          current-location="self.currentLocation"
          ng-repeat="atomUri in self.sortedOwnedActiveAtomUriArray track by atomUri"
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
        className="ownerinventory__noresults"
        ng-if="!self.hasOwnedActiveAtomUris"
      >
        <span className="ownerinventory__noresults__label">
          Nothing to display
        </span>
      </div>
      <div
        className="ownerinventory__header"
        ng-if="self.hasOwnedInactiveAtomUris"
      >
        <div className="ownerinventory__header__title">
          Archived
          <span className="ownerinventory__header__title__count">
            {"({{self.sortedOwnedInactiveAtomUriSize}})"}
          </span>
        </div>
      </div>
      <div
        className="ownerinventory__content"
        ng-if="self.hasOwnedInactiveAtomUris"
      >
        <won-atom-card
          className="ownerinventory__content__atom"
          atom-uri="atomUri"
          current-location="self.currentLocation"
          ng-repeat="atomUri in self.sortedOwnedInactiveAtomUriArray track by atomUri"
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

      const ownedAtoms = generalSelectors.getOwnedAtoms(state);
      const ownedActiveAtoms = ownedAtoms.filter(atom =>
        atomUtils.isActive(atom)
      );
      const ownedInactiveAtoms = ownedAtoms.filter(atom =>
        atomUtils.isInactive(atom)
      );

      const sortedOwnedActiveAtoms = sortByDate(
        ownedActiveAtoms,
        "creationDate"
      );
      const sortedOwnedActiveAtomUriArray = sortedOwnedActiveAtoms && [
        ...sortedOwnedActiveAtoms.flatMap(atom => get(atom, "uri")),
      ];

      const sortedOwnedInactiveAtoms = sortByDate(
        ownedInactiveAtoms,
        "creationDate"
      );
      const sortedOwnedInactiveAtomUriArray = sortedOwnedInactiveAtoms && [
        ...sortedOwnedInactiveAtoms.flatMap(atom => get(atom, "uri")),
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
        sortedOwnedActiveAtomUriArray,
        sortedOwnedInactiveAtomUriArray,
        hasOwnedActiveAtomUris:
          sortedOwnedActiveAtomUriArray &&
          sortedOwnedActiveAtomUriArray.length > 0,
        hasOwnedInactiveAtomUris:
          sortedOwnedInactiveAtomUriArray &&
          sortedOwnedInactiveAtomUriArray.length > 0,
        sortedOwnedActiveAtomUriSize: sortedOwnedActiveAtomUriArray
          ? sortedOwnedActiveAtomUriArray.length
          : 0,
        sortedOwnedInactiveAtomUriSize: sortedOwnedInactiveAtomUriArray
          ? sortedOwnedInactiveAtomUriArray.length
          : 0,
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
    //TODO: USE ONCE THIS IS COLLAPSIBLE
    /*if (this.unloadedAtomsSize > 0) {
      this.atoms__fetchUnloadedAtoms();
    }
    this.view__toggleClosedAtoms();*/
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
