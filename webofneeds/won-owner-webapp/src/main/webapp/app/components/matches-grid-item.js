;

import angular from 'angular';
import squareImageModule from './square-image';
import feedbackGridModule from './feedback-grid';
import { attach } from '../utils';
import { labels, relativeTime, updateRelativeTimestamps } from '../won-label-utils';

const serviceDependencies = ['$scope', '$interval'];
function genComponentConf() {
    let template = `
        <div class="mgi__description">
            <div class="mgi__description__post clickable">
                <!--won-square-image src="self.getRandomImage()" title="self.item.ownNeed.title"></won-square-image-->
                <div class="mgi__description__post__text">
                    <div class="mgi__description__post__text__topline">
                        <div class="mgi__description__post__text__topline__title">{{self.item.remoteNeed.title}}</div>
                        <div class="mgi__description__post__text__topline__date">{{self.creationDate}}</div>
                    </div>
                    <div class="mgi__description__post__text__subtitle">
                        <span class="mgi__description__post__text__subtitle__group" ng-show="self.item.group">
                            <img src="generated/icon-sprite.svg#ico36_group" class="mgi__description__post__text__subtitle__group__icon">{{self.item.group}}<span class="mgi__description__post__text__subtitle__group__dash"> &ndash; </span>
                        </span>
                        <span class="mgi__description__post__text__subtitle__type">{{self.labels.type[self.item.remoteNeed.basicNeedType]}}</span>
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
        <div class="mgi__match clickable" ng-if="!self.feedbackVisible" ng-click="self.showFeedback()" ng-mouseenter="self.showFeedback()">
            <div class="mgi__match__description">
                <div class="mgi__match__description__title">{{self.item.ownNeed.title}}</div>
                <div class="mgi__match__description__type">{{self.labels.type[self.item.ownNeed.basicNeedType]}}</div>
            </div>
            <won-square-image src="self.getRandomImage()" title="self.item.ownNeed.title"></won-square-image>
        </div>
        <won-feedback-grid item="self.item" request-item="self.requestItem" ng-mouseleave="self.hideFeedback()" ng-if="self.feedbackVisible"/>
    `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);
            this.images=[
                "images/furniture1.png",
                "images/furniture2.png",
                "images/furniture3.png",
                "images/furniture4.png"
            ];
            this.feedbackVisible = false;
            this.labels = labels;

            updateRelativeTimestamps(
                this.$scope,
                this.$interval,
                this.item.remoteNeed.creationDate,
                    t => this.creationDate = t);

        }

        showFeedback() {
            this.feedbackVisible = true;
        }

        hideFeedback() {
            this.feedbackVisible = false;
        }

        toggleFeedback(){
            this.feedbackVisible = !this.feedbackVisible;
        }

        getRandomImage(){
            let i = Math.floor((Math.random()*4));
            return this.images[0];
        }
    }
    Controller.$inject = serviceDependencies;

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

export default angular.module('won.owner.components.matchesGridItem', [
    squareImageModule,
    feedbackGridModule
])
    .directive('wonMatchesGridItem', genComponentConf)
    .name;

