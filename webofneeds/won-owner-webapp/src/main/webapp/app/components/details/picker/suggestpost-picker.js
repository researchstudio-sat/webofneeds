import angular from "angular";
import "ng-redux";
import { attach, sortBy } from "../../../utils.js";
import { DomCache } from "../../../cstm-ng-utils.js";
import wonInput from "../../../directives/input.js";
import { connect2Redux } from "../../../won-utils.js";
import { actionCreators } from "../../../actions/actions.js";
import postHeaderModule from "../../post-header.js";
import labelledHrModule from "../../labelled-hr.js";
import {
  selectOpenConnectionUri,
  getOwnedNeedByConnectionUri,
  getOpenPosts,
} from "../../../selectors/general-selectors.js";

import "style/_suggestpostpicker.scss";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
      <div class="suggestpostp__posts" ng-if="self.suggestionsAvailable">
        <div class="suggestpostp__posts__post clickable"
          ng-class="{'won--selected': self.isSelected(post)}"
          ng-repeat="post in self.sortedOpenNeeds"
          ng-click="self.selectPost(post)">
          <won-post-header
              need-uri="post.get('uri')"
              timestamp="post.get('creationDate')"
              hide-image="::false">
          </won-post-header>
        </div>
      </div>
      <div class="suggestpostp__noposts" ng-if="!self.suggestionsAvailable">
        No Posts available to suggest
      </div>
      <won-labelled-hr label="::'Not happy with the options? Add a Post-URI below'" class="suggestpostp__labelledhr"></won-labelled-hr>
      <div class="suggestpostp__input">
         <svg class="suggestpostp__input__icon clickable"
            style="--local-primary:var(--won-primary-color);"
            ng-if="self.showFetchButton"
            ng-click="self.fetchPost()">
            <use xlink:href="#ico16_checkmark" href="#ico16_checkmark"></use>
         </svg>
         <svg class="suggestpostp__input__icon clickable"
            style="--local-primary:var(--won-primary-color);"
            ng-if="self.showResetButton"
            ng-click="self.resetPostUriField()">
            <use xlink:href="#ico36_close" href="#ico36_close"></use>
         </svg>
         <input
            type="url"
            placeholder="{{self.detail.placeholder}}"
            class="suggestpostp__input__inner"
            won-input="::self.updateFetchPostUriField()"/>
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.domCache = new DomCache(this.$element);

      window.suggestpostp4dbg = this;

      this.showFetchButton = false;
      this.showResetButton = false;

      const selectFromState = state => {
        const openedConnectionUri = selectOpenConnectionUri(state);
        const openedOwnPost =
          openedConnectionUri &&
          getOwnedNeedByConnectionUri(state, openedConnectionUri);
        const connection =
          openedOwnPost &&
          openedOwnPost.getIn(["connections", openedConnectionUri]);

        const openedOwnPostUri = openedOwnPost && openedOwnPost.get("uri");
        const openedTheirPostUri =
          connection && connection.get("remoteNeedUri");

        const suggestedPostUri = this.initialValue;
        const allOpenNeeds = getOpenPosts(state);

        const allOpenNeedsWithoutCurrent =
          allOpenNeeds &&
          openedOwnPostUri &&
          allOpenNeeds.filter(
            post =>
              post.get("uri") != openedOwnPostUri &&
              post.get("uri") != openedTheirPostUri
          );
        const suggestedPost =
          allOpenNeedsWithoutCurrent &&
          suggestedPostUri &&
          allOpenNeedsWithoutCurrent.get(suggestedPostUri);

        const sortedOpenNeeds =
          allOpenNeedsWithoutCurrent &&
          sortBy(allOpenNeedsWithoutCurrent, elem =>
            (elem.get("humanReadable") || "").toLowerCase()
          );

        return {
          suggestedPostUri,
          suggestionsAvailable:
            allOpenNeedsWithoutCurrent && allOpenNeedsWithoutCurrent.size > 0,
          sortedOpenNeeds,
          suggestedPost,
        };
      };

      connect2Redux(
        selectFromState,
        actionCreators,
        ["self.initialValue", "self.detail"],
        this
      );
    }

    isSelected(post) {
      return (
        post &&
        this.suggestedPost &&
        post.get("uri") === this.suggestedPost.get("uri")
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

    updateFetchPostUriField() {
      const text = this.fetchUriField().value;

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

    resetPostUriField() {
      this.fetchUriField().value = "";
      this.showResetButton = false;
      this.showFetchButton = false;
    }

    fetchPost() {
      let uriToFetch = this.fetchUriField().value;
      uriToFetch = uriToFetch.trim();

      //TODO: ERROR HANDLING IF URL WAS NOT A FETCHABLE URL
      this.needs__fetchSuggested(uriToFetch);
      //this.update(uriToFetch);
      this.resetPostUriField();
    }

    fetchUriField() {
      if (!this._fetchUriInput) {
        this._fetchUriInput = this.$element[0].querySelector(
          ".suggestpostp__input__inner"
        );
      }
      return this._fetchUriInput;
    }

    selectPost(post) {
      const postUri = post && post.get("uri");

      if (postUri) {
        this.update(postUri);
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
