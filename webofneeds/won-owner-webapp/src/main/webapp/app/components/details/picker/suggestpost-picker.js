import angular from "angular";
import "ng-redux";
import { attach, sortBy, get, getIn, delay } from "../../../utils.js";
import { DomCache } from "../../../cstm-ng-utils.js";
import wonInput from "../../../directives/input.js";
import { connect2Redux } from "../../../won-utils.js";
import { actionCreators } from "../../../actions/actions.js";
import postHeaderModule from "../../post-header.js";
import labelledHrModule from "../../labelled-hr.js";
import {
  getConnectionUriFromRoute,
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
          ng-repeat="post in self.sortedOpenPosts"
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
            ng-if="!self.suggestedPostLoading && self.showFetchButton && !self.suggestedPostFailedToLoad"
            ng-click="self.fetchPost()">
            <use xlink:href="#ico16_checkmark" href="#ico16_checkmark"></use>
         </svg>
         <svg class="suggestpostp__input__icon clickable"
            style="--local-primary:var(--won-primary-color);"
            ng-if="!self.suggestedPostLoading && (self.showResetButton || self.suggestedPostFailedToLoad) && self.fetchPostUriFieldHasText()"
            ng-click="self.resetPostUriField()">
            <use xlink:href="#ico36_close" href="#ico36_close"></use>
         </svg>
         <svg class="suggestpostp__input__icon hspinner"
            ng-if="self.suggestedPostLoading">
            <use xlink:href="#ico_loading_anim" href="#ico_loading_anim"></use>
         </svg>
         <input
            type="url"
            placeholder="{{self.detail.placeholder}}"
            class="suggestpostp__input__inner"
            won-input="::self.updateFetchPostUriField()"/>
      </div>
      <div class="suggestpostp__error" ng-if="self.suggestedPostFailedToLoad && self.fetchPostUriFieldHasText()">
        Failed to Load Post, might not be a valid uri.
      </div>
      <div class="suggestpostp__error" ng-if="self.suggestedPostIsOpenedPost && self.fetchPostUriFieldHasText()">
        Suggestion invalid, you are trying to suggest your own post, they already know about it.
      </div>
      <div class="suggestpostp__error" ng-if="self.suggestedPostIsTheirPost && self.fetchPostUriFieldHasText()">
        Suggestion invalid, you are trying to suggest their post, they already know about it.
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
        const openedConnectionUri = getConnectionUriFromRoute(state);
        const openedOwnPost =
          openedConnectionUri &&
          getOwnedNeedByConnectionUri(state, openedConnectionUri);
        const connection = getIn(openedOwnPost, [
          "connections",
          openedConnectionUri,
        ]);

        const openedOwnPostUri = get(openedOwnPost, "uri");
        const openedTheirPostUri = get(connection, "remoteNeedUri");

        const suggestedPostUri = this.initialValue;
        const allOpenPosts = getOpenPosts(state);

        const allOpenPostsWithoutCurrent =
          allOpenPosts &&
          openedOwnPostUri &&
          openedTheirPostUri &&
          allOpenPosts.filter(
            post =>
              post.get("uri") != openedOwnPostUri &&
              post.get("uri") != openedTheirPostUri
          );
        const suggestedPost = get(allOpenPostsWithoutCurrent, suggestedPostUri);
        const sortedOpenPosts =
          allOpenPostsWithoutCurrent &&
          sortBy(allOpenPostsWithoutCurrent, elem =>
            (elem.get("humanReadable") || "").toLowerCase()
          );

        const suggestedPostProcess = getIn(state, [
          "process",
          "needs",
          this.uriToFetch,
        ]);
        const suggestedPostLoading = !!get(suggestedPostProcess, "loading");
        const suggestedPostFailedToLoad = !!get(
          suggestedPostProcess,
          "failedToLoad"
        );
        const suggestedPostIsOpenedPost = this.uriToFetch === openedOwnPostUri;
        const suggestedPostIsTheirPost = this.uriToFetch === openedTheirPostUri;

        return {
          suggestedPostUri,
          suggestedPostLoading,
          suggestedPostFailedToLoad,
          suggestedPostIsOpenedPost,
          suggestedPostIsTheirPost,
          allOpenPostsWithoutCurrent,
          suggestionsAvailable:
            allOpenPostsWithoutCurrent && allOpenPostsWithoutCurrent.size > 0,
          sortedOpenPosts,
          suggestedPost,
          uriToFetchSuccess:
            this.uriToFetch &&
            !suggestedPostLoading &&
            !suggestedPostFailedToLoad,
        };
      };

      connect2Redux(
        selectFromState,
        actionCreators,
        ["self.initialValue", "self.detail"],
        this
      );

      this.$scope.$watch(
        () => this.uriToFetchSuccess,
        () =>
          delay(0).then(() => {
            if (this.uriToFetchSuccess) {
              this.update(this.uriToFetch);
              this.resetPostUriField();
            }
          })
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

    fetchPostUriFieldHasText() {
      const text = this.fetchUriField().value;
      return text && text.length > 0;
    }

    resetPostUriField() {
      this.fetchUriField().value = "";
      this.showResetButton = false;
      this.showFetchButton = false;
      this.uriToFetch = undefined;
    }

    fetchPost() {
      let uriToFetch = this.fetchUriField().value;
      uriToFetch = uriToFetch.trim();

      //TODO: ERROR HANDLING IF URL WAS NOT A FETCHABLE URL
      if (!getIn(this.allOpenPostsWithoutCurrent, uriToFetch)) {
        this.uriToFetch = uriToFetch;
        this.needs__fetchUnloadedNeed(uriToFetch);
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
