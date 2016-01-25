/**
 * Created by ksinger on 20.08.2015.
 */
;

import angular from 'angular';
import { labels } from '../won-label-utils';

function genComponentConf() {
    let template = `
        <nav class="visitor-title-bar">
            <div class="vtb__inner">
                <div class="vtb__inner__left">
                    <a ng-click="self.back()">
                        <img src="generated/icon-sprite.svg#ico36_backarrow" class="vtb__icon">
                    </a>
                    <won-square-image title="self.item.title" src="self.item.titleImgSrc"></won-square-image>
                    <div class="vtb__inner__left__titles">
                        <h1 class="vtb__title">{{self.item.title}}</h1>
                        <div class="vtb__inner__left__titles__type">{{self.labels.type[self.item.type]}}, {{self.item.creationDate}} </div>
                    </div>
                </div>
                <div class="vtb__inner__right">
                    <button class="won-button--filled red">Quit Contact</button>
                    <ul class="vtb__tabs">
                        <li ng-class="self.selection == 0? 'vtb__tabs__selected' : ''" ng-click="self.selection = 0">
                        <a ui-sref="postVisitorMsgs({postId: 'http://example.org/121337345'})">
                            Messages
                            <span class="vtb__tabs__unread">{{self.item.messages.length}}</span>
                        </a></li>
                        <li ng-class="self.selection == 1? 'vtb__tabs__selected' : ''" ng-click="self.selection = 1">
                        <a ui-sref="postVisitor({postId: 'http://example.org/121337345'})">
                            Post Info
                        </a></li>
                    </ul>
                </div>
            </div>
        </nav>
    `;

    class Controller {
        constructor() {
            this.labels = labels;
        }
        back() { window.history.back() }
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
