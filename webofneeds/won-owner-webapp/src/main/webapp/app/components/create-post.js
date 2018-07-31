/**
 * Created by ksinger on 24.08.2015.
 */
import angular from "angular";
import ngAnimate from "angular-animate";

import "ng-redux";
import labelledHrModule from "./labelled-hr.js";
import imageDropzoneModule from "./image-dropzone.js";
import matchingContextModule from "./details/picker/matching-context-picker.js"; // TODO: should be renamed
import createIsseeksModule from "./create-isseeks.js";
import { get, getIn, attach, delay } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import { connect2Redux } from "../won-utils.js";
import { selectIsConnected } from "../selectors.js";

// import { details } from "detailDefinitions";
import { useCases } from "useCaseDefinitions";
// TODO: these should be replaced by importing defintions from config
import descriptionPickerModule from "./details/picker/description-picker.js";
import locationPickerModule from "./details/picker/location-picker.js";
import personPickerModule from "./details/picker/person-picker.js";
import travelActionPickerModule from "./details/picker/travel-action-picker.js";
import tagsPickerModule from "./details/picker/tags-picker.js";
import titlePickerModule from "./details/picker/title-picker.js";
import ttlPickerModule from "./details/picker/ttl-picker.js";
import numberPickerModule from "./details/picker/number-picker.js";
import datePickerModule from "./details/picker/date-picker.js";
import datetimePickerModule from "./details/picker/datetime-picker.js";
import timePickerModule from "./details/picker/time-picker.js";
import monthPickerModule from "./details/picker/month-picker.js";
import dropdownPickerModule from "./details/picker/dropdown-picker.js";
import selectPickerModule from "./details/picker/select-picker.js";
import rangePickerModule from "./details/picker/range-picker.js";

const serviceDependencies = [
  "$ngRedux",
  "$scope",
  "$element" /*'$routeParams' /*injections as strings here*/,
];

