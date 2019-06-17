/**
 * Created by quasarchimaere on 19.11.2018.
 */
import angular from "angular";
import Immutable from "immutable";
import { attach, getIn } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import { connect2Redux } from "../won-utils.js";

import "~/style/_howto.scss";
import * as generalSelectors from "../redux/selectors/general-selectors";

function genTopnavConf() {
  let template = `
        <h1 class="howto__title">How it works</h1>
        <h3 class="howto__subtitle">
          in {{ self.howItWorksSteps.length }} Steps
        </h3>
        <div class="howto__steps">
          <div
            class="howto__steps__process"
            ng-style="{'--howToColCount': self.howItWorksSteps.length}"
          >
            <svg
              class="howto__steps__process__icon"
              ng-class="{'howto__steps__process__icon--selected': $index == self.selectedHowItWorksStep}"
              ng-repeat="item in self.howItWorksSteps"
              ng-click="self.selectedHowItWorksStep = $index"
            >
              <use
                xlink:href="{{ self.getSvgIconFromItem(item) }}"
                href="{{ self.getSvgIconFromItem(item) }}"
              ></use>
            </svg>
            <div
              class="howto__steps__process__stepcount"
              ng-repeat="item in self.howItWorksSteps"
              ng-class="{'howto__steps__process__stepcount--selected': $index == self.selectedHowItWorksStep}"
              ng-click="self.selectedHowItWorksStep = $index"
            >
              {{ $index + 1 }}
            </div>
            <div class="howto__steps__process__stepline" ></div>
          </div>
          <svg
            class="howto__steps__button howto__steps__button--prev"
            ng-class="{'howto__steps__button--invisible': self.selectedHowItWorksStep <= 0}"
            ng-click="self.selectedHowItWorksStep = self.selectedHowItWorksStep - 1"
          >
            <use xlink:href="#ico36_backarrow" href="#ico36_backarrow" ></use>
          </svg>
          <div class="howto__steps__detail">
            <div class="howto__detail__title">
              {{ self.howItWorksSteps[self.selectedHowItWorksStep].title }}
            </div>
            <div class="howto__steps__detail__text">
              {{ self.howItWorksSteps[self.selectedHowItWorksStep].text }}
            </div>
          </div>
          <svg
            class="howto__steps__button howto__steps__button--next"
            ng-class="{'howto__steps__button--invisible': self.selectedHowItWorksStep >= (self.howItWorksSteps.length-1)}"
            ng-click="self.selectedHowItWorksStep = self.selectedHowItWorksStep + 1"
          >
            <use xlink:href="#ico36_backarrow" href="#ico36_backarrow" ></use>
          </svg>
        </div>
        <h2 class="howto__title">Ready to start?</h2>
        <h3 class="howto__subtitle">
          Post your atom or offer and let {{ self.appTitle }} do the rest
        </h3>
        <div class="howto__createx">
          <button
            class="won-button--filled red howto__createx__button"
            ng-click="self.viewWhatsAround()"
          >
            <svg class="won-button-icon" style="--local-primary:white;">
              <use
                xlink:href="#ico36_location_current"
                href="#ico36_location_current"
              ></use>
            </svg>
            <span>What's in your Area?</span>
          </button>
          <button
            class="won-button--filled red howto__createx__button"
            ng-click="self.viewWhatsNew()"
          >
            <span>What's new?</span>
          </button>
          <won-labelled-hr
            label="::'Or'"
            class="howto__createx__labelledhr"
          ></won-labelled-hr>
          <button
            class="won-button--filled red howto__createx__spanbutton"
            ng-click="self.router__stateGo('create')"
          >
            <span>Post something now!</span>
          </button>
        </div>
  `;

  const serviceDependencies = [
    "$ngRedux",
    "$scope",
    "$state" /*injections as strings here*/,
  ];

  const howItWorksSteps = [
    {
      svgSrc: "#ico36_description",
      title: "Post your atom anonymously",
      text:
        "Atoms can be very personal, so privacy is important. You don't have to reveal your identity here.",
    },
    {
      svgSrc: "#ico36_match",
      title: "Get matches",
      text:
        "Based on the" +
        " information you provide, we will try to connect you with others",
    },
    {
      svgSrc: "#ico36_incoming",
      title: "Request contact – or be contacted",
      text:
        "If you're interested," +
        " make a contact request - or get one if your counterpart is faster than you",
    },
    {
      svgSrc: "#ico36_message",
      title: "Interact and exchange",
      text:
        "You found someone" +
        " who has what you need, wants to meet or change something in your common environment? Go chat with them! ",
    },
  ];

  class Controller {
    constructor(/* arguments <- serviceDependencies */) {
      attach(this, serviceDependencies, arguments);

      const selectFromState = state => {
        return {
          appTitle: getIn(state, ["config", "theme", "title"]),
          isLocationAccessDenied: generalSelectors.isLocationAccessDenied(
            state
          ),
        };
      };

      this.howItWorksSteps = howItWorksSteps;
      this.selectedHowItWorksStep = 0;

      connect2Redux(selectFromState, actionCreators, [], this);
    }

    getSvgIconFromItem(item) {
      return item.svgSrc ? item.svgSrc : "#ico36_uc_question";
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
  .module("won.owner.components.howTo", [])
  .directive("wonHowTo", genTopnavConf).name;
