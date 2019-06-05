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
  getPostUriFromRoute,
} from "../selectors/general-selectors.js";
import * as connectionSelectors from "../selectors/connection-selectors.js";
import { classOnComponentRoot } from "../cstm-ng-utils.js";
import * as connectionUtils from "../connection-utils.js";

import "~/style/_suggestion-selection-item-line.scss";

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
      <div class="ssi__right" ng-click="self.setOpen()">
        <div class="ssi__right__topline">
          <div class="ssi__right__topline__title">
            Suggestions
          </div>
        </div>
        <div class="ssi__right__subtitle">
          <div class="ssi__right__subtitle__label">
            <span>{{ self.matchesCount }} Suggestions</span>
            <span ng-if="self.hasUnreadMatches">, {{ self.unreadMatchesCount }} new</span
          </div>
        </div>
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);

      const selectFromState = state => {
        const allPosts = getPosts(state);
        const chatConnectionsByAtomUri =
          this.atomUri &&
          connectionSelectors.getChatConnectionsByAtomUri(state, this.atomUri);
        const matches =
          chatConnectionsByAtomUri &&
          chatConnectionsByAtomUri.filter(conn => {
            const targetAtomUri = conn.get("targetAtomUri");
            const targetAtomActiveOrLoading =
              targetAtomUri &&
              allPosts &&
              allPosts.get(targetAtomUri) &&
              (getIn(state, ["process", "atoms", targetAtomUri, "loading"]) ||
                allPosts.getIn([targetAtomUri, "state"]) ===
                  won.WON.ActiveCompacted);

            return (
              targetAtomActiveOrLoading &&
              connectionUtils.isSuggested(conn) &&
              (connectionSelectors.isChatToXConnection(allPosts, conn) ||
                connectionSelectors.isGroupToXConnection(allPosts, conn))
            );
          });

        const unreadMatches =
          matches && matches.filter(conn => conn.get("unread"));
        const unreadMatchesCount = unreadMatches ? unreadMatches.size : 0;

        const matchesCount = matches ? matches.size : 0;

        const openPostUri = getPostUriFromRoute(state);

        return {
          matchesCount,
          unreadMatchesCount,
          hasUnreadMatches: unreadMatchesCount > 0,
          openPostUri,
        };
      };

      connect2Redux(selectFromState, actionCreators, ["self.atomUri"], this);

      classOnComponentRoot("selected", () => this.isOpen(), this);
      classOnComponentRoot("won-unread", () => this.hasUnreadMatches, this);
    }
    isOpen() {
      //FIXME: Currently just checks if atom atom-details are open
      return this.openPostUri === this.atomUri;
    }

    setOpen() {
      this.onSelected({ atomUri: this.atomUri }); //trigger callback with scope-object
    }
  }
  Controller.$inject = serviceDependencies;
  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      atomUri: "=",
      onSelected: "&",
    },
    template: template,
  };
}
export default angular
  .module("won.owner.components.SuggestionSelectionItem", [])
  .directive("wonSuggestionSelectionItem", genComponentConf).name;
