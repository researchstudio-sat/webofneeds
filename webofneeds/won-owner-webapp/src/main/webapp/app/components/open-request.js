;

import angular from 'angular';
import {
    labels,
} from '../won-label-utils';
import {
    attach,
} from '../utils.js'
import { actionCreators }  from '../actions/actions';
import {
    selectOpenConnectionUri,
    selectNeedByConnectionUri,
} from '../selectors';

import postContentModule from './post-content';
import postHeaderModule from './post-header';

const serviceDependencies = ['$ngRedux', '$scope'];
function genComponentConf() {
    let template = `
      <div class="or__header">
        <a ui-sref="{{::self.closeRequestItemUrl()}}">
          <img class="or__header__icon clickable" src="generated/icon-sprite.svg#ico36_close"/>
        </a>

        <won-post-header
          need-uri="self.remoteNeedUri"
          timestamp="self.lastUpdateTimestamp"
          hide-image="::true">
        </won-post-header>
      </div>

      <won-post-content
        need-uri="self.remoteNeedUri"
        text-message="self.textMsg">
      </won-post-content>

      <div class="or__footer" ng-show="self.isSentRequest">
        Waiting for them to accept your contact request.
      </div>

      <div class="or__footer" ng-show="self.isReceivedRequest">
        {{self.textMsg}}
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

                const ownNeed = selectNeedByConnectionUri(state, connectionUri);
                const connection = ownNeed && ownNeed.getIn(["connections", connectionUri]);
                const connectMsg = connection && connection.get("messages").filter(msg => msg.get("connectMessage") && !msg.get("outgoingMessage")).first();

                return {
                    connectionUri,
                    remoteNeedUri: connection && connection.get("remoteNeedUri"),
                    isSentRequest: connection && connection.get('state') === won.WON.RequestSent,
                    isReceivedRequest: connection && connection.get('state') === won.WON.RequestReceived,
                    lastUpdateTimestamp: connection && connection.get('creationDate'), //TODO: CORRECT TIMESTAMP LAST UPDATE
                    textMsg: connectMsg && connectMsg.get("text"),
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
            this.connections__open(this.connectionUri, message);
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

