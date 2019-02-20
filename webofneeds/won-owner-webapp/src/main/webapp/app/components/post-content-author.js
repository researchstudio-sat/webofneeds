/**
 * Created by quasarchimaere on 20.02.2019.
 */

import angular from "angular";
import { attach, get, getIn } from "../utils.js";
import { connect2Redux } from "../won-utils.js";
import {
  getConnectionUriFromRoute,
  getOwnedNeedByConnectionUri,
} from "../selectors/general-selectors.js";
import * as processUtils from "../process-utils.js";
import { actionCreators } from "../actions/actions.js";
import ratingView from "./rating-view.js";
import squareImageModule from "./square-image.js";
import descriptionDetailViewerModule from "./details/viewer/description-viewer.js";
import { details } from "../../config/detail-definitions.js";

import "style/_post-content-author.scss";
import { getOwnedConnectionByUri } from "../selectors/connection-selectors.js";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
      <div class="pca__header">
          <won-square-image ng-if="!self.personaLoading"
              uri="::self.personaUri">
          </won-square-image>
          <div class="pca__header__name"
              ng-if="self.personaName">
              <span class="pca__header__name__label">{{ self.personaName }}</span>
              <span class="pca__header__name__verification pca__header__name__verification--verified" ng-if="self.personaVerified" title="The Authorship of this Post is verified by the Persona">Verified</span>
              <span class="pca__header__name__verification pca__header__name__verification--unverified" ng-if="!self.personaVerified" title="The Authorship of this Post is NOT verified by the Persona">Unverified!</span>
          </div>
          <div class="pca__websitelabel" ng-if="self.personaWebsite">Website:</div>
          <a class="pca__websitelink" target="_blank" href="{{self.personaWebsite}}" ng-if="self.personaWebsite">{{ self.personaWebsite }}</a>
      </div>
      <div class="pca__rating">
        <won-rating-view rating="self.rating()" rating-connection-uri="self.ratingConnectionUri"></won-rating-view>
      </div>
      <won-description-viewer detail="::self.descriptionDetail" content="self.personaDescription" ng-if="self.descriptionDetail && self.personaDescription"></won-description-viewer>
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
          get(connection, "remoteNeedUri") == this.holdsUri &&
          get(ownNeed, "heldBy")
            ? connectionUri
            : null;

        const post = this.holdsUri && state.getIn(["needs", this.holdsUri]);
        const personaUri = get(post, "heldBy");
        const persona = post
          ? state.getIn(["needs", post.get("heldBy")])
          : undefined;
        const personaHolds = persona && persona.get("holds");
        const personaRating = persona && persona.get("rating");

        const process = get(state, "process");

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
          personaVerified: personaHolds && personaHolds.includes(this.holdsUri),
          persona,
          personaRating: personaRating,
          ratingConnectionUri: ratingConnectionUri,
          descriptionDetail: details.description,
        };
      };
      connect2Redux(selectFromState, actionCreators, ["self.holdsUri"], this);
    }

    rating() {
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
      holdsUri: "=", //we use the postUri that the author to display is holding, instead of the author itself, this is due to review/rating purposes
    },
  };
}

export default angular
  .module("won.owner.components.postContentAuthor", [
    ratingView,
    squareImageModule,
    descriptionDetailViewerModule,
  ])
  .directive("wonPostContentAuthor", genComponentConf).name;
