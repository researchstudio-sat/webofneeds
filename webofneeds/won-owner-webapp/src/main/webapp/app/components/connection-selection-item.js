/**
 * Created by ksinger on 10.04.2017.
 */

import won from '../won-es6';
import angular from 'angular';
import squareImageModule from './square-image';
import {
    labels,
    relativeTime,
} from '../won-label-utils';
import { attach, decodeUriComponentProperly } from '../utils';
import { actionCreators }  from '../actions/actions';
import {
    selectOpenConnectionUri,
    selectAllByConnections,
    selectOpenPost,
    selectOpenPostUri,
    selectLastUpdatedPerConnection,
    selectLastUpdateTime,
    selectUnreadCountsByConnectionAndType,
} from '../selectors';

import {
    selectTimestamp,
    seeksOrIs,
    inferLegacyNeedType,
} from '../won-utils'
import postHeaderModule from './post-header';

const serviceDependencies = ['$ngRedux', '$scope'];
function genComponentConf() {
    let template = `
      <div
      class="conn__inner"
      ng-class="self.isOpen() ? 'selected' : ''">
        <won-post-header
          need-uri="self.theirNeed.get('@id')"
          timestamp="self.lastUpdateTimestamp"
          ng-click="self.setOpen()">
        </won-post-header>

        <div class="conn__unreadCount">
          {{ self.unreadCount }}
        </div>
        <img
          class="conn__icon clickable"
          src="generated/icon-sprite.svg#ico_settings_grey"
          ng-click="self.settingsOpen = true">
      </div>

      <div class="conn__contextmenu"
        ng-show="self.settingsOpen">
          <img
            class="conn__icon clickable"
            src="generated/icon-sprite.svg#ico_settings_hi"
            ng-click="self.settingsOpen = false">
          <button
            class="won-button--filled thin red"
            ng-click="self.closeConnection()">
              Close Connection
          </button>
      </div>
    `;

    class Controller {
        constructor() {
            window.connSelItm4dbg = this;
            attach(this, serviceDependencies, arguments);
            this.labels = labels;
            //this.settingsOpen = false;

            this.cnctState2MessageType = won.cnctState2MessageType;

            const self = this;

            const selectFromState = (state)=> {

                const connectionData = selectAllByConnections(state).get(this.connectionUri);
                const connectionUri = connectionData && connectionData.getIn(['connection', 'uri']);
                const ownNeed = connectionData && connectionData.get('ownNeed');
                const theirNeed = connectionData && connectionData.get('remoteNeed');

                const lastStateUpdate = selectLastUpdateTime(state);
                const lastUpdatedPerConnection = selectLastUpdatedPerConnection(state);

                const connectionType = connectionData && connectionData
                    .getIn(['connection','hasConnectionState']);

                const unreadCounts = selectUnreadCountsByConnectionAndType(state);
                const messageType = won.cnctState2MessageType[connectionType];
                return {
                    openConnectionUri: selectOpenConnectionUri(state),

                    ownNeed,
                    ownNeedType: ownNeed && inferLegacyNeedType(ownNeed),
                    ownNeedContent: ownNeed && seeksOrIs(ownNeed),

                    theirNeed,
                    theirNeedType: theirNeed && inferLegacyNeedType(theirNeed),
                    theirNeedContent: theirNeed && seeksOrIs(theirNeed),
                    theirNeedCreatedOn: theirNeed && relativeTime(
                        lastStateUpdate,
                        theirNeed.get('dct:created')
                    ),
                    lastUpdateTimestamp: lastUpdatedPerConnection.get(connectionUri),
                    lastUpdated: lastUpdatedPerConnection &&
                        relativeTime(
                            lastStateUpdate,
                            lastUpdatedPerConnection.get(connectionUri)
                        ),

                    unreadCount: unreadCounts && unreadCounts
                        .getIn([connectionUri, messageType])
                }
            }

            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);
        }
        isOpen() {
            return this.openConnectionUri === this.connectionUri;
        }

        setOpen() {
            this.onSelectedConnection({connectionUri: this.connectionUri}); //trigger callback with scope-object
            //TODO either publish a dom-event as well; or directly call the route-change
        }

        closeConnection() {
            this.settingsOpen = false;
            this.connections__close(this.connectionUri);
        }
    }
    Controller.$inject = serviceDependencies;
    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {
            connectionUri: "=",
            /*
             * Usage:
             *  on-selected-connection="myCallback(connectionUri)"
             */
            onSelectedConnection: "&"
        },
        template: template
    }
}
export default angular.module('won.owner.components.connectionSelectionItem', [
        postHeaderModule,
    ])
    .directive('wonConnectionSelectionItem', genComponentConf)
    .name;
