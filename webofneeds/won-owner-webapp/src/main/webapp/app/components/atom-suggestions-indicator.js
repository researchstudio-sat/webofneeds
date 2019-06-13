/**
 * Created by quasarchimaere on 14.02.2019.
 */

import angular from "angular";
import { attach, getIn } from "../utils.js";
import { connect2Redux } from "../won-utils.js";
import { actionCreators } from "../actions/actions.js";

import "~/style/_atom-suggestions-indicator.scss";
import * as atomUtils from "../atom-utils";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
      <svg class="asi__icon"
          ng-click="self.setOpen()"
          ng-class="{
            'asi__icon--reads': !self.hasUnreadSuggestions,
            'asi__icon--unreads': self.hasUnreadSuggestions
          }">
          <use xlink:href="#ico36_match" href="#ico36_match"></use>
      </svg>
      <div class="asi__right" ng-click="self.setOpen()">
        <div class="asi__right__topline">
          <div class="asi__right__topline__title">
            Suggestions
          </div>
        </div>
        <div class="asi__right__subtitle">
          <div class="asi__right__subtitle__label">
            <span>{{ self.suggestionsCount }} Suggestions</span>
            <span ng-if="self.hasUnreadSuggestions">, {{ self.unreadSuggestionsCount }} new</span
          </div>
        </div>
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);

      const selectFromState = state => {
        const atom = getIn(state, ["atoms", this.atomUri]);
        const suggestedConnections = atomUtils.getSuggestedConnections(atom);

        const suggestionsCount = suggestedConnections
          ? suggestedConnections.size
          : 0;
        const unreadSuggestions =
          suggestedConnections &&
          suggestedConnections.filter(conn => conn.get("unread"));
        const unreadSuggestionsCount = unreadSuggestions
          ? unreadSuggestions.size
          : 0;

        return {
          suggestionsCount,
          unreadSuggestionsCount,
          hasUnreadSuggestions: unreadSuggestionsCount > 0,
        };
      };

      connect2Redux(selectFromState, actionCreators, ["self.atomUri"], this);
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
  .module("won.owner.components.AtomSuggestionsIndicator", [])
  .directive("wonAtomSuggestionsIndicator", genComponentConf).name;
