;

import angular from 'angular';
import squareImageModule from './square-image';
import dynamicTextFieldModule from './dynamic-textfield';
import { attach } from '../utils.js'
import { actionCreators }  from '../actions/actions';

const serviceDependencies = ['$ngRedux', '$scope'];

function genComponentConf() {
    let template = `
        <div class="pm__header">
            <img class="pm__header__icon clickable" src="generated/icon-sprite.svg#ico36_close" ng-click="self.closeConversation()"/>
            <div class="pm__header__title">Conversation about "{{self.item.remoteNeed.title}}"</div>
            <div class="pm__header__options">Options  </div>
            <img class="pm__header__options__icon clickable" src="generated/icon-sprite.svg#ico_settings" ng-click="self.openConversationOption()"/>
        </div>
        <div class="pm__content">
            <div class="pm__content__message" ng-repeat="message in self.item.events |filterByEventMsgs" ng-class="message.hasSenderNeed == self.item.ownNeed.uri? 'right' : 'left'">
                <won-square-image title="self.item.remoteNeed.title" src="self.item.remoteNeed.titleImgSrc" ng-show="message.hasSenderNeed != self.item.ownNeed.uri"></won-square-image>
                <div class="pm__content__message__content">
                    <div class="pm__content__message__content__text">{{message.hasTextMessage}}</div>
                    <div class="pm__content__message__content__time">{{message.hasReceivedTimestamp}}</div>
                </div>
            </div>
        </div>
        <div class="pm__footer">
            <won-dynamic-textfield
                placeholder="::'Your Message'"
                on-input="::self.input(value)">
            </won-dynamic-textfield>
            <button
                class="won-button--filled red"
                ng-click="::self.send()">Send</button>
        </div>
    `;

    class Controller {
        constructor(/* arguments = dependency injections */) {
            attach(this, serviceDependencies, arguments);
            window.pm4dbg = this;
            //this.postmsg = this;
            const selectFromState = state => ({
                state4dbg: state
            });

            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);
        }

        input(input) {
            console.log('post message: ', input);
            this.chatMessage = input;
        }

        send() {
            const trimmedMsg = this.chatMessage.trim();
            if(trimmedMsg) {
               this.connections__sendChatMessage(trimmedMsg);
            }
        }
    }
    Controller.$inject = serviceDependencies;

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {item: "=",
                openConversation:"="},
        template: template
    }
}

export default angular.module('won.owner.components.postMessages', [
    squareImageModule,
    dynamicTextFieldModule
])
    .directive('wonPostMessages', genComponentConf)
    .name;

