/**
 * Created by ksinger on 20.08.2015.
 */
import angular from "angular";
import { get, getIn } from "../utils.js";
import { attach } from "../cstm-ng-utils.js";
import { connect2Redux } from "../configRedux.js";
import { actionCreators } from "../actions/actions.js";
import postContextDropDownModule from "../components/post-context-dropdown.js";
import shareDropdownModule from "../components/share-dropdown.js";
import * as atomUtils from "../redux/utils/atom-utils.js";

import "~/style/_atom-header-big.scss";

const serviceDependencies = ["$ngRedux", "$scope"];
function genComponentConf() {
  let template = `
        <nav class="atom-header-big">
            <div class="ahb__inner">
                <div class="ahb__inner__left">
                    <won-square-image
                        uri="self.postUri">
                    </won-square-image>
                    <hgroup>
                        <h1 class="ahb__title" ng-if="self.hasTitle()">{{ self.generateTitle() }}</h1>
                        <h1 class="ahb__title ahb__title--notitle" ng-if="!self.hasTitle() && self.isDirectResponse">RE: no title</h1>
                        <h1 class="ahb__title ahb__title--notitle" ng-if="!self.hasTitle() && !self.isDirectResponse">no title</h1>
                        <span class="ahb__titles__persona" ng-if="self.personaName">{{ self.personaName }}</span>
                        <span class="ahb__titles__groupchat"
                          ng-if="self.isGroupChatEnabled && !self.isChatEnabled">
                          Group Chat
                        </span>
                        <span class="ahb__titles__groupchat"
                          ng-if="self.isGroupChatEnabled && self.isChatEnabled">
                          Group Chat enabled
                        </span>
                        <div class="ahb__titles__type">{{ self.atomTypeLabel }}</div>
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
      window.ahb4dbg = this;

      const selectFromState = state => {
        const postUri = this.atomUri;
        const post = state.getIn(["atoms", postUri]);

        const personaUri = atomUtils.getHeldByUri(post);
        const persona = getIn(state, ["atoms", personaUri]);
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
          atomTypeLabel: post && atomUtils.generateTypeLabel(post),
        };
      };
      connect2Redux(
        selectFromState,
        actionCreators,
        ["self.atomUri", "self.hideBackButton"],
        this
      );
    }

    hasTitle() {
      if (this.isDirectResponse && this.responseToAtom) {
        return !!get(this.responseToAtom, "humanReadable");
      } else {
        return !!get(this.post, "humanReadable");
      }
    }

    generateTitle() {
      if (this.isDirectResponse && this.responseToAtom) {
        return "Re: " + get(this.responseToAtom, "humanReadable");
      } else {
        return get(this.post, "humanReadable");
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
      atomUri: "=",
      hideBackButton: "=",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.atomHeaderBig", [
    postContextDropDownModule,
    shareDropdownModule,
  ])
  .directive("wonAtomHeaderBig", genComponentConf).name;
