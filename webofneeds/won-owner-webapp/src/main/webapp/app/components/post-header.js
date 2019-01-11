/**
 * Component for rendering need-title, type and timestamp
 * Created by ksinger on 10.04.2017.
 */
import angular from "angular";
import "ng-redux";
import squareImageModule from "./square-image.js";
import { actionCreators } from "../actions/actions.js";
import { relativeTime } from "../won-label-utils.js";
import { attach, getIn, delay } from "../utils.js";
import { connect2Redux } from "../won-utils.js";
import { selectLastUpdateTime } from "../selectors/general-selectors.js";
import won from "../won-es6.js";
import { classOnComponentRoot } from "../cstm-ng-utils.js";
import {
  isDirectResponseNeed,
  hasGroupFacet,
  hasChatFacet,
  generateFullNeedTypesLabel,
} from "../need-utils.js";

import "style/_post-header.scss";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `

    <won-square-image
        ng-if="!self.postLoading"
        src="self.need.get('TODO')"
        uri="self.needUri"
        ng-show="!self.hideImage">
    </won-square-image>
    <div class="ph__right" ng-if="!self.need.get('isBeingCreated') && !self.postLoading">
      <div class="ph__right__topline" ng-if="!self.postFailedToLoad">
        <div class="ph__right__topline__title" ng-if="self.hasTitle()">
          {{ self.generateTitle() }}
        </div>
        <div class="ph__right__topline__notitle" ng-if="!self.hasTitle() && self.isDirectResponse">
          RE: no title
        </div>
        <div class="ph__right__topline__notitle" ng-if="!self.hasTitle() && !self.isDirectResponse">
          no title
        </div>
      </div>
      <div class="ph__right__subtitle" ng-if="!self.postFailedToLoad">
        <span class="ph__right__subtitle__type">
          <span class="ph__right__subtitle__type__groupchat"
            ng-if="self.isGroupChatEnabled && !self.isChatEnabled">
            Group Chat
          </span>
          <span class="ph__right__subtitle__type__groupchat"
            ng-if="self.isGroupChatEnabled && self.isChatEnabled">
            Group Chat enabled
          </span>
          {{ self.generateFullNeedTypesLabel(self.need) }}
        </span>
        <div class="ph__right__subtitle__date">
          {{ self.friendlyTimestamp }}
        </div>
      </div>
      <div class="ph__right__topline" ng-if="self.postFailedToLoad">
        <div class="ph__right__topline__notitle">
          Need Loading failed
        </div>
      </div>
      <div class="ph__right__subtitle" ng-if="self.postFailedToLoad">
        <span class="ph__right__subtitle__type">
          Need might have been deleted.
        </span>
      </div>
    </div>
    
    <div class="ph__right" ng-if="self.need.get('isBeingCreated')">
      <div class="ph__right__topline">
        <div class="ph__right__topline__notitle">
          Creating...
        </div>
      </div>
      <div class="ph__right__subtitle">
        <span class="ph__right__subtitle__type">
          <span class="ph__right__subtitle__type__groupchat"
            ng-if="self.isGroupChatEnabled && !self.isChatEnabled">
            Group Chat
          </span>
          <span class="ph__right__subtitle__type__groupchat"
            ng-if="self.isGroupChatEnabled && self.isChatEnabled">
            Group Chat enabled
          </span>
          {{ self.generateFullNeedTypesLabel(self.need) }}
        </span>
      </div>
    </div>
    <div class="ph__icon__skeleton" ng-if="self.postLoading"></div>
    <div class="ph__right" ng-if="self.postLoading">
      <div class="ph__right__topline">
        <div class="ph__right__topline__title"></div>
      </div>
      <div class="ph__right__subtitle">
        <span class="ph__right__subtitle__type"></span>
      </div>
    </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      window.ph4dbg = this;
      this.generateFullNeedTypesLabel = generateFullNeedTypesLabel;
      this.WON = won.WON;
      const selectFromState = state => {
        const need = getIn(state, ["needs", this.needUri]);
        const isDirectResponse = isDirectResponseNeed(need);
        const responseToUri =
          isDirectResponse && getIn(need, ["content", "responseToUri"]);
        const responseToNeed =
          responseToUri && getIn(state, ["needs", responseToUri]);

        return {
          responseToNeed,
          need,
          postLoading:
            !need ||
            getIn(state, ["process", "needs", need.get("uri"), "loading"]),
          postToLoad:
            !need ||
            getIn(state, ["process", "needs", need.get("uri"), "toLoad"]),
          postFailedToLoad:
            need &&
            getIn(state, ["process", "needs", need.get("uri"), "failedToLoad"]),
          isDirectResponse: isDirectResponse,
          isGroupChatEnabled: hasGroupFacet(need),
          isChatEnabled: hasChatFacet(need),
          friendlyTimestamp:
            need &&
            relativeTime(
              selectLastUpdateTime(state),
              need.get("lastUpdateDate")
            ),
        };
      };

      connect2Redux(
        selectFromState,
        actionCreators,
        ["self.needUri", "self.timestamp"],
        this
      );

      classOnComponentRoot("won-is-loading", () => this.postLoading, this);
      classOnComponentRoot("won-is-toload", () => this.postToLoad, this);

      this.$scope.$watch(
        () => this.needUri,
        () => delay(0).then(() => this.ensureNeedIsLoaded())
      );
    }

    ensureNeedIsLoaded() {
      if (
        this.needUri &&
        (!this.need || (this.postToLoad && !this.postLoading))
      ) {
        this.needs__fetchUnloadedNeed(this.needUri);
      }
    }

    hasTitle() {
      if (this.isDirectResponse && this.responseToNeed) {
        return !!this.responseToNeed.get("humanReadable");
      } else {
        return !!this.need.get("humanReadable");
      }
    }

    generateTitle() {
      if (this.isDirectResponse && this.responseToNeed) {
        return "Re: " + this.responseToNeed.get("humanReadable");
      } else {
        return this.need.get("humanReadable");
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
      needUri: "=",

      /**
       * Will be used instead of the posts creation date if specified.
       * Use if you e.g. instead want to show the date when a request was made.
       */
      timestamp: "=",
      /**
       * one of:
       * - "fullpage" (NOT_YET_IMPLEMENTED) (used in post-info page)
       * - "medium" (NOT_YET_IMPLEMENTED) (used in incoming/outgoing requests)
       * - "small" (NOT_YET_IMPLEMENTED) (in matches-list)
       */
      //size: '=',

      /**
       * if set, the avatar will be hidden
       */
      hideImage: "=",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.postHeader", [squareImageModule])
  .directive("wonPostHeader", genComponentConf).name;
