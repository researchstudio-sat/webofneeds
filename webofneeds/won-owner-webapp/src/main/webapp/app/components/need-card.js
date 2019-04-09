/**
 * Component for rendering need-title
 * Created by ksinger on 10.04.2017.
 */
import angular from "angular";
import inviewModule from "angular-inview";
import "ng-redux";
import squareImageModule from "./square-image.js";
import { actionCreators } from "../actions/actions.js";
import { relativeTime } from "../won-label-utils.js";
import { attach, getIn, get } from "../utils.js";
import { connect2Redux } from "../won-utils.js";
import { selectLastUpdateTime } from "../selectors/general-selectors.js";
import { classOnComponentRoot } from "../cstm-ng-utils.js";
import * as needUtils from "../need-utils.js";
import * as processUtils from "../process-utils.js";

import "style/_need-card.scss";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
    <won-square-image
        ng-if="self.needLoaded"
        uri="::self.needUri">
    </won-square-image>
    <div class="ph__icon__skeleton"
      ng-if="self.needToLoad"
      in-view="$inview && self.ensureNeedIsLoaded()">
    </div>
    <div class="ph__icon__skeleton" ng-if="self.needLoading || self.needFailedToLoad"></div>
    <div class="ph__right" ng-if="self.needLoaded">
        <div class="ph__right__topline">
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
        <div class="ph__right__subtitle">
            <span class="ph__right__subtitle__type">
                <span class="ph__right__subtitle__type__persona"
                    ng-if="self.personaName">
                    {{self.personaName}}
                </span>
                <span class="ph__right__subtitle__type__groupchat"
                    ng-if="self.isGroupChatEnabled && !self.isChatEnabled">
                    Group Chat
                </span>
                <span class="ph__right__subtitle__type__groupchat"
                    ng-if="self.isGroupChatEnabled && self.isChatEnabled">
                    Group Chat enabled
                </span>
                <span>
                    {{ self.needTypeLabel }}
                </span>
            </span>
            <div class="ph__right__subtitle__date">
                {{ self.friendlyTimestamp }}
            </div>
        </div>
    </div>
    <div class="ph__right" ng-if="self.needFailedToLoad">
        <div class="ph__right__topline">
            <div class="ph__right__topline__notitle">
                Need Loading failed
            </div>
        </div>
        <div class="ph__right__subtitle">
            <span class="ph__right__subtitle__type">
                Need might have been deleted.
            </span>
        </div>
    </div>
    <div class="ph__right" ng-if="self.needLoading || self.needToLoad">
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

      const selectFromState = state => {
        const need = getIn(state, ["needs", this.needUri]);
        const isDirectResponse = needUtils.isDirectResponseNeed(need);
        const responseToUri =
          isDirectResponse && getIn(need, ["content", "responseToUri"]);
        const responseToNeed =
          responseToUri && getIn(state, ["needs", responseToUri]);

        const personaUri = get(need, "heldBy");
        const persona = personaUri && getIn(state, ["needs", personaUri]);
        const personaName = get(persona, "humanReadable");

        const process = get(state, "process");

        return {
          responseToNeed,
          need,
          needTypeLabel: need && needUtils.generateNeedTypeLabel(need),
          personaName,
          needLoaded: processUtils.isNeedLoaded(process, this.needUri),
          needLoading: processUtils.isNeedLoading(process, this.needUri),
          needToLoad: processUtils.isNeedToLoad(process, this.needUri),
          needFailedToLoad: processUtils.hasNeedFailedToLoad(
            process,
            this.needUri
          ),
          isDirectResponse: isDirectResponse,
          isGroupChatEnabled: needUtils.hasGroupFacet(need),
          isChatEnabled: needUtils.hasChatFacet(need),
          friendlyTimestamp:
            need &&
            relativeTime(
              selectLastUpdateTime(state),
              get(need, "lastUpdateDate")
            ),
        };
      };

      connect2Redux(selectFromState, actionCreators, ["self.needUri"], this);

      classOnComponentRoot("won-is-loading", () => this.needLoading, this);
      classOnComponentRoot("won-is-toload", () => this.needToLoad, this);
      classOnComponentRoot("won-is-invisible", () => this.hideNeed(), this);
    }

    ensureNeedIsLoaded() {
      if (
        this.needUri &&
        !this.needLoaded &&
        !this.needLoading &&
        this.needToLoad
      ) {
        this.needs__fetchUnloadedNeed(this.needUri);
      }
    }

    hideNeed() {
      return (
        this.needFailedToLoad ||
        (this.needLoaded && needUtils.isInactive(this.need))
      );
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
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.needCard", [
    squareImageModule,
    inviewModule.name,
  ])
  .directive("wonNeedCard", genComponentConf).name;
