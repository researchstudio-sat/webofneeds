/**
 * Created by ksinger on 10.05.2016.
 */

import angular from "angular";
import postShareLinkModule from "./post-share-link.js";
import { attach, get } from "../utils.js";
import won from "../won-es6.js";
import { labels, relativeTime } from "../won-label-utils.js";
import { connect2Redux } from "../won-utils.js";
import {
  generateFullNeedTypesLabel,
  // generateShortNeedTypesLabel,
} from "../need-utils.js";
import {
  selectLastUpdateTime,
  getConnectionUriFromRoute,
  getOwnedNeedByConnectionUri,
} from "../selectors/general-selectors.js";
import { actionCreators } from "../actions/actions.js";
import ratingView from "./rating-view.js";

import "style/_post-content-general.scss";
import { getOwnedConnectionByUri } from "../selectors/connection-selectors.js";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
      <div class="pcg__columns">
        <div class="pcg__columns__left">
          <!-- PERSONA -->
          <div class="pcg__columns__left__item" ng-if="self.persona">
            <div class="pcg__columns__left__item__label">
              Author
            </div>
            <div class="pcg__columns__left__item__value">
              {{ self.persona.getIn(['jsonld', 's:name']) }}
              <won-rating-view rating="self.rating()" rating-connection-uri="self.ratingConnectionUri"></won-rating-view>
            </div>
          </div>
          <!-- RATING -->
          <div class="pcg__columns__left__item" ng-if="self.friendlyTimestamp">
            <div class="pcg__columns__left__item__label">
              Created
            </div>
            <div class="pcg__columns__left__item__value">
              {{ self.friendlyTimestamp }}
            </div>
          </div>
        </div>

        <div class="pcg__columns__right" ng-if="self.shouldShowTypes">
          <!-- TYPES -->
          <!-- TODO: We Do not store a single type anymore but a list of types... adapt accordingly -->
          <div class="pcg__columns__left__item">
            <div class="pcg__columns__left__item__label">
              Types
            </div>
            <div class="pcg__columns__left__item__value">
              {{ self.generateFullNeedTypesLabel(self.post) }}
            </div>
          </div>
        </div>

        <div class="pcg__columns__right" ng-if="self.shouldShowRdf && self.flags && self.flags.size > 0">
          <!-- FLAGS -->
          <div class="pcg__columns__right__item">
            <div class="pcg__columns__right__item__label">
              Flags
            </div>
            <div class="pcg__columns__right__item__value">
              <span class="pcg__columns__right__item__value__flag" ng-repeat="flag in self.flags.toArray()">{{ self.labels.flags[flag]? self.labels.flags[flag] : flag }}</span>
            </div>
          </div>
        </div>
        <div class="pcg__columns__right" ng-if="self.shouldShowRdf && self.facets && self.facets.size > 0">
          <!-- FACETS-->
          <div class="pcg__columns__right__item">
            <div class="pcg__columns__right__item__label">
              Facets
            </div>
            <div class="pcg__columns__right__item__value">
              <span class="pcg__columns__right__item__value__facet" ng-repeat="facet in self.facets.toArray()">{{ self.labels.facets[facet]? self.labels.facets[facet] : facet }}</span>
            </div>
          </div>
        </div>
      </div>
      <won-post-share-link
        ng-if="!self.preventSharing"
        post-uri="self.post.get('uri')">
      </won-post-share-link>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      window.pcg4dbg = this;
      this.labels = labels;
      this.generateFullNeedTypesLabel = generateFullNeedTypesLabel;
      //  this.generateShortNeedTypesLabel = generateShortNeedTypesLabel;
      this.shouldShowTypes = false;

      const selectFromState = state => {
        const connectionUri = getConnectionUriFromRoute(state);
        const connection = getOwnedConnectionByUri(state, connectionUri);

        const ownNeed = getOwnedNeedByConnectionUri(state, connectionUri);

        const ratingConnectionUri =
          get(connection, "remoteNeedUri") == this.postUri &&
          get(ownNeed, "heldBy")
            ? connectionUri
            : null;

        const post = this.postUri && state.getIn(["needs", this.postUri]);
        const flags = post && post.getIn(["content", "flags"]);
        const facets = post && post.get("facets");

        const persona = post
          ? state.getIn(["needs", post.get("heldBy")])
          : undefined;
        const personaHolds = persona && persona.get("holds");

        return {
          WON: won.WON,
          post,
          flags,
          facets,
          persona:
            personaHolds && personaHolds.includes(post.get("uri"))
              ? persona
              : undefined,
          preventSharing:
            (post && post.get("state") === won.WON.InactiveCompacted) ||
            (flags &&
              flags.filter(
                flag => flag === won.WON.NoHintForCounterpartCompacted
              ).size > 0),
          friendlyTimestamp:
            post &&
            relativeTime(selectLastUpdateTime(state), post.get("creationDate")),
          ratingConnectionUri: ratingConnectionUri,
          shouldShowRdf: state.getIn(["view", "showRdf"]),
        };
      };
      connect2Redux(selectFromState, actionCreators, ["self.postUri"], this);
    }

    rating() {
      let sum = 0;
      for (const char of this.persona.get("uri")) {
        sum += char.charCodeAt(0);
      }
      return (sum % 5) + 1;
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
  .module("won.owner.components.postContentGeneral", [
    postShareLinkModule,
    ratingView,
  ])
  .directive("wonPostContentGeneral", genComponentConf).name;
