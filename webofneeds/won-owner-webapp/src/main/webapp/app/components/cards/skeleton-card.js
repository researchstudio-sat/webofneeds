import angular from "angular";
import inviewModule from "angular-inview";
import "ng-redux";
import atomSuggestionsIndicatorModule from "../atom-suggestions-indicator.js";
import { actionCreators } from "../../actions/actions.js";
import { getIn, get } from "../../utils.js";
import { attach } from "../../cstm-ng-utils.js";
import { connect2Redux } from "../../configRedux.js";
import * as processUtils from "../../redux/utils/process-utils.js";

import "~/style/_skeleton-card.scss";
import Immutable from "immutable";
import { classOnComponentRoot } from "../../cstm-ng-utils";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
      <!-- Icon Information -->
      <div class="card__icon__skeleton" ng-if="!self.atomLoaded"
        in-view="$inview && self.atomToLoad && self.ensureAtomIsLoaded()">
      </div>
      <!-- Main Information -->
      <div class="card__main" ng-if="self.atomFailedToLoad">
          <div class="card__main__topline">
              <div class="card__main__topline__notitle">
                  Atom Loading failed
              </div>
          </div>
          <div class="card__main__subtitle">
              <span class="card__main__subtitle__type">
                  Atom might have been deleted.
              </span>
          </div>
      </div>
      <div class="card__main" ng-if="self.atomLoading || self.atomToLoad || self.atomInCreation">
          <div class="card__main__topline">
              <div class="card__main__topline__title"></div>
          </div>
          <div class="card__main__subtitle">
              <span class="card__main__subtitle__type"></span>
          </div>
      </div>
      <!-- Attached Persona Info -->
      <div class="card__nopersona" ng-if="::self.showPersona && !self.atomLoaded">
      </div>
      <won-atom-suggestions-indicator
          ng-if="::self.showSuggestions"
          class="card__indicators"
          atom-uri="::self.atomUri"
          on-selected="::self.showAtomSuggestions(self.atomUri)">
      </won-atom-suggestions-indicator>
    </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);

      const selectFromState = state => {
        const atom = getIn(state, ["atoms", this.atomUri]);
        const process = get(state, "process");

        const atomInCreation = get(atom, "isBeingCreated");
        const atomLoaded =
          processUtils.isAtomLoaded(process, this.atomUri) && !atomInCreation;
        const atomLoading = processUtils.isAtomLoading(process, this.atomUri);
        const atomToLoad = processUtils.isAtomToLoad(process, this.atomUri);
        const atomFailedToLoad = processUtils.hasAtomFailedToLoad(
          process,
          this.atomUri
        );

        return {
          atomLoaded,
          atomLoading,
          atomInCreation,
          atomToLoad,
          atomFailedToLoad,
        };
      };

      connect2Redux(
        selectFromState,
        actionCreators,
        ["self.atomUri", "self.showSuggestions", "self.showPersona"],
        this
      );

      classOnComponentRoot(
        "won-is-loading",
        () => this.atomLoading || this.atomInCreation,
        this
      );
      classOnComponentRoot("won-is-toload", () => this.atomToLoad, this);
    }

    ensureAtomIsLoaded() {
      if (
        this.atomUri &&
        !this.atomLoaded &&
        !this.atomLoading &&
        !this.atomInCreation &&
        this.atomToLoad
      ) {
        this.atoms__fetchUnloadedAtom(this.atomUri);
      }
    }

    showAtomSuggestions(atomUri) {
      this.showAtomTab(atomUri, "SUGGESTIONS");
    }

    showAtomTab(atomUri, tab = "DETAIL") {
      this.atoms__selectTab(
        Immutable.fromJS({ atomUri: atomUri, selectTab: tab })
      );
      this.router__stateGo("post", { postUri: atomUri });
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
      showSuggestions: "=",
      showPersona: "=",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.skeletonCard", [
    inviewModule.name,
    atomSuggestionsIndicatorModule,
  ])
  .directive("wonSkeletonCard", genComponentConf).name;
