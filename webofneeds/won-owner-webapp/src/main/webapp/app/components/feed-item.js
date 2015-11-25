;

import angular from 'angular';
import squareImageModule from '../components/square-image';

function genComponentConf() {
    let template = `
            <div class="fi clickable">
                <won-square-image src="self.item.titleImgSrc" title="self.item.title"></won-square-image>
                <div class="fi__description">
                    <div class="fi__description__topline">
                        <div class="fi__description__topline__title">{{self.item.title}}</div>
                        <div class="fi__description__topline__date">Today 15:30</div>
                    </div>
                    <div class="fi__description__subtitle">
                        <span class="fi__description__subtitle__group" ng-show="self.item.group">
                            <img src="generated/icon-sprite.svg#ico36_group" class="fi__description__subtitle__group__icon">{{self.item.group}}<span class="fi__description__subtitle__group__dash"> &ndash; </span>
                        </span>
                        <span class="fi__description__subtitle__type">{{self.getType(self.item.type)}}</span>
                    </div>
                </div>
            </div>
            <div class="fmil">
                <div class="fmil__item clickable" ng-repeat="request in self.item.requests track by $index" ng-show="self.open || $index < 3">
                    <won-square-image src="request.titleImgSrc" title="request.title"></won-square-image>
                    <div class="fmil__item__description">
                        <div class="fmil__item__description__topline">
                            <div class="fmil__item__description__topline__title">{{request.title}}</div>
                        </div>
                        <div class="fmil__item__description__message">{{request.message}}</div>
                    </div>
                </div>
                <div class="fmil__more clickable" ng-show="!self.open && self.item.requests.length > 3" ng-click="self.expandActivities()">{{self.item.requests.length - 3}} more activities</div>
            </div>
            <div class="fi__footer">
                <div class="fi__footer__indicators">
                    <a class="fi__footer__indicators__item clickable" ng-href="post/{{self.item.id}}/owner/matches" ng-show="self.item.matches">
                        <img src="generated/icon-sprite.svg#ico36_match" class="fi__footer__indicators__item__icon"/>
                        <span class="fi__footer__indicators__item__caption">{{self.item.matches.length}} Matches</span>
                    </a>
                    <a class="fi__footer__indicators__item clickable" ng-href="post/{{self.item.id}}/owner/requests" ng-show="self.item.requests">
                        <img src="generated/icon-sprite.svg#ico36_incoming" class="fi__footer__indicators__item__icon"/>
                        <span class="fi__footer__indicators__item__caption">{{self.item.requests.length}} Incoming Requests</span>
                    </a>
                </div>
            </div>`;

    class Controller {
        constructor() {}

        expandActivities() {
            this.open = true;
        }

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

export default angular.module('won.owner.components.feedItem', [
    squareImageModule
])
    .directive('wonFeedItem', genComponentConf)
    .name;

