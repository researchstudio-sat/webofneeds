;

import angular from 'angular';

function genComponentConf() {
    let template = `
            <a ng-href="post/{{self.item.id}}">
                <img class="pil__image clickable" ng-show="self.item.titleImgSrc" ng-src="{{self.item.titleImgSrc}}"/>
                <!--<img class="pil__image clickable" ng-show="!self.item.titleImgSrc" src="generated/icon-sprite.svg#illu_drag_here"/>-->
                <div class="pil__image clickable" style="background-color: {{self.generateHexColor(self.item.title)}}" ng-show="!self.item.titleImgSrc"><!-- figure out some better way to color -->
                    <span class="pil__image__noimage">{{self.item.title.charAt(0)}}</span>
                </div>
            </a>
            <a class="pil__description clickable" ng-href="post/{{self.item.id}}">
                <div class="pil__description__topline">
                    <div class="pil__description__topline__title">{{self.item.title}}</div>
                    <div class="pil__description__topline__creationdate">{{self.item.creationDate}}</div>
                </div>
                <div class="pil__description__subtitle">
                    <span class="pil__description__subtitle__group" ng-show="self.item.group">
                        <img src="generated/icon-sprite.svg#ico36_group" class="pil__description__subtitle__group__icon">{{self.item.group}}<span class="pil__description__subtitle__group__dash"> &ndash; </span>
                    </span>
                    <span class="pil__description__subtitle__type">{{self.getType(self.item.type)}}</span>
                </div>
            </a>
            <div class="pil__indicators">
                <a class="pil__indicators__item clickable" ng-href="post/{{self.item.id}}/owner/messages">
                    <img src="generated/icon-sprite.svg#ico36_message" ng-show="self.item.messages" class="pil__indicators__item__icon">
                    <img src="generated/icon-sprite.svg#ico36_message_grey" ng-show="!self.item.messages" class="pil__indicators__item__icon">
                    <span class="pil__indicators__item__caption">{{self.item.messages.length}}</span>
                </a>
                <a class="pil__indicators__item clickable" ng-href="post/{{self.item.id}}/owner/requests">
                    <img src="generated/icon-sprite.svg#ico36_incoming" ng-show="self.item.requests"  class="pil__indicators__item__icon">
                    <img src="generated/icon-sprite.svg#ico36_incoming_grey"  ng-show="!self.item.requests" class="pil__indicators__item__icon">
                    <span class="pil__indicators__item__caption">{{self.item.requests.length}}</span>
                </a>
                <a class="pil__indicators__item clickable" ng-href="post/{{self.item.id}}/owner/matches">
                    <img src="generated/icon-sprite.svg#ico36_match" ng-show="self.item.matches" class="pil__indicators__item__icon">
                    <img src="generated/icon-sprite.svg#ico36_match_grey" ng-show="!self.item.matches" class="pil__indicators__item__icon">
                    <span class="pil__indicators__item__caption">{{self.item.matches.length}}</span>
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

        generateHexColor(title) {
            var hash = 0;
            for (var i = 0; i < title.length; i++) {
                hash = title.charCodeAt(i) + ((hash << 5) - hash);
            }

            var c = (hash & 0x00FFFFFF)
                .toString(16);

            return "#"+("00000".substring(0, 6 - c.length) + c);
        }
    }

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {item: "="},
        template: template
    }
}

export default angular.module('won.owner.components.postItemLine', [])
    .directive('wonPostItemLine', genComponentConf)
    .name;

