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
    selectOwnNeeds,
    selectConnectionsByNeed,
    selectLastUpdatedPerConnection,
} from '../selectors';

const serviceDependencies = ['$ngRedux', '$scope'];
function genComponentConf() {
    let template = `
      <div ng-repeat="needUri in self.relevantOwnNeedUris">
        <won-post-header
          need-uri="needUri"
          timestamp="'TODOlatestOfThatType'">
        </won-post-header>
        <div ng-repeat="connectionUri in self.relevantConnectionUrisByNeed.get(needUri)">
          {{ connectionUri }}
        </div>
        <won-connection-selection-item
          ng-repeat="cnctUri in self.relevantConnectionUrisByNeed.get(needUri)"
          selected-connection="self.setOpen(connectionUri)"
          connection-uri="cnctUri">
        </won-connection-selection-item>
      </div>
    `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);

            window.connOvw4dbg = this;
            this.labels = labels;

            const self = this;

            const selectFromState = (state)=> {

                const connectionsByNeed = selectConnectionsByNeed(state);
                const relevantConnectionUrisByNeed = connectionsByNeed && connectionsByNeed
                    .map(connections => connections
                        .filter(c => c.get('hasConnectionState') === self.connectionType)
                        .map(c => c.get('uri'))
                        .toJS()
                    )
                    .filter(cnctUris => cnctUris.length > 0); // filter out needs without connections of that type/state

                const ownNeeds = selectOwnNeeds(state);
                const ownNeedUris = ownNeeds &&
                    ownNeeds.keySeq().toSet();

                // filter out needs without connections of that type/state
                const relevantOwnNeedUris = relevantConnectionUrisByNeed &&
                    relevantConnectionUrisByNeed.keySeq().toSet();

                const relevantOwnNeeds = relevantOwnNeedUris && ownNeeds &&
                    ownNeeds.filter(n => relevantOwnNeedUris.has(n.get('@id')));


                return {
                    lastUpdatePerConnection: selectLastUpdatedPerConnection(state),
                    relevantOwnNeedUris: relevantOwnNeedUris && relevantOwnNeedUris.toArray(),
                    relevantOwnNeeds: relevantOwnNeeds && relevantOwnNeeds.toArray(),
                    relevantConnectionUrisByNeed,
                }
            };
            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);
        }
        setOpen(connectionUri) {
            this.onSelectedConnection({connectionUri: this.connectionUri}); //trigger callback with scope-object
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
