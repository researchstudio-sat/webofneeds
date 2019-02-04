/**
 * Created by ksinger on 10.05.2016.
 */

import angular from "angular";
import postHeaderModule from "./post-header.js";
import postContextDropdownModule from "./post-context-dropdown.js";
import postContentModule from "./post-content.js";
import shareDropdownModule from "./share-dropdown.js";
import { attach, getIn } from "../utils.js";
import { connect2Redux } from "../won-utils.js";
import { isWhatsNewNeed } from "../need-utils.js";
import { getPostUriFromRoute } from "../selectors/general-selectors.js";
import { actionCreators } from "../actions/actions.js";
import { classOnComponentRoot } from "../cstm-ng-utils.js";

import "style/_post-info.scss";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
        <div class="post-info__header" ng-if="self.includeHeader">
            <a class="post-info__header__back clickable"
                ng-if="!self.showOverlayNeed"
                ng-click="self.router__stateGoCurrent({postUri : null})">
                <svg style="--local-primary:var(--won-primary-color);"
                     class="post-info__header__back__icon clickable">
                    <use xlink:href="#ico36_backarrow" href="#ico36_backarrow"></use>
                </svg>
            </a>
            <a class="post-info__header__back clickable"
                ng-if="self.showOverlayNeed"
                ng-click="self.router__back()">
                <svg style="--local-primary:var(--won-primary-color);"
                     class="post-info__header__back__icon clickable">
                    <use xlink:href="#ico36_close" href="#ico36_close"></use>
                </svg>
            </a>
            <won-post-header
                need-uri="self.post.get('uri')">
            </won-post-header>
            <won-share-dropdown need-uri="self.post.get('uri')"></won-share-dropdown>
            <won-post-context-dropdown need-uri="self.post.get('uri')"></won-post-context-dropdown>
        </div>
        <won-post-content post-uri="self.postUri"></won-post-content>
        <div class="post-info__footer" ng-if="!self.postLoading && !self.postFailedToLoad && self.showCreateWhatsAround()">
            <button class="won-button--filled red post-info__footer__button"
                ng-if="self.showCreateWhatsAround()"
                ng-click="self.createWhatsAround()"
                ng-disabled="self.processingPublish">
                <svg class="won-button-icon" style="--local-primary:white;">
                    <use xlink:href="#ico36_location_current" href="#ico36_location_current"></use>
                </svg>
                <span ng-if="!self.processingPublish">What's in your Area?</span>
                <span ng-if="self.processingPublish">Finding out what's going on&hellip;</span>
            </button>
        </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      window.pi4dbg = this;

      const selectFromState = state => {
        /*
          If the post-info component has a need-uri attribute we display this need-uri instead of the postUriFromTheRoute
          This way we can include a overlay-close button instead of the current back-button handling
        */
        const postUri = this.needUri
          ? this.needUri
          : getPostUriFromRoute(state);
        const post = state.getIn(["needs", postUri]);

        return {
          processingPublish: state.getIn(["process", "processingPublish"]),
          postUri,
          post,
          postLoading:
            !post ||
            getIn(state, ["process", "needs", post.get("uri"), "loading"]),
          postFailedToLoad:
            post &&
            getIn(state, ["process", "needs", post.get("uri"), "failedToLoad"]),
          createdTimestamp: post && post.get("creationDate"),
          showOverlayNeed: !!this.needUri,
        };
      };
      connect2Redux(
        selectFromState,
        actionCreators,
        ["self.includeHeader", "self.needUri"],
        this
      );

      classOnComponentRoot("won-is-loading", () => this.postLoading, this);
    }

    showCreateWhatsAround() {
      return this.post && this.post.get("isOwned") && isWhatsNewNeed(this.post);
    }

    createWhatsAround() {
      if (!this.processingPublish) {
        this.needs__whatsAround();
      }
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
      needUri: "=",
    },
  };
}

export default angular
  .module("won.owner.components.postInfo", [
    postHeaderModule,
    postContextDropdownModule,
    postContentModule,
    shareDropdownModule,
  ])
  .directive("wonPostInfo", genComponentConf).name;
