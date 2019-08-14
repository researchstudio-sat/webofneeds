import angular from "angular";
import "ng-redux";
import Immutable from "immutable";
import { delay, get, getIn, sortBy } from "../../../utils.js";
import { attach } from "../../../cstm-ng-utils.js";
import wonInput from "../../../directives/input.js";
import { connect2Redux } from "../../../configRedux.js";
import { actionCreators } from "../../../actions/actions.js";
import { getActiveAtoms } from "../../../redux/selectors/general-selectors.js";
import WonAtomHeader from "../../atom-header.jsx";

import "~/style/_suggestpostpicker.scss";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
      <div class="suggestpostp__posts" ng-if="self.suggestionsAvailable">
        <div class="suggestpostp__posts__post clickable"
          ng-class="{'won--selected': self.isSelected(atom)}"
          ng-repeat="atom in self.sortedActiveAtoms"
          ng-click="self.selectAtom(atom)">
          <won-preact
              class="atomHeader"
              component="self.WonAtomHeader"
              props="{atomUri: atom.get('uri')}">
          </won-preact>
        </div>
      </div>
      <div class="suggestpostp__noposts" ng-if="!self.suggestionsAvailable">
        {{ self.noSuggestionsLabel }}
      </div>
      <won-preact component="self.WonLabelledHr" class="labelledHr suggestpostp__labelledhr" props="{label: 'Not happy with the options? Add an Atom-URI below'}"></won-preact>
      <div class="suggestpostp__input">
         <svg class="suggestpostp__input__icon clickable"
            style="--local-primary:var(--won-primary-color);"
            ng-if="!self.uriToFetchLoading && self.showFetchButton && !self.uriToFetchFailed && self.fetchAtomUriFieldHasText()"
            ng-click="self.fetchAtom()">
            <use xlink:href="#ico16_checkmark" href="#ico16_checkmark"></use>
         </svg>
         <svg class="suggestpostp__input__icon clickable"
            style="--local-primary:var(--won-primary-color);"
            ng-if="!self.uriToFetchLoading && (self.showResetButton || self.uriToFetchFailed) && self.fetchAtomUriFieldHasText()"
            ng-click="self.resetAtomUriField()">
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
            won-input="::self.updateFetchAtomUriField()"/>
      </div>
      <div class="suggestpostp__error" ng-if="self.uriToFetchFailedToLoad && self.fetchAtomUriFieldHasText()">
          Failed to Load Suggestion, might not be a valid uri.
      </div>
      <div class="suggestpostp__error" ng-if="self.uriToFetchIsExcluded && self.fetchAtomUriFieldHasText()">
          {{ self.excludedText }}
      </div>
      <div class="suggestpostp__error" ng-if="self.uriToFetchIsNotAllowed && self.fetchAtomUriFieldHasText()">
          {{ self.notAllowedSocketText }}
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);

      window.suggestpostp4dbg = this;

      this.showFetchButton = false;
      this.showResetButton = false;

      this.uriToFetch = undefined;

      this.WonAtomHeader = WonAtomHeader;

      const selectFromState = state => {
        const suggestedAtomUri = this.initialValue;
        const allActiveAtoms = getActiveAtoms(state);

        const allSuggestableAtoms =
          allActiveAtoms &&
          allActiveAtoms.filter(atom => this.isSuggestable(atom));

        const allForbiddenAtoms =
          allActiveAtoms &&
          allActiveAtoms.filter(atom => !this.isSuggestable(atom));

        const suggestedAtom = get(allSuggestableAtoms, suggestedAtomUri);
        const sortedActiveAtoms =
          allSuggestableAtoms &&
          sortBy(allSuggestableAtoms, elem =>
            (elem.get("humanReadable") || "").toLowerCase()
          );

        const uriToFetchProcess = getIn(state, [
          "process",
          "atoms",
          this.uriToFetch,
        ]);
        const uriToFetchLoading = !!get(uriToFetchProcess, "loading");
        const uriToFetchFailedToLoad = !!get(uriToFetchProcess, "failedToLoad");
        const uriToFetchIsNotAllowed =
          !!get(allForbiddenAtoms, this.uriToFetch) &&
          !this.hasAtLeastOneAllowedSocket(
            get(allForbiddenAtoms, this.uriToFetch)
          );
        const uriToFetchIsExcluded = this.isExcludedAtom(
          get(allForbiddenAtoms, this.uriToFetch)
        );

        return {
          suggestedAtomUri,
          uriToFetchLoading,
          uriToFetchFailedToLoad,
          uriToFetchIsExcluded,
          uriToFetchIsNotAllowed,
          allSuggestableAtoms,
          allForbiddenAtoms,
          suggestionsAvailable:
            allSuggestableAtoms && allSuggestableAtoms.size > 0,
          sortedActiveAtoms,
          suggestedAtom,
          noSuggestionsLabel:
            this.noSuggestionsText || "No Atoms available to suggest",
          uriToFetchSuccess:
            this.uriToFetch &&
            !uriToFetchLoading &&
            !uriToFetchFailedToLoad &&
            get(allSuggestableAtoms, this.uriToFetch),
          uriToFetchFailed:
            this.uriToFetch &&
            !uriToFetchLoading &&
            (uriToFetchFailedToLoad ||
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
          "self.allowedSockets",
          "self.notAllowedSocketText",
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
              this.resetAtomUriField();
            }
          })
      );
    }

    hasAtLeastOneAllowedSocket(atom) {
      if (this.allowedSockets) {
        const allowedSocketsImm = Immutable.fromJS(this.allowedSockets);
        const atomSocketsImm = atom && atom.getIn(["content", "sockets"]);

        return (
          atomSocketsImm &&
          atomSocketsImm.find(socket => allowedSocketsImm.contains(socket))
        );
      }
      return true;
    }

    isExcludedAtom(atom) {
      if (this.excludedUris) {
        const excludedUrisImm = Immutable.fromJS(this.excludedUris);

        return excludedUrisImm.contains(get(atom, "uri"));
      }
      return false;
    }

    isSuggestable(atom) {
      return (
        !this.isExcludedAtom(atom) && this.hasAtLeastOneAllowedSocket(atom)
      );
    }

    isSelected(atom) {
      return (
        atom &&
        this.suggestedAtom &&
        atom.get("uri") === this.suggestedAtom.get("uri")
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

    updateFetchAtomUriField() {
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

    fetchAtomUriFieldHasText() {
      const text = this.fetchUriField().value;
      return text && text.length > 0;
    }

    resetAtomUriField() {
      this.fetchUriField().value = "";
      this.showResetButton = false;
      this.showFetchButton = false;
      this.uriToFetch = undefined;
    }

    fetchAtom() {
      let uriToFetch = this.fetchUriField().value;
      uriToFetch = uriToFetch.trim();

      if (
        !getIn(this.allSuggestableAtoms, uriToFetch) &&
        !get(this.allForbiddenAtoms, uriToFetch)
      ) {
        this.uriToFetch = uriToFetch;
        this.atoms__fetchUnloadedAtom(uriToFetch);
      } else if (get(this.allForbiddenAtoms, uriToFetch)) {
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

    selectAtom(atom) {
      const atomUri = get(atom, "uri");

      if (atomUri) {
        this.update(atomUri);
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
      allowedSockets: "=", //list of sockets where at least one socket needs to be present in the atom for it to be allowed as a suggestion
      notAllowedSocketText: "=", //error message to display if atom does not have any allowed sockets
      excludedText: "=", //error message to display when excluded atom is added via the fetch input
      noSuggestionsText: "=", //Text to display when no suggestions are available
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.suggestpostPicker", [wonInput])
  .directive("wonSuggestpostPicker", genComponentConf).name;
