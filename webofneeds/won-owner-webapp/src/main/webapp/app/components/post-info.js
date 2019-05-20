/**
 * Created by ksinger on 10.05.2016.
 */

import angular from "angular";
import postHeaderModule from "./post-header.js";
import postContextDropdownModule from "./post-context-dropdown.js";
import postContentModule from "./post-content.js";
import postMenuModule from "./post-menu.js";
import shareDropdownModule from "./share-dropdown.js";
import { attach, get, getIn } from "../utils.js";
import { connect2Redux } from "../won-utils.js";
import * as processUtils from "../process-utils.js";
import * as generalSelectors from "../selectors/general-selectors.js";
import { actionCreators } from "../actions/actions.js";
import { classOnComponentRoot } from "../cstm-ng-utils.js";

import { getUseCaseLabel, getUseCaseIcon } from "../usecase-utils.js";

import "~/style/_post-info.scss";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
        <div class="post-info__header" ng-if="self.includeHeader">
            <div class="post-info__header__back">
              <a class="post-info__header__back__button clickable show-in-responsive"
                 ng-if="!self.showOverlayAtom"
                 ng-click="self.router__back()"> <!-- TODO: Clicking on the back button in non-mobile view might lead to some confusing changes -->
                  <svg class="post-info__header__back__button__icon">
                      <use xlink:href="#ico36_backarrow" href="#ico36_backarrow"></use>
                  </svg>
              </a>
              <a class="post-info__header__back__button clickable hide-in-responsive"
                  ng-if="!self.showOverlayAtom"
                  ng-click=" self.router__stateGoCurrent({postUri : undefined})">
                  <svg class="post-info__header__back__button__icon">
                      <use xlink:href="#ico36_backarrow" href="#ico36_backarrow"></use>
                  </svg>
              </a>
              <a class="post-info__header__back__button clickable"
                  ng-if="self.showOverlayAtom"
                  ng-click="self.router__back()">
                  <svg class="post-info__header__back__button__icon clickable hide-in-responsive">
                      <use xlink:href="#ico36_close" href="#ico36_close"></use>
                  </svg>
                  <svg class="post-info__header__back__button__icon clickable show-in-responsive">
                      <use xlink:href="#ico36_backarrow" href="#ico36_backarrow"></use>
                  </svg>
              </a>
            </div>
            <won-post-header
                atom-uri="self.post.get('uri')">
            </won-post-header>
            <won-share-dropdown atom-uri="self.post.get('uri')"></won-share-dropdown>
            <won-post-context-dropdown atom-uri="self.post.get('uri')"></won-post-context-dropdown>
        </div>
        <won-post-menu post-uri="self.postUri"></won-post-menu>
        <won-post-content post-uri="self.postUri"></won-post-content>
        <div class="post-info__footer" ng-if="self.showFooter">
            <button class="won-button--filled red post-info__footer__button"
                ng-if="self.hasReactionUseCases"
                ng-repeat="ucIdentifier in self.reactionUseCasesArray"
                ng-click="self.router__stateGoCurrent({useCase: ucIdentifier, useCaseGroup: undefined, postUri: undefined, fromAtomUri: self.postUri, mode: 'CONNECT'})">
                <svg class="won-button-icon" style="--local-primary:white;" ng-if="self.getUseCaseIcon(ucIdentifier)">
                    <use xlink:href="{{ self.getUseCaseIcon(ucIdentifier) }}" href="{{ self.getUseCaseIcon(ucIdentifier) }}"></use>
                </svg>
                <span>{{ self.getUseCaseLabel(ucIdentifier) }}</span>
            </button>
            <won-labelled-hr label="::'Or'" class="pm__footer__labelledhr"  ng-if="self.hasEnabledUseCases"></won-labelled-hr>
            <button class="won-button--filled red post-info__footer__button" style="margin: 0rem 0rem .3rem 0rem;"
                    ng-if="self.hasEnabledUseCases"
                    ng-repeat="ucIdentifier in self.enabledUseCasesArray"
                    ng-click="self.router__stateGoCurrent({useCase: ucIdentifier, useCaseGroup: undefined, postUri: undefined, fromAtomUri: self.postUri, mode: 'CONNECT'})">
                    <svg class="won-button-icon" style="--local-primary:white;" ng-if="self.getUseCaseIcon(ucIdentifier)">
                        <use xlink:href="{{ self.getUseCaseIcon(ucIdentifier) }}" href="{{ self.getUseCaseIcon(ucIdentifier) }}"></use>
                    </svg>
                    <span>{{ self.getUseCaseLabel(ucIdentifier) }}</span>
            </button>
        </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      window.pi4dbg = this;

      const selectFromState = state => {
        /*
          If the post-info component has an atom-uri attribute we display this atom-uri instead of the postUriFromTheRoute
          This way we can include a overlay-close button instead of the current back-button handling
        */
        const postUri = this.atomUri
          ? this.atomUri
          : generalSelectors.getPostUriFromRoute(state);
        const post = state.getIn(["atoms", postUri]);
        const process = get(state, "process");

        const postLoading =
          !post || processUtils.isAtomLoading(process, postUri);
        const postFailedToLoad =
          post && processUtils.hasAtomFailedToLoad(process, postUri);
        const isOwned = generalSelectors.isAtomOwned(state, postUri);

        const reactionUseCases =
          post &&
          !isOwned &&
          getIn(post, ["matchedUseCase", "reactionUseCases"]);
        const hasReactionUseCases =
          reactionUseCases && reactionUseCases.size > 0;

        const enabledUseCases =
          post && isOwned && getIn(post, ["matchedUseCase", "enabledUseCases"]);
        const hasEnabledUseCases = enabledUseCases && enabledUseCases.size > 0;
        return {
          postUri,
          post,
          postLoading,
          postFailedToLoad,
          hasReactionUseCases,
          reactionUseCasesArray: reactionUseCases && reactionUseCases.toArray(),
          hasEnabledUseCases,
          enabledUseCasesArray: enabledUseCases && enabledUseCases.toArray(),
          createdTimestamp: post && post.get("creationDate"),
          showOverlayAtom: !!this.atomUri,
          showFooter:
            !postLoading &&
            !postFailedToLoad &&
            (hasReactionUseCases || hasEnabledUseCases),
        };
      };
      connect2Redux(
        selectFromState,
        actionCreators,
        ["self.includeHeader", "self.atomUri"],
        this
      );

      classOnComponentRoot("won-is-loading", () => this.postLoading, this);
    }

    getUseCaseIcon(ucIdentifier) {
      return getUseCaseIcon(ucIdentifier);
    }

    getUseCaseLabel(ucIdentifier) {
      return getUseCaseLabel(ucIdentifier);
    }
  }

  Controller.$inject = serviceDependencies;
  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    template: template,
    scope: {
      includeHeader: "=",
      atomUri: "=",
    },
  };
}

export default angular
  .module("won.owner.components.postInfo", [
    postHeaderModule,
    postMenuModule,
    postContextDropdownModule,
    postContentModule,
    shareDropdownModule,
  ])
  .directive("wonPostInfo", genComponentConf).name;
