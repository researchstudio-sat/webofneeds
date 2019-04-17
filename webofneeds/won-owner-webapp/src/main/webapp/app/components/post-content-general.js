/**
 * Created by ksinger on 10.05.2016.
 */

import angular from "angular";
import { attach, get, getIn } from "../utils.js";
import won from "../won-es6.js";
import { relativeTime } from "../won-label-utils.js";
import { connect2Redux } from "../won-utils.js";
import * as atomUtils from "../atom-utils.js";
import * as viewUtils from "../view-utils.js";
import {
  selectLastUpdateTime,
  getConnectionUriFromRoute,
  getOwnedAtomByConnectionUri,
} from "../selectors/general-selectors.js";
import { actionCreators } from "../actions/actions.js";

import "style/_post-content-general.scss";
import { getOwnedConnectionByUri } from "../selectors/connection-selectors.js";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
      <div class="pcg__columns">
      <!-- LEFT COLUMN -->
        <div class="pcg__columns__left">
          <div class="pcg__columns__left__item" ng-if="self.friendlyCreationDate">
            <div class="pcg__columns__left__item__label">
              Created
            </div>
            <div class="pcg__columns__left__item__value">
              {{ self.friendlyCreationDate }}
            </div>
          </div>
          <div class="pcg__columns__left__item" ng-if="self.friendlyModifiedDate">
            <div class="pcg__columns__left__item__label">
              Modified
            </div>
            <div class="pcg__columns__left__item__value">
              {{ self.friendlyModifiedDate }}
            </div>
          </div>
          <div class="pcg__columns__left__item">
            <div class="pcg__columns__left__item__label">
              Types
            </div>
            <div class="pcg__columns__left__item__value">
              {{ self.atomTypeLabel }}
            </div>
          </div>
        </div>

      <!-- RIGHT COLUMN -->
        <!-- FLAGS -->
        <div class="pcg__columns__right" ng-if="self.shouldShowRdf || (self.shortFlags && self.shortFlags.length > 0)">
          <div class="pcg__columns__right__item">
            <div class="pcg__columns__right__item__label">
              Flags
            </div>
            <div class="pcg__columns__right__item__value" ng-if="self.shouldShowRdf">
              <span class="pcg__columns__right__item__value__flag" ng-repeat="flag in self.fullFlags">{{flag}}</span>
            </div>
            <div class="pcg__columns__right__item__value" ng-if="!self.shouldShowRdf">
              <span class="pcg__columns__right__item__value__flag" ng-repeat="flag in self.shortFlags">{{flag}}</span>
            </div>
          </div>
        </div>
        <!-- SOCKETS -->
        <div class="pcg__columns__right" ng-if="self.shouldShowRdf || (self.shortSockets && self.shortSockets.length > 0)">
          <div class="pcg__columns__right__item">
            <div class="pcg__columns__right__item__label">
              Sockets
            </div>
            <div class="pcg__columns__right__item__value" ng-if="self.shouldShowRdf">
              <span class="pcg__columns__right__item__value__socket" ng-repeat="socket in self.fullSockets">{{socket}}</span>
            </div>
            <div class="pcg__columns__right__item__value" ng-if="!self.shouldShowRdf">
              <span class="pcg__columns__right__item__value__socket" ng-repeat="socket in self.shortSockets">{{socket}}</span>
            </div>
          </div>
        </div>
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      window.pcg4dbg = this;

      const selectFromState = state => {
        const connectionUri = getConnectionUriFromRoute(state);
        const connection = getOwnedConnectionByUri(state, connectionUri);

        const ownAtom = getOwnedAtomByConnectionUri(state, connectionUri);

        const ratingConnectionUri =
          get(connection, "targetAtomUri") == this.postUri &&
          get(ownAtom, "heldBy")
            ? connectionUri
            : null;

        const post = this.postUri && getIn(state, ["atoms", this.postUri]);
        const viewState = get(state, "view");

        const creationDate = get(post, "creationDate");
        const modifiedDate = get(post, "modifiedDate");

        return {
          WON: won.WON,
          atomTypeLabel: post && atomUtils.generateAtomTypeLabel(post),
          fullFlags: post && atomUtils.generateFullAtomFlags(post),
          shortFlags: post && atomUtils.generateShortAtomFlags(post),
          fullSockets: post && atomUtils.generateFullAtomSockets(post),
          shortSockets: post && atomUtils.generateShortAtomSockets(post),
          friendlyCreationDate:
            creationDate &&
            relativeTime(selectLastUpdateTime(state), creationDate),
          friendlyModifiedDate:
            modifiedDate &&
            modifiedDate != creationDate &&
            relativeTime(selectLastUpdateTime(state), modifiedDate),
          ratingConnectionUri: ratingConnectionUri,
          shouldShowRdf: viewUtils.showRdf(viewState),
        };
      };
      connect2Redux(selectFromState, actionCreators, ["self.postUri"], this);
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
      postUri: "=",
    },
  };
}

export default angular
  .module("won.owner.components.postContentGeneral", [])
  .directive("wonPostContentGeneral", genComponentConf).name;
