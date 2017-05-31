/**
 * Created by ksinger on 22.03.2016.
 */
;

import won from '../won-es6';
import angular from 'angular';
import {
    labels,
    relativeTime,
} from '../won-label-utils';
import { attach, decodeUriComponentProperly } from '../utils.js';
import { actionCreators }  from '../actions/actions';
import {
    selectOpenConnectionUri,
    selectAllByConnections,
    selectOpenPost,
    selectOpenPostUri,
    selectLastUpdatedPerConnection,
    selectLastUpdateTime,
    selectConnectionsByNeed,
} from '../selectors';

import connectionSelectionItemModule from './connection-selection-item';

import {
    selectTimestamp,
    seeksOrIs,
    inferLegacyNeedType,
} from '../won-utils'

const serviceDependencies = ['$ngRedux', '$scope'];
function genComponentConf() {
    let template = `
      <won-connection-selection-item
        ng-repeat="(key,cnctUri) in self.connectionUris"
        on-selected-connection="self.setOpen(connectionUri)"
        connection-uri="cnctUri">
      </won-connection-selection-item>
    `;

    class Controller {
        constructor() {
            window.connSel4dbg = this;
            attach(this, serviceDependencies, arguments);
            this.labels = labels;
            this.settingsOpen = false;

            const self = this;

            const selectFromState = (state)=>{
                const postUri = selectOpenPostUri(state);
                const allByConnections = selectAllByConnections(state);
                const ownNeed = selectOpenPost(state);

                const connectionTypeInParams = decodeUriComponentProperly(
                    state.getIn(['router', 'currentParams', 'connectionType'])
                );
                const connectionType = connectionTypeInParams || self.connectionType;

                const connectionsByNeed = selectConnectionsByNeed(state);
                const connections = connectionsByNeed && connectionsByNeed.get(postUri)

                const connectionUris = !connections?
                    [] :
                    connections
                        .filter(c => c && c.get('hasConnectionState') === this.connectionType)
                        .map(c => c.get('uri'))
                        .toJS();

                return {
                    connectionUris,
                };
            }

            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);
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



export default angular.module('won.owner.components.connectionSelection', [
        connectionSelectionItemModule,
    ])
    .directive('wonConnectionSelection', genComponentConf)
    .name;
