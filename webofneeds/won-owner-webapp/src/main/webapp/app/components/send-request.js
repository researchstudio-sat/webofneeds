;

import angular from 'angular';
import 'ng-redux';
import postContentModule from './post-content.js';
import postHeaderModule from './post-header.js';
import {
    selectOpenPostUri,
    selectNeedByConnectionUri,
} from '../selectors.js';
import {
    connect2Redux,
} from '../won-utils.js';
import {
    attach,
    getIn,
} from '../utils.js';
import { actionCreators }  from '../actions/actions.js';

const serviceDependencies = ['$ngRedux', '$scope'];


function genComponentConf() {
    let template = `
      <div class="request__header">
        <a ng-click="self.router__stateGoCurrent({connectionUri: null, sendAdHocRequest: null})"
            class="clickable">
            <svg style="--local-primary:var(--won-primary-color);"
              class="request__header__icon clickable">
                <use href="#ico36_close"></use>
            </svg>
        </a>

        <won-post-header
            need-uri="self.postUriToConnectTo"
            timestamp="self.lastUpdateTimestamp"
            hide-image="::true">
        </won-post-header>
      </div>

      <won-post-content class="request__content"
        need-uri="self.postUriToConnectTo">
      </won-post-content>

      <div class="request__footer">
        <input
          type="text"
          ng-model="self.message"
          placeholder="Request Message (optional)"/>
        <div class="flexbuttons">
          <button
            class="won-button--filled black"
            ng-click="self.router__stateGoCurrent({connectionUri: null, sendAdHocRequest: null})">
              Cancel
          </button>
          <button
            class="won-button--filled red"
            ng-click="self.sendRequest(self.message)">
              Chat
          </button>
        </div>
        <a target="_blank"
          href="{{self.sendAdHocRequest ? self.postUriToConnectTo : self.connectionUri}}">
            <svg class="rdflink__big clickable">
                <use href="#rdf_logo_1"></use>
            </svg>
        </a>
      </div>
    `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);
            this.maxThumbnails = 9;
            this.message = '';
            window.openMatch4dbg = this;

            const selectFromState = (state) => {
                const sendAdHocRequest = getIn(state, ['router', 'currentParams', 'sendAdHocRequest']); //if this parameter is set we will not have a connection to send this request to

                const connectionUri = decodeURIComponent(getIn(state, ['router', 'currentParams', 'connectionUri']));
                const ownNeed = connectionUri && selectNeedByConnectionUri(state, connectionUri);
                const connection = ownNeed && ownNeed.getIn(["connections", connectionUri]);
                const postUriToConnectTo = sendAdHocRequest? selectOpenPostUri(state) : connection && connection.get("remoteNeedUri");

                return {
                    ownNeed,
                    sendAdHocRequest,
                    lastUpdateTimestamp: connection && connection.get('creationDate'), //TODO: CORRECT TIMESTAMP LAST UPDATE
                    connectionUri,
                    postUriToConnectTo,
                }
            };
            connect2Redux(selectFromState, actionCreators, [], this);
        }

        sendRequest(message) {
            if(this.sendAdHocRequest || (this.ownNeed && this.ownNeed.get("isWhatsAround"))){
                if(this.ownNeed && this.ownNeed.get("isWhatsAround")){
                    //Close the connection if there was a present connection for a whatsaround need
                    this.connections__close(this.connectionUri);
                }

                if(this.postUriToConnectTo){
                    this.connections__connectAdHoc(this.postUriToConnectTo, message);
                }

                this.router__stateGoCurrent({connectionUri: null, sendAdHocRequest: null});
            }else{
                this.needs__connect(
                		this.ownNeed.get("uri"), 
                		this.connectionUri,
                		this.ownNeed.getIn(['connections',this.connectionUri]).get("remoteNeedUri"), 
                		message);
                this.router__stateGoCurrent({connectionUri: null})
            }
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

export default angular.module('won.owner.components.sendRequest', [
    postContentModule,
    postHeaderModule,
])
    .directive('wonSendRequest', genComponentConf)
    .name;

