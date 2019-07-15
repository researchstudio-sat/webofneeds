/**
 * Created by quasarchimaere on 19.11.2018.
 */
import angular from "angular";
import { getIn } from "../utils.js";
import { attach } from "../cstm-ng-utils.js";
import { actionCreators } from "../actions/actions.js";
import { connect2Redux } from "../configRedux.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";

import "~/style/_menu.scss";
import Immutable from "immutable";

function genTopnavConf() {
  let template = `
    <div class="menu">
      <a class="menu__tab" ng-click="self.router__stateGo('inventory')"
        ng-class="{
          'menu__tab--selected': self.showInventory,
          'menu__tab--unread': self.hasUnreadSuggestedConnections || self.hasUnreadBuddyConnections,
        }"
      >
        <span class="menu__tab__unread"></span>
        <span class="menu__tab__label">Inventory</span>
      </a>
      <a class="menu__tab" ng-click="self.router__stateGo('connections')"
        ng-class="{
          'menu__tab--selected': self.showChats,
          'menu__tab--inactive': !self.hasChatAtoms,
          'menu__tab--unread': self.hasUnreadChatConnections,
        }"
      >
        <span class="menu__tab__unread"></span>
        <span class="menu__tab__label">Chats</span>
      </a>
      <a class="menu__tab" ng-click="self.router__stateGo('create')" ng-class="{'menu__tab--selected': self.showCreate}">
        <span class="menu__tab__label">Create</span>
      </a>
      <a class="menu__tab" ng-click="self.viewWhatsNew()" ng-class="{'menu__tab--selected': self.showWhatsNew}">
        <span class="menu__tab__label">What's New</span>
      </a>
      <a class="menu__tab" ng-click="self.viewWhatsAround()" ng-class="{'menu__tab--selected': self.showWhatsAround}">
        <span class="menu__tab__label">What's Around</span>
      </a>
    </div>
  `;

  const serviceDependencies = [
    "$ngRedux",
    "$scope",
    "$state" /*injections as strings here*/,
  ];

  class Controller {
    constructor(/* arguments <- serviceDependencies */) {
      attach(this, serviceDependencies, arguments);

      window.menu4dbg = this;

      const selectFromState = state => {
        const currentRoute = getIn(state, ["router", "currentState", "name"]);

        return {
          isLocationAccessDenied: generalSelectors.isLocationAccessDenied(
            state
          ),
          showInventory: currentRoute === "inventory",
          showChats: currentRoute === "connections",
          showCreate: currentRoute === "create",
          showWhatsNew: currentRoute === "overview",
          showWhatsAround: currentRoute === "map",
          hasChatAtoms: generalSelectors.hasChatAtoms(state),
          hasUnreadSuggestedConnections: generalSelectors.hasUnreadSuggestedConnections(
            state
          ),
          hasUnreadBuddyConnections: generalSelectors.hasUnreadBuddyConnections(
            state,
            true,
            false
          ),
          hasUnreadChatConnections: generalSelectors.hasUnreadChatConnections(
            state
          ),
        };
      };

      connect2Redux(selectFromState, actionCreators, [], this);
    }
    //This method is for debug purposes only, we currently dont offer the createSearch within the ui call menu4dbg.createSearchPost() to access the createSearch View
    createSearchPost() {
      this.router__stateGo("create", {
        useCase: "search",
        useCaseGroup: undefined,
        fromAtomUri: undefined,
        mode: undefined,
      });
    }

    viewWhatsAround() {
      this.viewWhatsX(() => {
        this.router__stateGo("map");
      });
    }

    viewWhatsNew() {
      this.viewWhatsX(() => {
        this.router__stateGo("overview");
      });
    }

    viewWhatsX(callback) {
      if (this.isLocationAccessDenied) {
        callback();
      } else if ("geolocation" in navigator) {
        navigator.geolocation.getCurrentPosition(
          currentLocation => {
            const lat = currentLocation.coords.latitude;
            const lng = currentLocation.coords.longitude;

            this.view__updateCurrentLocation(
              Immutable.fromJS({ location: { lat, lng } })
            );
            callback();
          },
          error => {
            //error handler
            console.error(
              "Could not retrieve geolocation due to error: ",
              error.code,
              ", continuing map initialization without currentLocation. fullerror:",
              error
            );
            this.view__locationAccessDenied();
            callback();
          },
          {
            //options
            enableHighAccuracy: true,
            maximumAge: 30 * 60 * 1000, //use if cache is not older than 30min
          }
        );
      } else {
        console.error("location could not be retrieved");
        this.view__locationAccessDenied();
        callback();
      }
    }
  }
  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    scope: {}, //isolate scope to allow usage within other controllers/components
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    template: template,
  };
}

export default angular
  .module("won.owner.components.menu", [])
  .directive("wonMenu", genTopnavConf).name;
