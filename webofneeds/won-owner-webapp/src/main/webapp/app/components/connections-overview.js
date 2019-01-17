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
import extendedConnectionIndicatorsModule from "./extended-connection-indicators.js";
import connectionSelectionItemModule from "./connection-selection-item.js";
import groupAdministrationSelectionItemModule from "./group-administration-selection-item.js";
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
import { hasGroupFacet, hasChatFacet } from "../need-utils.js";

const serviceDependencies = ["$ngRedux", "$scope"];
function genComponentConf() {
  let template = `
        <won-create-post-item ng-class="{'selected' : !!self.useCaseGroup || !!self.useCase}"></won-create-post-item>
        <div ng-repeat="need in self.beingCreatedNeeds" class="co__item">
            <!-- ng-if="self.beingCreatedNeeds.size > 0" -->
            <div class="co__item__need" ng-class="{'selected' : need.get('uri') === self.needUriInRoute}">
                <div class="co__item__need__header">
                    <won-post-header
                        need-uri="need.get('uri')"
                        timestamp="'TODOlatestOfThatType'"
                        ng-click="self.toggleDetails(need.get('uri'))"
                        ng-class="{ 'clickable' : !self.isNeedLoading(need) }">
                    </won-post-header>
                </div>
            </div>
        </div>
        <div ng-repeat="need in self.sortedOpenNeeds" class="co__item"
            ng-class="{'co__item--withconn' : self.isOpen(need.get('uri')) && self.hasOpenOrLoadingChatConnections(need, self.allNeeds, self.process)}">
            <div class="co__item__need" ng-class="{'won-unread': need.get('unread'), 'selected' : need.get('uri') === self.needUriInRoute}">
                <div class="co__item__need__header">
                    <won-post-header
                        need-uri="need.get('uri')"
                        timestamp="'TODOlatestOfThatType'"
                        ng-click="self.toggleDetails(need.get('uri'))"
                        ng-class="{ 'clickable' : !self.isNeedLoading(need) }">
                    </won-post-header>
                    <won-connection-indicators
                        on-selected-connection="self.selectConnection(connectionUri)"
                        need-uri="need.get('uri')"
                        ng-if="!self.isOpen(need.get('uri'))">
                    </won-connection-indicators>
                    <button
                        class="co__item__need__header__button red"
                        ng-if="self.isOpen(need.get('uri'))"
                        ng-click="need.get('uri') === self.needUriInRoute ? self.selectNeed(undefined) : self.selectNeed(need.get('uri'))"
                        ng-class="{
                          'won-button--filled' : need.get('uri') === self.needUriInRoute,
                          'won-button--outlined thin': need.get('uri') !== self.needUriInRoute
                        }">
                        Details
                    </button>
                    <div class="co__item__need__header__carret clickable" ng-click="self.toggleDetails(need.get('uri'))" ng-if="!self.isNeedLoading(need)">
                        <svg
                            style="--local-primary:var(--won-secondary-color);"
                            class="co__item__need__header__carret__icon"
                            ng-if="self.isOpen(need.get('uri'))">
                                <use xlink:href="#ico16_arrow_up" href="#ico16_arrow_up"></use>
                        </svg>
                        <svg
                            style="--local-primary:var(--won-secondary-color);"
                            class="co__item__need__header__carret__icon"
                            ng-if="!self.isOpen(need.get('uri'))">
                                <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
                        </svg>
                    </div>
                    <div class="co__item__need__header__carret" ng-if="self.isNeedLoading(need)">
                        <svg
                            style="--local-primary:var(--won-skeleton-color);"
                            class="co__item__need__header__carret__icon"
                            ng-if="self.isOpen(need.get('uri'))">
                                <use xlink:href="#ico16_arrow_up" href="#ico16_arrow_up"></use>
                        </svg>
                        <svg
                            style="--local-primary:var(--won-skeleton-color);"
                            class="co__item__need__header__carret__icon"
                            ng-if="!self.isOpen(need.get('uri'))">
                                <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
                        </svg>
                    </div>
                </div>
                <div class="co__item__need__detail" ng-if="self.isOpen(need.get('uri'))">
                    <won-extended-connection-indicators
                        class="co__item__need__detail__indicators"
                        on-selected-connection="self.selectConnection(connectionUri)"
                        need-uri="need.get('uri')">
                    </won-extended-connection-indicators>
                </div>
            </div>
            <div class="co__item__connections"
                ng-if="self.isOpen(need.get('uri')) && (self.hasOpenOrLoadingChatConnections(need, self.allNeeds, self.process) || self.hasGroupFacet(need))">
                <won-connection-selection-item
                    ng-if="self.hasChatFacet(need)"
                    ng-repeat="conn in self.getOpenChatConnectionsArraySorted(need, self.allNeeds, self.process)"
                    on-selected-connection="self.selectConnection(connectionUri)"
                    connection-uri="conn.get('uri')"
                    ng-class="{'won-unread': conn.get('unread')}">
                </won-connection-selection-item>
                <won-group-administration-selection-item
                    ng-if="self.hasGroupFacet(need)"
                    need-uri="need.get('uri')"
                    on-selected="self.selectGroupChat(needUri)">
                </won-group-administration-selection-item>
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
            <div ng-repeat="need in self.sortedClosedNeeds" class="co__item">
                <div class="co__item__need" ng-class="{'won-unread': need.get('unread'), 'selected' : need.get('uri') === self.needUriInRoute}">
                    <div class="co__item__need__header">
                        <won-post-header
                            need-uri="need.get('uri')"
                            timestamp="'TODOlatestOfThatType'"
                            ng-click="self.toggleDetails(need.get('uri'))"
                            ng-class="{ 'clickable' : !self.isNeedLoading(need) }">
                        </won-post-header>
                        <button
                            class="co__item__need__header__button red"
                            ng-if="self.isOpen(need.get('uri'))"
                            ng-click="need.get('uri') === self.needUriInRoute ? self.selectNeed(undefined) : self.selectNeed(need.get('uri'))"
                            ng-class="{
                              'won-button--filled' : need.get('uri') === self.needUriInRoute,
                              'won-button--outlined thin': need.get('uri') !== self.needUriInRoute
                            }">
                            Details
                        </button>
                        <div class="co__item__need__header__carret clickable" ng-click="self.toggleDetails(need.get('uri'))" ng-if="!self.isNeedLoading(need)">
                            <svg
                                style="--local-primary:var(--won-secondary-color);"
                                class="co__item__need__header__carret__icon"
                                ng-if="self.isOpen(need.get('uri'))">
                                    <use xlink:href="#ico16_arrow_up" href="#ico16_arrow_up"></use>
                            </svg>
                            <svg style="--local-primary:var(--won-secondary-color);"
                                class="co__item__need__header__carret__icon"
                                ng-if="!self.isOpen(need.get('uri'))">
                                    <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
                            </svg>
                        </div>
                        <div class="co__item__need__header__carret" ng-if="self.isNeedLoading(need)">
                            <svg
                                style="--local-primary:var(--won-skeleton-color);"
                                class="co__item__need__header__carret__icon"
                                ng-if="self.isOpen(need.get('uri'))">
                                    <use xlink:href="#ico16_arrow_up" href="#ico16_arrow_up"></use>
                            </svg>
                            <svg
                                style="--local-primary:var(--won-skeleton-color);"
                                class="co__item__need__header__carret__icon"
                                ng-if="!self.isOpen(need.get('uri'))">
                                    <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
                            </svg>
                        </div>
                    </div>
                    <div class="co__item__need__detail" ng-if="self.isOpen(need.get('uri'))">
                        <won-extended-connection-indicators
                            class="co__item__need__detail__indicators"
                            on-selected-connection="self.selectConnection(connectionUri)"
                            need-uri="need.get('uri')">
                        </won-extended-connection-indicators>
                    </div>
                </div>
            </div>
        </div>
        <!-- TODO: REMOVE THIS AGAIN ONLY FOR DEBUGGING NOW
        <div class="co__loading" ng-if="self.showLoadingIndicator">
            <svg class="co__loading__spinner hspinner">
                <use xlink:href="#ico_loading_anim" href="#ico_loading_anim"></use>
            </svg>
        </div> -->
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.open = open;
      this.WON = won.WON;
      this.hasGroupFacet = hasGroupFacet;
      this.hasChatFacet = hasChatFacet;
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
          beingCreatedNeeds: beingCreatedNeeds && beingCreatedNeeds.toArray(),
          sortedOpenNeeds,
          sortedClosedNeeds,
          connectionsToCrawl: connectionsToCrawl || Immutable.Map(),
          unloadedNeedsSize: unloadedNeeds ? unloadedNeeds.size : 0,
          closedNeedsSize: closedNeeds ? closedNeeds.size : 0,

          //showLoadingIndicator: getIn(state, ["process", "processingInitialLoad"]) || getIn(state, ["process", "processingLogin"]),
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
            const receivedMessagesReadPresent =
              receivedMessages.filter(msg => !msg.get("unread")).size > 0;

            if (!receivedMessagesReadPresent) {
              this.connections__showMoreMessages(conn.get("uri"), MESSAGECOUNT);
            }
          }
        });
      });
    }

    toggleDetails(ownedNeedUri) {
      if (this.isOpen(ownedNeedUri)) {
        this.open[ownedNeedUri] = false;
        if (this.isOpenByConnection(ownedNeedUri)) {
          this.router__stateGoCurrent({
            useCase: undefined,
            useCaseGroup: undefined,
            connectionUri: undefined,
            groupPostAdminUri: undefined,
          });
        }
      } else {
        this.open[ownedNeedUri] = true;
      }
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

    isNeedLoading(need) {
      return this.process.getIn(["needs", need.get("uri"), "loading"]);
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

    hasOpenOrLoadingChatConnections(need, allNeeds, process) {
      return (
        need.get("state") === won.WON.ActiveCompacted &&
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

          if (!remoteNeedPresent) return true; //if the remoteNeed is not present yet we assume its a connection we want

          const remoteNeedActiveOrLoading =
            process.getIn(["needs", remoteNeedUri, "loading"]) ||
            process.getIn(["needs", remoteNeedUri, "failedToLoad"]) ||
            allNeeds.getIn([remoteNeedUri, "state"]) ===
              won.WON.ActiveCompacted;

          return (
            remoteNeedActiveOrLoading && conn.get("state") !== won.WON.Closed
          );
        }).size > 0
      );
    }

    getOpenChatConnectionsArraySorted(need, allNeeds, process) {
      return sortByDate(
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
            remoteNeedActiveOrLoading && conn.get("state") !== won.WON.Closed
          );
        }),
        "creationDate"
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
    postHeaderModule,
    connectionIndicatorsModule,
    extendedConnectionIndicatorsModule,
    ngAnimate,
    createPostItemModule,
  ])
  .directive("wonConnectionsOverview", genComponentConf).name;
