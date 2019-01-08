/**
 * Component for rendering the connection indicators as an svg images, with unread count and select handle on the latest (possibly unread) connnectionuri
 * Created by fsuda on 10.04.2017.
 */
import angular from "angular";
import won from "../won-es6.js";
import "ng-redux";
import { labels } from "../won-label-utils.js";
import { actionCreators } from "../actions/actions.js";
import { getPosts, getOwnedPosts } from "../selectors/general-selectors.js";
import { getChatConnectionsByNeedUri } from "../selectors/connection-selectors.js";

import { attach, sortByDate, getIn } from "../utils.js";
import { connect2Redux } from "../won-utils.js";
import { classOnComponentRoot } from "../cstm-ng-utils.js";
import { isChatConnection } from "../connection-utils.js";

import "style/_connection-indicators.scss";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
        <a
            class="indicators__item clickable"
            ng-show="!self.postLoading && self.latestConnectedUri"
            ng-click="self.setOpen(self.latestConnectedUri)">
                <svg class="indicators__item__icon"
                    title="Show latest message/request"
                    style="--local-primary:var(--won-primary-color-light);"
                    ng-show="!self.unreadConnectedCount">
                        <use xlink:href="#ico36_message" href="#ico36_message"></use>
                </svg>

                <svg style="--local-primary:var(--won-primary-color);"
                     title="Show latest unread message/request"
                     ng-show="self.unreadConnectedCount"
                     class="indicators__item__icon">
                        <use xlink:href="#ico36_message" href="#ico36_message"></use>
                </svg>

                <span class="indicators__item__caption" title="Number of chats with unread messages/requests">
                    {{ self.getCountLimited(self.unreadConnectedCount)}}
                </span>
        </a>
        <div class="indicators__item" ng-show="!self.postLoading && !self.latestConnectedUri" title="No chats in this post">
            <svg class="indicators__item__icon"
                style="--local-primary:var(--won-disabled-color);">
                    <use xlink:href="#ico36_message" href="#ico36_message"></use>
            </svg>
             <span class="indicators__item__caption"></span>
        </div>
        <a
            class="indicators__item clickable"
            ng-show="!self.postLoading && self.latestMatchUri"
            ng-click="self.setOpen(self.latestMatchUri)">

                <svg class="indicators__item__icon"
                    style="--local-primary:var(--won-primary-color-light);"
                    ng-show="!self.unreadMatchesCount">
                        <use xlink:href="#ico36_match" href="#ico36_match"></use>
                </svg>

                <svg style="--local-primary:var(--won-primary-color);"
                    ng-show="self.unreadMatchesCount"
                    class="indicators__item__icon">
                        <use xlink:href="#ico36_match" href="#ico36_match"></use>
                </svg>
                <span class="indicators__item__caption" title="Number of new matches">
                    {{ self.getCountLimited(self.unreadMatchesCount) }}
                </span>
        </a>
        <div class="indicators__item" ng-show="!self.postLoading && !self.latestMatchUri" title="No matches for this post">
            <svg class="indicators__item__icon"
                style="--local-primary:var(--won-disabled-color);">
                    <use xlink:href="#ico36_match" href="#ico36_match"></use>
            </svg>
            <span class="indicators__item__caption"></span>
        </div>
        <span class="mobile__indicator" ng-show="!self.postLoading && self.unreadCountSum">{{ self.getCountLimited(self.unreadCountSum) }}</span>

        <div class="indicators__item" ng-if="self.postLoading">
            <svg class="indicators__item__icon"
                style="--local-primary:var(--won-skeleton-color);">
                    <use xlink:href="#ico36_message" href="#ico36_message"></use>
            </svg>
            <span class="indicators__item__caption"></span>
        </div>
        <div class="indicators__item" ng-if="self.postLoading">
            <svg class="indicators__item__icon"
                style="--local-primary:var(--won-skeleton-color);">
                    <use xlink:href="#ico36_message" href="#ico36_match"></use>
            </svg>
            <span class="indicators__item__caption"></span>
        </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.labels = labels;

      const selectFromState = state => {
        const ownedPosts = getOwnedPosts(state);
        const allPosts = getPosts(state);
        const ownedPost = ownedPosts && ownedPosts.get(this.needUri);
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
              isChatConnection(conn) &&
              conn.get("state") === won.WON.Suggested
            );
          });
        const connected =
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
              isChatConnection(conn) &&
              conn.get("state") !== won.WON.Suggested &&
              conn.get("state") !== won.WON.Closed
            );
          });

        const unreadMatches =
          matches && matches.filter(conn => conn.get("unread"));
        const unreadConversations =
          connected && connected.filter(conn => conn.get("unread"));

        const unreadMatchesCount = unreadMatches && unreadMatches.size;
        const unreadConnectedCount =
          unreadConversations && unreadConversations.size;

        const unreadCountSum = unreadConnectedCount + unreadMatchesCount;

        return {
          WON: won.WON,
          ownedPost,
          postLoading:
            !ownedPost ||
            getIn(state, ["process", "needs", ownedPost.get("uri"), "loading"]),
          unreadCountSum: unreadCountSum > 0 ? unreadCountSum : undefined,
          unreadConnectedCount:
            unreadConnectedCount > 0 ? unreadConnectedCount : undefined,
          unreadMatchesCount:
            unreadMatchesCount > 0 ? unreadMatchesCount : undefined,
          latestConnectedUri: this.retrieveLatestUri(connected),
          latestMatchUri: this.retrieveLatestUri(matches),
        };
      };

      connect2Redux(selectFromState, actionCreators, ["self.needUri"], this);

      classOnComponentRoot("won-is-loading", () => this.postLoading, this);
    }

    /**
     * This method returns either the latest unread uri of the given connection elements, or the latest uri of a read connection, if nothing is found undefined is returned
     * @param elements connection elements to retrieve the latest uri from
     * @returns {*}
     */
    retrieveLatestUri(elements) {
      const unreadElements =
        elements && elements.filter(conn => conn.get("unread"));

      const sortedUnreadElements = sortByDate(unreadElements);
      const unreadUri =
        sortedUnreadElements &&
        sortedUnreadElements[0] &&
        sortedUnreadElements[0].get("uri");

      if (unreadUri) {
        return unreadUri;
      } else {
        const sortedElements = sortByDate(elements);
        return (
          sortedElements && sortedElements[0] && sortedElements[0].get("uri")
        );
      }
    }

    setOpen(connectionUri) {
      this.onSelectedConnection({ connectionUri: connectionUri }); //trigger callback with scope-object
      //TODO either publish a dom-event as well; or directly call the route-change
    }

    getCountLimited(count, threshold = 100) {
      if (!!count && threshold < count) {
        return threshold - 1 + "+";
      }
      return count;
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
      onSelectedConnection: "&",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.connectionIndicators", [])
  .directive("wonConnectionIndicators", genComponentConf).name;
