/**
 * A list over all owned posts/needs with their connections
 * below each other. Usually limited to a connections of a
 * specific state (e.g. "hint")
 * Created by ksinger on 12.04.2017.
 */

import won from "../won-es6.js";
import angular from "angular";
import Immutable from "immutable";
import ngAnimate from "angular-animate";
import squareImageModule from "./square-image.js";
import postHeaderModule from "./post-header.js";
import connectionIndicatorsModule from "./connection-indicators.js";
import connectionSelectionItemModule from "./connection-selection-item.js";
import groupAdministrationSelectionItemModule from "./group-administration-selection-item.js";
import suggestionSelectionItemModule from "./suggestion-selection-item.js";
import createPostItemModule from "./create-post-item.js";

import { attach, delay, sortByDate, get, getIn } from "../utils.js";
import { connect2Redux } from "../won-utils.js";
import { actionCreators } from "../actions/actions.js";

import "style/_connections-overview.scss";

import {
  getRouterParams,
  getGroupPostAdminUriFromRoute,
  getOwnedNeedByConnectionUri,
  getOwnedNeedsInCreation,
  getConnectionUriFromRoute,
  getPostUriFromRoute,
  getPosts,
  getOwnedClosedPosts,
  getOwnedOpenPosts,
} from "../selectors/general-selectors.js";
import { getChatConnectionsToCrawl } from "../selectors/connection-selectors.js";
import {
  isChatConnection,
  isGroupChatConnection,
} from "../connection-utils.js";
import * as needUtils from "../need-utils.js";

