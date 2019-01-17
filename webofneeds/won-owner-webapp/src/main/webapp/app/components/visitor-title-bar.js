/**
 * Created by ksinger on 20.08.2015.
 */
import angular from "angular";
import { attach } from "../utils.js";
import { connect2Redux } from "../won-utils.js";
import { getPostUriFromRoute } from "../selectors/general-selectors.js";
import {
  generateFullNeedTypesLabel,
  generateShortNeedTypesLabel,
  generateNeedMatchingContext,
} from "../need-utils.js";
import { actionCreators } from "../actions/actions.js";
import postContextDropDownModule from "../components/post-context-dropdown.js";

import "style/_visitor-title-bar.scss";

const serviceDependencies = ["$ngRedux", "$scope"];
function genComponentConf() {
  let template = `
        <nav class="visitor-title-bar">
            <div class="vtb__inner">
                <div class="vtb__inner__left">
                    <won-square-image
                        title="self.post.get('humanReadable')"
                        src="self.post.get('titleImgSrc')"
                        uri="self.post.get('uri')">
                    </won-square-image>
                    <hgroup>
                        <h1 class="vtb__title">{{ self.post.get('humanReadable') }}</h1>
                        <div class="vtb__titles__type" ng-if="!self.shouldShowRdf">{{ self.shortTypesLabel }}{{ self.matchingContext }}</div>
                        <div class="vtb__titles__type" ng-if="self.shouldShowRdf">{{ self.fullTypesLabel }}</div>
                    </hgroup>
                </div>
            </div>
            <won-post-context-dropdown></won-post-context-dropdown>
        </nav>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      window.vtb4dbg = this;

      const selectFromState = state => {
        const postUri = getPostUriFromRoute(state);
        const post = state.getIn(["needs", postUri]);
        return {
          postUri,
          post,
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
