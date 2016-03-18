;

import angular from 'angular';
import squareImageModule from './square-image';
import dynamicTextFieldModule from './dynamic-textfield';
import { attach } from '../utils.js'
import { actionCreators }  from '../actions/actions';
import { labels, updateRelativeTimestamps } from '../won-label-utils';

const serviceDependencies = ['$ngRedux', '$scope'];

function genComponentConf() {
    let template = `
        <div class="pm__header">
            <img class="pm__header__icon clickable" src="generated/icon-sprite.svg#ico36_close" ng-click="self.closeConversation()"/>
            <div class="pm__header__title">Conversation about "{{self.connectionAndRelatedData.remoteNeed.title}}"</div>
            <div class="pm__header__options">Options  </div>
            <img class="pm__header__options__icon clickable" src="generated/icon-sprite.svg#ico_settings" ng-click="self.openConversationOption()"/>
        </div>
        <div class="pm__content">
            <div
                class="pm__content__message"
                ng-repeat="message in self.chatMessages()"
                ng-class="message.hasSenderNeed == self.connectionAndRelatedData.ownNeed.uri? 'right' : 'left'">
                    <won-square-image
                        title="self.connectionAndRelatedData.remoteNeed.title"
                        src="self.connectionAndRelatedData.remoteNeed.titleImgSrc"
                        ng-show="message.hasSenderNeed != self.connectionAndRelatedData.ownNeed.uri">
                    </won-square-image>
                    <div class="pm__content__message__content">
                        <div class="pm__content__message__content__text">{{ message.hasTextMessage }}</div>
                        <div class="pm__content__message__content__time">{{ message.hasReceivedTimestamp }}</div>
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
        chatMessages() {
            return this.connectionAndRelatedData.events.filter(e => {
                if(e.hasTextMessage) return true;
                else {
                    const remote = e.hasCorrespondingRemoteMessage;
                    return remote && remote.hasTextMessage;
                }
            }).map(e => {
                const remote = e.hasCorrespondingRemoteMessage;
                if(e.hasTextMessage)
                    return e;
                else
                    return remote;
            })

        }

        input(input) {
            this.chatMessage = input;
        }

        send() {
            const trimmedMsg = this.chatMessage.trim();
            const connectionUri = this.connectionAndRelatedData.connection.uri;
            if(trimmedMsg) {
               this.connections__sendChatMessage(trimmedMsg, connectionUri);
            }
        }
    }
    Controller.$inject = serviceDependencies;

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {connectionAndRelatedData: "=",
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

