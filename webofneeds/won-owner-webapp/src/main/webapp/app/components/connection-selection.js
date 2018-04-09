/**
 * Created by ksinger on 22.03.2016.
 */
;

import angular from 'angular';
import {
    getIn,
    attach,
    decodeUriComponentProperly,
} from '../utils.js';
import {
    labels,
} from '../won-label-utils.js';
import {
    connect2Redux,
} from '../won-utils.js';
import { actionCreators }  from '../actions/actions.js';
import {
    selectOpenPostUri,
} from '../selectors.js';
import won from '../won-es6.js';

import connectionSelectionItemModule from './connection-selection-item.js';


const serviceDependencies = ['$ngRedux', '$scope'];
function genComponentConf() {
    let template = `
      <won-connection-selection-item
        ng-repeat="conn in self.connectionsArray"
        on-selected-connection="self.setOpen(connectionUri)"
        connection-uri="conn.get('uri')"
        ng-class="{'won-unread': conn.get('unread')}">
      </won-connection-selection-item>
    `;

    class Controller {
        constructor() {
            window.connSel4dbg = this;
            attach(this, serviceDependencies, arguments);
            this.labels = labels;
            this.settingsOpen = false;
            this.WON = won.WON;

            const self = this;

            const selectFromState = (state)=>{
                const postUri = selectOpenPostUri(state);
                const ownNeed = getIn(state, ["needs", postUri]);

                // TODO: delete connectionTypeInParams
                const connectionTypeInParams = decodeUriComponentProperly(
                    getIn(state, ['router', 'currentParams', 'connectionType'])
                );
                
                const connectionType = self.connectionType;
                // const connectionType = connectionTypeInParams || self.connectionType;
                const connections = ownNeed && ownNeed.get("connections").filter(conn => conn.get("state") !== won.WON.Closed);

                let sortedConnections = connections && connections.toArray();
                if(sortedConnections) {
                    sortedConnections.sort(function(a,b) {
                        const bDate = b.get("lastUpdateDate");
                        const aDate = b.get("lastUpdateDate");

                        if(!!bDate && !!aDate) return 0;
                        if(!!bDate) return -1;
                        if(!!aDate) return 1;
                        return b.get("lastUpdateDate").getTime() - a.get("lastUpdateDate").getTime();
                    });
                }

                return {
                    connections,
                    connectionsArray: sortedConnections,
                };
            };

            connect2Redux(selectFromState, actionCreators, ['self.connectionType'], this);
        }

        setOpen(connectionUri) {
            this.onSelectedConnection({connectionUri: connectionUri}); //trigger callback with scope-object
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
            /*
             * Usage:
             *  on-selected-connection="myCallback(connectionUri)"
             */
            onSelectedConnection: "&"
        },
        template: template
    }

}



export default angular.module('won.owner.components.connectionSelection', [
        connectionSelectionItemModule,
    ])
    .directive('wonConnectionSelection', genComponentConf)
    .name;
