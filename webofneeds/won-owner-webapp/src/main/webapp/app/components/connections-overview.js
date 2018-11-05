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
import createPostItemModule from "./create-post-item.js";

import { attach, delay, sortByDate, get } from "../utils.js";
import { connect2Redux } from "../won-utils.js";
import { actionCreators } from "../actions/actions.js";

import "style/_connections-overview.scss";

import {
  selectRouterParams,
  selectNeedByConnectionUri,
  selectNeedsInCreationProcess,
  selectOpenConnectionUri,
  selectOpenPostUri,
  selectAllPosts,
  selectClosedPosts,
  selectOpenPosts,
} from "../selectors/general-selectors.js";
import { selectPostConnectionsWithoutConnectMessage } from "../selectors/connection-selectors.js";

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
                        ng-class="{ 'clickable' : !self.isLoading(need) }">
                    </won-post-header>
                </div>
            </div>
        </div>
        <div ng-repeat="need in self.sortedOpenNeeds" class="co__item"
            ng-class="{'co__item--withconn' : self.isOpen(need.get('uri')) && self.hasOpenOrLoadingConnections(need, self.allNeeds)}">
            <div class="co__item__need" ng-class="{'won-unread': need.get('unread'), 'selected' : need.get('uri') === self.needUriInRoute}">
                <div class="co__item__need__header">
                    <won-post-header
                        need-uri="need.get('uri')"
                        timestamp="'TODOlatestOfThatType'"
                        ng-click="self.toggleDetails(need.get('uri'))"
                        ng-class="{ 'clickable' : !self.isLoading(need) }">
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
                    <div class="co__item__need__header__carret clickable" ng-click="self.toggleDetails(need.get('uri'))" ng-if="!self.isLoading(need)">
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
                    <div class="co__item__need__header__carret" ng-if="self.isLoading(need)">
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
                ng-if="self.isOpen(need.get('uri')) && self.hasOpenOrLoadingConnections(need, self.allNeeds)">
                <won-connection-selection-item
                    ng-repeat="conn in self.getOpenConnectionsArraySorted(need, self.allNeeds)"
                    on-selected-connection="self.selectConnection(connectionUri)"
                    connection-uri="conn.get('uri')"
                    ng-class="{'won-unread': conn.get('unread')}">
                </won-connection-selection-item>
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
                            ng-class="{ 'clickable' : !self.isLoading(need) }">
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
                        <div class="co__item__need__header__carret clickable" ng-click="self.toggleDetails(need.get('uri'))" ng-if="!self.isLoading(need)">
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
                        <div class="co__item__need__header__carret" ng-if="self.isLoading(need)">
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
        const allNeeds = selectAllPosts(state);
        const openNeeds = selectOpenPosts(state);
        const closedNeeds = selectClosedPosts(state);

        // needs that have been created but are not confirmed by the server yet
        const beingCreatedNeeds = selectNeedsInCreationProcess(state);

        const connectionsToCrawl = selectPostConnectionsWithoutConnectMessage(
          state
        );

        const routerParams = selectRouterParams(state);
        const useCase = get(routerParams, "useCase");
        const useCaseGroup = get(routerParams, "useCaseGroup");
        const connUriInRoute = selectOpenConnectionUri(state);
        const needUriInRoute = selectOpenPostUri(state);
        const needImpliedInRoute =
          connUriInRoute && selectNeedByConnectionUri(state, connUriInRoute);
        const needUriImpliedInRoute =
          needImpliedInRoute && needImpliedInRoute.get("uri");

        const sortedOpenNeeds = sortByDate(openNeeds, "creationDate");
        const sortedClosedNeeds = sortByDate(closedNeeds, "creationDate");

        const unloadedNeeds = closedNeeds.filter(need => need.get("toLoad"));

        return {
          allNeeds,
          showClosedNeeds: state.get("showClosedNeeds"),
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
          if (conn.get("isLoadingMessages")) return;
          const messages = conn.get("messages");
          const messageCount = messages ? messages.size : 0;

          if (messageCount == 0) {
            console.debug(
              "DISPATCH connections__showLatestMessages for connUri:",
              conn.get("uri"),
              " -> NO messages have already been loaded in the connection: ",
              conn
            );
            this.connections__showLatestMessages(conn.get("uri"), MESSAGECOUNT);
          } else {
            const receivedMessages = messages.filter(
              msg => !msg.get("outgoingMessage")
            );
            const receivedMessagesReadPresent =
              receivedMessages.filter(msg => !msg.get("unread")).size > 0;

            if (!receivedMessagesReadPresent) {
              console.debug(
                "DISPATCH connections__showMoreMessages for connUri:",
                conn.get("uri"),
                " -> ONLY unread messages are currently present:",
                conn
              );
              this.connections__showMoreMessages(conn.get("uri"), MESSAGECOUNT);
            }
          }
        });
      });
    }

    toggleDetails(ownNeedUri) {
      if (this.isOpen(ownNeedUri)) {
        this.open[ownNeedUri] = false;
        if (this.isOpenByConnection(ownNeedUri)) {
          this.router__stateGoCurrent({
            useCase: undefined,
            useCaseGroup: undefined,
            connectionUri: undefined,
          });
        }
      } else {
        this.open[ownNeedUri] = true;
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
      this.toggleClosedNeedsDisplay();
    }

    isOpen(ownNeedUri) {
      return this.isOpenByConnection(ownNeedUri) || !!this.open[ownNeedUri];
    }

    isLoading(ownNeed) {
      return ownNeed.get("isLoading");
    }

    isOpenByConnection(ownNeedUri) {
      return this.needUriImpliedInRoute === ownNeedUri;
    }

    selectConnection(connectionUri) {
      this.onSelectedConnection({ connectionUri }); //trigger callback with scope-object
    }
    selectNeed(needUri) {
      this.onSelectedNeed({ needUri }); //trigger callback with scope-object
    }

    hasOpenOrLoadingConnections(need, allNeeds) {
      return (
        need.get("state") === won.WON.ActiveCompacted &&
        need.get("connections").filter(conn => {
          if (conn.get("isLoading")) return true; //if connection is currently loading we assume its a connection we want to show

          const remoteNeedUri = conn.get("remoteNeedUri");
          const remoteNeedPresent =
            remoteNeedUri && allNeeds && !!allNeeds.get(remoteNeedUri);

          if (!remoteNeedPresent) return true; //if the remoteNeed is not present yet we assume its a connection we want

          const remoteNeedActiveOrLoading =
            allNeeds.getIn([remoteNeedUri, "isLoading"]) ||
            allNeeds.getIn([remoteNeedUri, "state"]) ===
              won.WON.ActiveCompacted;

          return (
            remoteNeedActiveOrLoading && conn.get("state") !== won.WON.Closed
          );
        }).size > 0
      );
    }

    getOpenConnectionsArraySorted(need, allNeeds) {
      return sortByDate(
        need.get("connections").filter(conn => {
          if (conn.get("isLoading")) return true; //if connection is currently loading we assume its a connection we want to show

          const remoteNeedUri = conn.get("remoteNeedUri");
          const remoteNeedPresent =
            remoteNeedUri && allNeeds && !!allNeeds.get(remoteNeedUri);

          if (!remoteNeedPresent) return false;

          const remoteNeedActiveOrLoading =
            allNeeds.getIn([remoteNeedUri, "isLoading"]) ||
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
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.connectionsOverview", [
    squareImageModule,
    connectionSelectionItemModule,
    postHeaderModule,
    connectionIndicatorsModule,
    extendedConnectionIndicatorsModule,
    ngAnimate,
    createPostItemModule,
  ])
  .directive("wonConnectionsOverview", genComponentConf).name;
