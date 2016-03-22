;

import angular from 'angular';
import squareImageModule from './square-image';
import dynamicTextFieldModule from './dynamic-textfield';
import { attach } from '../utils.js'
import { actionCreators }  from '../actions/actions';
import { labels, updateRelativeTimestamps } from '../won-label-utils';
import { selectAllByConnections, selectOpenConnectionUri } from '../selectors';

const serviceDependencies = ['$ngRedux', '$scope'];

function genComponentConf() {
    let template = `
        <div class="pm__header">
            <img class="pm__header__icon clickable" src="generated/icon-sprite.svg#ico36_close" ng-click="self.closeConversation()"/>
            <div class="pm__header__title">Conversation about "{{ self.connectionData.getIn(['remoteNeed', 'title']) }}"</div>
            <div class="pm__header__options">Options  </div>
            <img class="pm__header__options__icon clickable" src="generated/icon-sprite.svg#ico_settings" ng-click="self.openConversationOption()"/>
        </div>
        <div class="pm__content">
            <div
                class="pm__content__message"
                ng-repeat="message in self.chatMessages().toJS()"
                ng-class="message.hasSenderNeed == self.connectionData.getIn(['ownNeed', 'uri']) ? 'right' : 'left'">
                    <won-square-image
                        title="self.connectionData.getIn(['remoteNeed', 'title'])"
                        src="self.connectionData.getIn(['remoteNeed', 'titleImgSrc'])"
                        ng-show="message.hasSenderNeed != self.connectionData.getIn(['ownNeed', 'uri'])">
                    </won-square-image>
                    <div class="pm__content__message__content">
                        <div class="pm__content__message__content__text">{{ message.hasTextMessage }}</div>
                        <div class="pm__content__message__content__time">{{ message.humanReadableTimestamp }}</div>
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
            const selectFromState = state => {
                const connectionUri = selectOpenConnectionUri(state);
                return {
                    connectionData: selectAllByConnections(state).get(connectionUri),
                    state4dbg: state,
                }
            };

            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);
        }
        chatMessages() {
            if (!this.connectionData || !this.connectionData.get('events')) {
                return [];
            }else {
                const toDate = (ts) => new Date(Number.parseInt(ts));
                return this.connectionData.get('events').filter(e => {
                    if (e.get('hasTextMessage')) return true;
                    else {
                        const remote = e.get('hasCorrespondingRemoteMessage');
                        return remote && remote.get('hasTextMessage');
                    }
                }).map(e => {
                    const remote = e.get('hasCorrespondingRemoteMessage');
                    if (e.get('hasTextMessage'))
                        return e;
                    else
                        return remote;
                }).sort((a, b) =>
                    toDate(a.get('hasReceivedTimestamp')) > toDate(b.get('hasReceivedTimestamp'))
                ).map(e => {
                    e.set('humanReadableTimestamp', (
                            toDate(e.get('hasReceivedTimestamp'))
                        ).toString());
                    return e;
                });
            }

        }

        input(input) {
            this.chatMessage = input;
        }

        send() {
            const trimmedMsg = this.chatMessage.trim();
            const connectionUri = this.connectionData.getIn(['connection', 'uri']);
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
        scope: {
            openConversation: "=", //TODO used bidirectional binding :(
        },
        template: template
    }
}

export default angular.module('won.owner.components.postMessages', [
    squareImageModule,
    dynamicTextFieldModule
])
    .directive('wonPostMessages', genComponentConf)
    .name;
