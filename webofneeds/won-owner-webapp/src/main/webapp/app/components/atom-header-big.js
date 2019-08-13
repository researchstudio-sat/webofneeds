/**
 * Created by ksinger on 20.08.2015.
 */
import angular from "angular";
import { get, getIn } from "../utils.js";
import { attach } from "../cstm-ng-utils.js";
import { connect2Redux } from "../configRedux.js";
import { actionCreators } from "../actions/actions.js";
import WonAtomContextDropdown from "../components/atom-context-dropdown.jsx";
import WonAtomIcon from "../components/atom-icon.jsx";
import WonShareDropdown from "../components/share-dropdown.jsx";
import * as atomUtils from "../redux/utils/atom-utils.js";

import "~/style/_atom-header-big.scss";

const serviceDependencies = ["$ngRedux", "$scope"];
function genComponentConf() {
  let template = `
        <nav class="atom-header-big">
            <div class="ahb__inner">
                <won-preact class="atomImage" component="self.WonAtomIcon" props="{atomUri: self.postUri}"></won-preact>
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
            <won-preact class="shareDropdown" component="self.WonShareDropdown" props="{atomUri: self.postUri}"></won-preact>
            <won-preact class="atomContextDropdown" component="self.WonAtomContextDropdown" props="{atomUri: self.postUri}"></won-preact>
        </nav>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      window.ahb4dbg = this;
      this.WonAtomIcon = WonAtomIcon;
      this.WonShareDropdown = WonShareDropdown;
      this.WonAtomContextDropdown = WonAtomContextDropdown;

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
      connect2Redux(selectFromState, actionCreators, ["self.atomUri"], this);
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
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.atomHeaderBig", [])
  .directive("wonAtomHeaderBig", genComponentConf).name;
