;

import angular from 'angular';
import squareImageModule from '../components/square-image.js';
import {
    attach,
    decodeUriComponentProperly,
    getIn,
} from '../utils.js';
import {
    connect2Redux,
} from '../won-utils.js';
import won from '../won-es6.js';
import { labels } from '../won-label-utils.js';
import {
    selectOpenPostUri,
    selectAllMessagesByNeedUriAndConnected,
} from '../selectors.js';
import { actionCreators }  from '../actions/actions.js';

const serviceDependencies = ['$ngRedux', '$scope'];
function genComponentConf() {
    let template = `
        <nav class="need-tab-bar" ng-cloak ng-show="{{true}}">
            <div class="ntb__inner">


                <div class="ntb__inner__left">
                    <a ng-click="self.router__stateGoResetParams('overviewPosts')" class="clickable">
                        <svg style="--local-primary:var(--won-primary-color);"
                            class="ntb__icon">
                                <use href="#ico36_backarrow"></use>
                        </svg>
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
                            <div class="ntb__titles__type">{{self.labels.type[self.post.get('type')]}}{{self.post.get('matchingContexts')? ' in '+ self.post.get('matchingContexts').join(', ') : ' (no matching context specified)' }}</div>
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
                            <li ng-class="{'ntb__tabs__selected' : self.selectedTab === 'Info'}"
                                class="clickable">
                                <a ng-click="self.router__stateGoAbs('post', {postUri: self.postUri})"
                                    class="clickable">
                                    Post Info
                                </a>
                            </li>
                            <li ng-class="{'ntb__tabs__selected' : self.selectedTab === self.WON.Connected}"
                                class="clickable">
                                <a ng-click="self.router__stateGoAbs('post', {connectionType: self.WON.Connected, postUri: self.postUri})"
                                    ng-class="{'disabled' : (!self.hasConnections) || !self.isActive}">
                                    Chats
                                    <span class="ntb__tabs__unread">{{ self.unreadConnectionsCount }}</span>
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
                const messages = selectAllMessagesByNeedUriAndConnected(state, postUri);
                const unreadConnectionCount = connections && connections.filter(conn => conn.get("state") !== won.WON.Connected && conn.get('newConnection')).size;
                const unreadMessagesCount = messages && messages.filter(msg => msg.get('newMessage') && !msg.get("connectMessage")).size;
                const unreadConnectionsCount = unreadConnectionCount + unreadMessagesCount;

                return {
                    selectedTab: decodeUriComponentProperly(getIn(state, ['router', 'currentParams', 'connectionType'])) || 'Info',
                    WON: won.WON,
                    postUri: postUri,
                    post: post,
                    hasConnections: connections && connections.size > 0,
                    unreadConnectionsCount: unreadConnectionsCount > 0 ? unreadConnectionsCount : undefined,
                    isActive: post && post.get('state') === won.WON.ActiveCompacted,
                };
            };

            connect2Redux(selectFromState, actionCreators, [], this);
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