function genComponentConf() {
  const template = `
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
                title="{{self.useCase['label']}}"
                ng-if="self.useCase['icon']"
                style="--local-primary:var(--won-primary-color);">
                    <use xlink:href="{{self.useCase['icon']}}" href="{{self.useCase['icon']}}"></use>
            </svg>
            <span class="cp__header__title">{{self.useCase.label}}</span>
        </div>
        <div class="cp__content">

            <!-- ADD TITLE AND DETAILS -->
            <div class="cp__content__branchheader"
              ng-if="self.useCase.isDetails">
              Your offer or self description
            </div>
            <won-create-isseeks 
                ng-if="self.useCase.isDetails" 
                is-or-seeks="::'Description'"
                detail-list="self.useCase.isDetails"
                initial-draft="self.useCase.draft.is"
                on-update="::self.updateDraft(draft, 'is')" 
                on-scroll="::self.scrollIntoView(element)">
            </won-create-isseeks>
            <div class="cp__content__branchheader"
              ng-if="self.useCase.seeksDetails">
              Looking For
            </div>
            <won-create-isseeks 
                ng-if="self.useCase.seeksDetails" 
                is-or-seeks="::'Search'" 
                detail-list="self.useCase.seeksDetails"
                initial-draft="self.useCase.draft.seeks"
                on-update="::self.updateDraft(draft, 'seeks')" 
                on-scroll="::self.scrollIntoView(element)">
            </won-create-isseeks>

            <!-- TUNE MATCHING -->
            <div class="cp__content__branchheader b detailPicker clickable"
                ng-if="self.useCase.isDetails || self.useCase.seeksDetails"
                ng-click="self.toggleTuningOptions()">
                <span>Tune Matching Behaviour</span>
                <svg class="cp__content__branchheader__carret" ng-show="!self.showTuningOptions">
                    <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
                </svg>
                <svg class="cp__content__branchheader__carret" ng-show="self.showTuningOptions">
                    <use xlink:href="#ico16_arrow_up" href="#ico16_arrow_up"></use>
                </svg>
            </div>
            <div class="cp__content__tuning"
            ng-if="self.useCase.isDetails || self.useCase.seeksDetails">
                <div class="cp__content__tuning_matching-context">
                    <won-matching-context-picker
                      ng-if="self.showTuningOptions"
                      default-matching-context="::self.defaultMatchingContext"
                      initial-matching-context="::self.draftObject.matchingContext"
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
                  <span ng-show="!self.pendingPublishing">
                      Publish
                  </span>
                  <span ng-show="self.pendingPublishing">
                      Publishing&nbsp;&hellip;
                  </span>
              </button>
            </div>
        </div>
        <!-- PUBLISH BUTTON - NON-RESPONSIVE MODE -->
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

  class Controller {
    constructor(/* arguments <- serviceDependencies */) {
      attach(this, serviceDependencies, arguments);
      this.focusedElement = null;
      window.cnc4dbg = this;

      this.windowHeight = window.screen.height;
      this.scrollContainer().addEventListener("scroll", e => this.onResize(e));

      this.draftObject = {};

      this.showTuningOptions = false;

      this.pendingPublishing = false;
      this.details = { is: [], seeks: [] };
      this.isNew = true;

      const selectFromState = state => {
        const useCaseString = getIn(state, [
          "router",
          "currentParams",
          "useCase",
        ]);

        // needed to be able to reset matching context to default
        // TODO: is there an easier way to do this?
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
          useCaseString,
          useCase: selectUseCaseFrom(useCaseString, useCases),
          defaultMatchingContext: defaultMatchingContext,
        };
      };

      // TODO: think about how to deal with contexts predefined in usecases
      delay(0).then(() => {
        this.loadInitialDraft();
      });

      // Using actionCreators like this means that every action defined there is available in the template.
      connect2Redux(selectFromState, actionCreators, [], this);
    }

    onResize() {
      //TODO: delete
      //console.log("ResizeEvent: ", window.screen.height);
      if (this.focusedElement) {
        if (this.windowHeight < window.screen.height) {
          this.windowHeight < window.screen.height;
          this.scrollIntoView(document.querySelector(this.focusedElement));
        } else {
          this.windowHeight = window.screen.height;
        }
      }
    }

    scrollIntoView(element) {
      this._programmaticallyScrolling = true;

      if (element) {
        element.scrollIntoView({ behavior: "smooth", block: "nearest" });
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
      const isBranch = get(draft, "is");
      const seeksBranch = get(draft, "seeks");

      if (isBranch || seeksBranch) {
        const mandatoryIsDetailsSet = this.mandatoryDetailsSet(
          isBranch,
          this.useCase.isDetails
        );
        const mandatorySeeksDetailsSet = this.mandatoryDetailsSet(
          seeksBranch,
          this.useCase.seeksDetails
        );
        if (mandatoryIsDetailsSet && mandatorySeeksDetailsSet) {
          const hasIsContent = this.isBranchContentPresent(isBranch);
          const hasSeeksContent = this.isBranchContentPresent(seeksBranch);

          return (
            !this.connectionHasBeenLost && (hasIsContent || hasSeeksContent)
          );
        }
      }
      return false;
    }

    loadInitialDraft() {
      // just to be sure this is loaded already
      if (!this.useCase) {
        selectUseCaseFrom(this.useCaseString, useCases);
      }

      if (this.useCase && this.useCase.draft) {
        // deep clone of draft
        this.draftObject = JSON.parse(JSON.stringify(this.useCase.draft));
      }

      // combine preset matching context with default matching context
      if (this.defaultMatchingContext && this.draftObject.matchingContext) {
        const combinedContext = [
          ...this.defaultMatchingContext,
          ...this.draftObject.matchingContext,
        ].reduce(function(a, b) {
          if (a.indexOf(b) < 0) a.push(b);
          return a;
        }, []);

        this.draftObject.matchingContext = combinedContext;
      } else if (this.defaultMatchingContext) {
        this.draftObject.matchingContext = this.defaultMatchingContext;
      }
    }

    updateMatchingContext(matchingContext) {
      // also accepts []!
      // if (matchingContext && this.draftObject.matchingContext) {
      //   const combinedContext = [
      //     ...matchingContext,
      //     ...this.draftObject.matchingContext,
      //   ].reduce(function(a, b) {
      //     if (a.indexOf(b) < 0) a.push(b);
      //     return a;
      //   }, []);

      //   this.draftObject.matchingContext = combinedContext;
      // } else
      if (matchingContext) {
        this.draftObject.matchingContext = matchingContext;
      }
    }

    updateDraft(updatedDraft, isSeeks) {
      if (this.isNew) {
        this.isNew = false;
      }

      this.draftObject[isSeeks] = updatedDraft;
    }

    publish() {
      if (!this.pendingPublishing) {
        this.pendingPublishing = true;

        if (this.useCase && this.useCase.identifier) {
          this.draftObject.useCase = this.useCase.identifier;
        }

        const draft = this.getPublishObject(this.draftObject);

        this.needs__create(
          draft,
          this.$ngRedux.getState().getIn(["config", "defaultNodeUri"])
        );
      }
    }

    getPublishObject(draft) {
      if (!this.isBranchContentPresent(draft.is)) {
        delete draft.is;
      }
      if (!this.isBranchContentPresent(draft.seeks)) {
        delete draft.seeks;
      }
      return draft;
    }

    // returns true if the branch has any content present
    isBranchContentPresent(isOrSeeks) {
      if (isOrSeeks) {
        const details = Object.keys(isOrSeeks);
        for (let d of details) {
          if (isOrSeeks[d] && d !== "type") {
            return true;
          }
        }
      }
      return false;
    }

    // returns true if the part in isOrSeeks, has all the mandatory details of the useCaseBranchDetails
    mandatoryDetailsSet(isOrSeeks, useCaseBranchDetails) {
      if (!useCaseBranchDetails) {
        return true;
      }

      for (const key in useCaseBranchDetails) {
        if (useCaseBranchDetails[key].mandatory) {
          const detailSaved = isOrSeeks && isOrSeeks[key];
          if (!detailSaved) {
            return false;
          }
        }
      }
      return true;
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

function selectUseCaseFrom(useCaseString, useCases) {
  if (useCaseString) {
    for (const useCaseName in useCases) {
      if (useCaseString === useCases[useCaseName]["identifier"]) {
        return useCases[useCaseName];
      }
    }
  }
  return undefined;
}

export default //.controller('CreateNeedController', [...serviceDependencies, CreateNeedController])
angular
  .module("won.owner.components.createPost", [
    labelledHrModule,
    imageDropzoneModule,
    descriptionPickerModule,
    locationPickerModule,
    personPickerModule,
    travelActionPickerModule,
    tagsPickerModule,
    titlePickerModule,
    numberPickerModule,
    datePickerModule,
    timePickerModule,
    datetimePickerModule,
    monthPickerModule,
    ttlPickerModule,
    dropdownPickerModule,
    createIsseeksModule,
    matchingContextModule,
    selectPickerModule,
    rangePickerModule,
    ngAnimate,
  ])
  .directive("wonCreatePost", genComponentConf).name;
