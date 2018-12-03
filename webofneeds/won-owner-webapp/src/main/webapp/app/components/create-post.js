/**
 * Created by ksinger on 24.08.2015.
 */
import angular from "angular";
import ngAnimate from "angular-animate";

import "ng-redux";
import labelledHrModule from "./labelled-hr.js";
import matchingContextModule from "./details/picker/matching-context-picker.js"; // TODO: should be renamed
import createIsseeksModule from "./create-isseeks.js";
import publishButtonModule from "./publish-button.js";
import { get, getIn, attach, delay } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import { connect2Redux } from "../won-utils.js";
import { selectIsConnected } from "../selectors/general-selectors.js";

import { useCases } from "useCaseDefinitions";

import "style/_create-post.scss";
import "style/_responsiveness-utils.scss";

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
              ng-if="self.useCase.details">
              Your offer or self description
            </div>
            <won-create-isseeks 
                ng-if="self.useCase.details"
                is-or-seeks="::'Description'"
                detail-list="self.useCase.details"
                initial-draft="self.useCase.draft.content"
                on-update="::self.updateDraft(draft, 'content')"
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
                ng-if="self.useCase.details || self.useCase.seeksDetails"
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
            ng-if="self.useCase.details || self.useCase.seeksDetails">
                <div class="cp__content__tuning_matching-context">
                    <won-matching-context-picker
                      ng-if="self.showTuningOptions"
                      default-matching-context="::self.defaultMatchingContext"
                      initial-matching-context="::self.draftObject.matchingContext"
                      on-matching-context-updated="::self.updateMatchingContext(matchingContext)">
                    </won-matching-context-picker>
                </div>
            </div>
        </div>
        <div class="cp__footer" >
            <won-labelled-hr label="::'done?'" class="cp__footer__labelledhr"></won-labelled-hr>
            <won-publish-button on-publish="self.publish(persona)" is-valid="self.isValid()"></won-publish-button>
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
          processingPublish: state.getIn(["process", "processingPublish"]),
          connectionHasBeenLost: !selectIsConnected(state),
          useCaseString,
          useCase: selectUseCaseFrom(useCaseString, useCases),
          defaultMatchingContext: defaultMatchingContext,
        };
      };

      delay(0).then(() => {
        this.loadInitialDraft();
      });

      // Using actionCreators like this means that every action defined there is available in the template.
      connect2Redux(selectFromState, actionCreators, [], this);
    }

    onResize() {
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
      const draftContent = get(draft, "content");
      const seeksBranch = get(draft, "seeks");

      if (draftContent || seeksBranch) {
        const mandatoryContentDetailsSet = mandatoryDetailsSet(
          draftContent,
          this.useCase.details
        );
        const mandatorySeeksDetailsSet = mandatoryDetailsSet(
          seeksBranch,
          this.useCase.seeksDetails
        );
        if (mandatoryContentDetailsSet && mandatorySeeksDetailsSet) {
          const hasContent = isBranchContentPresent(draftContent);
          const hasSeeksContent = isBranchContentPresent(seeksBranch);

          return !this.connectionHasBeenLost && (hasContent || hasSeeksContent);
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

    updateDraft(updatedDraft, branch) {
      if (this.isNew) {
        this.isNew = false;
      }

      this.draftObject[branch] = updatedDraft;
    }

    publish(persona) {
      if (!this.processingPublish) {
        this.draftObject.useCase = get(this.useCase, "identifier");

        if (!isBranchContentPresent(this.draftObject.content)) {
          delete this.draftObject.content;
        }
        if (!isBranchContentPresent(this.draftObject.seeks)) {
          delete this.draftObject.seeks;
        }

        this.needs__create(
          this.draftObject,
          persona,
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
// returns true if the branch has any content present
function isBranchContentPresent(isOrSeeks) {
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
function mandatoryDetailsSet(isOrSeeks, useCaseBranchDetails) {
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

export default //.controller('CreateNeedController', [...serviceDependencies, CreateNeedController])
angular
  .module("won.owner.components.createPost", [
    labelledHrModule,
    createIsseeksModule,
    matchingContextModule,
    ngAnimate,
    publishButtonModule,
  ])
  .directive("wonCreatePost", genComponentConf).name;
