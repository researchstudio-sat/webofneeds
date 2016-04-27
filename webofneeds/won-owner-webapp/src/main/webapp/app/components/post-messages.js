;

import angular from 'angular';
import Immutable from 'immutable';
import squareImageModule from './square-image';
import dynamicTextFieldModule from './dynamic-textfield';
import { attach, is, delay } from '../utils.js'
import { actionCreators }  from '../actions/actions';
import { labels, relativeTime } from '../won-label-utils';
import { selectAllByConnections, selectOpenConnectionUri } from '../selectors';

const serviceDependencies = ['$ngRedux', '$scope', '$element'];

function genComponentConf() {
    let template = `
        <div class="pm__header">
            <a ui-sref="postConversations({openConversation : null})">
                <img class="pm__header__icon clickable"
                     src="generated/icon-sprite.svg#ico36_close"/>
            </a>
            <div class="pm__header__title">
                Conversation about "{{ self.connectionData.getIn(['remoteNeed', 'title']) }}"
            </div>
            <div class="pm__header__options">
                Options
            </div>
            <img
                class="pm__header__options__icon clickable"
                src="generated/icon-sprite.svg#ico_settings"
                ng-click="self.openConversationOption()"/>
        </div>
        <div class="pm__content">
            <div
                class="pm__content__message"
                ng-repeat="message in self.chatMessages"
                ng-class="message.hasSenderNeed == self.connectionData.getIn(['ownNeed', 'uri']) ? 'right' : 'left'">
                    <won-square-image
                        title="self.connectionData.getIn(['remoteNeed', 'title'])"
                        src="self.connectionData.getIn(['remoteNeed', 'titleImgSrc'])"
                        ng-show="message.hasSenderNeed != self.connectionData.getIn(['ownNeed', 'uri'])">
                    </won-square-image>
                    <div class="pm__content__message__content">
                        <div class="pm__content__message__content__text">
                            {{ message.hasTextMessage }}
                        </div>
                        <div
                            ng-show="message.unconfirmed"
                            class="pm__content__message__content__time">
                                Pending‥
                        </div>
                        <div
                            ng-hide="message.unconfirmed"
                            class="pm__content__message__content__time">
                                {{ message.humanReadableTimestamp }}
                        </div>
                    </div>
            </div>
        </div>
        <won-dynamic-textfield
            class="pm__footer"
            placeholder="::'Your Message'"
            on-input="::self.input(value)"
            on-submit="::self.send()"
            submit-button-label="::'Send'"
            >
        </won-dynamic-textfield>
    `;

    class Controller {
        constructor(/* arguments = dependency injections */) {
            attach(this, serviceDependencies, arguments);
            window.pm4dbg = this;
            window.selectOpenConnectionUri4dbg = selectOpenConnectionUri;
            window.selectChatMessages4dbg = selectChatMessages;

            const self = this;

            //this.postmsg = this;
            const selectFromState = state => {

                //TODO seems like rather bad practice to have sideffects here
                //scroll to bottom directly after rendering, if snapped
                delay(0).then(() => {
                    self.updateScrollposition();
                    console.log('pm - delay ', self._snapBottom, self.chatMessages.length)
                });

                const connectionUri = selectOpenConnectionUri(state);
                const chatMessages = selectChatMessages(state);
                return {
                    lastUpdateTime: state.get('lastUpdateTime'),
                    connectionData: selectAllByConnections(state).get(connectionUri),
                    chatMessages: chatMessages && chatMessages.toJS(), //toJS needed as ng-repeat won't work otherwise :|
                    state4dbg: state,
                }
            };

            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);

            this.snapToBottom();
        }

        snapToBottom() {
            this._snapBottom = true;
            this.scrollToBottom();
        }
        unsnapFromBottom() {
            this._snapBottom = false;
        }
        updateScrollposition() {
            if(this._snapBottom) {
                this.scrollToBottom();
            }
        }
        scrollToBottom() {
            this.scrollContainer().scrollTop = this.scrollContainer().scrollHeight;
        }
        }
        scrollContainerNg() {
            if(!this._scrollContainer) {
                this._scrollContainer = this.$element.find('.pm__content');
            }
            return this._scrollContainer;
        }
        scrollContainer() {
            return this.scrollContainerNg()[0];
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
        scope: { },
        template: template,
    }
}

export default angular.module('won.owner.components.postMessages', [
    squareImageModule,
    dynamicTextFieldModule
])
    .directive('wonPostMessages', genComponentConf)
    .name;


//TODO refactor so that it always returns an array of immutable messages to
// allow ng-repeat without giving up the cheaper digestion
function selectChatMessages(state) {
    const connectionUri = selectOpenConnectionUri(state);
    const connectionData = selectAllByConnections(state).get(connectionUri);

    if (!connectionData || !connectionData.get('events')) {
        return Immutable.List();

    } else {

        const chatMessages = connectionData.get('events')

            /* filter for valid chat messages */
            .filter(event => {
                if (event.get('hasTextMessage')) return true;
                else {
                    let remote = event.get('hasCorrespondingRemoteMessage');
                    if(is('String', remote)) {
                        remote = state.getIn(['events', 'events', remote]);
                    }
                    return remote && remote.get('hasTextMessage');
                }
            }).map(event => {
                const remote = event.get('hasCorrespondingRemoteMessage');
                if (event.get('hasTextMessage'))
                    return event;
                else
                    return remote;
            })

            /* sort them so the latest get shown last */
            .sort((event1, event2) =>
                selectTimestamp(event1) - selectTimestamp(event2)
            )
            /*
             * sort so the latest, optimistic/unconfirmed
             * messages are always at the bottom.
             */
            .sort((event1, event2) => {
                const u1 = event1.get('unconfirmed');
                const u2 = event2.get('unconfirmed');

                if(u1 && !u2) {
                  return 1;
                }
                else if (!u1 && u2) {
                    return -1;
                }
                else {
                    return 0;
                }
            })

            /* add a nice relative timestamp */
            .map(event => event.set(
                    'humanReadableTimestamp',
                    relativeTime(
                        state.get('lastUpdateTime'),
                        selectTimestamp(event)
                    )
                )
            );

        return chatMessages;
    }

}

function toDate(ts) {
    return new Date(Number.parseInt(ts));
}
function selectTimestamp(event) {
    if(event.get('hasReceivedTimestamp')) {
        return toDate(event.get('hasReceivedTimestamp'));
    } else if(event.get('hasSentTimestamp')) {
        return toDate(event.get('hasSentTimestamp')) ;
    }
}
