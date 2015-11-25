/**
 * Created by ksinger on 20.08.2015.
 */
;

import angular from 'angular';

function genComponentConf() {
    let template = `
        <nav class="visitor-title-bar">
            <div class="vtb__inner">
                <div class="vtb__inner__left">
                    <a href="javascript:void(0)" ng-click="self.back()">
                        <img src="generated/icon-sprite.svg#ico36_backarrow" class="vtb__icon">
                    </a>
                    <won-square-image title="self.item.title" src="self.item.titleImgSrc"></won-square-image>
                    <div class="vtb__inner__left__titles">
                        <h1 class="vtb__title">{{self.item.title}}</h1>
                        <div class="vtb__inner__left__titles__type">{{self.getType(self.item.type)}}, {{self.item.creationDate}} </div>
                    </div>
                </div>
                <div class="vtb__inner__right">
                    <button class="won-button--filled red">Quit Contact</button>
                    <ul class="vtb__tabs">
                        <li ng-class="self.selection == 0? 'vtb__tabs__selected' : ''" ng-click="self.selection = 0"><a ng-href="post/121337245/visitor">
                            Messages
                            <span class="vtb__tabs__unread">{{self.item.messages.length}}</span>
                        </a></li>
                        <li ng-class="self.selection == 1? 'vtb__tabs__selected' : ''" ng-click="self.selection = 1"><a ng-href="post/121337245/visitor">
                            Post Info
                        </a></li>
                    </ul>
                </div>
            </div>
        </nav>
    `;

    class Controller {
        constructor() { }
        back() { window.history.back() }

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
        scope: {selection: "=",
                item: "="},
        template: template
    }
}

export default angular.module('won.owner.components.visitorTitleBar', [])
    .directive('wonVisitorTitleBar', genComponentConf)
    .name;