const serviceDependencies = ["$ngRedux", "$scope"];
function genComponentConf() {
  let template = `
        <won-create-post-item ng-class="{'selected' : !!self.useCaseGroup || !!self.useCase}"></won-create-post-item>
        <div ng-repeat="needUri in self.beingCreatedNeedUris track by needUri" class="co__item">
            <div class="co__item__need" ng-class="{'selected' : needUri === self.needUriInRoute}">
                <div class="co__item__need__header">
                    <won-post-header
                        need-uri="::needUri"
                        ng-click="self.toggleDetails(needUri)"
                        ng-class="{ 'clickable' : !self.isNeedLoading(needUri) }">
                    </won-post-header>
                </div>
            </div>
        </div>
        <div ng-repeat="needUri in self.sortedOpenNeedUris track by needUri" class="co__item"
            ng-class="{'co__item--withconn' : self.isOpen(needUri) && self.hasOpenOrLoadingChatConnections(needUri, self.allNeeds, self.process)}">
            <div class="co__item__need" ng-class="{'won-unread': self.isUnread(needUri), 'selected' : needUri === self.needUriInRoute, 'open': self.isOpen(needUri)}">
                <div class="co__item__need__header">
                    <won-post-header
                        need-uri="::needUri"
                        ng-click="self.toggleDetails(needUri)"
                        ng-class="{ 'clickable' : !self.isNeedLoading(needUri) }">
                    </won-post-header>
                    <won-connection-indicators
                        on-selected-connection="::self.selectConnection(connectionUri)"
                        need-uri="::needUri">
                    </won-connection-indicators>
                    <button
                        class="co__item__need__header__button red"
                        ng-click="needUri === self.needUriInRoute ? self.selectNeed(undefined) : self.selectNeed(needUri)"
                        ng-class="{
                          'won-button--filled' : needUri === self.needUriInRoute,
                          'won-button--outlined thin': needUri !== self.needUriInRoute
                        }">
                        Details
                    </button>
                    <div class="co__item__need__header__carret clickable" ng-click="!self.isNeedLoading(needUri) && self.toggleDetails(needUri)">
                        <svg class="co__item__need__header__carret__icon"
                            ng-class="{
                              'won-icon-expanded': self.isOpen(needUri),
                              'won-icon-disabled': self.isNeedLoading(needUri),
                            }">
                            <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
                        </svg>
                    </div>
                </div>
            </div>
            <div class="co__item__connections"
                ng-if="self.isOpen(needUri) && (self.hasGroupFacet(needUri) || self.hasOpenOrLoadingChatConnections(needUri, self.allNeeds, self.process))">
                <div class="co__item__connections__item"
                  ng-if="self.hasChatFacet(needUri)"
                  ng-repeat="connUri in self.getOpenChatConnectionUrisArraySorted(needUri, self.allNeeds, self.process) track by connUri"
                  ng-class="{
                    'won-unread': self.isConnectionUnread(needUri, connUri),
                    'selected': connUri === self.connUriInRoute
                  }">
                  <won-connection-selection-item
                      on-selected-connection="::self.selectConnection(connectionUri)"
                      connection-uri="::connUri"
                      ng-class="{'won-unread': self.isConnectionUnread(needUri, connUri)}">
                  </won-connection-selection-item>
                </div>
                <div class="co__item__connections__item nonsticky" ng-if="self.hasGroupFacet(needUri)">
                  <won-group-administration-selection-item
                      need-uri="::needUri"
                      on-selected="self.selectGroupChat(needUri)">
                  </won-group-administration-selection-item>
                </div>
                <div class="co__item__connections__item nonsticky" ng-if="self.hasSuggestedConnections(needUri)"
                  ng-class="{
                    'won-unread': self.hasUnreadSuggestedConnections(needUri),
                    'selected': self.isShowingSuggestions(needUri),
                  }">
                  <won-suggestion-selection-item
                      need-uri="::needUri"
                      on-selected="self.showSuggestions(needUri)">
                  </won-suggestion-selection-item>
                </div>
            </div>
        </div>
        <div class="co__separator clickable" ng-class="{'co__separator--open' : self.showClosedNeeds}" ng-if="self.hasClosedNeeds()" ng-click="self.toggleClosedNeeds()">
            <span class="co__separator__text">Archived Posts ({{self.getClosedNeedsText()}})</span>
            <svg
                style="--local-primary:var(--won-secondary-color);"
                class="co__separator__arrow"
                ng-if="self.showClosedNeeds">
                <use xlink:href="#ico16_arrow_up" href="#ico16_arrow_up"></use>
            </svg>
            <svg style="--local-primary:var(--won-secondary-color);"
                class="co__separator__arrow"
                ng-if="!self.showClosedNeeds">
                <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
            </svg>
        </div>
        <div class="co__closedNeeds" ng-if="self.showClosedNeeds && self.closedNeedsSize > 0">
            <div ng-repeat="needUri in self.sortedClosedNeedUris track by needUri" class="co__item">
                <div class="co__item__need" ng-class="{'won-unread': self.isUnread(needUri), 'selected' : needUri === self.needUriInRoute}">
                    <div class="co__item__need__header">
                        <won-post-header
                            need-uri="::needUri"
                            ng-click="self.toggleDetails(needUri)"
                            ng-class="{ 'clickable' : !self.isNeedLoading(needUri) }">
                        </won-post-header>
                        <button
                            class="co__item__need__header__button red"
                            ng-click="needUri === self.needUriInRoute ? self.selectNeed(undefined) : self.selectNeed(needUri)"
                            ng-class="{
                              'won-button--filled' : needUri === self.needUriInRoute,
                              'won-button--outlined thin': needUri !== self.needUriInRoute
                            }">
                            Details
                        </button>
                        <div class="co__item__need__header__carret clickable" ng-click="!self.isNeedLoading(needUri) && self.toggleDetails(needUri)">
                          <svg class="co__item__need__header__carret__icon"
                              ng-class="{
                                'won-icon-expanded': self.isOpen(needUri),
                                'won-icon-disabled': self.isNeedLoading(needUri),
                              }">
                              <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
                          </svg>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.open = open;
      this.WON = won.WON;
      //this.labels = labels;
      window.co4dbg = this;

      const self = this;
      const selectFromState = state => {
        const allNeeds = getPosts(state);
        const openNeeds = getOwnedOpenPosts(state);
        const closedNeeds = getOwnedClosedPosts(state);

        // needs that have been created but are not confirmed by the server yet
        const beingCreatedNeeds = getOwnedNeedsInCreation(state);

        const connectionsToCrawl = getChatConnectionsToCrawl(state);

        const routerParams = getRouterParams(state);
        const useCase = get(routerParams, "useCase");
        const useCaseGroup = get(routerParams, "useCaseGroup");
        const connUriInRoute = getConnectionUriFromRoute(state);
        const groupPostAdminUriInRoute = getGroupPostAdminUriFromRoute(state);
        const needUriInRoute = getPostUriFromRoute(state);
        const needImpliedInRoute =
          (connUriInRoute &&
            getOwnedNeedByConnectionUri(state, connUriInRoute)) ||
          (groupPostAdminUriInRoute &&
            state.getIn(["needs", groupPostAdminUriInRoute]));
        const needUriImpliedInRoute =
          needImpliedInRoute && needImpliedInRoute.get("uri");

        const sortedOpenNeeds = sortByDate(openNeeds, "creationDate");
        const sortedClosedNeeds = sortByDate(closedNeeds, "creationDate");

        const unloadedNeeds = closedNeeds.filter(need =>
          getIn(state, ["process", "needs", need.get("uri"), "toLoad"])
        );

        return {
          allNeeds,
          process: state.get("process"),
          showClosedNeeds: state.getIn(["view", "showClosedNeeds"]),
          useCase,
          useCaseGroup,
          needUriInRoute,
          needUriImpliedInRoute,
          connUriInRoute,
          beingCreatedNeedUris: beingCreatedNeeds && [
            ...beingCreatedNeeds.keys(),
          ],
          sortedOpenNeedUris: sortedOpenNeeds && [
            ...sortedOpenNeeds.flatMap(need => need.get("uri")),
          ],
          sortedClosedNeedUris: sortedClosedNeeds && [
            ...sortedClosedNeeds.flatMap(need => need.get("uri")),
          ],
          connectionsToCrawl: connectionsToCrawl || Immutable.Map(),
          unloadedNeedsSize: unloadedNeeds ? unloadedNeeds.size : 0,
          closedNeedsSize: closedNeeds ? closedNeeds.size : 0,
        };
      };
      connect2Redux(
        selectFromState,
        actionCreators,
        ["self.connectionUri"],
        this
      );

      this.$scope.$watch("self.needUriImpliedInRoute", (newValue, oldValue) => {
        if (newValue && !oldValue) {
          self.open[newValue] = true;
        }
      });

      this.$scope.$watch("self.connectionsToCrawl", cnctToCrawl =>
        this.ensureUnreadMessagesAreLoaded(cnctToCrawl)
      );
    }

    ensureUnreadMessagesAreLoaded(connectionsToCrawl) {
      delay(0).then(() => {
        const MESSAGECOUNT = 10;

        connectionsToCrawl.map(conn => {
          const messages = conn.get("messages");
          const messageCount = messages ? messages.size : 0;

          if (messageCount == 0) {
            this.connections__showLatestMessages(conn.get("uri"), MESSAGECOUNT);
          } else {
            const receivedMessages = messages.filter(
              msg => !msg.get("outgoingMessage")
            );
            const receivedMessagesReadPresent = receivedMessages.find(
              msg => !msg.get("unread")
            );

            if (!receivedMessagesReadPresent) {
              this.connections__showMoreMessages(conn.get("uri"), MESSAGECOUNT);
            }
          }
        });
      });
    }

    toggleDetails(ownedNeedUri) {
      for (let key in this.open) {
        if (key !== ownedNeedUri) {
          this.open[key] = false;
        }
      }

      if (this.isOpen(ownedNeedUri)) {
        this.open[ownedNeedUri] = false;
        if (this.isOpenByConnection(ownedNeedUri)) {
          this.router__stateGoCurrent({
            postUri: undefined,
            useCase: undefined,
            useCaseGroup: undefined,
            connectionUri: undefined,
            groupPostAdminUri: undefined,
            fromNeedUri: undefined,
            mode: undefined,
          });
        }
      } else {
        this.open[ownedNeedUri] = true;
      }
    }

    showSuggestions(ownedNeedUri) {
      //FIXME: Currently just opens need-details
      this.router__stateGoCurrent({
        postUri: ownedNeedUri,
        useCase: undefined,
        useCaseGroup: undefined,
        connectionUri: undefined,
        groupPostAdminUri: undefined,
        fromNeedUri: undefined,
        mode: undefined,
      });
    }

    hasChatFacet(needUri) {
      const need = get(this.allNeeds, needUri);
      return needUtils.hasChatFacet(need);
    }

    hasGroupFacet(needUri) {
      const need = get(this.allNeeds, needUri);
      return needUtils.hasGroupFacet(need);
    }

    hasSuggestedConnections(needUri) {
      const need = get(this.allNeeds, needUri);
      return needUtils.hasSuggestedConnections(need);
    }

    hasUnreadSuggestedConnections(needUri) {
      const need = get(this.allNeeds, needUri);
      return needUtils.hasUnreadSuggestedConnections(need);
    }

    isUnread(needUri) {
      const need = get(this.allNeeds, needUri);
      return get(need, "unread");
    }

    isConnectionUnread(needUri, connUri) {
      const conn = getIn(this.allNeeds, [needUri, "connections", connUri]);
      return get(conn, "unread");
    }

    hasClosedNeeds() {
      return this.closedNeedsSize > 0;
    }

    getClosedNeedsText() {
      let output = [];
      if (this.closedNeedsSize > 0) {
        output.push(`${this.closedNeedsSize}`);
      }
      if (this.unloadedNeedsSize > 0) {
        output.push(`${this.unloadedNeedsSize} unloaded`);
      }

      return output.join(" - ");
    }

    toggleClosedNeeds() {
      if (this.unloadedNeedsSize > 0) {
        this.needs__fetchUnloadedNeeds();
      }
      this.view__toggleClosedNeeds();
    }

    isOpen(ownedNeedUri) {
      return this.isOpenByConnection(ownedNeedUri) || !!this.open[ownedNeedUri];
    }

    isShowingSuggestions(ownedNeedUri) {
      //FIXME: Currently just checks if need need-details are open
      return !!this.open[ownedNeedUri] && ownedNeedUri === this.needUriInRoute;
    }

    isNeedLoading(needUri) {
      return this.process.getIn(["needs", needUri, "loading"]);
    }

    isOpenByConnection(ownedNeedUri) {
      return this.needUriImpliedInRoute === ownedNeedUri;
    }

    selectConnection(connectionUri) {
      this.onSelectedConnection({ connectionUri }); //trigger callback with scope-object
    }
    selectNeed(needUri) {
      this.onSelectedNeed({ needUri }); //trigger callback with scope-object
    }
    selectGroupChat(needUri) {
      this.onSelectedGroupChat({ needUri }); //trigger callback with scope-object
    }
    selectSuggested(needUri) {
      console.debug("stuff should happen now IMPL ME for: ", needUri);
    }

    hasOpenOrLoadingChatConnections(needUri, allNeeds, process) {
      const need = get(this.allNeeds, needUri);

      if (!need) {
        return undefined;
      }
      return (
        need.get("state") === won.WON.ActiveCompacted &&
        !!need.get("connections").find(conn => {
          if (!isChatConnection(conn) && !isGroupChatConnection(conn))
            return false;
          if (
            process &&
            process.getIn(["connections", conn.get("uri"), "loading"])
          )
            return true; //if connection is currently loading we assume its a connection we want to show

          const remoteNeedUri = conn.get("remoteNeedUri");
          const remoteNeedPresent =
            remoteNeedUri && allNeeds && !!allNeeds.get(remoteNeedUri);

          if (!remoteNeedPresent) return true; //if the remoteNeed is not present yet we assume its a connection we want

          const remoteNeedActiveOrLoading =
            process.getIn(["needs", remoteNeedUri, "loading"]) ||
            process.getIn(["needs", remoteNeedUri, "failedToLoad"]) ||
            allNeeds.getIn([remoteNeedUri, "state"]) ===
              won.WON.ActiveCompacted;

          return (
            remoteNeedActiveOrLoading && conn.get("state") !== won.WON.Closed
          );
        })
      );
    }

    getOpenChatConnectionUrisArraySorted(needUri, allNeeds, process) {
      const need = get(this.allNeeds, needUri);

      if (!need) {
        return undefined;
      }
      const sortedConnections = sortByDate(
        need.get("connections").filter(conn => {
          if (!isChatConnection(conn) && !isGroupChatConnection(conn))
            return false;
          if (
            process &&
            process.getIn(["connections", conn.get("uri"), "loading"])
          )
            return true; //if connection is currently loading we assume its a connection we want to show

          const remoteNeedUri = conn.get("remoteNeedUri");
          const remoteNeedPresent =
            remoteNeedUri && allNeeds && !!allNeeds.get(remoteNeedUri);

          if (!remoteNeedPresent) return false;

          const remoteNeedActiveOrLoading =
            process.getIn(["needs", remoteNeedUri, "loading"]) ||
            process.getIn(["needs", remoteNeedUri, "failedToLoad"]) ||
            allNeeds.getIn([remoteNeedUri, "state"]) ===
              won.WON.ActiveCompacted;

          return (
            remoteNeedActiveOrLoading &&
            conn.get("state") !== won.WON.Closed &&
            conn.get("state") !== won.WON.Suggested
          );
        }),
        "creationDate"
      );
      return (
        sortedConnections && [
          ...sortedConnections.flatMap(conn => conn.get("uri")),
        ]
      );
    }
  }
  Controller.$inject = serviceDependencies;
  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      open: "=",
      /*
       * Usage:
       *  on-selected-connection="myCallback(connectionUri)"
       */
      onSelectedConnection: "&",
      onSelectedNeed: "&",
      onSelectedGroupChat: "&",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.connectionsOverview", [
    squareImageModule,
    connectionSelectionItemModule,
    groupAdministrationSelectionItemModule,
    suggestionSelectionItemModule,
    postHeaderModule,
    connectionIndicatorsModule,
    ngAnimate,
    createPostItemModule,
  ])
  .directive("wonConnectionsOverview", genComponentConf).name;
