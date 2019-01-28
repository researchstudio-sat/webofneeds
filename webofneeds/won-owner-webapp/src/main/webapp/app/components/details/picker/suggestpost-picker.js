import angular from "angular";
import "ng-redux";
import { attach, sortBy, get, getIn, delay } from "../../../utils.js";
import { DomCache } from "../../../cstm-ng-utils.js";
import wonInput from "../../../directives/input.js";
import { connect2Redux } from "../../../won-utils.js";
import { actionCreators } from "../../../actions/actions.js";
import postHeaderModule from "../../post-header.js";
import labelledHrModule from "../../labelled-hr.js";
import { getActiveNeeds } from "../../../selectors/general-selectors.js";
import { isWhatsAroundNeed, isWhatsNewNeed } from "../../../need-utils.js";

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
        No Needs available to suggest
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
      <div class="suggestpostp__error" ng-if="self.uriToFetchIsWhatsNew && self.fetchNeedUriFieldHasText()">
          Suggestion invalid, you are trying to share a What's New Need.
      </div>
      <div class="suggestpostp__error" ng-if="self.uriToFetchIsWhatsAround && self.fetchNeedUriFieldHasText()">
          Suggestion invalid, you are trying to share a What's Around Need.
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
        const uriToFetchIsWhatsNew = isWhatsNewNeed(
          get(allForbiddenNeeds, this.uriToFetch)
        );
        const uriToFetchIsWhatsAround = isWhatsAroundNeed(
          get(allForbiddenNeeds, this.uriToFetch)
        );

        return {
          suggestedNeedUri,
          uriToFetchLoading,
          uriToFetchFailedToLoad,
          uriToFetchIsWhatsNew,
          uriToFetchIsWhatsAround,
          allSuggestableNeeds,
          allForbiddenNeeds,
          suggestionsAvailable:
            allSuggestableNeeds && allSuggestableNeeds.size > 0,
          sortedActiveNeeds,
          suggestedNeed,
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
              uriToFetchIsWhatsNew),
        };
      };

      connect2Redux(
        selectFromState,
        actionCreators,
        ["self.initialValue", "self.detail", "self.uriToFetch"],
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

    isSuggestable(need) {
      return !isWhatsAroundNeed(need) && !isWhatsNewNeed(need);
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
