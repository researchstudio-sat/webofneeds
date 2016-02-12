;

import angular from 'angular';
import squareImageModule from './square-image';
import { labels } from '../won-label-utils';

function genComponentConf() {
    let template = `
        <div class="mli clickable" ng-click="self.toggleMatches()">
                <won-square-image src="self.item.titleImgSrc" title="self.item.ownNeed.title"></won-square-image>
                <div class="mli__description">
                    <div class="mli__description__topline">
                        <div class="mli__description__topline__title">{{self.item[0].ownNeed.title}}</div>
                        <div class="mli__description__topline__matchcount">{{self.item.length}}</div>
                    </div>
                    <div class="mli__description__subtitle">
                        <span class="mli__description__subtitle__group" ng-show="self.item.group">
                            <img src="generated/icon-sprite.svg#ico36_group" class="mli__description__subtitle__group__icon">{{self.item.group}}<span class="mli__description__subtitle__group__dash"> &ndash; </span>
                        </span>
                        <span class="mli__description__subtitle__type">{{self.labels.type[self.item[0].ownNeed.basicNeedType]}}</span>
                    </div>
                </div>
                <div class="mli__carret">
                    <img class="mli__arrow" ng-show="self.open" src="generated/icon-sprite.svg#ico16_arrow_up"/>
                    <img class="mli__arrow" ng-show="!self.open" src="generated/icon-sprite.svg#ico16_arrow_down"/>
                </div>
            </div>
            <div class="smli" ng-show="self.open">
                <div class="smli__item clickable" ng-class="{'selected' : self.openRequest === match}" ng-repeat="match in self.item" ng-mouseenter="self.showFeedback()" ng-mouseleave="self.hideFeedback()">
                    <div class="smli__item__header">
                        <won-square-image src="match.images[0].src" title="match.remoteNeed.title"></won-square-image>
                        <div class="smli__item__header__text">
                            <div class="smli__item__header__text__topline">
                                <div class="smli__item__header__text__topline__title">{{match.remoteNeed.title}}</div>
                                <div class="smli__item__header__text__topline__date">{{match.remoteNeed.timeStamp}}</div>
                            </div>
                            <div class="smli__item__header__text__subtitle">
                                <span class="smli__item__header__text__subtitle__group" ng-show="request.group">
                                    <img src="generated/icon-sprite.svg#ico36_group" class="smli__item__header__text__subtitle__group__icon">{{match.group}}<span class="smli__item__header__text__subtitle__group__dash"> &ndash; </span>
                                </span>
                                <span class="smli__item__header__text__subtitle__type">{{self.labels.type[match.remoteNeed.basicNeedType]}}</span>
                            </div>
                        </div>
                    </div>
                    <div class="smli__item__content">
                        <div class="smli__item__content__location">
                            <img class="smli__item__content__indicator" src="generated/icon-sprite.svg#ico16_indicator_location"/>
                            <span>Vienna area</span>
                        </div>
                        <div class="smli__item__content__datetime">
                            <img class="smli__item__content__indicator" src="generated/icon-sprite.svg#ico16_indicator_time"/>
                            <span>Available until 5th May</span>
                        </div>
                        <div class="smli__item__content__text">
                            <img class="smli__item__content__indicator" src="generated/icon-sprite.svg#ico16_indicator_description"/>
                            <span>{{match.message}}</span>
                        </div>
                    </div>
                </div>
            </div>
    `;

    class Controller {
        constructor() {
            this.maxThumbnails = 4;
            this.labels = labels;
        }

        toggleMatches() {
            this.open = !this.open;
        }
    }

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {item: "=",
                requestItem: "="},
        template: template
    }
}

export default angular.module('won.owner.components.matchesListItem', [
    squareImageModule
])
    .directive('wonMatchesListItem', genComponentConf)
    .name;

