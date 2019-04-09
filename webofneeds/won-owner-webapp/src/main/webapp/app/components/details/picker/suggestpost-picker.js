import angular from "angular";
import "ng-redux";
import Immutable from "immutable";
import { attach, sortBy, get, getIn, delay } from "../../../utils.js";
import { DomCache } from "../../../cstm-ng-utils.js";
import wonInput from "../../../directives/input.js";
import { connect2Redux } from "../../../won-utils.js";
import { actionCreators } from "../../../actions/actions.js";
import postHeaderModule from "../../post-header.js";
import labelledHrModule from "../../labelled-hr.js";
import { getActiveNeeds } from "../../../selectors/general-selectors.js";
import * as needUtils from "../../../need-utils.js";

import "style/_suggestpostpicker.scss";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
      <div class="suggestpostp__posts" ng-if="self.suggestionsAvailable">
        <div class="suggestpostp__posts__post clickable"
          ng-class="{'won--selected': self.isSelected(need)}"
          ng-repeat="need in self.sortedActiveNeeds"
          ng-click="self.selectNeed(need)">
          <won-post-header
              need-uri="need.get('uri')">
          </won-post-header>
        </div>
      </div>
      <div class="suggestpostp__noposts" ng-if="!self.suggestionsAvailable">
        {{ self.noSuggestionsLabel }}
      </div>
      <won-labelled-hr label="::'Not happy with the options? Add a Need-URI below'" class="suggestpostp__labelledhr"></won-labelled-hr>
      <div class="suggestpostp__input">
         <svg class="suggestpostp__input__icon clickable"
            style="--local-primary:var(--won-primary-color);"
            ng-if="!self.uriToFetchLoading && self.showFetchButton && !self.uriToFetchFailed && self.fetchNeedUriFieldHasText()"
            ng-click="self.fetchNeed()">
            <use xlink:href="#ico16_checkmark" href="#ico16_checkmark"></use>
         </svg>
         <svg class="suggestpostp__input__icon clickable"
            style="--local-primary:var(--won-primary-color);"
            ng-if="!self.uriToFetchLoading && (self.showResetButton || self.uriToFetchFailed) && self.fetchNeedUriFieldHasText()"
            ng-click="self.resetNeedUriField()">
            <use xlink:href="#ico36_close" href="#ico36_close"></use>
         </svg>
         <svg class="suggestpostp__input__icon hspinner"
            ng-if="self.uriToFetchLoading">
            <use xlink:href="#ico_loading_anim" href="#ico_loading_anim"></use>
         </svg>
         <input
            type="url"
            placeholder="{{self.detail.placeholder}}"
            class="suggestpostp__input__inner"
            won-input="::self.updateFetchNeedUriField()"/>
      </div>
      <div class="suggestpostp__error" ng-if="self.uriToFetchFailedToLoad && self.fetchNeedUriFieldHasText()">
          Failed to Load Suggestion, might not be a valid uri.
      </div>
      <div class="suggestpostp__error" ng-if="self.uriToFetchIsWhatsAround && self.fetchNeedUriFieldHasText()">
          Suggestion invalid, you are trying to share a What's Around Need.
      </div>
      <div class="suggestpostp__error" ng-if="self.uriToFetchIsExcluded && self.fetchNeedUriFieldHasText()">
          {{ self.excludedText }}
      </div>
      <div class="suggestpostp__error" ng-if="self.uriToFetchIsNotAllowed && self.fetchNeedUriFieldHasText()">
          {{ self.notAllowedFacetText }}
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.domCache = new DomCache(this.$element);

      window.suggestpostp4dbg = this;

      this.showFetchButton = false;
      this.showResetButton = false;

      this.uriToFetch = undefined;

      const selectFromState = state => {
        const suggestedNeedUri = this.initialValue;
        const allActiveNeeds = getActiveNeeds(state);

        const allSuggestableNeeds =
          allActiveNeeds &&
          allActiveNeeds.filter(need => this.isSuggestable(need));

        const allForbiddenNeeds =
          allActiveNeeds &&
          allActiveNeeds.filter(need => !this.isSuggestable(need));

        const suggestedNeed = get(allSuggestableNeeds, suggestedNeedUri);
        const sortedActiveNeeds =
          allSuggestableNeeds &&
          sortBy(allSuggestableNeeds, elem =>
            (elem.get("humanReadable") || "").toLowerCase()
          );

        const uriToFetchProcess = getIn(state, [
          "process",
          "needs",
          this.uriToFetch,
        ]);
        const uriToFetchLoading = !!get(uriToFetchProcess, "loading");
        const uriToFetchFailedToLoad = !!get(uriToFetchProcess, "failedToLoad");
        const uriToFetchIsWhatsAround = needUtils.isWhatsAroundNeed(
          get(allForbiddenNeeds, this.uriToFetch)
        );
        const uriToFetchIsNotAllowed =
          !!get(allForbiddenNeeds, this.uriToFetch) &&
          !this.hasAtLeastOneAllowedFacet(
            get(allForbiddenNeeds, this.uriToFetch)
          );
        const uriToFetchIsExcluded = this.isExcludedNeed(
          get(allForbiddenNeeds, this.uriToFetch)
        );

        return {
          suggestedNeedUri,
          uriToFetchLoading,
          uriToFetchFailedToLoad,
          uriToFetchIsWhatsAround,
          uriToFetchIsExcluded,
          uriToFetchIsNotAllowed,
          allSuggestableNeeds,
          allForbiddenNeeds,
          suggestionsAvailable:
            allSuggestableNeeds && allSuggestableNeeds.size > 0,
          sortedActiveNeeds,
          suggestedNeed,
          noSuggestionsLabel:
            this.noSuggestionsText || "No Needs available to suggest",
          uriToFetchSuccess:
            this.uriToFetch &&
            !uriToFetchLoading &&
            !uriToFetchFailedToLoad &&
            get(allSuggestableNeeds, this.uriToFetch),
          uriToFetchFailed:
            this.uriToFetch &&
            !uriToFetchLoading &&
            (uriToFetchFailedToLoad ||
              uriToFetchIsWhatsAround ||
              uriToFetchIsExcluded ||
              uriToFetchIsNotAllowed),
        };
      };

      connect2Redux(
        selectFromState,
        actionCreators,
        [
          "self.initialValue",
          "self.detail",
          "self.uriToFetch",
          "self.excludedUris",
          "self.allowedFacets",
          "self.notAllowedFacetText",
          "self.excludedText",
          "self.noSuggestionsText",
        ],
        this
      );

      this.$scope.$watch(
        () => this.uriToFetchSuccess,
        () =>
          delay(0).then(() => {
            if (this.uriToFetchSuccess) {
              this.update(this.uriToFetch);
              this.resetNeedUriField();
            }
          })
      );
    }

    hasAtLeastOneAllowedFacet(need) {
      if (this.allowedFacets) {
        const allowedFacetsImm = Immutable.fromJS(this.allowedFacets);
        const needFacetsImm = need && need.getIn(["content", "facets"]);

        return (
          needFacetsImm &&
          needFacetsImm.find(facet => allowedFacetsImm.contains(facet))
        );
      }
      return true;
    }

    isExcludedNeed(need) {
      if (this.excludedUris) {
        const excludedUrisImm = Immutable.fromJS(this.excludedUris);

        return excludedUrisImm.contains(get(need, "uri"));
      }
      return false;
    }

    isSuggestable(need) {
      return (
        !needUtils.isWhatsAroundNeed(need) &&
        !this.isExcludedNeed(need) &&
        this.hasAtLeastOneAllowedFacet(need)
      );
    }

    isSelected(need) {
      return (
        need &&
        this.suggestedNeed &&
        need.get("uri") === this.suggestedNeed.get("uri")
      );
    }

    /**
     * Checks validity and uses callback method
     */
    update(title) {
      if (title && title.trim().length > 0) {
        this.onUpdate({ value: title });
      } else {
        this.onUpdate({ value: undefined });
      }
    }

    updateFetchNeedUriField() {
      const text = this.fetchUriField().value;
      this.uriToFetch = undefined;

      if (text && text.trim().length > 0) {
        if (this.fetchUriField().checkValidity()) {
          this.showResetButton = false;
          this.showFetchButton = true;
        } else {
          this.showResetButton = true;
          this.showFetchButton = false;
        }
      }
    }

    fetchNeedUriFieldHasText() {
      const text = this.fetchUriField().value;
      return text && text.length > 0;
    }

    resetNeedUriField() {
      this.fetchUriField().value = "";
      this.showResetButton = false;
      this.showFetchButton = false;
      this.uriToFetch = undefined;
    }

    fetchNeed() {
      let uriToFetch = this.fetchUriField().value;
      uriToFetch = uriToFetch.trim();

      if (
        !getIn(this.allSuggestableNeeds, uriToFetch) &&
        !get(this.allForbiddenNeeds, uriToFetch)
      ) {
        this.uriToFetch = uriToFetch;
        this.needs__fetchUnloadedNeed(uriToFetch);
      } else if (get(this.allForbiddenNeeds, uriToFetch)) {
        this.uriToFetch = uriToFetch;
      } else {
        this.update(uriToFetch);
      }
    }

    fetchUriField() {
      if (!this._fetchUriInput) {
        this._fetchUriInput = this.$element[0].querySelector(
          ".suggestpostp__input__inner"
        );
      }
      return this._fetchUriInput;
    }

    selectNeed(need) {
      const needUri = get(need, "uri");

      if (needUri) {
        this.update(needUri);
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
      onUpdate: "&",
      initialValue: "=",
      detail: "=",
      excludedUris: "=", //list of uris that should be excluded from the suggestions
      allowedFacets: "=", //list of facets where at least one facet needs to be present in the need for it to be allowed as a suggestion
      notAllowedFacetText: "=", //error message to display if need does not have any allowed facets
      excludedText: "=", //error message to display when excluded need is added via the fetch input
      noSuggestionsText: "=", //Text to display when no suggestions are available
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.suggestpostPicker", [
    wonInput,
    postHeaderModule,
    labelledHrModule,
  ])
  .directive("wonSuggestpostPicker", genComponentConf).name;
