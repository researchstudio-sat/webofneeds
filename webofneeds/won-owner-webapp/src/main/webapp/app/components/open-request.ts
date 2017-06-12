;

import angular from 'angular';
import {
    labels,
    relativeTime
} from '../won-label-utils';
import {
    attach,
    msStringToDate,
} from '../utils.js'
import { actionCreators }  from '../actions/actions';
import {
    selectOpenConnectionUri,
    displayingOverview,
    selectEventsOfOpenConnection,
    selectLastUpdateTime,
    selectOpenConnection,
    selectConnectMessageOfOpenConnection,
    selectLastUpdatedPerConnection,
} from '../selectors';

import {
    selectTimestamp,
    seeksOrIs,
    inferLegacyNeedType,
} from '../won-utils'

import postContentModule from './post-content';
import postHeaderModule from './post-header';

const serviceDependencies = ['$q', '$ngRedux', '$scope'];
function genComponentConf() {
    let template = `
      <div class="or__header">
        <a ui-sref="{{::self.closeRequestItemUrl()}}">
          <img class="or__header__icon clickable" src="generated/icon-sprite.svg#ico36_close"/>
        </a>

        <won-post-header
          need-uri="self.theirNeed.get('@id')"
          timestamp="self.lastUpdateTimestamp"
          hide-image="::true">
        </won-post-header>
      </div>

      <won-post-content
        need-uri="self.theirNeed.get('@id')"
        text-message="self.textMsg">
      </won-post-content>

      <div class="or__footer" ng-show="self.isSentRequest">
        Waiting for them to accept your contact request.
      </div>

      <div class="or__footer" ng-show="self.isReceivedRequest">
        <input type="text" ng-model="self.message" placeholder="Reply Message (optional, in case of acceptance)"/>
        <div class="flexbuttons">
          <button
            class="won-button--filled black"
            ui-sref="{connectionUri: null}"
            ng-click="self.closeRequest()">Decline</button>
          <button class="won-button--filled red" ng-click="self.openRequest(self.message)">Accept</button>
        </div>
        <a ng-show="self.debugmode" class="debuglink" target="_blank" href="{{self.connectionUri}}">[CNCT_DATA]</a>
    </div>
    `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);
            window.openreq4dbg = this;
            this.message='';
            this.labels = labels;
            const selectFromState = (state) => {
                const connectionUri = selectOpenConnectionUri(state);
                const connection = selectOpenConnection(state);
                const connectionState = connection && connection.get('hasConnectionState');
                const theirNeedUri = connection && connection.get('hasRemoteNeed');
                const theirNeed = state.getIn(['needs','theirNeeds', theirNeedUri]);
                const connectMsg = selectConnectMessageOfOpenConnection(state);

                const lastUpdatedPerConnection = selectLastUpdatedPerConnection(state)

                const lastStateUpdate = selectLastUpdateTime(state);

                return {
                    theirNeed,

                    connectionUri: connectionUri,
                    isSentRequest: connectionState === won.WON.RequestSent,
                    isReceivedRequest: connectionState === won.WON.RequestReceived,

                    isOverview: displayingOverview(state),
                    connection: selectOpenConnection(state),

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

                    textMsg: connectMsg && (
                        connectMsg.get('hasTextMessage') ||
                        connectMsg.getIn(['hasCorrespondingRemoteMessage', 'hasTextMessage'])
                    ),
                    debugmode: won.debugmode,
                }
            };
            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);
        }

        closeRequestItemUrl() {
            return "{connectionUri: null}";
        }

        openRequest(message){
            this.connections__open(this.connectionUri,message);
        }
        closeRequest(){
            this.connections__close(this.connectionUri);
        }
    }
    Controller.$inject = serviceDependencies;
    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {},
        template: template
    }
}

export default angular.module('won.owner.components.openRequest', [
    postContentModule,
    postHeaderModule,
])
    .directive('wonOpenRequest', genComponentConf)
    .name;

