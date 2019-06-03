/** @jsx h */

import angular from "angular";
import ngAnimate from "angular-animate";
import { attach, get, sortByDate } from "../utils.js";
import { connect2Redux } from "../won-utils.js";
import { actionCreators } from "../actions/actions.js";
import postMessagesModule from "../components/post-messages.js";
import atomCardModule from "../components/atom-card.js";
import postHeaderModule from "../components/post-header.js";
import * as generalSelectors from "../selectors/general-selectors.js";
import * as viewSelectors from "../selectors/view-selectors.js";
import * as atomUtils from "../atom-utils.js";

import { h } from "preact";

import "~/style/_inventory.scss";
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
    <won-topnav page-title="::'Inventory'" />
    <won-toasts />
    <won-slide-in ng-if="self.showSlideIns" />
    <main className="ownerinventory">
      <div className="ownerinventory__header">
        <div className="ownerinventory__header__title">
          Open
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
      </div>
      <div
        className="ownerinventory__noresults"
        ng-if="!self.hasOwnedActiveAtomUris"
      >
        <span className="ownerinventory__noresults__label">
          Nothing to display
        </span>
      </div>
      <div className="ownerinventory__header">
        <div className="ownerinventory__header__title">
          Closed
          <span
            className="ownerinventory__header__title__count"
            ng-if="self.hasOwnedInactiveAtomUris"
          >
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
      <div
        className="ownerinventory__noresults"
        ng-if="!self.hasOwnedInactiveAtomUris"
      >
        <span className="ownerinventory__noresults__label">
          Nothing to display
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

      return {
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
      };
    };

    connect2Redux(selectFromState, actionCreators, [], this);
  }
}

Controller.$inject = [];

export default {
  module: angular
    .module("won.owner.components.inventory", [
      ngAnimate,
      postMessagesModule,
      atomCardModule,
      postHeaderModule,
    ])
    .controller("InventoryController", [...serviceDependencies, Controller])
    .name,
  controller: "InventoryController",
  template: template,
};
