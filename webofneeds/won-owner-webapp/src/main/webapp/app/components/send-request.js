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
      <div class="sr__caption">
        <div class="sr__caption__title">Send Conversation Request</div>
        <a ng-click="self.router__stateGoCurrent({connectionUri: null, sendAdHocRequest: null})"
            class="clickable">
          <img
            class="sr__caption__icon clickable"
            src="generated/icon-sprite.svg#ico36_close"/>
        </a>
      </div>

      <won-post-header
        need-uri="self.postUriToConnectTo">
      </won-post-header>

      <won-post-content
        need-uri="self.postUriToConnectTo">
      </won-post-content>

      <div class="sr__footer">
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
              Request Contact
          </button>
        </div>
        <a target="_blank"
          href="{{self.sendAdHocRequest ? self.postUriToConnectTo : self.connectionUri}}">
            <img class="rdflink__big clickable" src="generated/icon-sprite.svg#rdf_logo_1">
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
                    connectionUri,
                    postUriToConnectTo,
                }
            };
            connect2Redux(selectFromState, actionCreators, [], this);
        }

        sendRequest(message) {
            if(this.sendAdHocRequest || (this.ownNeed && this.ownNeed.get("isWhatsAround"))){
                if(this.ownNeed && this.ownNeed.get("isWhatsAround")){
                    console.log("sending request from whatsaround need, close original connection");
                    //Close the connection if there was a present connection for a whatsaround need
                    this.connections__close(this.connectionUri);
                }else{
                    console.log("sending adhoc request");
                }

                if(this.postUriToConnectTo){
                    this.connections__connectAdHoc(this.postUriToConnectTo, message);
                }

                this.router__stateGoCurrent({connectionUri: null, sendAdHocRequest: null});
            }else{
                this.connections__open(this.connectionUri, message);
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

