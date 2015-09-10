;

import angular from 'angular';

function genComponentConf() {
    let template = `
            <div class="ril clickable" ng-click="self.toggleRequest()">
                <img class="ril__image" ng-show="self.item.titleImgSrc" ng-src="{{self.item.titleImgSrc}}"/>
                <!--<img class="ril__image clickable" ng-show="!self.item.titleImgSrc" src="generated/icon-sprite.svg#illu_drag_here"/>-->
                <div class="ril__image" style="background-color: {{self.generateHexColor(self.item.title)}}" ng-show="!self.item.titleImgSrc"><!-- figure out some better way to color -->
                    <span class="ril__image__noimage">{{self.item.title.charAt(0)}}</span>
                </div>
                <div class="ril__description">
                    <div class="ril__description__topline">
                        <div class="ril__description__topline__title">{{self.item.title}}</div>
                        <div class="ril__description__topline__messagecount">{{self.item.requests.length}}</div>
                    </div>
                    <div class="ril__description__subtitle">
                        <span class="ril__description__subtitle__group" ng-show="self.item.group">
                            <img src="generated/icon-sprite.svg#ico36_group" class="ril__description__subtitle__group__icon">{{self.item.group}}<span class="ril__description__subtitle__group__dash"> &ndash; </span>
                        </span>
                        <span class="ril__description__subtitle__type">{{self.getType(self.item.type)}}</span>
                    </div>
                </div>
                <div class="ril__carret">
                    <img class="ril__arrow" ng-show="self.open" src="generated/icon-sprite.svg#ico16_arrow_up"/>
                    <img class="ril__arrow" ng-show="!self.open" src="generated/icon-sprite.svg#ico16_arrow_down"/>
                </div>
            </div>
            <div class="mil" ng-show="self.open">
                <div class="mil__item clickable" ng-class="self.openRequest === request? 'selected' : ''" ng-repeat="request in self.item.requests" ng-click="self.openMessage(request)">
                    <img class="mil__item__image" ng-show="request.titleImgSrc" ng-src="{{request.titleImgSrc}}"/>
                    <!--<img class="ril__image clickable" ng-show="!self.item.titleImgSrc" src="generated/icon-sprite.svg#illu_drag_here"/>-->
                    <div class="mil__item__image" style="background-color: {{self.generateHexColor(request.title)}}" ng-show="!request.titleImgSrc"><!-- figure out some better way to color -->
                        <span class="mil__item__image__noimage">{{request.title.charAt(0)}}</span>
                    </div>
                    <div class="mil__item__description">
                        <div class="mil__item__description__topline">
                            <div class="mil__item__description__topline__title">{{request.title}}</div>
                            <div class="mil__item__description__topline__date">{{request.timeStamp}}</div>
                        </div>
                        <div class="mil__item__description__subtitle">
                            <span class="mil__item__description__subtitle__group" ng-show="request.group">
                                <img src="generated/icon-sprite.svg#ico36_group" class="mil__item__description__subtitle__group__icon">{{request.group}}<span class="mil__item__description__subtitle__group__dash"> &ndash; </span>
                            </span>
                            <span class="mil__item__description__subtitle__type">{{self.getType(request.type)}}</span>
                        </div>
                        <div class="mil__item__description__message">
                            <span class="mil__item__description__message__indicator" ng-show="!request.read"/>{{request.message}}
                        </div>
                    </div>
                </div>
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

        toggleRequest() {
            console.log("toggle the request");
            this.open = !this.open;
        }

        openMessage(request) {
            console.log("opening the request");
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

