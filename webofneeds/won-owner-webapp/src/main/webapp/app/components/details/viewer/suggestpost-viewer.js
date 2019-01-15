import angular from "angular";
import "ng-redux";
import { actionCreators } from "../../../actions/actions.js";
import postHeaderModule from "../../post-header.js";
import { attach } from "../../../utils.js";
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
          <div class="suggestpostv__content__post" ng-if="self.suggestedPost">
            <won-post-header
               
                need-uri="self.suggestedPost.get('uri')"
                timestamp="self.suggestedPost.get('creationDate')"
             
                hide-image="::false">
            </won-post-header>
          </div>
          <div class="suggestpostv__content__notloaded" ng-if="!self.suggestedPost">
              Suggestion not loaded yet. Click the Button below to retrieve the Suggested Post.
          </div>
          <button class="suggestpostv__content__action won-button--outlined thin red"
              ng-if="!self.suggestedPost"
              ng-click="self.loadPost()">
              Load Post
          </button>
          <button class="suggestpostv__content__action won-button--outlined thin red"
              ng-if="self.suggestedPost && !self.suggestedPost.get('isOwned') && self.openedOwnPost"
              ng-disabled="self.hasConnectionBetweenPosts"
              ng-click="self.connectWithPost()">
              {{ self.getConnectButtonLabel() }}
          </button>
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
        const connection =
          openedOwnPost &&
          openedOwnPost.getIn(["connections", openedConnectionUri]);

        const suggestedPost = state.getIn(["needs", this.content]);
        const suggestedPostUri = suggestedPost && suggestedPost.get("uri");

        const connectionsOfOpenedOwnPost =
          openedOwnPost && openedOwnPost.get("connections");
        const connectionsBetweenPosts =
          suggestedPostUri &&
          connectionsOfOpenedOwnPost &&
          connectionsOfOpenedOwnPost.filter(
            conn => conn.get("remoteNeedUri") === suggestedPostUri
          );

        return {
          suggestedPost,
          openedOwnPost,
          multiSelectType: connection && connection.get("multiSelectType"),
          hasConnectionBetweenPosts:
            connectionsBetweenPosts && connectionsBetweenPosts.size > 0,
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
        : "Connect with Post";
    }

    loadPost() {
      if (this.content) {
        //this.content is the suggestedPostUri
        this.needs__fetchUnloadedNeed(this.content);
      }
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
