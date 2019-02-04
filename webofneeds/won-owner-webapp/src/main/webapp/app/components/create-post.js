/**
 * Created by ksinger on 24.08.2015.
 */
import angular from "angular";
import ngAnimate from "angular-animate";

import "ng-redux";
import labelledHrModule from "./labelled-hr.js";
import createIsseeksModule from "./create-isseeks.js";
import publishButtonModule from "./publish-button.js";
import { get, attach, delay, getIn } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import { connect2Redux } from "../won-utils.js";
import * as generalSelectors from "../selectors/general-selectors.js";
import * as needUtils from "../need-utils.js";
import * as processSelectors from "../selectors/process-selectors.js";

import { useCases } from "useCaseDefinitions";

import "style/_create-post.scss";
import "style/_responsiveness-utils.scss";
import { values } from "min-dash";

const serviceDependencies = [
  "$ngRedux",
  "$scope",
  "$element" /*'$routeParams' /*injections as strings here*/,
];

function genComponentConf() {
  const template = `
        <div class="cp__header">
            <a class="cp__header__back clickable"
                ng-click="self.router__back()">
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
            <span class="cp__header__title" ng-if="!self.isCreateFromNeed">{{self.useCase.label}}</span>
            <span class="cp__header__title" ng-if="self.isCreateFromNeed">Duplicate from '{{self.useCase.label}}'</span>
        </div>
        <div class="cp__content">
            <div class="cp__content__loading" ng-if="!self.showCreateInput && self.isFromNeedLoading">
                <svg class="cp__content__loading__spinner hspinner">
                    <use xlink:href="#ico_loading_anim" href="#ico_loading_anim"></use>
                </svg>
                <span class="cp__content__loading__label">
                    Loading...
                </span>
            </div>
            <div class="cp__content__failed" ng-if="!self.showCreateInput && self.hasFromNeedFailedToLoad">
                <svg class="cp__content__failed__icon">
                    <use xlink:href="#ico16_indicator_error" href="#ico16_indicator_error"></use>
                </svg>
                <span class="cp__content__failed__label">
                    Failed To Load - Need might have been deleted
                </span>
                <div class="cp__content__failed__actions">
                    <button class="cp__content__failed__actions__button red won-button--outlined thin"
                        ng-click="self.needs__fetchUnloadedNeed(self.fromNeedUri)()">
                        Try Reload
                    </button>
                </div>
            </div>
            <!-- ADD TITLE AND DETAILS -->
            <div class="cp__content__branchheader"
              ng-if="self.showCreateInput && self.useCase.details">
              Your offer or self description
            </div>
            <won-create-isseeks 
                ng-if="self.showCreateInput && self.useCase.details"
                is-or-seeks="::'Description'"
                detail-list="self.useCase.details"
                initial-draft="self.useCase.draft.content"
                on-update="::self.updateDraft(draft, 'content')"
                on-scroll="::self.scrollIntoView(element)">
            </won-create-isseeks>
            <div class="cp__content__branchheader"
              ng-if="self.showCreateInput && self.useCase.seeksDetails">
              Looking For
            </div>
            <won-create-isseeks 
                ng-if="self.showCreateInput && self.useCase.seeksDetails"
                is-or-seeks="::'Search'" 
                detail-list="self.useCase.seeksDetails"
                initial-draft="self.useCase.draft.seeks"
                on-update="::self.updateDraft(draft, 'seeks')" 
                on-scroll="::self.scrollIntoView(element)">
            </won-create-isseeks>
        </div>
        <div class="cp__footer" >
            <won-labelled-hr label="::'done?'" class="cp__footer__labelledhr"></won-labelled-hr>
            <won-publish-button on-publish="self.publish(persona)" is-valid="self.isValid()" show-personas="self.isHoldable" ng-if="self.showCreateInput"></won-publish-button>
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

      this.details = { is: [], seeks: [] };
      this.isNew = true;

      const selectFromState = state => {
        const fromNeedUri = generalSelectors.getFromNeedUriFromRoute(state);
        const mode = generalSelectors.getModeFromRoute(state);
        let fromNeed;

        let useCaseString;
        let useCase;

        const isCreateFromNeed = !!(fromNeedUri && mode === "DUPLICATE");

        let isFromNeedLoading = false;
        let isFromNeedToLoad = false;
        let hasFromNeedFailedToLoad = false;

        if (isCreateFromNeed) {
          (isFromNeedLoading = processSelectors.isNeedLoading(
            state,
            fromNeedUri
          )),
            (isFromNeedToLoad = processSelectors.isNeedToLoad(
              state,
              fromNeedUri
            )),
            (hasFromNeedFailedToLoad = processSelectors.hasNeedFailedToLoad(
              state,
              fromNeedUri
            )),
            (fromNeed =
              !isFromNeedLoading &&
              !isFromNeedToLoad &&
              !hasFromNeedFailedToLoad &&
              getIn(state, ["needs", fromNeedUri]));

          if (fromNeed) {
            //TODO: DETERMINE THE CORRECT USECASE
            useCaseString = "customUseCase";
            useCase = selectUseCaseFrom(useCaseString, useCases);

            const fromNeedContent = get(fromNeed, "content");
            const fromNeedSeeks = get(fromNeed, "seeks");
            const facetsReset = needUtils.getFacetsWithKeysReset(fromNeed);
            const defaultFacetReset = needUtils.getDefaultFacetWithKeyReset(
              fromNeed
            );
            const seeksFacetsReset = needUtils.getSeeksFacetsWithKeysReset(
              fromNeed
            );
            const seeksDefaultFacetReset = needUtils.getSeeksDefaultFacetWithKeyReset(
              fromNeed
            );

            if (fromNeedContent) {
              useCase.draft.content = fromNeedContent.toJS();
            }
            if (fromNeedSeeks) {
              useCase.draft.seeks = fromNeedSeeks.toJS();
            }

            if (facetsReset) {
              useCase.draft.content.facets = facetsReset.toJS();
            }
            if (defaultFacetReset) {
              useCase.draft.content.defaultFacet = defaultFacetReset.toJS();
            }

            if (seeksFacetsReset) {
              useCase.draft.seeks.facets = seeksFacetsReset.toJS();
            }
            if (seeksDefaultFacetReset) {
              useCase.draft.seeks.defaultFacet = seeksDefaultFacetReset.toJS();
            }
          }
        } else {
          useCaseString = generalSelectors.getUseCaseFromRoute(state);
          useCase = selectUseCaseFrom(useCaseString, useCases);
        }

        return {
          processingPublish: state.getIn(["process", "processingPublish"]),
          connectionHasBeenLost: !generalSelectors.selectIsConnected(state),
          useCaseString,
          useCase,
          fromNeed,
          fromNeedUri,
          isCreateFromNeed,
          isFromNeedLoading,
          isFromNeedToLoad,
          isHoldable:
            useCase &&
            useCase.draft &&
            useCase.draft.content &&
            useCase.draft.content.facets &&
            values(useCase.draft.content.facets).includes("won:HoldableFacet"),
          hasFromNeedFailedToLoad,
          showCreateInput:
            useCase &&
            !(
              isCreateFromNeed &&
              (isFromNeedLoading || hasFromNeedFailedToLoad || isFromNeedToLoad)
            ),
        };
      };
      // Using actionCreators like this means that every action defined there is available in the template.
      connect2Redux(selectFromState, actionCreators, [], this);

      this.$scope.$watch(
        () => this.isFromNeedToLoad,
        () => delay(0).then(() => this.ensureFromNeedIsLoaded())
      );

      this.$scope.$watch(
        () => this.showCreateInput,
        () => delay(0).then(() => this.loadInitialDraft())
      );
    }

    ensureFromNeedIsLoaded() {
      if (this.isFromNeedToLoad) {
        this.needs__fetchUnloadedNeed(this.fromNeedUri);
      }
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

    toggleTuningOptions() {}

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
      if (this.showCreateInput && this.useCase.draft) {
        // deep clone of draft
        this.draftObject = JSON.parse(JSON.stringify(this.useCase.draft));
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
    ngAnimate,
    publishButtonModule,
  ])
  .directive("wonCreatePost", genComponentConf).name;
