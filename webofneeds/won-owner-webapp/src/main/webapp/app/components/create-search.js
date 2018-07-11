/**
 * Created by ksinger on 24.08.2015.
 */
import angular from "angular";
import ngAnimate from "angular-animate";

import "ng-redux";
import labelledHrModule from "./labelled-hr.js";
import matchingContextModule from "./details/matching-context-picker.js"; // TODO: should be renamed
import { getIn, attach, delay } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import { connect2Redux } from "../won-utils.js";
import { selectIsConnected } from "../selectors.js";

import titlePickerModule from "./details/title-picker.js";

//TODO can't inject $scope with the angular2-router, preventing redux-cleanup
const serviceDependencies = [
  "$ngRedux",
  "$scope",
  "$element" /*'$routeParams' /*injections as strings here*/,
];

function genComponentConf() {
  const template = `
        <!-- HEADER: -->
        <div class="cp__header">
            <a class="cp__header__back clickable"
                ng-click="self.router__stateGoCurrent({useCase: undefined})">
                <svg style="--local-primary:var(--won-primary-color);"
                    class="cp__header__back__icon">
                    <use xlink:href="#ico36_backarrow" href="#ico36_backarrow"></use>
                </svg>
            </a>
            <span class="cp__header__title">Search</span>
        </div>

        <!-- CONTENT: -->
        <div class="cp__content">

            <!-- ADD SEARCH STRING -->
            <won-title-picker
              on-update="::self.updateDetail(value)"
              initial-value="::self.draftObject.searchString">
            </won-title-picker>
            

            <!-- TUNE MATCHING -->
            <won-labelled-hr label="::'Matching behaviour'" class="cp__content__labelledhr"> </won-labelled-hr>
            
            <!-- TODO: when should this be shown as an option? --> 
            <div class="cp__content__tuning">
                <div class="cp__content__tuning__title b detailPicker clickable"
                    ng-click="self.toggleTuningOptions()"
                    ng-class="{'closedDetailPicker': !self.showTuningOptions}">
                    <span>Tune Matching Behaviour</span>
                    <svg class="cp__content__tuning__title__carret" ng-show="!self.showTuningOptions">
                        <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
                    </svg>
                    <svg class="cp__content__tuning__title__carret" ng-show="self.showTuningOptions">
                        <use xlink:href="#ico16_arrow_up" href="#ico16_arrow_up"></use>
                    </svg>
                </div>
                <div class="cp__content__tuning_matching-context">
                    <won-matching-context-picker
                      ng-if="self.showTuningOptions"
                      default-matching-context="::self.defaultMatchingContext"
                      on-matching-context-updated="::self.updateMatchingContext(matchingContext)">
                    </won-matching-context-picker>
                </div>
            </div>

            <!-- PUBLISH BUTTON - RESPONSIVE MODE -->
            <won-labelled-hr label="::'done?'" class="cp__content__labelledhr show-in-responsive"></won-labelled-hr>

            <button type="submit" class="won-button--filled red cp__content__publish show-in-responsive"
                    ng-disabled="!self.isValid()"
                    ng-click="::self.publish()">
                <span ng-show="!self.pendingPublishing">
                    Publish
                </span>
                <span ng-show="self.pendingPublishing">
                    Publishing&nbsp;&hellip;
                </span>
            </button>

        </div>

        <!-- FOOTER: PUBLISH BUTTON - NON-RESPONSIVE MODE -->
        <div class="cp__footer hide-in-responsive" >
            <won-labelled-hr label="::'done?'" class="cp__footer__labelledhr"></won-labelled-hr>
            <button type="submit" class="won-button--filled red cp__footer__publish"
                    ng-disabled="!self.isValid()"
                    ng-click="::self.publish()">
                <span ng-show="!self.pendingPublishing">
                    Publish
                </span>
                <span ng-show="self.pendingPublishing">
                    Publishing&nbsp;&hellip;
                </span>
            </button>
        </div>
    `;

  /**
   * current status, TOODs
   * TODO: displaying search string in post-info - should no longer need seeks.title
   * TODO: search for mentions of title and searchString throughout the app and fix stuff
   * TODO: ask flo wegen ZEP
   */

  class Controller {
    constructor(/* arguments <- serviceDependencies */) {
      attach(this, serviceDependencies, arguments);
      this.focusedElement = null;
      //TODO debug; deleteme
      window.cns4dbg = this;

      this.windowHeight = window.screen.height;
      this.scrollContainer().addEventListener("scroll", e => this.onResize(e));

      this.draftObject = {};
      this.draftObject.useCase = "search";
      this.draftObject.seeks = {};

      this.showTuningOptions = false;

      this.pendingPublishing = false;
      this.isNew = true;

      const selectFromState = state => {
        const defaultMatchingContextList = getIn(state, [
          "config",
          "theme",
          "defaultContext",
        ]);
        const defaultMatchingContext = defaultMatchingContextList
          ? defaultMatchingContextList.toJS()
          : [];

        return {
          connectionHasBeenLost: !selectIsConnected(state),
          defaultMatchingContext: defaultMatchingContext,
        };
      };

      delay(0).then(() => {
        this.updateMatchingContext(this.defaultMatchingContext);
      });

      // Using actionCreators like this means that every action defined there is available in the template.
      connect2Redux(selectFromState, actionCreators, [], this);
    }

    onResize() {
      if (this.focusedElement) {
        if (this.windowHeight < window.screen.height) {
          this.windowHeight < window.screen.height;
          this.scrollToBottom(this.focusedElement);
        } else {
          this.windowHeight = window.screen.height;
        }
      }
    }

    scrollToBottom(element) {
      this._programmaticallyScrolling = true;

      if (element) {
        let heightHeader =
          this.$element[0].querySelector(".cp__header").offsetHeight + 10;
        let scrollTop = this.$element[0].querySelector(element).offsetTop;
        this.scrollContainer().scrollTop = scrollTop - heightHeader;

        this.focusedElement = element;
      }
    }

    scrollContainer() {
      if (!this._scrollContainer) {
        this._scrollContainer = this.$element[0].querySelector(".cp__content");
      }
      return this._scrollContainer;
    }

    toggleTuningOptions() {
      this.showTuningOptions = !this.showTuningOptions;
    }

    isValid() {
      const draft = this.draftObject;
      const hasSearchString =
        this.draftObject &&
        this.draftObject.searchString &&
        this.draftObject.searchString.trim().length > 0;
      return !this.connectionHasBeenLost && !!draft && hasSearchString;
    }

    updateMatchingContext(matchingContext) {
      if (matchingContext) {
        this.draftObject.matchingContext = matchingContext;
      }
    }

    updateDetail(value) {
      if (this.isNew) {
        this.isNew = false;
      }
      this.draftObject.searchString = value;
    }

    publish() {
      // Post both needs
      if (!this.pendingPublishing) {
        this.pendingPublishing = true;

        this.needs__create(
          this.draftObject,
          this.$ngRedux.getState().getIn(["config", "defaultNodeUri"])
        );
      }
    }
  }

  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      /*scope-isolation*/
    },
    template: template,
  };
}

export default //.controller('CreateNeedController', [...serviceDependencies, CreateNeedController])
angular
  .module("won.owner.components.createSearch", [
    labelledHrModule,
    titlePickerModule,
    matchingContextModule,
    ngAnimate,
  ])
  .directive("wonCreateSearch", genComponentConf).name;
