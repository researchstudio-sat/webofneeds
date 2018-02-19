;

import angular from 'angular';
import jld from 'jsonld';
import Immutable from 'immutable';
import squareImageModule from './square-image.js';
import chatTextFieldModule from './chat-textfield.js';
import chatTextFieldSimpleModule from './chat-textfield-simple.js';
import {
    relativeTime,
} from '../won-label-utils.js'
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
import autoresizingTextareaModule from '../directives/textarea-autogrow.js';

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
                        <div class="pm__content__message__content__text" title="{{ self.shouldShowRdf ? self.rdfToString(message.get('contentGraphs')) : undefined }}">
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
                                {{ self.relativeTime(self.lastUpdateTime, message.get('date')) }}
                        </div>
                        <a
                          ng-show="self.shouldShowRdf && message.get('outgoingMessage')"
                          target="_blank"
                          href="/owner/rest/linked-data/?requester={{self.encodeParam(self.ownNeed.get('uri'))}}&uri={{self.encodeParam(message.get('uri'))}}&deep=true">
                            <svg class="rdflink__small clickable">
                                    <use href="#rdf_logo_2"></use>
                            </svg>
                        </a>
                         <a
                          ng-show="self.shouldShowRdf && !message.get('outgoingMessage')"
                          target="_blank"
                          href="/owner/rest/linked-data/?requester={{self.encodeParam(self.ownNeed.get('uri'))}}&uri={{self.encodeParam(message.get('uri'))}}">
                            <svg class="rdflink__small clickable">
                                <use href="#rdf_logo_2"></use>
                            </svg>
                        </a>
                    </div>
            </div>
        </div>
        <chat-textfield
            class="pm__footer"
            placeholder="::'Your Message'"
            on-input="::self.input(value)"
            on-paste="::self.input(value)"
            on-submit="::self.send()"
            submit-button-label="::'Send'"
            >
        </chat-textfield>
        <!--
        <chat-textfield-simple
            class="pm__footer"
            placeholder="::'Your Message'"
            on-input="::self.input(value)"
            on-paste="::self.input(value)"
            on-submit="::self.send()"
            submit-button-label="::'Send'"
            >
        </chat-textfield-simple>
        -->

        <!-- 
        quick'n'dirty textfield and button so flo can use it for his branch. 
        TODO implement and style chat-textfield-simple and use that instead.
        -->
        <div style="display: flex;">
            <textarea 
                class="rdfTxtTmpDeletme" 
                ng-show="self.shouldShowRdf" 
                won-textarea-autogrow 
                style="resize: none; height: auto;   flex-grow: 1;"
                placeholder="any valid turtle syntax. all usages of \`msguri:placeholder\` will be string-replaced by the proper message uri. see \`won.minimalTurtlePrefixes \` for prefixes that will be added automatically."
            ></textarea>
            <button 
                class="rdfMsgBtnTmpDeletme" 
                ng-show="self.shouldShowRdf" 
                ng-click="self.sendRdfTmpDeletme()">
                    Send RDF
            </button>
        </div>



        <div>
            <a class="rdflink withlabel clickable"
               ng-click="self.toggleRdfDisplay()">
                   <svg class="rdflink__small">
                       <use href="#rdf_logo_1"></use>
                   </svg>
                  <span class="rdflink__text">[{{self.shouldShowRdf? "HIDE" : "SHOW"}}]</span> 
            </a>
        </div>
    `;



    class Controller {
        constructor(/* arguments = dependency injections */) {
            attach(this, serviceDependencies, arguments);
            this.relativeTime = relativeTime;
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
                    shouldShowRdf: state.get('showRdf'),

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
        
        rdfToString(jsonld){
        	return JSON.stringify(jsonld);
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

        sendRdfTmpDeletme() {
            const rdftxtEl = this.$element[0].querySelector('.rdfTxtTmpDeletme');
            if(rdftxtEl) {
                console.log('found rdftxtel: ', rdftxtEl.value);
                const trimmedMsg = rdftxtEl.value.trim();
                if(trimmedMsg) {
                    this.connections__sendChatMessage(trimmedMsg, this.connection.get('uri'), isTTL=true);
                }
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
    autoresizingTextareaModule,
    chatTextFieldSimpleModule,
])
    .directive('wonPostMessages', genComponentConf)
    .name;
