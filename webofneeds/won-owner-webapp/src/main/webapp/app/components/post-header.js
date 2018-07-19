/**
 * Component for rendering need-title, type and timestamp
 * Created by ksinger on 10.04.2017.
 */
import angular from "angular";
import "ng-redux";
import squareImageModule from "./square-image.js";
import { actionCreators } from "../actions/actions.js";
import { labels, relativeTime } from "../won-label-utils.js";
import { attach } from "../utils.js";
import { connect2Redux } from "../won-utils.js";
import { selectLastUpdateTime } from "../selectors.js";
import won from "../won-es6.js";
import { classOnComponentRoot } from "../cstm-ng-utils.js";
import { getHumanReadableStringFromNeed } from "../reducers/need-reducer/parse-need.js";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `

    <won-square-image
        ng-if="!self.isLoading()"
        ng-class="{'bigger' : self.biggerImage, 'inactive' : !self.need.get('isBeingCreated') && self.need.get('state') === self.WON.InactiveCompacted}"
        src="self.need.get('TODO')"
        uri="self.needUri"
        ng-show="!self.hideImage">
    </won-square-image>
    <div class="ph__right" ng-if="!self.need.get('isBeingCreated') && !self.isLoading()">
      <div class="ph__right__topline">
        <div class="ph__right__topline__title" ng-if="self.needHumanReadableString">
         {{ self.needHumanReadableString }}
        </div>
        <div class="ph__right__topline__notitle" ng-if="!self.needHumanReadableString">
         no title
        </div>
      </div>
      <div class="ph__right__subtitle">
        <span class="ph__right__subtitle__type">
          {{self.labels.type[self.need.get('type')]}}{{self.need.get('matchingContexts')? ' in '+ self.need.get('matchingContexts').join(', ') : '' }}
        </span>
        <div class="ph__right__subtitle__date">
          {{ self.friendlyTimestamp }}
        </div>
      </div>
    </div>
    
    <div class="ph__right" ng-if="self.need.get('isBeingCreated')">
      <div class="ph__right__topline">
        <div class="ph__right__topline__notitle>
          Creating...
        </div>
      </div>
      <div class="ph__right__subtitle">
        <span class="ph__right__subtitle__type">
          {{self.labels.type[self.need.get('type')]}}
        </span>
      </div>
    </div>
    <div class="ph__icon__skeleton" ng-if="self.isLoading()"></div>
    <div class="ph__right" ng-if="self.isLoading()">
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
      this.labels = labels;
      this.WON = won.WON;
      const selectFromState = state => {
        const need = state.getIn(["needs", this.needUri]);
        const needHumanReadableString =
          need && getHumanReadableStringFromNeed(need);

        return {
          need,
          needHumanReadableString,
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

      classOnComponentRoot("won-is-loading", () => this.isLoading(), this);
      classOnComponentRoot("won-is-toload", () => this.isToLoad(), this);
    }

    isLoading() {
      return !this.need || this.need.get("isLoading");
    }
    isToLoad() {
      return !this.need || this.need.get("toLoad");
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

      /**
       * If true, the title image will be a bit bigger. This
       * can be used to create visual contrast.
       */
      biggerImage: "=",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.postHeader", [squareImageModule])
  .directive("wonPostHeader", genComponentConf).name;
