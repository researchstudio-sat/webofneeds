/**
 * Created by ksinger on 10.05.2016.
 */

import angular from "angular";
import postHeaderModule from "./post-header.js";
import postContextDropdownModule from "./post-context-dropdown.js";
import postContentModule from "./post-content.js";
import { attach } from "../utils.js";
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
               ng-click="self.router__stateGoCurrent({postUri : null})">
                <svg style="--local-primary:var(--won-primary-color);"
                     class="post-info__header__back__icon clickable">
                    <use xlink:href="#ico36_backarrow" href="#ico36_backarrow"></use>
                </svg>
            </a>
            <won-post-header
                need-uri="self.post.get('uri')"
                timestamp="self.createdTimestamp"
                hide-image="::false">
            </won-post-header>
            <won-post-context-dropdown ng-if="self.post.get('isOwned')"></won-post-context-dropdown>
        </div>
        <won-post-content post-uri="self.postUri"></won-post-content>
        <div class="post-info__footer" ng-if="!self.isLoading()">
            <button class="won-button--filled red post-info__footer__button"
                ng-if="self.showCreateWhatsAround()"
                ng-click="self.createWhatsAround()"
                ng-disabled="self.pendingPublishing">
                <svg class="won-button-icon" style="--local-primary:white;">
                    <use xlink:href="#ico36_location_current" href="#ico36_location_current"></use>
                </svg>
                <span ng-if="!self.pendingPublishing">What's in your Area?</span>
                <span ng-if="self.pendingPublishing">Finding out what's going on&hellip;</span>
            </button>
        </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);

      this.pendingPublishing = false;

      window.pi4dbg = this;

      const selectFromState = state => {
        const postUri = getPostUriFromRoute(state);
        const post = state.getIn(["needs", postUri]);

        return {
          postUri,
          post,
          createdTimestamp: post && post.get("creationDate"),
        };
      };
      connect2Redux(
        selectFromState,
        actionCreators,
        ["self.includeHeader"],
        this
      );

      classOnComponentRoot("won-is-loading", () => this.isLoading(), this);
    }

    showCreateWhatsAround() {
      return this.post && this.post.get("isOwned") && isWhatsNewNeed(this.post);
    }

    isLoading() {
      return !this.post || this.post.get("isLoading");
    }

    createWhatsAround() {
      if (!this.pendingPublishing) {
        this.pendingPublishing = true;
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
    },
  };
}

export default angular
  .module("won.owner.components.postInfo", [
    postHeaderModule,
    postContextDropdownModule,
    postContentModule,
  ])
  .directive("wonPostInfo", genComponentConf).name;
