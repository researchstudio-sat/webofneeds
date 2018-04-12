;

import angular from 'angular';
import 'ng-redux';
import postHeaderModule from './post-header.js';
import feedbackGridModule from './feedback-grid.js';
import postSeeksInfoModule from './post-seeks-info.js';
import postIsInfoModule from './post-is-info.js';

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
        <div class="post-info__header">
            <a class="clickable"
               ng-click="self.router__stateGoCurrent({connectionUri : undefined, sendAdHocRequest: undefined})">
                <svg style="--local-primary:var(--won-primary-color);"
                     class="post-info__header__icon clickable">
                    <use href="#ico36_close"></use>
                </svg>
            </a>
            <won-post-header
                need-uri="self.postUriToConnectTo"
                timestamp="self.lastUpdateTimestamp"
                hide-image="::true">
            </won-post-header>
            <!-- TODO: Implement a menu with all the necessary buttons -->
            <!-- svg class="post-info__header__icon__small clickable"
                style="--local-primary:#var(--won-secondary-color);"
                ng-show="!self.contextMenuOpen"
                ng-click="self.contextMenuOpen = true">
                    <use href="#ico16_arrow_down"></use>
            </svg>
            <div class="post-info__header__contextmenu contextmenu" ng-show="self.contextMenuOpen">
                <div class="content">
                    <div class="topline">
                      <svg class="post-info__header__icon__small__contextmenu clickable"
                        style="--local-primary:black;"
                        ng-click="self.contextMenuOpen = false">
                            <use href="#ico16_arrow_up"></use>
                      </svg>
                    </div>
                  <button
                    class="won-button--filled thin red"
                    ng-click="">
                      DO POST ACTIONS
                  </button>
                </div>
            </div-->
        </div>
        <div class="post-info__content">
            <won-gallery ng-show="self.suggestedPost.get('hasImages')">
            </won-gallery>

            <!-- GENERAL Part -->
            <h2 class="post-info__heading" ng-show="self.friendlyTimestamp">
                Created
            </h2>
            <p class="post-info__details" ng-show="self.friendlyTimestamp">
                {{ self.friendlyTimestamp }}
            </p>
            <!-- IS Part -->
            <div ng-show="self.isPart">
                <won-post-is-info is-part="::self.isPart"></won-post-is-info>
            </div>
            </br>
            <!-- SEEKS Part -->
            <div ng-show="self.seeksPart">
                <won-post-seeks-info seeks-part="::self.seeksPart"></won-post-seeks-info>
            </div>
            </br>
        </div>
        <div class="post-info__footer">
            <won-feedback-grid ng-if="!self.sendAdHocRequest && !self.connection.get('isRated')" connection-uri="self.connectionUri"></won-feedback-grid>
            <input
                type="text"
                ng-if="self.sendAdHocRequest || self.connection.get('isRated')"
                ng-model="self.message"
                placeholder="Request Message (optional)"/>
            <button class="won-button--filled red"
                ng-if="self.sendAdHocRequest || self.connection.get('isRated')"
                ng-click="self.sendRequest(self.message)">
                Ask to Chat
            </button>
            <button ng-if="!self.sendAdHocRequest && self.connection.get('isRated')"
                class="won-button--filled black"
                ng-click="self.closeConnection()">
                    Remove This
            </button>
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

                const suggestedPost = state.getIn(["needs", postUriToConnectTo]);

                const is = suggestedPost? suggestedPost.get('is') : undefined;
                //TODO it will be possible to have more than one seeks
                const seeks = suggestedPost? suggestedPost.get('seeks') : undefined;

                return {
                    connection,
                    connectionUri,
                    ownNeed,
                    isPart: is? {
                        postUri: postUriToConnectTo,
                        is: is,
                        isString: 'is',
                        location: is && is.get('location'),
                        address: is.get('location') && is.get('location').get('address'),
                    }: undefined,
                    seeksPart: seeks? {
                        postUri: postUriToConnectTo,
                        seeks: seeks,
                        seeksString: 'seeks',
                        location: seeks && seeks.get('location'),
                        address: seeks.get('location') && seeks.get('location').get('address'),
                    }: undefined,
                    suggestedPost,
                    sendAdHocRequest,
                    lastUpdateTimestamp: connection && connection.get('lastUpdateDate'),
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
                this.router__stateGoCurrent({connectionUri: this.connectionUri})
            }
        }

        closeConnection(){
            this.connections__close(this.connectionUri);
            this.router__stateGoCurrent({connectionUri: null});
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
    postIsInfoModule,
    postSeeksInfoModule,
    postHeaderModule,
    feedbackGridModule,
])
    .directive('wonSendRequest', genComponentConf)
    .name;

