/**
 * Created by ksinger on 22.03.2016.
 */
;

import won from '../won-es6';
import angular from 'angular';
import squareImageModule from './square-image';
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
      <div class="connectionSelectionItemLine"
        ng-repeat="(key,connectionUri) in self.connectionUris">
        <won-connection-selection-item
          class="conn"
          selected-connection="self.selectedConnection({connectionUri: connectionUri})"
          connection-uri="connectionUri">
        </won-connection-selection-item>
      </div>
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

                const connectionUris = allByConnections
                    .filter(conn =>
                        conn.getIn(['connection', 'hasConnectionState']) === connectionType &&
                        conn.getIn(['ownNeed', '@id']) === postUri
                    )
                    .map(conn => conn.getIn(['connection','uri']))
                    .toList().toJS();

                return {
                    connectionUris,
                };
            }

            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);
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
             *  selected-connection="myCallback(connectionUri)"
             */
            selectedConnection: "&"
        },
        template: template
    }

}



export default angular.module('won.owner.components.connectionSelection', [
        connectionSelectionItemModule,
    ])
    .directive('wonConnectionSelection', genComponentConf)
    .name;
