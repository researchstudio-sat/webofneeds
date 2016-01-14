;

import angular from 'angular';
import squareImageModule from './square-image';
import dynamicTextFieldModule from './dynamic-textfield';

function genComponentConf() {
    let template = `
        <div class="pm__header">Conversation about "{{self.item.title}}"</div>
        <div class="pm__content">
            <div class="pm__content__message" ng-repeat="message in self.item.messages" ng-class="message.ownMessage? 'right' : 'left'">
                <won-square-image title="self.item.title" src="self.item.titleImgSrc" ng-show="!message.ownMessage"></won-square-image>
                <div class="pm__content__message__content">
                    <div class="pm__content__message__content__text">{{message.text}}</div>
                    <div class="pm__content__message__content__time">{{message.timeStamp}}</div>
                </div>
            </div>
        </div>
        <div class="pm__footer">
            <won-dynamic-textfield placeholder="::'Your Message'"></won-dynamic-textfield>
            <button class="won-button--filled red">Send</button>
        </div>
    `;

    class Controller {
        constructor() {}
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

export default angular.module('won.owner.components.postMessages', [
    squareImageModule,
    dynamicTextFieldModule
])
    .directive('wonPostMessages', genComponentConf)
    .name;

