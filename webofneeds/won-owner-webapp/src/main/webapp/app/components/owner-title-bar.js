;

import angular from 'angular';
import squareImageModule from '../components/square-image';
import { attach,mapToMatches } from '../utils';
import won from '../won-es6';
import { labels } from '../won-label-utils';
import { selectUnreadEventsByNeedAndType, selectAllByConnections } from '../selectors';
import { actionCreators }  from '../actions/actions';

const serviceDependencies = ['$q', '$ngRedux', '$scope'];
function genComponentConf() {
    let template = `
        <nav class="need-tab-bar" ng-cloak ng-show="{{true}}">
            <div class="ntb__inner">
                <div class="ntb__inner__left">
                    <a ui-sref="overviewPosts">
                        <img src="generated/icon-sprite.svg#ico36_backarrow" class="ntb__icon">
                    </a>
                    <won-square-image src="self.post.get('titleImgSrc')" title="self.post.get('title')"></won-square-image>
                    <div class="ntb__inner__left__titles">
                        <h1 class="ntb__title">{{self.post.get('title')}}</h1>
                        <div class="ntb__inner__left__titles__type">{{self.labels.type[self.post.get('basicNeedType')]}}</div>
                    </div>
                </div>
                <div class="ntb__inner__right">
                    <img class="ntb__icon clickable" src="generated/icon-sprite.svg#ico_settings" ng-show="!self.settingsOpen" ng-click="self.settingsOpen = true" ng-mouseenter="self.settingsOpen = true">
                    <button class="won-button--filled thin red" ng-show="self.isActive && self.settingsOpen" ng-mouseleave="self.settingsOpen=false" ng-click="self.closePost()">Close Post</button>
                    <button class="won-button--filled thin red" ng-show="!self.isActive && self.settingsOpen" ng-mouseleave="self.settingsOpen=false" ng-click="self.reOpenPost()">Reopen Post</button>
                    <ul class="ntb__tabs">
                        <li ng-class="{'ntb__tabs__selected' : self.selectedTab === 'Info'}">
                            <a ui-sref="post({connectionType: null, openConversation: null, connectionUri: null, postUri: self.postUri})">
                                Post Info
                            </a>
                        </li>
                        <li ng-class="{'ntb__tabs__selected' : self.selectedTab === self.WON.Connected}">

                            <a ui-sref="post({connectionType: self.WON.Connected, openConversation: null, connectionUri: null, postUri: self.postUri})"
                                ng-class="{'disabled' : !self.hasMessages}">
                                Messages
                                <span class="ntb__tabs__unread">{{ self.unreadMessages }}</span>
                            </a>
                        </li>
                        <li ng-class="{'ntb__tabs__selected' : self.selectedTab === self.WON.Suggested}">
                            <a ui-sref="post({connectionType: self.WON.Suggested, openConversation: null, connectionUri: null, postUri: self.postUri})"
                                ng-class="{'disabled' : !self.hasMatches}">
                                Matches
                                <span class="ntb__tabs__unread">{{ self.unreadMatches }}</span>
                            </a>
                        </li>
                        <li ng-class="{'ntb__tabs__selected' : self.selectedTab === self.WON.RequestReceived}">
                            <a ui-sref="post({connectionType: self.WON.RequestReceived, openConversation: null, connectionUri: null, postUri: self.postUri})"
                                ng-class="{'disabled' : !self.hasIncomingRequests}">
                                Requests
                                <span class="ntb__tabs__unread">{{ self.unreadIncomingRequests }}</span>
                            </a>
                        </li>
                        <li ng-class="{'ntb__tabs__selected' : self.selectedTab === self.WON.RequestSent}">
                            <a ui-sref="post({connectionType: self.WON.RequestSent, openConversation: null, connectionUri: null, postUri: self.postUri})"
                                ng-class="{'disabled' : !self.hasSentRequests}">
                                Sent Requests
                                <span class="ntb__tabs__unread">{{ self.unreadSentRequests }}</span>
                            </a>
                        </li>
                    </ul>
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
                const unreadCounts = selectUnreadEventsByNeedAndType(state);
                const connectionsDeprecated = selectAllByConnections(state).toJS(); //TODO plz don't do `.toJS()`. every time an ng-binding somewhere cries.

                const encodedPostUri = state.getIn(['router', 'currentParams', 'postUri']) ||
                    state.getIn(['router', 'currentParams', 'myUri']) ; // TODO old parameter
                const postUri = decodeURIComponent(encodedPostUri);

                const encodedConnectionType = state.getIn(['router', 'currentParams', 'connectionType']);
                const connectionTypeInParams = encodedConnectionType ? decodeURIComponent(encodedConnectionType) : undefined;

                return {
                    selectedTab: connectionTypeInParams || 'Info',
                    WON: won.WON,
                    postUri: postUri,
                    post: state.getIn(['needs','ownNeeds', postUri]),
                    hasIncomingRequests: state.getIn(['connections'])
                        .filter(conn =>
                            conn.get('hasConnectionState') === won.WON.RequestReceived
                            && conn.get('belongsToNeed') === postUri
                        ).size > 0,
                    hasSentRequests: Object.keys(connectionsDeprecated) //TODO immutable maps have a `.filter(...)` https://facebook.github.io/immutable-js/docs/
                        .map(key => connectionsDeprecated[key])
                        .filter(conn=>{
                            if(conn.connection.hasConnectionState===won.WON.RequestSent && conn.ownNeed.uri === postUri){
                                return true
                            }
                        }).length > 0,
                    hasMatches: Object.keys(connectionsDeprecated) //TODO immutable maps have a `.filter(...)` https://facebook.github.io/immutable-js/docs/
                        .map(key => connectionsDeprecated[key])
                        .filter(conn=>{
                            if(conn.connection.hasConnectionState===won.WON.Suggested && conn.ownNeed.uri === postUri){
                                return true
                            }
                        }).length > 0,
                    hasMessages: Object.keys(connectionsDeprecated) //TODO immutable maps have a `.filter(...)` https://facebook.github.io/immutable-js/docs/
                        .map(key => connectionsDeprecated[key])
                        .filter(conn=>{
                            return conn.connection.hasConnectionState===won.WON.Connected && conn.ownNeed.uri === postUri
                        }).length > 0,
                    unreadMessages: unreadCounts.getIn([postUri, won.WON.Connected]), //TODO: NOT REALLY THE MESSAGE COUNT ONLY THE CONVERSATION COUNT
                    unreadIncomingRequests: unreadCounts.getIn([postUri, won.WON.RequestReceived]),
                    unreadSentRequests: unreadCounts.getIn([postUri, won.WON.RequestSent]),
                    unreadMatches: unreadCounts.getIn([postUri, won.WON.Suggested]),
                    isActive: state.getIn(['needs','ownNeeds', postUri, 'state']) === won.WON.Active
                };
            };

            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);
        }

        closePost() {
            console.log("CLOSING THE POST: "+this.post.get("uri"));
            this.needs__close(this.post.get("uri"));
        }

        reOpenPost() {
            console.log("RE-OPENING THE POST: "+this.post.get("uri"));
            this.needs__reopen(this.post.get("uri"));
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
