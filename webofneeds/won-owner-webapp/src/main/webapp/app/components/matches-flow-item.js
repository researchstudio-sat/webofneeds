;

import angular from 'angular';
import squareImageModule from './square-image';
import extendedGalleryModule from './extended-gallery';

function genComponentConf() {
    let template = `
        <div ng-show="self.images" class="mfi__gallery">
            <won-extended-gallery max-thumbnails="self.maxThumbnails" items="self.images" class="horizontal"></won-extended-gallery>
        </div>
        <div class="mfi__description">
            <div class="mfi__description__topline">
                <div class="mfi__description__topline__title clickable"><b>{{self.item.remoteNeed.title}}</b></div>
                <div class="mfi__description__topline__date">{{self.item.remoteNeed.creationDate}}</div>
            </div>
            <div class="mfi__description__subtitle">
                <span class="mfi__description__subtitle__group" ng-show="self.item.group">
                    <img src="generated/icon-sprite.svg#ico36_group" class="mfi__description__subtitle__group__icon">{{self.item.group}}<span class="mfi__description__subtitle__group__dash"> &ndash; </span>
                </span>
                <span class="mfi__description__subtitle__type">{{self.getType(self.item.remoteNeed.basicNeedType)}}</span>
            </div>
            <div class="mfi__description__content">
                <div class="mfi__description__content__location">
                    <img class="mfi__description__content__indicator" src="generated/icon-sprite.svg#ico16_indicator_location"/>
                    <span>Vienna area</span>
                </div>
                <div class="mfi__description__content__datetime">
                    <img class="mfi__description__content__indicator" src="generated/icon-sprite.svg#ico16_indicator_time"/>
                    <span>Available until 5th May</span>
                </div>
            </div>
        </div>
        <div class="mfi__match clickable">
            <div class="mfi__match__description">
                <div class="mfi__match__description__title">{{self.item.ownNeed.title}}</div>
                <div class="mfi__match__description__type">{{self.getType(self.item.ownNeed.basicNeedType)}}</div>
            </div>
            <won-square-image src="self.getRandomImage()" title="self.item.ownNeed.title"></won-square-image>
        </div>
    `;

    class Controller {
        constructor() {
            this.maxThumbnails = 4;
            this.images=[
                "images/furniture1.png",
                "images/furniture2.png",
                "images/furniture3.png",
                "images/furniture4.png",
            ]
        }


        getRandomImage(){
            let i = Math.floor((Math.random()*4))
            return this.images[2];
        }
        getType(type) {
            switch(type){
                case won.WON.BasicNeedTypeDemand: return 'I want to have something';
                case won.WON.BasicNeedTypeSupply: return 'I offer something';
                case won.WON.BasicNeedTypeDotogether: return 'I want to do something together';
                case won.WON.BasicNeedTypeCritique: return 'I want to change something';
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

export default angular.module('won.owner.components.matchesFlowItem', [
    squareImageModule,
    extendedGalleryModule
])
    .directive('wonMatchesFlowItem', genComponentConf)
    .name;

