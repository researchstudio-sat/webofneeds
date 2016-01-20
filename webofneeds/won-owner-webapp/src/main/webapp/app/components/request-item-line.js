;

import angular from 'angular';
import squareImageModule from '../components/square-image';
import getType from '../utils.js';
function genComponentConf() {
    let template = `
            <div class="ril clickable" ng-click="self.toggleRequest()">
                <won-square-image src="self.item.titleImgSrc" title="self.item.ownNeed.title"></won-square-image>
                <div class="ril__description">
                    <div class="ril__description__topline">
                        <div class="ril__description__topline__title">{{self.item[0].ownNeed.title}}</div>
                        <div class="ril__description__topline__messagecount">{{self.item.length}}</div>
                    </div>
                    <div class="ril__description__subtitle">
                        <span class="ril__description__subtitle__group" ng-show="self.item.group">
                            <img src="generated/icon-sprite.svg#ico36_group" class="ril__description__subtitle__group__icon">{{self.item.group}}<span class="ril__description__subtitle__group__dash"> &ndash; </span>
                        </span>
                        <span class="ril__description__subtitle__type">{{self.getType(self.item[0].ownNeed.basicNeedType)}}</span>
                    </div>
                </div>
                <div class="ril__carret">
                    <img class="ril__arrow" ng-show="self.open" src="generated/icon-sprite.svg#ico16_arrow_up"/>
                    <img class="ril__arrow" ng-show="!self.open" src="generated/icon-sprite.svg#ico16_arrow_down"/>
                </div>
            </div>
            <div class="mil" ng-show="self.open">
                <div class="mil__item clickable" ng-class="self.openRequest === request? 'selected' : ''" ng-repeat="request in self.item" ng-click="self.openMessage(request)">
                    <won-square-image src="request.titleImgSrc" title="request.title"></won-square-image>
                    <div class="mil__item__description">
                        <div class="mil__item__description__topline">
                            <div class="mil__item__description__topline__title">{{self.item[0].remoteNeed.title}}</div>
                            <div class="mil__item__description__topline__date">{{request.timeStamp}}</div>
                        </div>
                        <div class="mil__item__description__subtitle">
                            <span class="mil__item__description__subtitle__group" ng-show="request.group">
                                <img src="generated/icon-sprite.svg#ico36_group" class="mil__item__description__subtitle__group__icon">{{request.group}}<span class="mil__item__description__subtitle__group__dash"> &ndash; </span>
                            </span>
                            <span class="mil__item__description__subtitle__type">{{self.getType(self.item[0].remoteNeed.basicNeedType)}}</span>
                        </div>
                        <div class="mil__item__description__message">
                            <span class="mil__item__description__message__indicator" ng-show="!request.read"/>{{request.message}}
                        </div>
                    </div>
                </div>
            </div>
    `;

    class Controller {
        constructor() {
            console.log(this.item)
        }


        toggleRequest() {
            this.open = !this.open;
        }

        openMessage(request) {
            request.read = true;
            this.openRequest = request;
        }
    }

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {item: "=",
                open: "=",
                openRequest: "="},
        template: template
    }
}

export default angular.module('won.owner.components.requestItemLine', [])
    .directive('wonRequestItemLine', genComponentConf)
    .name;

