;

import angular from 'angular';
import squareImageModule from '../components/square-image';
import { attach, decodeUriComponentProperly } from '../utils';
import won from '../won-es6';
import { labels } from '../won-label-utils';
import {
    selectOpenPostUri,
    selectAllMessagesByNeedUri,
} from '../selectors';
import { actionCreators }  from '../actions/actions';

const serviceDependencies = ['$ngRedux', '$scope'];
function genComponentConf() {
    let template = `
        <nav class="need-tab-bar" ng-cloak ng-show="{{true}}">
            <div class="ntb__inner">


                <div class="ntb__inner__left">
                    <a ui-sref="overviewPosts">
                        <img src="generated/icon-sprite.svg#ico36_backarrow" class="ntb__icon">
                    </a>
                    <won-square-image
                        ng-class="{'inactive' : !self.isActive}"
                        src="self.post.get('titleImgSrc')"
                        title="self.post.get('title')"
                        uri="self.post.get('uri')">
                    </won-square-image>
                </div>


                <div class="ntb__inner__right">


                    <div class ="ntb__inner__right__upper">
                        <hgroup>
                            <h1 class="ntb__title">{{ self.post.get('title') }}</h1>
                            <div class="ntb__titles__type">{{self.labels.type[self.post.get('type')]}}</div>
                        </hgroup>
                        <img
                            class="ntb__icon clickable"
                            src="generated/icon-sprite.svg#ico_settings"
                            ng-show="!self.settingsOpen"
                            ng-click="self.settingsOpen = true">
                        <div class="ntb__contextmenu contextmenu"
                            ng-show="self.settingsOpen">
                            <div class="content">
                                <div class="topline">
                                    <img
                                        class="contextmenu__icon clickable"
                                        src="generated/icon-sprite.svg#ico_settings"
                                        ng-click="self.settingsOpen = false">
                                </div>
                                <button
                                    class="won-button--filled thin red"
                                    ng-show="self.isActive"
                                    ng-click="self.closePost()">
                                        Close Post
                                </button>
                                <button
                                    class="won-button--filled thin red"
                                    ng-show="!self.isActive"
                                    ng-click="self.reOpenPost()">
                                        Reopen Post
                                </button>
                            </div>
                        </div>
                    </div>

                    <div class ="ntb__inner__right__lower">
                        <ul class="ntb__tabs">
                            <li ng-class="{'ntb__tabs__selected' : self.selectedTab === 'Info'}">
                                <a ui-sref="post({connectionType: null, openConversation: null, connectionUri: null, postUri: self.postUri})">
                                    Post Info
                                </a>
                            </li>
                            <li ng-class="{'ntb__tabs__selected' : self.selectedTab === self.WON.Connected}">

                                <a ui-sref="post({connectionType: self.WON.Connected, openConversation: null, connectionUri: null, postUri: self.postUri})"
                                    ng-class="{'disabled' : !self.hasMessages || !self.isActive}">
                                    Messages
                                    <span class="ntb__tabs__unread">{{ self.unreadMessages.size }}</span>
                                </a>
                            </li>
                            <li ng-class="{'ntb__tabs__selected' : self.selectedTab === self.WON.Suggested}">
                                <a ui-sref="post({connectionType: self.WON.Suggested, openConversation: null, connectionUri: null, postUri: self.postUri})"
                                    ng-class="{'disabled' : !self.hasMatches || !self.isActive}">
                                    Matches
                                    <span class="ntb__tabs__unread">{{ self.unreadMatches.size }}</span>
                                </a>
                            </li>
                            <li ng-class="{'ntb__tabs__selected' : self.selectedTab === self.WON.RequestReceived}">
                                <a ui-sref="post({connectionType: self.WON.RequestReceived, openConversation: null, connectionUri: null, postUri: self.postUri})"
                                    ng-class="{'disabled' : !self.hasIncomingRequests || !self.isActive}">
                                    Requests
                                    <span class="ntb__tabs__unread">{{ self.unreadIncomingRequests.size }}</span>
                                </a>
                            </li>
                            <li ng-class="{'ntb__tabs__selected' : self.selectedTab === self.WON.RequestSent}">
                                <a ui-sref="post({connectionType: self.WON.RequestSent, openConversation: null, connectionUri: null, postUri: self.postUri})"
                                    ng-class="{'disabled' : !self.hasSentRequests || !self.isActive}">
                                    Sent Requests
                                    <span class="ntb__tabs__unread">{{ self.unreadSentRequests.size }}</span>
                                </a>
                            </li>
                        </ul>
                    </div>


                </div>
            </div>
        </nav>
    `;


    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);

            window.otb = this;
            this.labels = labels;
            this.settingsOpen = false;

            const selectFromState = (state)=>{
                const postUri = selectOpenPostUri(state);
                const post = state.getIn(["needs", postUri]);

                const connections = post && post.get("connections");

                const sentRequests = connections && connections.filter(conn => conn.get("state") === won.WON.RequestSent);
                const incomingRequests = connections && connections.filter(conn => conn.get("state") === won.WON.RequestReceived);
                const matches = connections && connections.filter(conn => conn.get("state") === won.WON.Suggested);
                const messages = selectAllMessagesByNeedUri(state, postUri);

                return {
                    selectedTab: decodeUriComponentProperly(state.getIn(['router', 'currentParams', 'connectionType'])) || 'Info',
                    WON: won.WON,
                    postUri: postUri,
                    post: post,
                    hasIncomingRequests: incomingRequests && incomingRequests.size > 0,
                    hasSentRequests: sentRequests && sentRequests.size > 0,
                    hasMatches: matches && matches.size > 0,
                    hasMessages: messages && messages.size > 0,
                    unreadMessages: messages && messages.filter(msg => msg.get('newMessage')).size,
                    unreadIncomingRequests: incomingRequests && incomingRequests.filter(conn => conn.get("newConnection")).size,
                    unreadSentRequests: sentRequests && sentRequests.filter(conn => conn.get("newConnection")).size,
                    unreadMatches:  matches && matches.filter(conn => conn.get("newConnection")).size,
                    isActive: post && post.get('state') === won.WON.ActiveCompacted,
                };
            };

            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);
        }

        closePost() {
            console.log("CLOSING THE POST: "+this.postUri);
            this.settingsOpen = false;
            this.needs__close(this.postUri);
        }

        reOpenPost() {
            console.log("RE-OPENING THE POST: "+this.postUri);
            this.settingsOpen = false;
            this.needs__reopen(this.postUri);
        }
    }
    Controller.$inject = serviceDependencies;
    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        template: template,
        scope: {
            selection: "=",
        }
    }
}

export default angular.module('won.owner.components.needTitleBar', [])
    .directive('wonOwnerTitleBar', genComponentConf)
    .name;
