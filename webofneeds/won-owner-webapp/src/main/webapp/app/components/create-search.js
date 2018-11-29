/**
 * Created by ksinger on 24.08.2015.
 */
import angular from "angular";
import ngAnimate from "angular-animate";

import "ng-redux";
import labelledHrModule from "./labelled-hr.js";
import matchingContextModule from "./details/picker/matching-context-picker.js"; // TODO: should be renamed
import { getIn, attach, delay } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import { connect2Redux } from "../won-utils.js";
import { selectIsConnected } from "../selectors/general-selectors.js";

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
                    class="cp__header__back__icon show-in-responsive">
                    <use xlink:href="#ico36_backarrow" href="#ico36_backarrow"></use>
                </svg>
                <svg style="--local-primary:var(--won-primary-color);"
                    class="cp__header__back__icon hide-in-responsive">
                    <use xlink:href="#ico36_close" href="#ico36_close"></use>
                </svg>
            </a>
            <svg class="cp__header__icon"
                title="Search"
                style="--local-primary:var(--won-primary-color);">
                    <use xlink:href="#ico36_search" href="#ico36_search"></use>
            </svg>
            <span class="cp__header__title">Search</span>
        </div>

        <!-- CONTENT: -->
        <div class="cp__content">

            <!-- ADD SEARCH STRING -->
            <won-title-picker
              on-update="::self.updateDetail(value)"
              initial-value="::self.draftObject.content.searchString">
            </won-title-picker>
            

            <!-- TUNE MATCHING -->
            <!-- TODO: when should this be shown as an option? -->
            <div class="cp__content__branchheader b detailPicker clickable"
                ng-click="self.toggleTuningOptions()">
                <span>Tune Matching Behaviour</span>
                <svg class="cp__content__branchheader__carret" ng-show="!self.showTuningOptions">
                    <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
                </svg>
                <svg class="cp__content__branchheader__carret" ng-show="self.showTuningOptions">
                    <use xlink:href="#ico16_arrow_up" href="#ico16_arrow_up"></use>
                </svg>
            </div>
            <div class="cp__content__tuning">
                <div class="cp__content__tuning_matching-context">
                    <won-matching-context-picker
                      ng-if="self.showTuningOptions"
                      default-matching-context="::self.defaultMatchingContext"
                      on-matching-context-updated="::self.updateMatchingContext(matchingContext)">
                    </won-matching-context-picker>
                </div>
            </div>

            <!-- PUBLISH BUTTON - RESPONSIVE MODE -->
            <div class="cp__content__responsivebuttons show-in-responsive">
              <won-labelled-hr label="::'done?'" class="cp__content__labelledhr"></won-labelled-hr>
              <button type="submit" class="won-button--filled red cp__content__publish"
                      ng-disabled="!self.isValid()"
                      ng-click="::self.publish()">
                  <span ng-show="!self.processingPublish">
                      Publish
                  </span>
                  <span ng-show="self.processingPublish">
                      Publishing&nbsp;&hellip;
                  </span>
              </button>
            </div>
        </div>

        <!-- FOOTER: PUBLISH BUTTON - NON-RESPONSIVE MODE -->
        <div class="cp__footer hide-in-responsive" >
            <won-labelled-hr label="::'done?'" class="cp__footer__labelledhr"></won-labelled-hr>
            <button type="submit" class="won-button--filled red cp__footer__publish"
                    ng-disabled="!self.isValid()"
                    ng-click="::self.publish()">
                <span ng-show="!self.processingPublish">
                    Publish
                </span>
                <span ng-show="self.processingPublish">
                    Publishing&nbsp;&hellip;
                </span>
            </button>
        </div>
    `;

  class Controller {
    constructor(/* arguments <- serviceDependencies */) {
      attach(this, serviceDependencies, arguments);
      this.focusedElement = null;
      //TODO debug; deleteme
      window.cns4dbg = this;

      this.windowHeight = window.screen.height;
      this.scrollContainer().addEventListener("scroll", e => this.onResize(e));

      this.draftObject = {
        content: {},
        seeks: {},
        useCase: "search",
        matchingContext: undefined,
      };

      this.showTuningOptions = false;

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
          processingPublish: state.getIn(["process", "processingPublish"]),
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
        this.draftObject.content &&
        this.draftObject.content.searchString &&
        this.draftObject.content.searchString.trim().length > 0;
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
      this.draftObject.content.searchString = value;
    }

    publish() {
      // Post both needs
      if (!this.processingPublish) {
        this.needs__create(
          this.draftObject,
          undefined,
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
    matchingContextModule,
    ngAnimate,
  ])
  .directive("wonCreateSearch", genComponentConf).name;
