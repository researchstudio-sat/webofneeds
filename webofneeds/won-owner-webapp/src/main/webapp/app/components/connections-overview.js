/**
 * A list over all owned posts/atoms with their connections
 * below each other. Usually limited to a connections of a
 * specific state (e.g. "hint")
 * Created by ksinger on 12.04.2017.
 */

import angular from "angular";
import Immutable from "immutable";
import ngAnimate from "angular-animate";
import squareImageModule from "./square-image.js";
import postHeaderModule from "./post-header.js";
import connectionIndicatorsModule from "./connection-indicators.js";
import connectionSelectionItemModule from "./connection-selection-item.js";
import suggestionSelectionItemModule from "./suggestion-selection-item.js";
import createPostItemModule from "./create-post-item.js";

import { attach, delay, sortByDate, get, getIn } from "../utils.js";
import { connect2Redux } from "../won-utils.js";
import { actionCreators } from "../actions/actions.js";

import "~/style/_connections-overview.scss";

import * as generalSelectors from "../selectors/general-selectors.js";
import * as connectionSelectors from "../selectors/connection-selectors.js";
import * as connectionUtils from "../connection-utils.js";
import * as atomUtils from "../atom-utils.js";
import * as viewUtils from "../view-utils.js";
import * as processUtils from "../process-utils.js";

