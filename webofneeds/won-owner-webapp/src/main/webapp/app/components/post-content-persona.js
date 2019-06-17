/**
 * Created by quasarchimaere on 20.02.2019.
 */

import angular from "angular";
import Immutable from "immutable";
import { get, getIn } from "../utils.js";
import { attach } from "../cstm-ng-utils.js";
import { connect2Redux } from "../configRedux.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as processUtils from "../redux/utils/process-utils.js";
import { actionCreators } from "../actions/actions.js";
import squareImageModule from "./square-image.js";
import descriptionDetailViewerModule from "./details/viewer/description-viewer.js";
import { details } from "../../config/detail-definitions.js";

import { Elm } from "../../elm/RatingView.elm";
import elmModule from "./elm.js";

import "~/style/_post-content-persona.scss";
import { getOwnedConnectionByUri } from "../redux/selectors/connection-selectors.js";

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
      <div class="pcp__rating" ng-if="self.personaHasReviewSocket">
        <div class="pcp__rating__label">
          <span class="pcp__rating__label__title">Rating</span>
          <span class="pcp__rating__label__aggregate" ng-if="self.aggregateRatingString">(★ {{self.aggregateRatingString}})</span>
        </div>
        <won-elm
          module="self.ratingView"
          props="{
            rating: self.aggregateRatingRounded,
            connectionUri: self.ratingConnectionUri
          }"
        ></won-elm>
        <div class="pcp__rating__reviewcount" ng-if="self.reviewCount">{{ self.reviewCount }} Reviews</div>
        <button class="pcp__rating__view won-button--filled red" ng-if="self.reviewCount" ng-click="self.viewPersonaReviews()">View</button>
      </div>
      <div class="pcp__holds" ng-if="self.personaHasHolderSocket">
        <div class="pcp__holds__label">Holder of {{ self.personaHoldsSize }} Post(s)</div>
        <button class="pcp__holds__view won-button--filled red" ng-click="self.viewPersonaPosts()">View</button>
      </div>
      <div class="pcp__buddies" ng-if="self.personaHasBuddySocket">
        <div class="pcp__buddies__label">Buddy of {{ self.personaBuddySize }} Persona(s)</div>
        <button class="pcp__buddies__view won-button--filled red" ng-click="self.viewPersonaBuddies()">View</button>
      </div>
      <won-description-viewer detail="::self.descriptionDetail" content="self.personaDescription" ng-if="self.descriptionDetail && self.personaDescription"></won-description-viewer>
      <button ng-if="self.postIsOwned" class="won-button--filled red" ng-click="self.removePersona()">Remove Persona</button>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      window.pcp4dbg = this;

      this.ratingView = Elm.RatingView;

      const selectFromState = state => {
        const connectionUri = generalSelectors.getConnectionUriFromRoute(state);
        const connection = getOwnedConnectionByUri(state, connectionUri);
        const ownAtom = generalSelectors.getOwnedAtomByConnectionUri(
          state,
          connectionUri
        );

        const ratingConnectionUri =
          get(connection, "targetAtomUri") == this.holdsUri &&
          get(ownAtom, "heldBy")
            ? connectionUri
            : null;

        const post = this.holdsUri && getIn(state, ["atoms", this.holdsUri]);
        const personaUri = get(post, "heldBy");
        const persona = post ? getIn(state, ["atoms", personaUri]) : undefined;

        const personaHasHolderSocket = atomUtils.hasHolderSocket(persona);
        const personaHolds = personaHasHolderSocket && get(persona, "holds");
        const personaVerified =
          personaHolds && personaHolds.includes(this.holdsUri);

        const personaHasReviewSocket = atomUtils.hasReviewSocket(persona);
        const aggregateRating =
          personaHasReviewSocket &&
          getIn(persona, ["rating", "aggregateRating"]);

        const personaHasBuddySocket = atomUtils.hasBuddySocket(persona);
        const personaBuddies = personaHasBuddySocket && get(persona, "buddies");

        const process = get(state, "process");
        //TODO: CHECK IF PERSONA HAS REVIEWSOCKET
        return {
          post,
          personaUri,
          postIsOwned: generalSelectors.isAtomOwned(state, this.holdsUri),
          postHasHoldableSocket: atomUtils.hasHoldableSocket(post),
          personaLoading:
            !persona || processUtils.isAtomLoading(process, personaUri),
          personaFailedToLoad:
            persona && processUtils.hasAtomFailedToLoad(process, personaUri),
          personaName: getIn(persona, ["content", "personaName"]),
          personaDescription: getIn(persona, ["content", "description"]),
          personaWebsite: getIn(persona, ["content", "website"]),
          personaVerified,
          personaHoldsSize: personaHolds ? personaHolds.size : 0,
          personaBuddySize: personaBuddies ? personaBuddies.size : 0,
          personaHasReviewSocket,
          personaHasHolderSocket,
          personaHasBuddySocket,
          reviewCount:
            personaHasReviewSocket && getIn(persona, ["rating", "reviewCount"]),
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

    removePersona() {
      this.personas__disconnect(this.holdsUri, this.personaUri);
    }

    viewPersonaPosts() {
      this.atoms__selectTab(
        Immutable.fromJS({ atomUri: this.personaUri, selectTab: "HOLDS" })
      );
      this.router__stateGoCurrent({
        viewAtomUri: this.personaUri,
        viewConnUri: undefined,
      });
    }

    viewPersonaBuddies() {
      this.atoms__selectTab(
        Immutable.fromJS({ atomUri: this.personaUri, selectTab: "BUDDIES" })
      );
      this.router__stateGoCurrent({
        viewAtomUri: this.personaUri,
        viewConnUri: undefined,
      });
    }

    viewPersonaReviews() {
      this.atoms__selectTab(
        Immutable.fromJS({ atomUri: this.personaUri, selectTab: "REVIEWS" })
      );
      this.router__stateGoCurrent({
        viewAtomUri: this.personaUri,
        viewConnUri: undefined,
      });
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
    elmModule,
    squareImageModule,
    descriptionDetailViewerModule,
  ])
  .directive("wonPostContentPersona", genComponentConf).name;
