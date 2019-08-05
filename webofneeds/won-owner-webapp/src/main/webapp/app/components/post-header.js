/**
 * Component for rendering atom-title
 * Created by ksinger on 10.04.2017.
 */
import angular from "angular";
import "ng-redux";
import { actionCreators } from "../actions/actions.js";
import { relativeTime } from "../won-label-utils.js";
import { delay, get, getIn } from "../utils.js";
import { attach, classOnComponentRoot } from "../cstm-ng-utils.js";
import { connect2Redux } from "../configRedux.js";
import { selectLastUpdateTime } from "../redux/selectors/general-selectors.js";
import won from "../won-es6.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as processUtils from "../redux/utils/process-utils.js";

import "~/style/_post-header.scss";
import WonAtomIcon from "./atom-icon.jsx";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
    <won-preact class="atomImage" ng-if="!self.atomLoading" component="self.WonAtomIcon" props="{atomUri: self.atomUri}"></won-preact>
    <div class="ph__right" ng-if="!self.atom.get('isBeingCreated') && !self.atomLoading">
      <div class="ph__right__topline" ng-if="!self.atomFailedToLoad">
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
      <div class="ph__right__subtitle" ng-if="!self.atomFailedToLoad">
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
            {{ self.atomTypeLabel }}
          </span>
        </span>
        <div class="ph__right__subtitle__date">
          {{ self.friendlyTimestamp }}
        </div>
      </div>
      <div class="ph__right__topline" ng-if="self.atomFailedToLoad">
        <div class="ph__right__topline__notitle">
          Atom Loading failed
        </div>
      </div>
      <div class="ph__right__subtitle" ng-if="self.atomFailedToLoad">
        <span class="ph__right__subtitle__type">
          Atom might have been deleted.
        </span>
      </div>
    </div>
    
    <div class="ph__right" ng-if="self.atom.get('isBeingCreated')">
      <div class="ph__right__topline">
        <div class="ph__right__topline__notitle">
          Creating...
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
          <span class="ph__right__subtitle__type"
            {{ self.atomTypeLabel }}
          </span>
      </div>
    </div>
    <div class="ph__icon__skeleton" ng-if="self.atomLoading"></div>
    <div class="ph__right" ng-if="self.atomLoading">
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
      this.WonAtomIcon = WonAtomIcon;

      this.WON = won.WON;
      const selectFromState = state => {
        const atom = getIn(state, ["atoms", this.atomUri]);
        const isDirectResponse = atomUtils.isDirectResponseAtom(atom);
        const responseToUri =
          isDirectResponse && getIn(atom, ["content", "responseToUri"]);
        const responseToAtom =
          responseToUri && getIn(state, ["atoms", responseToUri]);

        const personaUri = atomUtils.getHeldByUri(atom);
        const persona = personaUri && getIn(state, ["atoms", personaUri]);
        const personaName = get(persona, "humanReadable");

        const process = get(state, "process");

        return {
          responseToAtom,
          atom,
          atomTypeLabel: atom && atomUtils.generateTypeLabel(atom),
          personaName,
          atomLoading:
            !atom || processUtils.isAtomLoading(process, this.atomUri),
          atomToLoad: !atom || processUtils.isAtomToLoad(process, this.atomUri),
          atomFailedToLoad:
            atom && processUtils.hasAtomFailedToLoad(process, this.atomUri),
          isDirectResponse: isDirectResponse,
          isGroupChatEnabled: atomUtils.hasGroupSocket(atom),
          isChatEnabled: atomUtils.hasChatSocket(atom),
          friendlyTimestamp:
            atom &&
            relativeTime(
              selectLastUpdateTime(state),
              get(atom, "lastUpdateDate")
            ),
        };
      };

      connect2Redux(selectFromState, actionCreators, ["self.atomUri"], this);

      classOnComponentRoot("won-is-loading", () => this.atomLoading, this);
      classOnComponentRoot("won-is-toload", () => this.atomToLoad, this);

      this.$scope.$watch(
        () =>
          this.atomUri &&
          (!this.atom || (this.atomToLoad && !this.atomLoading)),
        () => delay(0).then(() => this.ensureAtomIsLoaded())
      );
    }

    ensureAtomIsLoaded() {
      if (
        this.atomUri &&
        (!this.atom || (this.atomToLoad && !this.atomLoading))
      ) {
        this.atoms__fetchUnloadedAtom(this.atomUri);
      }
    }

    hasTitle() {
      if (this.isDirectResponse && this.responseToAtom) {
        return !!this.responseToAtom.get("humanReadable");
      } else {
        return !!this.atom.get("humanReadable");
      }
    }

    generateTitle() {
      if (this.isDirectResponse && this.responseToAtom) {
        return "Re: " + this.responseToAtom.get("humanReadable");
      } else {
        return this.atom.get("humanReadable");
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
  .module("won.owner.components.postHeader", [])
  .directive("wonPostHeader", genComponentConf).name;
