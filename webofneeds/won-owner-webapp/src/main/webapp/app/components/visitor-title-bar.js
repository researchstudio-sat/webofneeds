/**
 * Created by ksinger on 20.08.2015.
 */
import angular from "angular";
import { attach, get, getIn } from "../utils.js";
import { connect2Redux } from "../won-utils.js";
import { getPostUriFromRoute } from "../selectors/general-selectors.js";
import {
  generateFullNeedTypesLabel,
  generateShortNeedTypesLabel,
  generateNeedMatchingContext,
} from "../need-utils.js";
import { actionCreators } from "../actions/actions.js";
import postContextDropDownModule from "../components/post-context-dropdown.js";
import * as needUtils from "../need-utils.js";

import "style/_visitor-title-bar.scss";

const serviceDependencies = ["$ngRedux", "$scope"];
function genComponentConf() {
  let template = `
        <nav class="visitor-title-bar">
            <div class="vtb__inner">
                <div class="vtb__inner__left">
                    <won-square-image
                        uri="self.post.get('uri')">
                    </won-square-image>
                    <hgroup>
                        <h1 class="vtb__title">{{ self.post.get('humanReadable') }}</h1>
                        <span class="vtb__titles__persona" ng-if="self.personaName">{{ self.personaName }}</span>
                        <span class="vtb__titles__groupchat"
                          ng-if="self.isGroupChatEnabled && !self.isChatEnabled">
                          Group Chat
                        </span>
                        <span class="vtb__titles__groupchat"
                          ng-if="self.isGroupChatEnabled && self.isChatEnabled">
                          Group Chat enabled
                        </span>
                        <div class="vtb__titles__type" ng-if="!self.shouldShowRdf">{{ self.shortTypesLabel }}{{ self.matchingContext }}</div>
                        <div class="vtb__titles__type" ng-if="self.shouldShowRdf">{{ self.fullTypesLabel }}</div>
                    </hgroup>
                </div>
            </div>
            <won-post-context-dropdown need-uri="self.post.get('uri')"></won-post-context-dropdown>
        </nav>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      window.vtb4dbg = this;

      const selectFromState = state => {
        const postUri = getPostUriFromRoute(state);
        const post = state.getIn(["needs", postUri]);

        const personaUri = get(post, "heldBy");
        const persona = personaUri && getIn(state, ["needs", personaUri]);
        const personaName = get(persona, "humanReadable");

        return {
          postUri,
          post,
          personaName,
          isGroupChatEnabled: needUtils.hasChatFacet(post),
          isChatEnabled: needUtils.hasGroupFacet(post),
          fullTypesLabel: post && generateFullNeedTypesLabel(post),
          shortTypesLabel: post && generateShortNeedTypesLabel(post),
          matchingContext: post && generateNeedMatchingContext(post),
          shouldShowRdf: state.getIn(["view", "showRdf"]),
        };
      };
      connect2Redux(selectFromState, actionCreators, [], this);
    }
    back() {
      window.history.back();
    }
  }
  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: { item: "=" },
    template: template,
  };
}

export default angular
  .module("won.owner.components.visitorTitleBar", [postContextDropDownModule])
  .directive("wonVisitorTitleBar", genComponentConf).name;
