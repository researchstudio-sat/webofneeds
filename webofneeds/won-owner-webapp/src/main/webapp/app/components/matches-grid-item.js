;

import angular from 'angular';
import squareImageModule from './square-image';

function genComponentConf() {
    let template = `
        <div class="mgi__description">
            <div class="mgi__description__post clickable">
                <won-square-image src="self.item.images[0].src" title="self.item.match.title"></won-square-image>
                <div class="mgi__description__post__text">
                    <div class="mgi__description__post__text__topline">
                        <div class="mgi__description__post__text__topline__title">{{self.item.title}}</div>
                        <div class="mgi__description__post__text__topline__date">Today 15:30</div>
                    </div>
                    <div class="mgi__description__post__text__subtitle">
                        <span class="mgi__description__post__text__subtitle__group" ng-show="self.item.group">
                            <img src="generated/icon-sprite.svg#ico36_group" class="mgi__description__post__text__subtitle__group__icon">{{self.item.group}}<span class="mgi__description__post__text__subtitle__group__dash"> &ndash; </span>
                        </span>
                        <span class="mgi__description__post__text__subtitle__type">{{self.getType(self.item.type)}}</span>
                    </div>
                </div>
            </div>
            <div class="mgi__description__content">
                <div class="mgi__description__content__location">
                    <img class="mgi__description__content__indicator" src="generated/icon-sprite.svg#ico16_indicator_location"/>
                    <span>Vienna area</span>
                </div>
                <div class="mgi__description__content__datetime">
                    <img class="mgi__description__content__indicator" src="generated/icon-sprite.svg#ico16_indicator_time"/>
                    <span>Available until 5th May</span>
                </div>
            </div>
        </div>
        <div class="mgi__match clickable">
            <div class="mgi__match__description">
                <div class="mgi__match__description__title">{{self.item.match.title}}</div>
                <div class="mgi__match__description__type">{{self.getType(self.item.match.type)}}</div>
            </div>
            <won-square-image src="self.item.match.titleImgSrc" title="self.item.match.title"></won-square-image>
        </div>
    `;

    class Controller {
        constructor() {}

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
        scope: {item: "="},
        template: template
    }
}

export default angular.module('won.owner.components.matchesGridItem', [
    squareImageModule
])
    .directive('wonMatchesGridItem', genComponentConf)
    .name;

