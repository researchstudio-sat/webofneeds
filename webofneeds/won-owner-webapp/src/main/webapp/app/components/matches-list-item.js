;

import angular from 'angular';
import squareImageModule from './square-image';

function genComponentConf() {
    let template = `
        <div class="mli clickable" ng-click="self.toggleMatches()">
                <won-square-image src="self.item.titleImgSrc" title="self.item.title"></won-square-image>
                <div class="mli__description">
                    <div class="mli__description__topline">
                        <div class="mli__description__topline__title">{{self.item.title}}</div>
                        <div class="mli__description__topline__matchcount">{{self.item.matches.length}}</div>
                    </div>
                    <div class="mli__description__subtitle">
                        <span class="mli__description__subtitle__group" ng-show="self.item.group">
                            <img src="generated/icon-sprite.svg#ico36_group" class="mli__description__subtitle__group__icon">{{self.item.group}}<span class="mli__description__subtitle__group__dash"> &ndash; </span>
                        </span>
                        <span class="mli__description__subtitle__type">{{self.getType(self.item.type)}}</span>
                    </div>
                </div>
                <div class="mli__carret">
                    <img class="mli__arrow" ng-show="self.open" src="generated/icon-sprite.svg#ico16_arrow_up"/>
                    <img class="mli__arrow" ng-show="!self.open" src="generated/icon-sprite.svg#ico16_arrow_down"/>
                </div>
            </div>
            <div class="smli" ng-show="self.open">
                <div class="smli__item clickable" ng-class="self.openRequest === match? 'selected' : ''" ng-repeat="match in self.item.matches">
                    <div class="smli__item__header">
                        <won-square-image src="match.images[0].src" title="match.title"></won-square-image>
                        <div class="smli__item__header__text">
                            <div class="smli__item__header__text__topline">
                                <div class="smli__item__header__text__topline__title">{{match.title}}</div>
                                <div class="smli__item__header__text__topline__date">{{match.timeStamp}}</div>
                            </div>
                            <div class="smli__item__header__text__subtitle">
                                <span class="smli__item__header__text__subtitle__group" ng-show="request.group">
                                    <img src="generated/icon-sprite.svg#ico36_group" class="smli__item__header__text__subtitle__group__icon">{{match.group}}<span class="smli__item__header__text__subtitle__group__dash"> &ndash; </span>
                                </span>
                                <span class="smli__item__header__text__subtitle__type">{{self.getType(match.type)}}</span>
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
        }


        getType(type) {
            switch(type){
                case 1: return 'I want to have something';
                case 2: return 'I offer something';
                case 3: return 'I want to do something together';
                case 4: return 'I want to change something';
            }
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
        scope: {item: "="},
        template: template
    }
}

export default angular.module('won.owner.components.matchesListItem', [
    squareImageModule
])
    .directive('wonMatchesListItem', genComponentConf)
    .name;

