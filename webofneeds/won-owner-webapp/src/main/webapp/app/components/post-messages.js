;

import angular from 'angular';
import Immutable from 'immutable';
import squareImageModule from './square-image';
import chatTextFieldModule from './chat-textfield';
import {
    attach,
    is,
    delay,
    msStringToDate,
    urisToLookupMap,
} from '../utils'
import {
    actionCreators
}  from '../actions/actions';
import {
    labels,
    relativeTime,
} from '../won-label-utils';
import {
    selectAllByConnections,
    selectOpenConnectionUri,
    selectOpenConnection,
    selectSortedChatMessages,
    selectSortedChatMessagesArray,
} from '../selectors';
import {
    seeksOrIs,
    inferLegacyNeedType,
    selectTimestamp,
} from '../won-utils'

const serviceDependencies = ['$ngRedux', '$scope', '$element'];

function genComponentConf() {
    let template = `
        <div class="pm__header">
            <a ui-sref="{connectionUri : null}">
                <img class="pm__header__icon clickable"
                     src="generated/icon-sprite.svg#ico36_close"/>
            </a>
            <div class="pm__header__title"
              ui-sref="post({ postUri: self.theirNeed.get('@id'), connectionUri: null, connectionType: null})">
                {{ self.theirNeedContent.get('dc:title') }}
            </div>
        </div>
        <div class="pm__content">
            <img src="images/spinner/on_white.gif"
                alt="Loading&hellip;"
                ng-show="self.connection.get('loadingEvents')"
                class="hspinner"/>
                <a ng-show="self.eventsLoaded && !self.connection.get('loadingEvents') && !self.allLoaded"
                    ng-click="self.connections__showMoreMessages(self.connectionUri, 5)"
                    href="">
                        show more
                </a>
            <div
                class="pm__content__message"
                ng-repeat="message in self.chatMessages"
                ng-class="message.get('hasSenderNeed') == self.connectionData.getIn(['ownNeed', '@id']) ? 'right' : 'left'">
                    <won-square-image
                        title="self.theirNeedContent.get('dc:title')"
                        src="self.theirNeedContent.get('TODOtitleImgSrc')"
                        uri="self.theirNeed.get('@id')"
                        ui-sref="post({postUri: self.theirNeed.get('@id'), connectionUri: null, connectionType: null})"
                        ng-show="message.get('hasSenderNeed') != self.ownNeed.get('@id')">
                    </won-square-image>
                    <div class="pm__content__message__content">
                        <div class="pm__content__message__content__text">
                            {{ message.get('hasTextMessage') }}
                        </div>
                        <div
                            ng-show="message.get('unconfirmed')"
                            class="pm__content__message__content__time">
                                Pending&nbsp;&hellip;
                        </div>
                        <div
                            ng-hide="message.get('unconfirmed')"
                            class="pm__content__message__content__time">
                                {{ message.get('humanReadableTimestamp') }}
                        </div>
                        <a
                          ng-show="self.debugmode && message.get('hasSenderNeed') == self.ownNeed.get('@id')"
                          class="debuglink"
                          target="_blank"
                          href="/owner/rest/linked-data/?requester={{self.encodeParam(message.get('hasSenderNeed'))}}&uri={{self.encodeParam(message.get('uri'))}}&deep=true">
                            [MSGDATA]
                        </a>
                        <a
                          ng-show="self.debugmode && message.get('hasSenderNeed') != self.ownNeed.get('@id')"
                          class="debuglink"
                          target="_blank"
                          href="/owner/rest/linked-data/?requester={{self.encodeParam(message.get('hasReceiverNeed'))}}&uri={{self.encodeParam(message.get('uri'))}}&deep=true">
                            [MSGDATA]
                        </a>
                    </div>
            </div>
        </div>
        <div>
            <chat-textfield
                class="pm__footer"
                placeholder="::'Your Message'"
                on-input="::self.input(value)"
                on-paste="::self.input(value)"
                on-submit="::self.send()"
                submit-button-label="::'Send'"
                >
            </chat-textfield>
        </div>
    `;



    class Controller {
        constructor(/* arguments = dependency injections */) {
            attach(this, serviceDependencies, arguments);
            window.pm4dbg = this;
            window.selectOpenConnectionUri4dbg = selectOpenConnectionUri;

            const self = this;

            this.scrollContainerNg().bind('scroll', e => this.onScroll(e));

            //this.postmsg = this;
            const selectFromState = state => {
                const connectionUri = selectOpenConnectionUri(state);
                const connection = selectOpenConnection(state);
                const eventUris = connection && connection.get('hasEvents');
                const eventsLoaded = eventUris && eventUris.size > 0;
                const connectionData = selectAllByConnections(state).get(connectionUri);
                const ownNeed = connectionData && connectionData.get('ownNeed');
                const theirNeed = connectionData && connectionData.get('remoteNeed');
                const chatMessages = selectSortedChatMessages(state);
                const chatMessagesArray = selectSortedChatMessagesArray(state);
                return {
                    connectionData,
                    connectionUri,
                    connection,
                    eventsLoaded,
                    lastUpdateTime: state.get('lastUpdateTime'),
                    chatMessages: chatMessagesArray, //toArray needed as ng-repeat won't work otherwise :|
                    state4dbg: state,
                    debugmode: won.debugmode,

                    // if the connect-message is here, everything else should be as well
                    allLoaded: chatMessages
                        .some(m => m.get('hasMessageType') === won.WONMSG.connectMessage),

                    ownNeed,
                    ownNeedType: ownNeed && inferLegacyNeedType(ownNeed),
                    ownNeedContent: ownNeed && seeksOrIs(ownNeed),

                    theirNeed,
                    theirNeedType: theirNeed && inferLegacyNeedType(theirNeed),
                    theirNeedContent: theirNeed && seeksOrIs(theirNeed),
                }
            };

            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);

            this.snapToBottom();

            this.$scope.$watchGroup(
                ['self.connectionUri', 'self.connection'],
                () => this.ensureMessagesAreLoaded()
            );

            this.$scope.$watch(
                () => this.chatMessages && this.chatMessages.length, // trigger if there's messages added (or removed)
                () => delay(0).then(() =>
                    // scroll to bottom directly after rendering, if snapped
                    this.updateScrollposition()
                )
            )

        }

        ensureMessagesAreLoaded() {
            delay(0).then(() => {
                // make sure latest messages are loaded
                if (
                    this.connectionUri &&
                    this.connection &&
                    !this.connection.get('loadingEvents') &&
                    !this.eventsLoaded
                ) {
                    this.connections__showLatestMessages(this.connectionUri, 4);
                }
            })
        }

        encodeParam(param) {
            var encoded = encodeURIComponent(param);
            // console.log("encoding: ",param);
            // console.log("encoded: ",encoded)
            return encoded;
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
            this._programmaticallyScrolling = true;

            this.scrollContainer().scrollTop = this.scrollContainer().scrollHeight;
        }
        onScroll(e) {
            if(!this._programmaticallyScrolling) {
                //only unsnap if the user scrolled themselves
                this.unsnapFromBottom();
            }

            const sc = this.scrollContainer();
            const isAtBottom = sc.scrollTop + sc.offsetHeight >= sc.scrollHeight;
            if(isAtBottom) {
                this.snapToBottom();
            }

            this._programmaticallyScrolling = false
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

        input(userInput) {
            this.chatMessage = userInput;

            this.connections__typedAtChatMessage({
                message: userInput ,
                connectionUri: this.connectionUri,
            });
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
    chatTextFieldModule,
])
    .directive('wonPostMessages', genComponentConf)
    .name;
