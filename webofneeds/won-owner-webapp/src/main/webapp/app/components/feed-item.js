;

import angular from 'angular';

function genComponentConf() {
    let template = `
            <div class="fi clickable">
                <img class="fi__image" ng-show="self.item.titleImgSrc" ng-src="{{self.item.titleImgSrc}}"/>
                <!--<img class="fi__image clickable" ng-show="!self.item.titleImgSrc" src="generated/icon-sprite.svg#illu_drag_here"/>-->
                <div class="fi__image" style="background-color: {{self.generateHexColor(self.item.title)}}" ng-show="!self.item.titleImgSrc"><!-- figure out some better way to color -->
                    <span class="fi__image__noimage">{{self.item.title.charAt(0)}}</span>
                </div>
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
                    <img class="fmil__item__image" ng-show="request.titleImgSrc" ng-src="{{request.titleImgSrc}}"/>
                    <!--<img class="fi__image clickable" ng-show="!self.item.titleImgSrc" src="generated/icon-sprite.svg#illu_drag_here"/>-->
                    <div class="fmil__item__image" style="background-color: {{self.generateHexColor(request.title)}}" ng-show="!request.titleImgSrc"><!-- figure out some better way to color -->
                        <span class="fmil__item__image__noimage">{{request.title.charAt(0)}}</span>
                    </div>
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

export default angular.module('won.owner.components.feedItem', [])
    .directive('wonFeedItem', genComponentConf)
    .name;

