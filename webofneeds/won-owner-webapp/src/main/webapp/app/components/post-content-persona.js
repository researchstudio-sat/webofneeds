/**
 * Created by quasarchimaere on 20.02.2019.
 */

import angular from "angular";
import Immutable from "immutable";
import { attach, get, getIn } from "../utils.js";
import { connect2Redux } from "../won-utils.js";
import {
  getConnectionUriFromRoute,
  getOwnedNeedByConnectionUri,
} from "../selectors/general-selectors.js";
import * as needUtils from "../need-utils.js";
import * as processUtils from "../process-utils.js";
import { actionCreators } from "../actions/actions.js";
import ratingView from "./rating-view.js";
import squareImageModule from "./square-image.js";
import descriptionDetailViewerModule from "./details/viewer/description-viewer.js";
import { details } from "../../config/detail-definitions.js";

import "style/_post-content-persona.scss";
import { getOwnedConnectionByUri } from "../selectors/connection-selectors.js";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
      <div class="pcp__header">
          <won-square-image ng-if="!self.personaLoading"
              uri="::self.personaUri">
          </won-square-image>
          <div class="pcp__header__name"
              ng-if="self.personaName">
              <span class="pcp__header__name__label">{{ self.personaName }}</span>
              <span class="pcp__header__name__verification pcp__header__name__verification--verified" ng-if="self.personaVerified" title="The Persona-Relation of this Post is verified by the Persona">Verified</span>
              <span class="pcp__header__name__verification pcp__header__name__verification--unverified" ng-if="!self.personaVerified" title="The Persona-Relation of this Post is NOT verified by the Persona">Unverified!</span>
          </div>
          <div class="pcp__websitelabel" ng-if="self.personaWebsite">Website:</div>
          <a class="pcp__websitelink" target="_blank" href="{{self.personaWebsite}}" ng-if="self.personaWebsite">{{ self.personaWebsite }}</a>
      </div>
      <div class="pcp__rating" ng-if="self.personaHasReviewFacet">
        <div class="pcp__rating__label">
          <span class="pcp__rating__label__title">Rating</span>
          <span class="pcp__rating__label__aggregate" ng-if="self.aggregateRatingString">(★ {{self.aggregateRatingString}})</span>
        </div>
        <won-rating-view rating="self.aggregateRatingRounded" rating-connection-uri="self.ratingConnectionUri"></won-rating-view>
        <div class="pcp__rating__reviewcount" ng-if="self.reviewCount">{{ self.reviewCount }} Reviews</div>
        <button class="pcp__rating__view won-button--filled red" ng-if="self.reviewCount" ng-click="self.viewPersonaReviews()">View</button>
      </div>
      <div class="pcp__holds" ng-if="self.personaHasHolderFacet">
        <div class="pcp__holds__label">Author of {{ self.personaHoldsSize }} Post(s)</div>
        <button class="pcp__holds__view won-button--filled red" ng-click="self.viewPersonaPosts()">View</button>
      </div>
      <won-description-viewer detail="::self.descriptionDetail" content="self.personaDescription" ng-if="self.descriptionDetail && self.personaDescription"></won-description-viewer>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      window.pcp4dbg = this;

      const selectFromState = state => {
        const connectionUri = getConnectionUriFromRoute(state);
        const connection = getOwnedConnectionByUri(state, connectionUri);
        const ownNeed = getOwnedNeedByConnectionUri(state, connectionUri);

        const ratingConnectionUri =
          get(connection, "remoteNeedUri") == this.holdsUri &&
          get(ownNeed, "heldBy")
            ? connectionUri
            : null;

        const post = this.holdsUri && getIn(state, ["needs", this.holdsUri]);
        const personaUri = get(post, "heldBy");
        const persona = post ? getIn(state, ["needs", personaUri]) : undefined;

        const personaHasHolderFacet = needUtils.hasHolderFacet(persona);
        const personaHolds = personaHasHolderFacet && get(persona, "holds");
        const personaVerified =
          personaHolds && personaHolds.includes(this.holdsUri);

        const personaHasReviewFacet = needUtils.hasReviewFacet(persona);
        const aggregateRating =
          personaHasReviewFacet &&
          getIn(persona, ["rating", "aggregateRating"]);

        const process = get(state, "process");
        //TODO: CHECK IF PERSONA HAS REVIEWFACET
        return {
          post,
          personaUri,
          personaLoading:
            !persona || processUtils.isNeedLoading(process, personaUri),
          personaFailedToLoad:
            persona && processUtils.hasNeedFailedToLoad(process, personaUri),
          personaName: getIn(persona, ["content", "personaName"]),
          personaDescription: getIn(persona, ["content", "description"]),
          personaWebsite: getIn(persona, ["content", "website"]),
          personaVerified,
          personaHoldsSize: personaHolds ? personaHolds.size : 0,
          personaHasReviewFacet,
          personaHasHolderFacet,
          reviewCount:
            personaHasReviewFacet && getIn(persona, ["rating", "reviewCount"]),
          aggregateRatingString: aggregateRating && aggregateRating.toFixed(1),
          aggregateRatingRounded: aggregateRating
            ? Math.round(aggregateRating)
            : 0,
          ratingConnectionUri: ratingConnectionUri,
          descriptionDetail: details.description,
        };
      };
      connect2Redux(selectFromState, actionCreators, ["self.holdsUri"], this);
    }

    viewPersonaPosts() {
      console.debug("IMPL ME");
      this.needs__selectTab(
        Immutable.fromJS({ needUri: this.personaUri, selectTab: "HOLDS" })
      );
      this.router__stateGoCurrent({ viewNeedUri: this.personaUri });
    }

    viewPersonaReviews() {
      console.debug("IMPL ME");
      this.needs__selectTab(
        Immutable.fromJS({ needUri: this.personaUri, selectTab: "REVIEWS" })
      );
      this.router__stateGoCurrent({ viewNeedUri: this.personaUri });
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
      holdsUri: "=", //we use the postUri that the author to display is holding, instead of the author itself, this is due to review/rating purposes
    },
  };
}

export default angular
  .module("won.owner.components.postContentPersona", [
    ratingView,
    squareImageModule,
    descriptionDetailViewerModule,
  ])
  .directive("wonPostContentPersona", genComponentConf).name;
