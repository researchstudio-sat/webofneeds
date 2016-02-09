;

import angular from 'angular';
import extendedGalleryModule from '../components/extended-gallery';
import { labels } from '../won-label-utils';
import {attach} from '../utils.js'
import { actionCreators }  from '../actions/actions';

const serviceDependencies = ['$q', '$ngRedux', '$scope'];
function genComponentConf() {
    let template = `
        <div class="or__header">
            <img class="or__header__icon clickable" src="generated/icon-sprite.svg#ico36_close" ng-click="self.closeRequest()"/>
            <div class="or__header__title">
                <div class="or__header__title__topline">
                    <div class="or__header__title__topline__title">{{self.item.ownNeed.title}}</div>
                    <div class="or__header__title__topline__date">{{self.item.timeStamp}}</div>
                </div>
                <div class="or__header__title__subtitle">
                    <span class="or__header__title__subtitle__group" ng-show="self.item.group">
                        <img src="generated/icon-sprite.svg#ico36_group" class="or__header__title__subtitle__group__icon">{{self.item.group}}<span class="or__header__title__subtitle__group__dash"> &ndash; </span>
                    </span>
                    <span class="or__header__title__subtitle__type">{{self.labels.type[self.item.type]}}</span>
                </div>
            </div>
        </div>
        <div class="or__content">
            <div class="or__content__images" ng-show="self.item.images">
                <won-extended-gallery max-thumbnails="self.maxThumbnails" items="self.item.images" class="vertical"></won-extended-gallery>
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
        <div class="or__footer">
            <input type="text" ng-model="self.message" placeholder="Reply Message (optional, in case of acceptance)"/>
            <div class="flexbuttons">
                <button class="won-button--filled black">Decline</button>
                <button class="won-button--filled red" ng-click="openRequest()">Accept</button>
            </div>
        </div>
    `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);
            window.openconv = this;
            this.message='';
            this.labels = labels;
        }

        sendMessage(message){

        }
        closeRequest(){
            this.item = undefined;
        }
    }
    Controller.$inject = serviceDependencies;
    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {item: "="},
        template: template
    }
}

export default angular.module('won.owner.components.openConversation', [
    extendedGalleryModule
])
    .directive('wonOpenConversation', genComponentConf)
    .name;

