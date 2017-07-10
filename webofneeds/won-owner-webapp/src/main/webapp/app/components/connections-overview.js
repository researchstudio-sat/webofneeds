/**
 * A list over all owned posts/needs with their connections
 * below each other. Usually limited to a connections of a
 * specific state (e.g. "hint")
 * Created by ksinger on 12.04.2017.
 */

import won from '../won-es6';
import angular from 'angular';
import squareImageModule from './square-image';
import connectionSelectionModule from './connection-selection';
import postHeaderModule from './post-header';

import {
    labels,
} from '../won-label-utils';

import { attach } from '../utils.js';
import { actionCreators }  from '../actions/actions';

import {
    selectAllOwnNeeds,
    selectConnectionsByNeed,
    selectLastUpdatedPerConnection,
    selectRouterParams,
    selectUnreadCountsByNeedAndType,
} from '../selectors';

const serviceDependencies = ['$ngRedux', '$scope'];
function genComponentConf() {
    let template = `
      <div ng-repeat="needUri in self.relevantOwnNeedUris">
        <div class="covw__own-need clickable"
          ng-click="self.toggleConnections(needUri)">
          <won-post-header
            need-uri="needUri"
            timestamp="'TODOlatestOfThatType'">
          </won-post-header>

          <div class="covw__unreadCount">
            {{
              self.unreadCounts.getIn([
                  needUri, self.messageType
              ])
            }}
          </div>
          <img class="covw__arrow" ng-show="self.isOpen(needUri)"
              src="generated/icon-sprite.svg#ico16_arrow_up"/>
          <img class="covw__arrow" ng-show="!self.isOpen(needUri)"
              src="generated/icon-sprite.svg#ico16_arrow_down"/>
        </div>
        <won-connection-selection-item
          ng-show="self.isOpen(needUri)"
          ng-repeat="cnctUri in self.relevantConnectionUrisByNeed.get(needUri)"
          on-selected-connection="self.selectConnection(connectionUri)"
          connection-uri="cnctUri">
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

                const connectionsByNeed = selectConnectionsByNeed(state);
                const relevantConnectionUrisByNeed = connectionsByNeed && connectionsByNeed
                    .map(connections => connections
                        .filter(c => c && c.get('hasConnectionState') === self.connectionType)
                        .map(c => c.get('uri'))
                        .toSet()
                    )
                    .filter(cnctUris => cnctUris.size > 0); // filter out needs without connections of that type/state

                const ownNeeds = selectAllOwnNeeds(state);
                const ownNeedUris = ownNeeds && ownNeeds.keySeq().toSet();



                // filter out needs without connections of that type/state
                const relevantOwnNeedUris = relevantConnectionUrisByNeed &&
                    relevantConnectionUrisByNeed.keySeq().toSet();

                const relevantOwnNeeds = relevantOwnNeedUris && ownNeeds &&
                    ownNeeds.filter(n => relevantOwnNeedUris.has(n.get('@id')));

                const routerParams = selectRouterParams(state);
                const cnctInRoute = routerParams &&
                    decodeURIComponent(routerParams.get('connectionUri'));
                const needImpliedInRoute = cnctInRoute && relevantConnectionUrisByNeed &&
                    relevantConnectionUrisByNeed
                        .filter(cncts => cncts.has(cnctInRoute)) // find ownNeed associated with the connection
                        .keySeq() // get needUris
                        .first(); // the openConnectionUri can only be associated with one need

                return {
                    needImpliedInRoute,
                    lastUpdatePerConnection: selectLastUpdatedPerConnection(state),
                    relevantOwnNeedUris: relevantOwnNeedUris && relevantOwnNeedUris.toArray(),
                    relevantOwnNeeds: relevantOwnNeeds && relevantOwnNeeds.toArray(),
                    relevantConnectionUrisByNeed: relevantConnectionUrisByNeed &&
                        relevantConnectionUrisByNeed.map(cncts => cncts.toJS()),

                    unreadCounts: selectUnreadCountsByNeedAndType(state),

                    messageType: won.cnctState2MessageType[this.connectionType],
                }
            };
            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);
        }
        toggleConnections(ownNeedUri) {
            this.open[ownNeedUri] = !this.open[ownNeedUri]
        }
        isOpen(ownNeedUri) {
            return !!this.open[ownNeedUri] ||
                this.needImpliedInRoute === ownNeedUri;
        }
        selectConnection(connectionUri) {
            this.onSelectedConnection({connectionUri}); //trigger callback with scope-object
            //TODO either publish a dom-event as well; or directly call the route-change
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