const serviceDependencies = ["$ngRedux", "$scope"];
function genComponentConf() {
  let template = `
        <won-create-post-item ng-class="{'selected' : !!self.useCaseGroup || !!self.useCase}"></won-create-post-item>
        <div ng-repeat="atomUri in self.beingCreatedAtomUris track by atomUri" class="co__item">
            <div class="co__item__atom" ng-class="{'selected' : atomUri === self.atomUriInRoute}">
                <div class="co__item__atom__indicator"></div>
                <div class="co__item__atom__header">
                    <won-post-header
                        atom-uri="::atomUri"
                        ng-click="self.toggleDetails(atomUri)"
                        ng-class="{ 'clickable' : !self.isAtomLoading(atomUri) }">
                    </won-post-header>
                </div>
            </div>
        </div>
        <div ng-repeat="atomUri in self.sortedOpenAtomUris track by atomUri" class="co__item">
            <div class="co__item__atom" ng-class="{'won-unread': self.isUnread(atomUri), 'selected' : atomUri === self.atomUriInRoute, 'open': self.isOpen(atomUri)}">
                <div class="co__item__atom__indicator"></div>
                <div class="co__item__atom__header">
                    <won-post-header
                        atom-uri="::atomUri"
                        ng-click="self.toggleDetails(atomUri)"
                        ng-class="{ 'clickable' : !self.isAtomLoading(atomUri) }">
                    </won-post-header>
                    <won-connection-indicators
                        on-selected-connection="::self.selectConnection(connectionUri)"
                        atom-uri="::atomUri">
                    </won-connection-indicators>
                    <button
                        class="co__item__atom__header__button red"
                        ng-click="self.showAtomDetails(atomUri)"
                        ng-class="{
                          'won-button--filled' : atomUri === self.atomUriInRoute,
                          'won-button--outlined thin': atomUri !== self.atomUriInRoute
                        }">
                        Details
                    </button>
                    <div class="co__item__atom__header__carret clickable" ng-click="!self.isAtomLoading(atomUri) && self.toggleDetails(atomUri)">
                        <svg class="co__item__atom__header__carret__icon"
                            ng-class="{
                              'won-icon-expanded': self.isOpen(atomUri),
                              'won-icon-disabled': !self.hasOpenOrLoadingChatConnections(atomUri, self.allAtoms, self.process) || self.isAtomLoading(atomUri),
                            }">
                            <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
                        </svg>
                    </div>
                </div>
            </div>
            <div class="co__item__connections"
                ng-if="self.isOpen(atomUri) && self.hasOpenOrLoadingChatConnections(atomUri, self.allAtoms, self.process)">
                <div class="co__item__connections__item"
                  ng-if="self.hasChatSocket(atomUri)"
                  ng-repeat="connUri in self.getOpenChatConnectionUrisArraySorted(atomUri, self.allAtoms, self.process) track by connUri"
                  ng-class="{
                    'won-unread': self.isConnectionUnread(atomUri, connUri),
                    'selected': connUri === self.connUriInRoute
                  }">
                  <won-connection-selection-item
                      on-selected-connection="::self.selectConnection(connectionUri)"
                      connection-uri="::connUri"
                      ng-class="{'won-unread': self.isConnectionUnread(atomUri, connUri)}">
                  </won-connection-selection-item>
                </div>
                <div class="co__item__connections__item nonsticky" ng-if="self.hasChatSocket(atomUri) && self.hasSuggestedConnections(atomUri)"
                  ng-class="{
                    'won-unread': self.hasUnreadSuggestedConnections(atomUri),
                    'selected': self.isShowingSuggestions(atomUri),
                  }">
                  <won-suggestion-selection-item
                      atom-uri="::atomUri"
                      on-selected="self.showAtomSuggestions(atomUri)">
                  </won-suggestion-selection-item>
                </div>
            </div>
        </div>
        <div class="co__separator clickable" ng-class="{'co__separator--open' : self.showClosedAtoms}" ng-if="self.hasClosedAtoms()" ng-click="self.toggleClosedAtoms()">
            <span class="co__separator__text">Archived Posts ({{self.getClosedAtomsText()}})</span>
            <svg
                style="--local-primary:var(--won-secondary-color);"
                class="co__separator__arrow"
                ng-if="self.showClosedAtoms">
                <use xlink:href="#ico16_arrow_up" href="#ico16_arrow_up"></use>
            </svg>
            <svg style="--local-primary:var(--won-secondary-color);"
                class="co__separator__arrow"
                ng-if="!self.showClosedAtoms">
                <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
            </svg>
        </div>
        <div class="co__closedAtoms" ng-if="self.showClosedAtoms && self.closedAtomsSize > 0">
            <div ng-repeat="atomUri in self.sortedClosedAtomUris track by atomUri" class="co__item">
                <div class="co__item__atom" ng-class="{'won-unread': self.isUnread(atomUri), 'selected' : atomUri === self.atomUriInRoute, 'open': self.isOpen(atomUri)}">
                    <div class="co__item__atom__indicator"></div>
                    <div class="co__item__atom__header">
                        <won-post-header
                            atom-uri="::atomUri"
                            ng-click="self.toggleDetails(atomUri)"
                            ng-class="{ 'clickable' : !self.isAtomLoading(atomUri) }">
                        </won-post-header>
                        <button
                            class="co__item__atom__header__button red"
                            ng-click="self.showAtomDetails(atomUri)"
                            ng-class="{
                              'won-button--filled' : atomUri === self.atomUriInRoute,
                              'won-button--outlined thin': atomUri !== self.atomUriInRoute
                            }">
                            Details
                        </button>
                        <div class="co__item__atom__header__carret clickable" ng-click="!self.isAtomLoading(atomUri) && self.toggleDetails(atomUri)">
                          <svg class="co__item__atom__header__carret__icon"
                              ng-class="{
                                'won-icon-expanded': self.isOpen(atomUri),
                                'won-icon-disabled': self.isAtomLoading(atomUri),
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
      //this.labels = labels;
      window.co4dbg = this;

      const self = this;
      const selectFromState = state => {
        const allAtoms = generalSelectors.getPosts(state);
        const openAtoms = generalSelectors.getOwnedOpenPosts(state);
        const closedAtoms = generalSelectors.getOwnedClosedPosts(state);

        // atoms that have been created but are not confirmed by the server yet
        const beingCreatedAtoms = generalSelectors.getOwnedAtomsInCreation(
          state
        );

        const connectionsToCrawl = connectionSelectors.getChatConnectionsToCrawl(
          state
        );

        const useCase = generalSelectors.getUseCaseFromRoute(state);
        const useCaseGroup = generalSelectors.getUseCaseGroupFromRoute(state);
        const connUriInRoute = generalSelectors.getConnectionUriFromRoute(
          state
        );
        const atomUriInRoute = generalSelectors.getPostUriFromRoute(state);
        const atomImpliedInRoute =
          connUriInRoute &&
          generalSelectors.getOwnedAtomByConnectionUri(state, connUriInRoute);
        const atomUriImpliedInRoute =
          atomImpliedInRoute && atomImpliedInRoute.get("uri");

        const sortedOpenAtoms = sortByDate(openAtoms, "creationDate");
        const sortedClosedAtoms = sortByDate(closedAtoms, "creationDate");

        const process = get(state, "process");
        const unloadedAtoms = closedAtoms.filter(atom =>
          processUtils.isAtomToLoad(process, atom.get("uri"))
        );

        const viewState = get(state, "view");

        return {
          allAtoms,
          process,
          viewState,
          showClosedAtoms: viewUtils.showClosedAtoms(viewState),
          useCase,
          useCaseGroup,
          atomUriInRoute,
          atomUriImpliedInRoute,
          connUriInRoute,
          beingCreatedAtomUris: beingCreatedAtoms && [
            ...beingCreatedAtoms.keys(),
          ],
          sortedOpenAtomUris: sortedOpenAtoms && [
            ...sortedOpenAtoms.flatMap(atom => atom.get("uri")),
          ],
          sortedClosedAtomUris: sortedClosedAtoms && [
            ...sortedClosedAtoms.flatMap(atom => atom.get("uri")),
          ],
          connectionsToCrawl: connectionsToCrawl || Immutable.Map(),
          unloadedAtomsSize: unloadedAtoms ? unloadedAtoms.size : 0,
          closedAtomsSize: closedAtoms ? closedAtoms.size : 0,
        };
      };
      connect2Redux(
        selectFromState,
        actionCreators,
        ["self.connectionUri"],
        this
      );

      this.$scope.$watch("self.atomUriImpliedInRoute", (newValue, oldValue) => {
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

    toggleDetails(ownedAtomUri) {
      for (let key in this.open) {
        if (key !== ownedAtomUri) {
          this.open[key] = false;
        }
      }

      if (this.isOpen(ownedAtomUri)) {
        this.open[ownedAtomUri] = false;
        if (this.isOpenByConnection(ownedAtomUri)) {
          this.atoms__selectTab(
            Immutable.fromJS({ atomUri: ownedAtomUri, selectTab: "DETAIL" })
          );
          this.router__stateGoCurrent({
            useCase: undefined,
            useCaseGroup: undefined,
            connectionUri: undefined,
            fromAtomUri: undefined,
            mode: undefined,
          });
        }
      } else {
        this.open[ownedAtomUri] = true;
      }
    }

    showAtomSuggestions(atomUri) {
      this.showAtomTab(atomUri, "SUGGESTIONS");
    }

    showAtomDetails(atomUri) {
      this.showAtomTab(atomUri, "DETAIL");
    }

    showAtomTab(atomUri, tab = "DETAIL") {
      this.atoms__selectTab(
        Immutable.fromJS({ atomUri: atomUri, selectTab: tab })
      );
      this.router__stateGo("post", { postUri: atomUri });
    }

    hasChatSocket(atomUri) {
      const atom = get(this.allAtoms, atomUri);
      return atomUtils.hasChatSocket(atom);
    }

    hasSuggestedConnections(atomUri) {
      const atom = get(this.allAtoms, atomUri);
      return atomUtils.hasSuggestedConnections(atom);
    }

    hasUnreadSuggestedConnections(atomUri) {
      const atom = get(this.allAtoms, atomUri);
      return atomUtils.hasUnreadSuggestedConnections(atom);
    }

    isUnread(atomUri) {
      const atom = get(this.allAtoms, atomUri);
      return get(atom, "unread");
    }

    isConnectionUnread(atomUri, connUri) {
      const conn = getIn(this.allAtoms, [atomUri, "connections", connUri]);
      return get(conn, "unread");
    }

    hasClosedAtoms() {
      return this.closedAtomsSize > 0;
    }

    getClosedAtomsText() {
      let output = [];
      if (this.closedAtomsSize > 0) {
        output.push(`${this.closedAtomsSize}`);
      }
      if (this.unloadedAtomsSize > 0) {
        output.push(`${this.unloadedAtomsSize} unloaded`);
      }

      return output.join(" - ");
    }

    toggleClosedAtoms() {
      if (this.unloadedAtomsSize > 0) {
        this.atoms__fetchUnloadedAtoms();
      }
      this.view__toggleClosedAtoms();
    }

    isOpen(ownedAtomUri) {
      return this.isOpenByConnection(ownedAtomUri) || !!this.open[ownedAtomUri];
    }

    isShowingSuggestions(ownedAtomUri) {
      const visibleTab = viewUtils.getVisibleTabByAtomUri(
        this.viewState,
        ownedAtomUri
      );
      return (
        !!this.open[ownedAtomUri] &&
        ownedAtomUri === this.atomUriInRoute &&
        visibleTab === "SUGGESTIONS"
      );
    }

    isAtomLoading(atomUri) {
      return processUtils.isAtomLoading(this.process, atomUri);
    }

    isOpenByConnection(ownedAtomUri) {
      return this.atomUriImpliedInRoute === ownedAtomUri;
    }

    selectConnection(connectionUri) {
      this.onSelectedConnection({ connectionUri }); //trigger callback with scope-object
    }

    selectSuggested(atomUri) {
      console.debug("stuff should happen now IMPL ME for: ", atomUri);
    }

    hasOpenOrLoadingChatConnections(atomUri, allAtoms, process) {
      const atom = get(allAtoms, atomUri);

      if (!atom) {
        return false;
      }
      return (
        atomUtils.isActive(atom) &&
        !!atom.get("connections").find(conn => {
          if (
            !connectionSelectors.isChatToXConnection(allAtoms, conn) &&
            !connectionSelectors.isGroupToXConnection(allAtoms, conn)
          )
            return false;
          if (processUtils.isConnectionLoading(process, conn.get("uri")))
            return true; //if connection is currently loading we assume its a connection we want to show

          const targetAtomUri = conn.get("targetAtomUri");
          const targetAtomPresent =
            targetAtomUri && allAtoms && !!allAtoms.get(targetAtomUri);

          if (!targetAtomPresent) return true; //if the targetAtom is not present yet we assume its a connection we want

          const targetAtomActiveOrLoading =
            process.getIn(["atoms", targetAtomUri, "loading"]) ||
            process.getIn(["atoms", targetAtomUri, "failedToLoad"]) ||
            atomUtils.isActive(get(allAtoms, targetAtomUri));

          return targetAtomActiveOrLoading && !connectionUtils.isClosed(conn);
        })
      );
    }

    getOpenChatConnectionUrisArraySorted(atomUri, allAtoms, process) {
      const atom = get(allAtoms, atomUri);

      if (!atom) {
        return undefined;
      }
      const sortedConnections = sortByDate(
        atom.get("connections").filter(conn => {
          if (
            !connectionSelectors.isChatToXConnection(allAtoms, conn) &&
            !connectionSelectors.isGroupToXConnection(allAtoms, conn)
          )
            return false;
          if (processUtils.isConnectionLoading(process, conn.get("uri")))
            return true; //if connection is currently loading we assume its a connection we want to show

          const targetAtomUri = conn.get("targetAtomUri");
          const targetAtomPresent =
            targetAtomUri && allAtoms && !!allAtoms.get(targetAtomUri);

          if (!targetAtomPresent) return false;

          const targetAtomActiveOrLoading =
            process.getIn(["atoms", targetAtomUri, "loading"]) ||
            process.getIn(["atoms", targetAtomUri, "failedToLoad"]) ||
            atomUtils.isActive(get(allAtoms, targetAtomUri));

          return (
            targetAtomActiveOrLoading &&
            !connectionUtils.isClosed(conn) &&
            !connectionUtils.isSuggested(conn)
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
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.connectionsOverview", [
    squareImageModule,
    connectionSelectionItemModule,
    suggestionSelectionItemModule,
    postHeaderModule,
    connectionIndicatorsModule,
    ngAnimate,
    createPostItemModule,
  ])
  .directive("wonConnectionsOverview", genComponentConf).name;
