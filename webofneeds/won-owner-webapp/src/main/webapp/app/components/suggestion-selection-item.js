/**
 * Created by quasarchimaere on 14.02.2019.
 */

import angular from "angular";
import won from "../won-es6.js";
import { attach, getIn } from "../utils.js";
import { connect2Redux } from "../won-utils.js";
import { actionCreators } from "../actions/actions.js";
import {
  getPosts,
  getGroupPostAdminUriFromRoute,
} from "../selectors/general-selectors.js";
import { getChatConnectionsByNeedUri } from "../selectors/connection-selectors.js";
import { classOnComponentRoot } from "../cstm-ng-utils.js";
import {
  isChatConnection,
  isGroupChatConnection,
} from "../connection-utils.js";

import "style/_suggestion-selection-item-line.scss";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
      <svg class="ssi__icon"
          ng-click="self.setOpen()"
          ng-class="{
            'ssi__icon--reads': !self.hasUnreadMatches,
            'ssi__icon--unreads': self.hasUnreadMatches
          }">
          <use xlink:href="#ico36_match" href="#ico36_match"></use>
      </svg>
      <div class="ssi__label" ng-click="self.setOpen()">
        <span>{{ self.matchesCount }} Suggestions</span>
        <span ng-if="self.hasUnreadMatches">, {{ self.unreadMatchesCount }} new</span
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);

      const selectFromState = state => {
        const allPosts = getPosts(state);
        const chatConnectionsByNeedUri =
          this.needUri && getChatConnectionsByNeedUri(state, this.needUri);
        const matches =
          chatConnectionsByNeedUri &&
          chatConnectionsByNeedUri.filter(conn => {
            const remoteNeedUri = conn.get("remoteNeedUri");
            const remoteNeedActiveOrLoading =
              remoteNeedUri &&
              allPosts &&
              allPosts.get(remoteNeedUri) &&
              (getIn(state, ["process", "needs", remoteNeedUri, "loading"]) ||
                allPosts.getIn([remoteNeedUri, "state"]) ===
                  won.WON.ActiveCompacted);

            return (
              remoteNeedActiveOrLoading &&
              (isChatConnection(conn) || isGroupChatConnection(conn)) &&
              conn.get("state") === won.WON.Suggested
            );
          });

        const unreadMatches =
          matches && matches.filter(conn => conn.get("unread"));
        const unreadMatchesCount = unreadMatches ? unreadMatches.size : 0;

        const matchesCount = matches ? matches.size : 0;

        const openGroupChatPostUri = getGroupPostAdminUriFromRoute(state);

        return {
          matchesCount,
          unreadMatchesCount,
          hasUnreadMatches: unreadMatchesCount > 0,
          openGroupChatPostUri,
        };
      };

      connect2Redux(selectFromState, actionCreators, ["self.needUri"], this);

      classOnComponentRoot("selected", () => this.isOpen(), this);
      classOnComponentRoot("won-unread", () => this.hasUnreadMatches, this);
    }
    isOpen() {
      return this.openGroupChatPostUri === this.needUri;
    }

    setOpen() {
      this.onSelected({ needUri: this.needUri }); //trigger callback with scope-object
    }
  }
  Controller.$inject = serviceDependencies;
  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      needUri: "=",
      onSelected: "&",
    },
    template: template,
  };
}
export default angular
  .module("won.owner.components.SuggestionSelectionItem", [])
  .directive("wonSuggestionSelectionItem", genComponentConf).name;
