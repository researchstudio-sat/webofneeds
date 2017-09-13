/**
 * A list over all owned posts/needs with their connections
 * below each other. Usually limited to a connections of a
 * specific state (e.g. "hint")
 * Created by ksinger on 12.04.2017.
 */

import won from '../won-es6.js';
import angular from 'angular';
import squareImageModule from './square-image.js';
import connectionSelectionModule from './connection-selection.js';
import postHeaderModule from './post-header.js';

import {
    labels,
} from '../won-label-utils.js';

import { attach } from '../utils.js';
import {
    connect2Redux,
} from '../won-utils.js';
import { actionCreators }  from '../actions/actions.js';

import {
    selectAllOwnNeeds,
    selectRouterParams,
    selectNeedByConnectionUri,
} from '../selectors.js';

const serviceDependencies = ['$ngRedux', '$scope'];
function genComponentConf() {
    let template = `
      <div ng-repeat="need in self.relevantOwnNeeds">
        <div class="covw__own-need clickable"
          ng-click="self.toggleConnections(need.get('uri'))">
          <won-post-header
            need-uri="need.get('uri')"
            timestamp="'TODOlatestOfThatType'">
          </won-post-header>

          <div class="covw__unreadCount">
            {{self.getUnreadConnectionsCountFilteredByType(need)}}
          </div>
          <img class="covw__arrow" ng-show="self.isOpen(need.get('uri'))"
              src="generated/icon-sprite.svg#ico16_arrow_up"/>
          <img class="covw__arrow" ng-show="!self.isOpen(need.get('uri'))"
              src="generated/icon-sprite.svg#ico16_arrow_down"/>
        </div>
        <won-connection-selection-item
          ng-show="self.isOpen(need.get('uri'))"
          ng-repeat="conn in self.getConnectionsArrayFilteredByType(need)"
          on-selected-connection="self.selectConnection(connectionUri)"
          connection-uri="conn.get('uri')">
        </won-connection-selection-item>
      </div>
    `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);

            this.labels = labels;
            this.open = {};

            const self = this;
            const selectFromState = (state)=> {
                //Select all needs with at least one connection
                const relevantOwnNeeds = selectAllOwnNeeds(state).filter(need => need.get("connections").filter(conn => conn.get("state") === this.connectionType).size > 0);
                const routerParams = selectRouterParams(state);
                const connUriInRoute = routerParams && decodeURIComponent(routerParams['connectionUri']);
                const needImpliedInRoute = connUriInRoute && selectNeedByConnectionUri(state, connUriInRoute);

                return {
                    needImpliedInRoute,
                    relevantOwnNeeds: relevantOwnNeeds && relevantOwnNeeds.toArray(),
                }
            };
            connect2Redux(selectFromState, actionCreators, ['self.connectionType'], this);
        }
        toggleConnections(ownNeedUri) {
            this.open[ownNeedUri] = !this.open[ownNeedUri]
        }
        isOpen(ownNeedUri) {
            return !!this.open[ownNeedUri] || this.needImpliedInRoute === ownNeedUri;
        }
        selectConnection(connectionUri) {
            this.onSelectedConnection({connectionUri}); //trigger callback with scope-object
        }
        getConnectionsArrayFilteredByType(need) {
            return need.get('connections').filter(conn => conn.get('state') === this.connectionType).toArray();
        }
        getUnreadConnectionsCountFilteredByType(need){
            return need.get('connections').filter(conn => conn.get('newConnection') && conn.get('state') === this.connectionType).size;
        }
    }
    Controller.$inject = serviceDependencies;
    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {
            connectionType: "=",
            /*
             * Usage:
             *  on-selected-connection="myCallback(connectionUri)"
             */
            onSelectedConnection: "&"
        },
        template: template
    }

}



export default angular.module('won.owner.components.connectionsOverview', [
        squareImageModule,
        connectionSelectionModule,
        postHeaderModule,
    ])
    .directive('wonConnectionsOverview', genComponentConf)
    .name;
