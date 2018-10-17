import angular from "angular";
import "ng-redux";
import { attach, sortBy } from "../../../utils.js";
import { DomCache } from "../../../cstm-ng-utils.js";
import wonInput from "../../../directives/input.js";
import { connect2Redux } from "../../../won-utils.js";
import { actionCreators } from "../../../actions/actions.js";
import postHeaderModule from "../../post-header.js";
import {
  selectOpenConnectionUri,
  selectNeedByConnectionUri,
  selectAllOpenNeeds,
} from "../../../selectors.js";

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
      <div class="suggestpostp__input">
         <svg class="suggestpostp__input__icon clickable"
            style="--local-primary:var(--won-primary-color);"
            ng-if="self.showResetButton"
            ng-click="self.resetTitle()">
            <use xlink:href="#ico36_close" href="#ico36_close"></use>
         </svg>
         <input
            type="text"
            class="suggestpostp__input__inner"
            won-input="::self.updateTitle()" />
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.domCache = new DomCache(this.$element);

      window.suggestpostp4dbg = this;

      this.showResetButton = false;

      const selectFromState = state => {
        const openedConnectionUri = selectOpenConnectionUri(state);
        const openedOwnPost =
          openedConnectionUri &&
          selectNeedByConnectionUri(state, openedConnectionUri);
        const connection =
          openedOwnPost &&
          openedOwnPost.getIn(["connections", openedConnectionUri]);

        const openedOwnPostUri = openedOwnPost && openedOwnPost.get("uri");
        const openedTheirPostUri =
          connection && connection.get("remoteNeedUri");

        const addedTitle = this.initialValue;
        const allOpenNeeds = selectAllOpenNeeds(state);

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
          addedTitle &&
          allOpenNeedsWithoutCurrent.get(addedTitle);

        const sortedOpenNeeds =
          allOpenNeedsWithoutCurrent && sortBy(allOpenNeedsWithoutCurrent);

        return {
          addedTitle,
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

    updateTitle() {
      const text = this.textfield().value;

      if (text && text.trim().length > 0) {
        this.addedTitle = text.trim();
        this.update(this.addedTitle);
        this.showResetButton = true;
      } else {
        this.resetTitle();
      }
    }

    resetTitle() {
      this.addedTitle = undefined;
      this.textfield().value = "";
      this.update(undefined);
      this.showResetButton = false;
    }

    textfieldNg() {
      return angular.element(this.textfield());
    }
    textfield() {
      if (!this._titleInput) {
        this._titleInput = this.$element[0].querySelector(
          ".suggestpostp__input__inner"
        );
      }
      return this._titleInput;
    }

    selectPost(post) {
      console.log("TODO: IMPL POST SELECTION: ", post);
      const postUri = post && post.get("uri");

      if (postUri) {
        this.textfield().value = postUri;
        this.initialValue = postUri;
        this.updateTitle();
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
  ])
  .directive("wonSuggestpostPicker", genComponentConf).name;
