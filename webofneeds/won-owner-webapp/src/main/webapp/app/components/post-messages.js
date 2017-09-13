;

import angular from 'angular';
import Immutable from 'immutable';
import squareImageModule from './square-image.js';
import chatTextFieldModule from './chat-textfield.js';
import {
    connect2Redux,
} from '../won-utils.js';
import {
    attach,
    delay,
} from '../utils.js'
import {
    actionCreators
}  from '../actions/actions.js';
import {
    selectOpenConnectionUri,
    selectNeedByConnectionUri,
} from '../selectors.js';

const serviceDependencies = ['$ngRedux', '$scope', '$element'];

function genComponentConf() {
    let template = `
        <div class="pm__header">
            <a class="clickable"
                ng-click="self.router__stateGoCurrent({connectionUri : null})">
                <img class="pm__header__icon clickable"
                     src="generated/icon-sprite.svg#ico36_close"/>
            </a>
            <div class="pm__header__title clickable"
                ng-click="self.router__stateGoAbs('post', { postUri: self.theirNeed.get('uri')})">
                {{ self.theirNeed.get('title') }}
            </div>
        </div>
        <div class="pm__content">
            <img src="images/spinner/on_white.gif"
                alt="Loading&hellip;"
                ng-show="self.connection.get('loadingEvents')"
                class="hspinner"/>
                <a ng-show="self.eventsLoaded && !self.connection.get('loadingEvents') && !self.allLoaded"
                    ng-click="self.connections__showMoreMessages(self.connection.get('uri'), 5)"
                    href="">
                        show more
                </a>
            <div
                class="pm__content__message"
                ng-repeat="message in self.chatMessages"
                ng-class="message.get('outgoingMessage') ? 'right' : 'left'">
                    <won-square-image
                        title="self.theirNeed.get('title')"
                        src="self.theirNeed.get('TODOtitleImgSrc')"
                        uri="self.theirNeed.get('uri')"
                        ng-click="self.router__stateGoAbs('post', {postUri: self.theirNeed.get('uri')})"
                        ng-show="!message.get('outgoingMessage')">
                    </won-square-image>
                    <div class="pm__content__message__content">
                        <div class="pm__content__message__content__text">
                            {{ message.get('text') }}
                        </div>
                        <div
                            ng-show="message.get('unconfirmed')"
                            class="pm__content__message__content__time">
                                Pending&nbsp;&hellip;
                        </div>
                        <div
                            ng-hide="message.get('unconfirmed')"
                            class="pm__content__message__content__time">
                                {{ message.get('date') }}
                        </div>
                        <a
                          ng-show="self.debugmode && message.get('outgoingMessage')"
                          class="debuglink"
                          target="_blank"
                          href="/owner/rest/linked-data/?requester={{self.encodeParam(self.ownNeed.get('uri'))}}&uri={{self.encodeParam(message.get('uri'))}}&deep=true">
                            [MSGDATA]
                        </a>
                        <a
                          ng-show="self.debugmode && !message.get('outgoingMessage')"
                          class="debuglink"
                          target="_blank"
                          href="/owner/rest/linked-data/?requester={{self.encodeParam(self.ownNeed.get('uri'))}}&uri={{self.encodeParam(message.get('uri'))}}&deep=true">
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

            const self = this;

            this.scrollContainer().addEventListener('scroll', e => this.onScroll(e));

            //this.postmsg = this;
            const selectFromState = state => {
                const connectionUri = selectOpenConnectionUri(state);
                const ownNeed = selectNeedByConnectionUri(state, connectionUri);
                const connection = ownNeed && ownNeed.getIn(["connections", connectionUri]);

                const theirNeed = connection && state.getIn(["needs", connection.get('remoteNeedUri')]);
                const chatMessages = connection && connection.get("messages");
                const allLoaded = chatMessages && chatMessages.filter(msg => msg.get("connectMessage")).size > 0;

                let sortedMessages = chatMessages && chatMessages.toArray();
                if(sortedMessages){
                    sortedMessages.sort(function(a,b) {
                        return a.get("date").getTime() - b.get("date").getTime();
                    });
                }
                //TODO: SET RELATIVE TIMESTAMPS


                return {
                    ownNeed,
                    theirNeed,
                    connection,
                    eventsLoaded: true, //TODO: CHECK IF MESSAGES ARE CURRENTLY LOADED
                    lastUpdateTime: state.get('lastUpdateTime'),
                    chatMessages: sortedMessages,
                    debugmode: won.debugmode,

                    // if the connect-message is here, everything else should be as well
                    allLoaded,
                }
            };

            connect2Redux(selectFromState, actionCreators, [], this);

            this.snapToBottom();

            this.$scope.$watchGroup(
                ['self.connection'],
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
                    this.connection &&
                    !this.connection.get('loadingEvents')
                    //&& !this.eventsLoaded
                ) {
                    this.connections__showLatestMessages(this.connection.get('uri'), 4);
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
            return angular.element(this.scrollContainer());
        }
        scrollContainer() {
            if(!this._scrollContainer) {
                this._scrollContainer = this.$element[0].querySelector('.pm__content');
            }
            return this._scrollContainer;
        }

        input(userInput) {
            this.chatMessage = userInput;
        }

        send() {
            const trimmedMsg = this.chatMessage.trim();
            if(trimmedMsg) {
               this.connections__sendChatMessage(trimmedMsg, this.connection.get('uri'));
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
