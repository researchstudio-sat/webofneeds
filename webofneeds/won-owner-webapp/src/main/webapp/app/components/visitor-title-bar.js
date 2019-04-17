/**
 * Created by ksinger on 20.08.2015.
 */
import angular from "angular";
import { attach, get, getIn } from "../utils.js";
import { connect2Redux } from "../won-utils.js";
import { getPostUriFromRoute } from "../selectors/general-selectors.js";
import { actionCreators } from "../actions/actions.js";
import postContextDropDownModule from "../components/post-context-dropdown.js";
import shareDropdownModule from "../components/share-dropdown.js";
import * as atomUtils from "../atom-utils.js";

import "style/_visitor-title-bar.scss";

const serviceDependencies = ["$ngRedux", "$scope"];
function genComponentConf() {
  let template = `
        <nav class="visitor-title-bar">
            <div class="vtb__inner">
                <div class="vtb__inner__left">
                    <won-square-image
                        uri="self.postUri">
                    </won-square-image>
                    <hgroup>
                        <h1 class="vtb__title" ng-if="self.hasTitle()">{{ self.generateTitle() }}</h1>
                        <h1 class="vtb__title vtb__title--notitle" ng-if="!self.hasTitle() && self.isDirectResponse">RE: no title</h1>
                        <h1 class="vtb__title vtb__title--notitle" ng-if="!self.hasTitle() && !self.isDirectResponse">no title</h1>
                        <span class="vtb__titles__persona" ng-if="self.personaName">{{ self.personaName }}</span>
                        <span class="vtb__titles__groupchat"
                          ng-if="self.isGroupChatEnabled && !self.isChatEnabled">
                          Group Chat
                        </span>
                        <span class="vtb__titles__groupchat"
                          ng-if="self.isGroupChatEnabled && self.isChatEnabled">
                          Group Chat enabled
                        </span>
                        <div class="vtb__titles__type">{{ self.atomTypeLabel }}</div>
                    </hgroup>
                </div>
            </div>
            <won-share-dropdown atom-uri="self.postUri"></won-share-dropdown>
            <won-post-context-dropdown atom-uri="self.postUri"></won-post-context-dropdown>
        </nav>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      window.vtb4dbg = this;

      const selectFromState = state => {
        const postUri = getPostUriFromRoute(state);
        const post = state.getIn(["atoms", postUri]);

        const personaUri = get(post, "heldBy");
        const persona = personaUri && getIn(state, ["atoms", personaUri]);
        const personaName = get(persona, "humanReadable");
        const isDirectResponse = atomUtils.isDirectResponseAtom(post);
        const responseToUri =
          isDirectResponse && getIn(post, ["content", "responseToUri"]);
        const responseToAtom =
          responseToUri && getIn(state, ["atoms", responseToUri]);

        return {
          postUri,
          post,
          personaName,
          isDirectResponse,
          responseToAtom,
          isGroupChatEnabled: atomUtils.hasGroupSocket(post),
          isChatEnabled: atomUtils.hasChatSocket(post),
          atomTypeLabel: post && atomUtils.generateAtomTypeLabel(post),
        };
      };
      connect2Redux(selectFromState, actionCreators, [], this);
    }

    hasTitle() {
      if (this.isDirectResponse && this.responseToAtom) {
        return !!this.responseToAtom.get("humanReadable");
      } else {
        return !!this.post.get("humanReadable");
      }
    }

    generateTitle() {
      if (this.isDirectResponse && this.responseToAtom) {
        return "Re: " + this.responseToAtom.get("humanReadable");
      } else {
        return this.post.get("humanReadable");
      }
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
  .module("won.owner.components.visitorTitleBar", [
    postContextDropDownModule,
    shareDropdownModule,
  ])
  .directive("wonVisitorTitleBar", genComponentConf).name;
