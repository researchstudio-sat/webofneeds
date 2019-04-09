/**
 * Created by ksinger on 10.05.2016.
 */

import angular from "angular";
import { attach, get, getIn } from "../utils.js";
import won from "../won-es6.js";
import { relativeTime } from "../won-label-utils.js";
import { connect2Redux } from "../won-utils.js";
import * as needUtils from "../need-utils.js";
import * as viewUtils from "../view-utils.js";
import {
  selectLastUpdateTime,
  getConnectionUriFromRoute,
  getOwnedNeedByConnectionUri,
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
              {{ self.needTypeLabel }}
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
        <!-- FACETS -->
        <div class="pcg__columns__right" ng-if="self.shouldShowRdf || (self.shortFacets && self.shortFacets.length > 0)">
          <div class="pcg__columns__right__item">
            <div class="pcg__columns__right__item__label">
              Facets
            </div>
            <div class="pcg__columns__right__item__value" ng-if="self.shouldShowRdf">
              <span class="pcg__columns__right__item__value__facet" ng-repeat="facet in self.fullFacets">{{facet}}</span>
            </div>
            <div class="pcg__columns__right__item__value" ng-if="!self.shouldShowRdf">
              <span class="pcg__columns__right__item__value__facet" ng-repeat="facet in self.shortFacets">{{facet}}</span>
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

        const ownNeed = getOwnedNeedByConnectionUri(state, connectionUri);

        const ratingConnectionUri =
          get(connection, "remoteNeedUri") == this.postUri &&
          get(ownNeed, "heldBy")
            ? connectionUri
            : null;

        const post = this.postUri && getIn(state, ["needs", this.postUri]);
        const viewState = get(state, "view");

        const creationDate = get(post, "creationDate");
        const modifiedDate = get(post, "modifiedDate");

        return {
          WON: won.WON,
          needTypeLabel: post && needUtils.generateNeedTypeLabel(post),
          fullFlags: post && needUtils.generateFullNeedFlags(post),
          shortFlags: post && needUtils.generateShortNeedFlags(post),
          fullFacets: post && needUtils.generateFullNeedFacets(post),
          shortFacets: post && needUtils.generateShortNeedFacets(post),
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
