import angular from "angular";
import "ng-redux";
import { actionCreators } from "../../../actions/actions.js";
import postHeaderModule from "../../post-header.js";
import { attach, getIn, get } from "../../../utils.js";
import { connect2Redux } from "../../../won-utils.js";
import * as atomUtils from "../../../atom-utils.js";
import * as generalSelectors from "../../../selectors/general-selectors.js";

import "style/_suggestpost-viewer.scss";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
        <div class="suggestpostv__header">
          <svg class="suggestpostv__header__icon" ng-if="self.detail.icon">
              <use xlink:href={{self.detail.icon}} href={{self.detail.icon}}></use>
          </svg>
          <span class="suggestpostv__header__label" ng-if="self.detail.label">{{self.detail.label}}</span>
        </div>
        <div class="suggestpostv__content">
          <div class="suggestpostv__content__post">
            <won-post-header
                class="clickable"
                atom-uri="self.content"
                ng-click="self.router__stateGoCurrent({viewAtomUri: self.content, viewConnUri: undefined})"
                ng-disabled="!self.fetchedSuggestion">
            </won-post-header>
            <button class="suggestpostv__content__post__action won-button--outlined thin red"
              ng-if="self.failedToLoad"
              ng-click="self.reloadSuggestion()">
              Reload
            </button>
            <button class="suggestpostv__content__post__action won-button--outlined thin red"
              ng-if="self.showConnectAction"
              ng-click="self.connectWithPost()">
              Connect
            </button>
            <button class="suggestpostv__content__post__action won-button--outlined thin red"
              ng-if="self.showJoinAction"
              ng-click="self.connectWithPost()">
              Join
            </button>
            <button class="suggestpostv__content__post__action won-button--outlined thin red"
              ng-if="self.hasConnectionBetweenPosts"
              ng-click="self.router__stateGoCurrent({connectionUri: self.establishedConnectionUri})">
              View Chat
            </button>
            <div class="suggestpostv__content__post__info">
              {{ self.getInfoText() }}
            </div>
          </div>
        </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      window.suggestpostv4dbg = this;

      const selectFromState = state => {
        const openedConnectionUri = generalSelectors.getConnectionUriFromRoute(
          state
        );
        const openedOwnPost =
          openedConnectionUri &&
          generalSelectors.getOwnedAtomByConnectionUri(
            state,
            openedConnectionUri
          );
        const connection = getIn(openedOwnPost, [
          "connections",
          openedConnectionUri,
        ]);

        const suggestedPost = getIn(state, ["atoms", this.content]);
        const suggestedPostUri = get(suggestedPost, "uri");

        const connectionsOfOpenedOwnPost = get(openedOwnPost, "connections");
        const connectionsBetweenPosts =
          suggestedPostUri &&
          connectionsOfOpenedOwnPost &&
          connectionsOfOpenedOwnPost.filter(
            conn => conn.get("targetAtomUri") === suggestedPostUri
          );

        const hasConnectionBetweenPosts =
          connectionsBetweenPosts && connectionsBetweenPosts.size > 0;

        const isLoading = state.getIn([
          "process",
          "atoms",
          this.content,
          "loading",
        ]);
        const toLoad = state.getIn([
          "process",
          "atoms",
          this.content,
          "toLoad",
        ]);
        const failedToLoad = state.getIn([
          "process",
          "atoms",
          this.content,
          "failedToLoad",
        ]);

        const fetchedSuggestion = !isLoading && !toLoad && !failedToLoad;
        const isSuggestedOwned = generalSelectors.isAtomOwned(
          state,
          suggestedPostUri
        );

        return {
          suggestedPost,
          openedOwnPost,
          hasChatSocket: atomUtils.hasChatSocket(suggestedPost),
          hasGroupSocket: atomUtils.hasGroupSocket(suggestedPost),
          isSuggestedOwned,
          showConnectAction:
            suggestedPost &&
            fetchedSuggestion &&
            atomUtils.isActive(suggestedPost) &&
            !atomUtils.hasGroupSocket(suggestedPost) &&
            atomUtils.hasChatSocket(suggestedPost) &&
            !hasConnectionBetweenPosts &&
            !isSuggestedOwned &&
            openedOwnPost,
          showJoinAction:
            suggestedPost &&
            fetchedSuggestion &&
            atomUtils.isActive(suggestedPost) &&
            atomUtils.hasGroupSocket(suggestedPost) &&
            !atomUtils.hasChatSocket(suggestedPost) &&
            !hasConnectionBetweenPosts &&
            !isSuggestedOwned &&
            openedOwnPost,
          isLoading,
          toLoad,
          failedToLoad,
          multiSelectType: connection && connection.get("multiSelectType"),
          hasConnectionBetweenPosts,
          establishedConnectionUri:
            hasConnectionBetweenPosts &&
            get(connectionsBetweenPosts.first(), "uri"),
        };
      };

      connect2Redux(
        selectFromState,
        actionCreators,
        ["self.content", "self.detail"],
        this
      );
    }

    getConnectButtonLabel() {
      return this.hasConnectionBetweenPosts
        ? "Already Connected With Post"
        : "Request";
    }

    getInfoText() {
      if (this.isLoading) {
        return "Loading Suggestion...";
      } else if (this.toLoad) {
        return "Suggestion marked toLoad";
      } else if (this.failedToLoad) {
        return "Failed to load Suggestion";
      } else if (atomUtils.isInactive(this.suggestedPost)) {
        return "This Suggestion is inactive";
      }

      if (atomUtils.isPersona(this.suggestedPost)) {
        return this.isSuggestedOwned
          ? "This is one of your Personas"
          : "This is someone elses Persona";
      } else if (this.hasConnectionBetweenPosts) {
        return "Already established a Connection with this Suggestion";
      } else if (this.isSuggestedOwned) {
        return "This is one of your own Atoms";
      } else if (this.hasChatSocket && !this.hasGroupSocket) {
        return "Click 'Connect' to connect with this Atom";
      } else if (!this.hasChatSocket && this.hasGroupSocket) {
        return "Click 'Join' to connect with this Group";
      }

      return "Click on the Icon to view Details";
    }

    connectWithPost() {
      const openedOwnPostUri =
        this.openedOwnPost && this.openedOwnPost.get("uri");
      const suggestedPostUri =
        this.suggestedPost && this.suggestedPost.get("uri");

      if (openedOwnPostUri && suggestedPostUri) {
        this.atoms__connect(
          this.openedOwnPost.get("uri"),
          undefined,
          this.suggestedPost.get("uri"),
          "Hey a Friend told me about you, let's chat!"
        );
      } else {
        console.warn(
          "No Connect, either openedOwnPost(Uri) or suggestedPost(Uri) not present"
        );
      }
    }

    reloadSuggestion() {
      if (this.content && this.failedToLoad) {
        this.atoms__fetchUnloadedAtom(this.content);
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
      content: "=",
      detail: "=",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.suggestpostViewer", [postHeaderModule])
  .directive("wonSuggestpostViewer", genComponentConf).name;
