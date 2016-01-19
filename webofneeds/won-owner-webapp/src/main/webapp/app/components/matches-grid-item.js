;

import angular from 'angular';
import squareImageModule from './square-image';

function genComponentConf() {
    let template = `
        <div class="mgi__description">
            <div class="mgi__description__post clickable">
                <!--won-square-image src="self.getRandomImage()" title="self.item.ownNeed.title"></won-square-image-->
                <div class="mgi__description__post__text">
                    <div class="mgi__description__post__text__topline">
                        <div class="mgi__description__post__text__topline__title">{{self.item.remoteNeed.title}}</div>
                        <div class="mgi__description__post__text__topline__date">{{self.item.remoteNeed.creationDate}}</div>
                    </div>
                    <div class="mgi__description__post__text__subtitle">
                        <span class="mgi__description__post__text__subtitle__group" ng-show="self.item.group">
                            <img src="generated/icon-sprite.svg#ico36_group" class="mgi__description__post__text__subtitle__group__icon">{{self.item.group}}<span class="mgi__description__post__text__subtitle__group__dash"> &ndash; </span>
                        </span>
                        <span class="mgi__description__post__text__subtitle__type">{{self.getType(self.item.remoteNeed.basicNeedType)}}</span>
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
                <div class="mgi__match__description__title">{{self.item.ownNeed.title}}</div>
                <div class="mgi__match__description__type">{{self.getType(self.item.ownNeed.basicNeedType)}}</div>
            </div>
            <won-square-image src="self.getRandomImage()" title="self.item.ownNeed.title"></won-square-image>
        </div>
    `;

    class Controller {
        constructor() {
            this.images=[
                "images/furniture1.png",
                "images/furniture2.png",
                "images/furniture3.png",
                "images/furniture4.png"
            ]
        }

        getType(type) {
            switch(type){
                case 1: return 'I want to have something';
                case 2: return 'I offer something';
                case 3: return 'I want to do something together';
                case 4: return 'I want to change something';
            }
        }

        getRandomImage(){
            let i = Math.floor((Math.random()*4))
            return this.images[0];
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

