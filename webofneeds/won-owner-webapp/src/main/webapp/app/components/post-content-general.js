/**
 * Created by ksinger on 10.05.2016.
 */

import angular from "angular";
import postShareLinkModule from "./post-share-link.js";
import { attach, get } from "../utils.js";
import won from "../won-es6.js";
import { relativeTime } from "../won-label-utils.js";
import { connect2Redux } from "../won-utils.js";
import {
  generateFullNeedTypesLabel,
  generateShortNeedTypesLabel,
  generateFullNeedFlags,
  generateFullNeedFacets,
  generateShortNeedFlags,
  generateShortNeedFacets,
  generateNeedMatchingContext,
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
      <!-- LEFT COLUMN -->
        <div class="pcg__columns__left">
          <!-- PERSONA -->
          <div class="pcg__columns__left__item" ng-if="self.persona">
            <div class="pcg__columns__left__item__label">
              Author
            </div>
            <div class="pcg__columns__left__item__value">
              {{ self.persona.get('humanReadable') }}
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
          <!-- TYPES - IF SHOW RDF IS TRUE -->
          <div class="pcg__columns__left__item" ng-if="self.shouldShowRdf">
            <div class="pcg__columns__left__item__label">
              Types
            </div>
            <div class="pcg__columns__left__item__value">
              {{ self.fullTypesLabel }}
            </div>
          </div>
        </div>

      <!-- RIGHT COLUMN -->
        <!-- TYPES - IF SHOW RDF IS FALSE -->
        <div class="pcg__columns__right" ng-if="!self.shouldShowRdf && self.shortTypesLabel.length > 0">
          <div class="pcg__columns__left__item">
            <div class="pcg__columns__left__item__label">
              Type
            </div>
            <div class="pcg__columns__left__item__value">
              {{ self.shortTypesLabel }} {{self.matchingContext}}
            </div>
          </div>
        </div>
        <div class="pcg__columns__right" ng-if="!self.shouldShowRdf && self.shortTypesLabel.length === 0 && self.matchingContext.length > 0">
          <div class="pcg__columns__left__item">
            <div class="pcg__columns__left__item__label">
              Context
            </div>
            <div class="pcg__columns__left__item__value">
              {{self.matchingContext}}
            </div>
          </div>
        </div>
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

      <won-post-share-link
        ng-if="!self.preventSharing"
        post-uri="self.post.get('uri')">
      </won-post-share-link>
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

        const post = this.postUri && state.getIn(["needs", this.postUri]);
        // move this down when refactoring preventSharing
        const fullFlags = post && generateFullNeedFlags(post);

        const persona = post
          ? state.getIn(["needs", post.get("heldBy")])
          : undefined;
        const personaHolds = persona && persona.get("holds");
        const personaRating = persona && persona.get("rating");

        return {
          WON: won.WON,
          post,
          fullTypesLabel: post && generateFullNeedTypesLabel(post),
          shortTypesLabel: post && generateShortNeedTypesLabel(post),
          matchingContext: post && generateNeedMatchingContext(post),
          fullFlags,
          shortFlags: post && generateShortNeedFlags(post),
          fullFacets: post && generateFullNeedFacets(post),
          shortFacets: post && generateShortNeedFacets(post),
          persona:
            personaHolds && personaHolds.includes(post.get("uri"))
              ? persona
              : undefined,
          personaRating: personaRating,
          // TODO: this probably should not be checked like that - util method?
          preventSharing:
            (post && post.get("state") === won.WON.InactiveCompacted) ||
            (fullFlags &&
              !!fullFlags.find(flag => flag === "No Hint For Others")),
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
      // Return actuall rating!
      /*
      let sum = 0;
      for (const char of this.persona.get("uri")) {
        sum += char.charCodeAt(0);
      }
      return (sum % 5) + 1;
      */
      const rating = this.personaRating
        ? this.personaRating.get("aggregateRating")
        : 0;

      return Math.round(rating);
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
