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
                    <img class="vtb__inner__left__image" src="images/someNeedTitlePic.png"></img>
                    <div class="vtb__inner__left__titles">
                        <h1 class="vtb__title">New flat, need furniture (Visitors view)</h1>
                        <div class="vtb__inner__left__titles__type">I want to have something, 27.2.2015 </div>
                    </div>
                </div>
                <div class="vtb__inner__right">
                    <button class="won-button--filled red">Quit Contact</button>
                    <ul class="vtb__tabs">
                        <li><a href="#">
                            Messages
                            <span class="vtb__tabs__unread">5</span>
                        </a></li>
                        <li class="vtb__tabs__selected"><a href="#">
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
    }

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {},
        template: template
    }
}

export default angular.module('won.owner.components.visitorTitleBar', [])
    .directive('wonVisitorTitleBar', genComponentConf)
    .name;
