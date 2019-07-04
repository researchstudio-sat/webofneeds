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

import { sortByDate, get, getIn } from "../utils.js";
import { attach } from "../cstm-ng-utils.js";
import { connect2Redux } from "../configRedux.js";
import { actionCreators } from "../actions/actions.js";

import "~/style/_connections-overview.scss";

import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as connectionSelectors from "../redux/selectors/connection-selectors.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as processUtils from "../redux/utils/process-utils.js";

const serviceDependencies = ["$ngRedux", "$scope"];
function genComponentConf() {
  let template = `
        <div ng-repeat="atomUri in self.sortedOpenAtomUris track by atomUri" class="co__item">
            <div class="co__item__atom">
                <div class="co__item__atom__header">
                    <won-post-header
                        atom-uri="::atomUri"
                        ng-click="!self.isAtomLoading(atomUri) && self.showAtomDetails(atomUri)"
                        ng-class="{ 'clickable' : !self.isAtomLoading(atomUri) }">
                    </won-post-header>
                    <won-connection-indicators
                        on-selected-connection="::self.selectConnection(connectionUri)"
                        atom-uri="::atomUri">
                    </won-connection-indicators>
                </div>
            </div>
            <div class="co__item__connections">
                <div class="co__item__connections__item"
                  ng-repeat="connUri in self.getOpenChatConnectionUrisArraySorted(atomUri, self.allAtoms, self.process) track by connUri"
                  ng-class="{
                    'won-unread': self.isConnectionUnread(atomUri, connUri)
                  }">
                  <won-connection-selection-item
                      on-selected-connection="::self.selectConnection(connectionUri)"
                      connection-uri="::connUri"
                      ng-class="{'won-unread': self.isConnectionUnread(atomUri, connUri)}">
                  </won-connection-selection-item>
                </div>
            </div>
        </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      window.co4dbg = this;

      const selectFromState = state => {
        const allAtoms = generalSelectors.getPosts(state);
        const openAtoms = generalSelectors.getChatAtoms(state);

        const connUriInRoute = generalSelectors.getConnectionUriFromRoute(
          state
        );

        const sortedOpenAtoms = sortByDate(openAtoms, "creationDate");
        const process = get(state, "process");

        return {
          allAtoms,
          process,
          connUriInRoute,
          sortedOpenAtomUris: sortedOpenAtoms && [
            ...sortedOpenAtoms.flatMap(atom => atom.get("uri")),
          ],
        };
      };
      connect2Redux(
        selectFromState,
        actionCreators,
        ["self.connectionUri"],
        this
      );
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

    isConnectionUnread(atomUri, connUri) {
      const conn = getIn(this.allAtoms, [atomUri, "connections", connUri]);
      return get(conn, "unread");
    }

    isAtomLoading(atomUri) {
      return processUtils.isAtomLoading(this.process, atomUri);
    }

    selectConnection(connectionUri) {
      this.onSelectedConnection({ connectionUri }); //trigger callback with scope-object
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
    postHeaderModule,
    connectionIndicatorsModule,
    ngAnimate,
  ])
  .directive("wonConnectionsOverview", genComponentConf).name;
