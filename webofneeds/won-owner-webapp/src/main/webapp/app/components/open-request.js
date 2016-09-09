;

import angular from 'angular';
import extendedGalleryModule from '../components/extended-gallery';
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

const serviceDependencies = ['$q', '$ngRedux', '$scope'];
function genComponentConf() {
    let template = `
        <div class="or__header">
            <a ui-sref="{{::self.closeRequestItemUrl()}}">
                <img class="or__header__icon clickable" src="generated/icon-sprite.svg#ico36_close"/>
            </a>
            <div class="or__header__title">
                <div class="or__header__title__topline">
                    <div class="or__header__title__topline__title">
                        {{self.theirNeed.getIn(['won:hasContent','dc:title'])}}
                    </div>
                    <div class="or__header__title__topline__date">
                        {{self.timestamp}}
                    </div>
                </div>
                <div class="or__header__title__subtitle">
                    <span class="or__header__title__subtitle__group" ng-show="{{self.theirNeed.get('group')}}">
                        <img
                            src="generated/icon-sprite.svg#ico36_group"
                            class="or__header__title__subtitle__group__icon">
                        {{self.theirNeed.get('group')}}
                        <span class="or__header__title__subtitle__group__dash"> &ndash; </span>
                    </span>
                    <span class="or__header__title__subtitle__type">
                        {{self.labels.type[self.theirNeed.getIn(['won:hasBasicNeedType','@id'])]}}
                    </span>
                </div>
            </div>
        </div>
        <div class="or__content">
            <!--
            <div class="or__content__images" ng-show="self.theirNeed.get('images')">
                <won-extended-gallery max-thumbnails="self.maxThumbnails" items="self.theirNeed.get('images')" class="vertical"></won-extended-gallery>
            </div>
            -->
            <div class="or__content__description">
                <!--
                <div class="or__content__description__location">
                    <img class="or__content__description__indicator" src="generated/icon-sprite.svg#ico16_indicator_location"/>
                    <span>Vienna area</span>
                </div>
                <div class="or__content__description__datetime">
                    <img class="or__content__description__indicator" src="generated/icon-sprite.svg#ico16_indicator_time"/>
                    <span>Available until 5th May</span>
                </div>
                -->
                <div class="or__content__description__text"
                    ng-show="!!self.theirNeed.getIn(['won:hasContent','won:hasTextDescription'])">
                    <img
                        class="or__content__description__indicator"
                        src="generated/icon-sprite.svg#ico16_indicator_description"/>
                    <span>
                        <p>{{ self.theirNeed.getIn(['won:hasContent','won:hasTextDescription']) }}</p>
                    </span>
                </div>
                <div class="or__content__description__text"
                    ng-show="!!self.textMsg">
                    <img
                        class="or__content__description__indicator"
                        src="generated/icon-sprite.svg#ico16_indicator_message"/>
                    <span>
                        <p>{{ self.textMsg }}</p>
                    </span>
                </div>
                
            </div>
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


                return {
                    theirNeed,

                    connectionUri: connectionUri,
                    isSentRequest: connectionState === won.WON.RequestSent,
                    isReceivedRequest: connectionState === won.WON.RequestReceived,

                    isOverview: displayingOverview(state),
                    connection: selectOpenConnection(state),

                    timestamp: theirNeed && relativeTime(
                        selectLastUpdateTime(state),
                        //theirNeed.get('dct:created')
                        lastUpdatedPerConnection && lastUpdatedPerConnection.get(connectionUri)
                    ),

                    textMsg: connectMsg && (
                        connectMsg.get('hasTextMessage') ||
                        connectMsg.getIn(['hasCorrespondingRemoteMessage', 'hasTextMessage'])
                    ),


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
    extendedGalleryModule
])
    .directive('wonOpenRequest', genComponentConf)
    .name;

