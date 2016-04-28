;

import angular from 'angular';
import extendedGalleryModule from '../components/extended-gallery';
import { labels } from '../won-label-utils';
import {attach} from '../utils.js'
import { actionCreators }  from '../actions/actions';
import { selectOpenConnectionUri } from '../selectors';

const serviceDependencies = ['$q', '$ngRedux', '$scope'];
function genComponentConf() {
    let template = `
        <div class="or__header">
            <a ui-sref="{{::self.closeRequestItemUrl()}}">
                <img class="or__header__icon clickable" src="generated/icon-sprite.svg#ico36_close"/>
            </a>
            <div class="or__header__title">
                <div class="or__header__title__topline">
                    <div class="or__header__title__topline__title">{{self.theirNeed.get('title')}}</div>
                    <div class="or__header__title__topline__date">{{self.theirNeed.get('creationDate')}}</div>
                </div>
                <div class="or__header__title__subtitle">
                    <span class="or__header__title__subtitle__group" ng-show="{{self.theirNeed.get('group')}}">
                        <img src="generated/icon-sprite.svg#ico36_group" class="or__header__title__subtitle__group__icon">{{self.theirNeed.get('group')}}<span class="or__header__title__subtitle__group__dash"> &ndash; </span>
                    </span>
                    <span class="or__header__title__subtitle__type">{{self.labels.type[self.theirNeed.get('basicNeedType')]}}</span>
                </div>
            </div>
        </div>
        <div class="or__content">
            <div class="or__content__images" ng-show="self.theirNeed.get('images')">
                <won-extended-gallery max-thumbnails="self.maxThumbnails" items="self.theirNeed.get('images')" class="vertical"></won-extended-gallery>
            </div>
            <div class="or__content__description">
                <div class="or__content__description__location">
                    <img class="or__content__description__indicator" src="generated/icon-sprite.svg#ico16_indicator_location"/>
                    <span>Vienna area</span>
                </div>
                <div class="or__content__description__datetime">
                    <img class="or__content__description__indicator" src="generated/icon-sprite.svg#ico16_indicator_time"/>
                    <span>Available until 5th May</span>
                </div>
                <div class="or__content__description__text">
                    <img class="or__content__description__indicator" src="generated/icon-sprite.svg#ico16_indicator_description"/>
                    <span>These lovley Chairs need a new home since I am moving These are the first X chars of the message et eaquuntiore dolluptaspid quam que quatur quisinia aspe sus voloreiusa plis Sae quatectibus eumendi bla volupita dolupta el et andunt …</span>
                </div>
            </div>
        </div>
        <div class="or__footer" ng-show="::(!self.isSentRequest())">
            <input type="text" ng-model="self.message" placeholder="Reply Message (optional, in case of acceptance)"/>
            <div class="flexbuttons">
                <button class="won-button--filled black" ui-sref="overviewIncomingRequests({connectionUri: null})" ng-click="self.closeRequest()">Decline</button>
                <button class="won-button--filled red" ng-click="self.openRequest(self.message)">Accept</button>
            </div>
        </div>
    `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);
            window.openreq = this;
            this.message='';
            this.labels = labels;
            const selectFromState = (state) => {
                const connectionUri = selectOpenConnectionUri(state);
                const theirNeedUri = state.getIn(['connections', connectionUri, 'hasRemoteNeed']);

                return {
                    currentPage: state.getIn(['router','currentState','name']),
                    connectionUri: connectionUri,
                    connection: state.getIn(['connections', connectionUri]),
                    theirNeed: state.getIn(['needs','theirNeeds', theirNeedUri]),
                }
            };
            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);
        }

        isSentRequest() {
            return this.currentPage === "overviewSentRequests";
        }

        closeRequestItemUrl() {
            return this.isSentRequest() ? "overviewSentRequests({connectionUri: null})" : "overviewIncomingRequests({connectionUri: null})";
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

