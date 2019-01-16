import angular from "angular";
import "ng-redux";
import { actionCreators } from "../../../actions/actions.js";
import postHeaderModule from "../../post-header.js";
import { attach, getIn, get } from "../../../utils.js";
import {
  isOwned,
  isPersona,
  hasChatFacet,
  hasGroupFacet,
} from "../../../need-utils.js";
import { connect2Redux } from "../../../won-utils.js";
import {
  getConnectionUriFromRoute,
  getOwnedNeedByConnectionUri,
} from "../../../selectors/general-selectors.js";

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
                need-uri="self.content"
                timestamp="self.suggestedPost && self.suggestedPost.get('creationDate')"
                hide-image="::false">
            </won-post-header>
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
        const openedConnectionUri = getConnectionUriFromRoute(state);
        const openedOwnPost =
          openedConnectionUri &&
          getOwnedNeedByConnectionUri(state, openedConnectionUri);
        const connection = getIn(openedOwnPost, [
          "connections",
          openedConnectionUri,
        ]);

        const suggestedPost = getIn(state, ["needs", this.content]);
        const suggestedPostUri = get(suggestedPost, "uri");

        const connectionsOfOpenedOwnPost = get(openedOwnPost, "connections");
        const connectionsBetweenPosts =
          suggestedPostUri &&
          connectionsOfOpenedOwnPost &&
          connectionsOfOpenedOwnPost.filter(
            conn => conn.get("remoteNeedUri") === suggestedPostUri
          );

        const hasConnectionBetweenPosts =
          connectionsBetweenPosts && connectionsBetweenPosts.size > 0;

        const isLoading = state.getIn([
          "process",
          "needs",
          this.content,
          "loading",
        ]);
        const toLoad = state.getIn([
          "process",
          "needs",
          this.content,
          "toLoad",
        ]);
        const failedToLoad = state.getIn([
          "process",
          "needs",
          this.content,
          "failedToLoad",
        ]);

        const fetchedSuggestion = !isLoading && !toLoad && !failedToLoad;

        return {
          suggestedPost,
          openedOwnPost,
          hasChatFacet: hasChatFacet(suggestedPost),
          hasGroupFacet: hasGroupFacet(suggestedPost),
          showConnectAction:
            suggestedPost &&
            fetchedSuggestion &&
            !hasGroupFacet(suggestedPost) &&
            hasChatFacet(suggestedPost) &&
            !hasConnectionBetweenPosts &&
            !isOwned(suggestedPost) &&
            openedOwnPost,
          showJoinAction:
            suggestedPost &&
            fetchedSuggestion &&
            hasGroupFacet(suggestedPost) &&
            !hasChatFacet(suggestedPost) &&
            !hasConnectionBetweenPosts &&
            !isOwned(suggestedPost) &&
            openedOwnPost,
          isLoading,
          toLoad,
          failedToLoad,
          multiSelectType: connection && connection.get("multiSelectType"),
          hasConnectionBetweenPosts,
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
      }

      if (isPersona(this.suggestedPost)) {
        return isOwned(this.suggestedPost)
          ? "This is one of your Personas"
          : "This is someone elses Persona";
      } else if (this.hasConnectionBetweenPosts) {
        return "Already established a Connection with this Suggestion";
      } else if (isOwned(this.suggestedPost)) {
        return "This is one of your own Needs";
      } else if (this.hasChatFacet && !this.hasGroupFacet) {
        return "Click 'Request' to connect with this Need";
      } else if (!this.hasChatFacet && this.hasGroupFacet) {
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
        this.needs__connect(
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
