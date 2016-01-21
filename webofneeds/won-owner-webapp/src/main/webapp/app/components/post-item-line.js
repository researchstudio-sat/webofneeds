;

import angular from 'angular';
import squareImageModule from '../components/square-image';

function genComponentConf() {
    let template = `
            <a ng-href="post/{{self.item.id}}">
                <won-square-image src="self.item.titleImgSrc" title="self.item.title"></won-square-image>
            </a>
            <a class="pil__description clickable" ng-href="post/{{self.item.id}}">
                <div class="pil__description__topline">
                    <div class="pil__description__topline__title">{{self.item.title}}</div>
                    <div class="pil__description__topline__creationdate">{{self.item.creationDate}}</div>
                </div>
                <div class="pil__description__subtitle">
                    <span class="pil__description__subtitle__group" ng-show="self.item.group">
                        <img src="generated/icon-sprite.svg#ico36_group"
                             class="pil__description__subtitle__group__icon">
                         {{self.item.group}}
                         <span class="pil__description__subtitle__group__dash"> &ndash; </span>
                    </span>
                    <span class="pil__description__subtitle__type">
                        {{self.getType(self.item.basicNeedType)}}
                    </span>
                </div>
            </a>
            <div class="pil__indicators">
                <a class="pil__indicators__item clickable" ng-href="post/{{self.item.id}}/owner/messages">
                    <img src="generated/icon-sprite.svg#ico36_message"
                         ng-show="self.item.messages.length"
                         class="pil__indicators__item__icon">
                    <img src="generated/icon-sprite.svg#ico36_message_grey"
                         ng-show="!self.item.messages.length"
                         class="pil__indicators__item__icon">
                    <span class="pil__indicators__item__caption">
                        {{self.item.messages.length}}
                    </span>
                </a>
                <a class="pil__indicators__item clickable" ng-href="post/{{self.item.id}}/owner/requests">
                    <img src="generated/icon-sprite.svg#ico36_incoming"
                         ng-show="self.unreadMatchEventsOfNeed"
                         class="pil__indicators__item__icon">
                    <img src="generated/icon-sprite.svg#ico36_incoming_grey"
                         ng-show="!self.unreadMatchEventsOfNeed"
                         class="pil__indicators__item__icon">
                    <span class="pil__indicators__item__caption">
                        {{self.unreadMatchEventsOfNeed}}
                    </span>
                </a>
                <a class="pil__indicators__item clickable" ng-href="post/{{self.item.id}}/owner/matches">
                    <img src="generated/icon-sprite.svg#ico36_match"
                         ng-show="self.unreadMatchEventsOfNeed"
                         class="pil__indicators__item__icon">
                    <img src="generated/icon-sprite.svg#ico36_match_grey"
                         ng-show="!self.unreadMatchEventsOfNeed"
                         class="pil__indicators__item__icon">
                    <span class="pil__indicators__item__caption">
                        {{self.unreadMatchEventsOfNeed}}
                    </span>
                </a>
            </div>
    `;

    class Controller {
        constructor() { }


        getType(type) {
            switch(type){
                case 1: return 'I want to have something';
                case 2: return 'I offer something';
                case 3: return 'I want to do something together';
                case 4: return 'I want to change something';
            }
        }
    }

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {item: "=",unreadMatchEventsOfNeed:"="},
        template: template
    }
}

export default angular.module('won.owner.components.postItemLine', [
    squareImageModule
])
    .directive('wonPostItemLine', genComponentConf)
    .name;

